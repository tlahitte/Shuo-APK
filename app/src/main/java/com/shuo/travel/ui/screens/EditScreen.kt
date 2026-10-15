package com.shuo.travel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import com.shuo.travel.domain.*
import com.shuo.travel.ui.*
import com.shuo.travel.ui.components.*
import com.shuo.travel.ui.theme.*
import java.util.UUID

@Composable fun EditScreen(existing: Phrase?, state: UiState, model: LibraryViewModel, back: () -> Unit, onDirtyChanged: (Boolean) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var french by rememberSaveable { mutableStateOf(existing?.french.orEmpty()) }
    var chinese by rememberSaveable { mutableStateOf(existing?.chinese.orEmpty()) }
    var pinyin by rememberSaveable { mutableStateOf(existing?.pinyin.orEmpty()) }
    var english by rememberSaveable { mutableStateOf(existing?.english.orEmpty()) }
    var keywords by rememberSaveable { mutableStateOf(existing?.keywords?.joinToString(", ").orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.categoryId ?: state.categories.firstOrNull()?.id) }
    var extras by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val dirty = french != existing?.french.orEmpty() || chinese != existing?.chinese.orEmpty() || pinyin != existing?.pinyin.orEmpty() ||
        english != existing?.english.orEmpty() || keywords != existing?.keywords?.joinToString(", ").orEmpty() ||
        category != (existing?.categoryId ?: state.categories.firstOrNull()?.id)
    val close = { if (dirty) confirmDiscard = true else back() }
    DisposableEffect(dirty) {
        onDirtyChanged(dirty)
        onDispose { onDirtyChanged(false) }
    }
    BackHandler { close() }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Header(if (existing == null) "Nouvelle carte" else "Modifier la carte", back = close,
                action = { Action("x", "Fermer", true, onClick = close) })
            PhraseInput("Français · pour retrouver votre carte", french, "french-input") { french = it }
            PhraseInput("Chinois simplifié · à montrer", chinese, "chinese-input", true) { chinese = it }
            PhraseInput("Pinyin · avec les tons", pinyin, "pinyin-input") { pinyin = it }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Symbol("info", size = 18, tint = MaterialTheme.colorScheme.primary)
                Label("Saisissez le chinois et le pinyin vous-même. Aucune traduction automatique.", 12, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section("Choisir une catégorie")
            CategoryGrid(state.categories, category, true) { category = it.id }
            TextButton({ extras = true }) { Label("Traduction anglaise et mots-clés", 12, MaterialTheme.colorScheme.primary) }
            if (existing != null) TextButton({ confirmDelete = true }) { Label("Supprimer cette carte", 12, MaterialTheme.colorScheme.error) }
        }
        Button(onClick = {
            submitting = true
            model.save(Phrase(id, chinese, pinyin, french, english.ifBlank { null },
                keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }, category!!), done = back, finished = { submitting = false })
        }, enabled = !submitting && french.isNotBlank() && chinese.isNotBlank() && pinyin.isNotBlank() && category != null,
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp).heightIn(min = 52.dp).testTag("save-phrase"), shape = RoundedCornerShape(16.dp)) {
            Symbol("check", tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Enregistrer la carte")
        }
    }
    if (extras) AlertDialog(onDismissRequest = { extras = false }, title = { Text("Champs facultatifs") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(english, { if (it.length <= 2000) english = it }, label = { Text("Anglais") })
            OutlinedTextField(keywords, { if (it.length <= 5000) keywords = it }, label = { Text("Mots-clés séparés par des virgules") })
        }
    }, confirmButton = { TextButton({ extras = false }) { Text("Terminé") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Supprimer cette carte ?") },
        text = { Text("Cette carte sera retirée de votre bibliothèque et de vos favoris.") },
        confirmButton = { TextButton({ model.delete(id, back); confirmDelete = false }) { Text("Supprimer") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Annuler") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Quitter sans enregistrer ?") },
        text = { Text("Vos modifications ne seront pas conservées.") },
        confirmButton = { TextButton(back) { Text("Quitter") } }, dismissButton = { TextButton({ confirmDiscard = false }) { Text("Continuer") } })
}

@Composable private fun PhraseInput(label: String, value: String, tag: String, chinese: Boolean = false, change: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(label, 13, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        OutlinedTextField(value, { if (it.length <= 2000) change(it) }, Modifier.fillMaxWidth().testTag(tag),
            shape = RoundedCornerShape(16.dp), textStyle = TextStyle(fontFamily = if (chinese) ChineseFont else Inter, fontSize = if (chinese) 26.sp else 14.sp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline))
    }
}
