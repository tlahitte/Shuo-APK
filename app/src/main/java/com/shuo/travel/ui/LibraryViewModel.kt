package com.shuo.travel.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shuo.travel.ShuoApplication
import com.shuo.travel.data.*
import com.shuo.travel.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException

data class UiState(val phrases: List<Phrase> = emptyList(), val categories: List<Category> = emptyList(),
    val preferences: Preferences = Preferences(), val ready: Boolean = false, val startupError: String? = null)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ShuoApplication
    private val repository = app.library
    private val loading = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()
    val state = combine(repository.phrases, repository.categories, app.preferences.preferences, loading) { p, c, pref, load ->
        UiState(p, c, pref, load.first, load.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init { initialize() }
    fun initialize() { viewModelScope.launch {
        loading.value = false to null
        try {
            withContext(Dispatchers.IO) { repository.seedOnce(app.assets.open("starter-library.json").bufferedReader().use { it.readText() }) }
            loading.value = true to null
        } catch (error: Exception) { if (error is CancellationException) throw error; loading.value = false to "Impossible d’ouvrir la bibliothèque locale." }
    } }
    private fun operation(finished: () -> Unit = {}, block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (error: Exception) {
            if (error is CancellationException) throw error
            messageChannel.send(when (error) {
                is SerializationException -> "Fichier JSON invalide ou incomplet. Aucune carte importée."
                is IllegalArgumentException -> error.message ?: "Données invalides."
                else -> "Opération impossible. Vérifiez le fichier ou l’espace disponible."
            })
        } finally { finished() }
    } }
    fun favorite(id: String) = operation { repository.toggleFavorite(id) }
    fun used(id: String) = operation { repository.markUsed(id) }
    fun save(phrase: Phrase, done: () -> Unit, finished: () -> Unit) = operation(finished) { repository.savePhrase(phrase); done() }
    fun delete(id: String, done: () -> Unit) = operation { repository.deletePhrase(id); done() }
    fun saveCategory(category: Category, done: () -> Unit) = operation { repository.saveCategory(category); done() }
    fun deleteCategory(id: String, done: () -> Unit) = operation { repository.deleteCategory(id); done() }
    fun reorder(categories: List<Category>) = operation { repository.reorder(categories) }
    fun appearance(value: Appearance) = operation { app.preferences.appearance(value) }
    fun scale(value: Float) = operation { app.preferences.scale(value) }
    fun export(uri: Uri) = operation {
        withContext(Dispatchers.IO) {
            val json = repository.exportJson()
            val stream = app.contentResolver.openOutputStream(uri, "wt") ?: error("Document inaccessible")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
        messageChannel.send("Bibliothèque exportée.")
    }
    fun import(uri: Uri) = operation {
        val result = withContext(Dispatchers.IO) {
            val stream = app.contentResolver.openInputStream(uri) ?: error("Document inaccessible")
            val bytes = stream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= LibraryCodec.MAX_BYTES) {
                    val read = input.read(buffer, 0, minOf(buffer.size, LibraryCodec.MAX_BYTES + 1 - output.size()))
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            require(bytes.size <= LibraryCodec.MAX_BYTES) { "Fichier trop volumineux (5 Mo maximum)." }
            repository.importJson(bytes.toString(Charsets.UTF_8))
        }
        messageChannel.send("${result.added} cartes importées · ${result.skipped} cartes existantes conservées.")
    }
}
