package com.shuo.travel.data

import androidx.room.withTransaction
import com.shuo.travel.domain.*
import kotlinx.coroutines.flow.map

data class ImportResult(val added: Int, val skipped: Int)

class LibraryRepository(private val db: ShuoDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.libraryDao()
    val phrases = dao.observePhrases().map { rows -> rows.map { it.content } }
    val categories = dao.observeCategories().map { rows -> rows.map { it.content } }

    // Read/parse bundled JSON only when a library update is actually needed.
    suspend fun seedBundled(loadCurrent: () -> String, loadLegacy: () -> String) {
        if (dao.metadata("seed-v2") != null) return
        val library = LibraryCodec.decode(loadCurrent())
        val legacy = if (dao.metadata("seed-v1") != null) LibraryCodec.decode(loadLegacy()) else null
        installLibrary(library, legacy)
    }

    suspend fun seedOnce(text: String) = installLibrary(LibraryCodec.decode(text), null)

    private suspend fun installLibrary(library: Library, legacy: Library?) = db.withTransaction {
        val key = if (library.version == 1) "seed-v1" else "seed-v2"
        if (dao.metadata(key) != null) return@withTransaction
        dao.insertCategories(library.categories.map(::CategoryEntity))
        val existing = dao.phrases().associate { it.content.id to it.content }
        val previous = legacy?.phrases?.associateBy { it.id }.orEmpty()
        val now = clock()
        val currentIds = library.phrases.map { it.id }.toSet()
        val retainedExamples = previous.values.filter { it.id !in currentIds }.map { old ->
            when (old.id) {
                "vegetarian" -> old.copy(spanish = "Soy vegetariano / vegetariana.", italian = "Sono vegetariano / vegetariana.")
                "ticket" -> old.copy(spanish = "Un billete, por favor.", italian = "Un biglietto, per favore.")
                "water" -> old.copy(spanish = "Un vaso de agua, por favor.", italian = "Un bicchiere d'acqua, per favore.")
                else -> old
            }
        }
        (library.phrases + retainedExamples).forEach { incoming ->
            val local = existing[incoming.id]
            val old = previous[incoming.id]
            when {
                // A missing v1 card was deliberately deleted; do not resurrect it.
                local == null && old != null -> Unit
                local == null -> dao.insertPhrases(listOf(PhraseEntity(incoming.copy(builtIn = true, createdAt = now, modifiedAt = now))))
                old != null && local.builtIn && sameContent(local, old) -> dao.savePhrase(PhraseEntity(incoming.copy(
                    builtIn = true, favorite = local.favorite, createdAt = local.createdAt,
                    modifiedAt = maxOf(now, local.modifiedAt), lastUsedAt = local.lastUsedAt)))
                // Custom cards and edited built-ins retain all their fields.
                else -> Unit
            }
        }
        dao.insertMetadata(Metadata(key, "complete"))
    }

    private fun sameContent(a: Phrase, b: Phrase) =
        a.chinese == b.chinese && a.pinyin == b.pinyin && a.french == b.french &&
        a.english == b.english && a.spanish == b.spanish && a.italian == b.italian &&
        a.categoryId == b.categoryId && a.keywords == b.keywords

    suspend fun savePhrase(phrase: Phrase) {
        db.withTransaction {
            val existing = dao.phrase(phrase.id)?.content
            val now = clock()
            val value = phrase.copy(chinese = phrase.chinese.trim(), pinyin = phrase.pinyin.trim(), french = phrase.french.trim(),
                createdAt = existing?.createdAt ?: now, modifiedAt = maxOf(now, existing?.createdAt ?: 0),
                builtIn = existing?.builtIn ?: false, favorite = existing?.favorite ?: phrase.favorite,
                lastUsedAt = existing?.lastUsedAt)
            LibraryCodec.validatePhrase(value)
            dao.savePhrase(PhraseEntity(value))
        }
    }
    suspend fun deletePhrase(id: String) = dao.deletePhrase(id)
    suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id, clock())
    suspend fun markUsed(id: String) = dao.markUsed(id, clock())
    suspend fun saveCategory(category: Category) {
        LibraryCodec.validateCategory(category)
        dao.saveCategory(CategoryEntity(category.copy(name = category.name.trim())))
    }
    suspend fun deleteCategory(id: String) {
        db.withTransaction {
            require(dao.phrases().none { it.content.categoryId == id }) { "Déplacez ou supprimez les cartes de cette catégorie avant de la supprimer." }
            require(dao.categories().size > 1) { "Conservez au moins une catégorie." }
            dao.deleteCategory(id)
        }
    }
    suspend fun reorder(categories: List<Category>) = db.withTransaction {
        categories.forEachIndexed { index, category -> dao.saveCategory(CategoryEntity(category.copy(order = index))) }
    }
    suspend fun exportJson(): String = db.withTransaction {
        LibraryCodec.encode(Library(categories = dao.categories().map { it.content }, phrases = dao.phrases().map { it.content }))
    }
    suspend fun importJson(text: String): ImportResult {
        val library = LibraryCodec.decode(text)
        return db.withTransaction {
            dao.insertCategories(library.categories.map(::CategoryEntity))
            val inserted = dao.insertPhrases(library.phrases.map { PhraseEntity(it.copy(builtIn = false)) })
            ImportResult(inserted.count { it != -1L }, inserted.count { it == -1L })
        }
    }
}
