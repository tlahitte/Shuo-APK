package com.shuo.travel.data

import androidx.room.withTransaction
import com.shuo.travel.domain.*
import kotlinx.coroutines.flow.map

data class ImportResult(val added: Int, val skipped: Int)

class LibraryRepository(private val db: ShuoDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.libraryDao()
    val phrases = dao.observePhrases().map { rows -> rows.map { it.content } }
    val categories = dao.observeCategories().map { rows -> rows.map { it.content } }

    suspend fun seedOnce(text: String) {
        db.withTransaction {
            if (dao.metadata("seed-v1") != null) return@withTransaction
            val library = LibraryCodec.decode(text)
            dao.insertCategories(library.categories.map(::CategoryEntity))
            val now = clock()
            dao.insertPhrases(library.phrases.map { PhraseEntity(it.copy(builtIn = true, createdAt = now, modifiedAt = now)) })
            dao.insertMetadata(Metadata("seed-v1", "complete"))
        }
    }
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
