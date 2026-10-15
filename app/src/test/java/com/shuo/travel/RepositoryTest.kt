package com.shuo.travel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shuo.travel.data.*
import com.shuo.travel.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class RepositoryTest {
    private lateinit var context: Context
    private lateinit var db: ShuoDatabase
    private lateinit var repository: LibraryRepository
    private lateinit var seed: String
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ShuoDatabase::class.java).build()
        repository = LibraryRepository(db) { 1000L }
        seed = context.assets.open("starter-library.json").bufferedReader().use { it.readText() }
    }
    @After fun close() { db.close() }
    @Test fun starterLibrarySeedsOnlyOnceIncludingAfterDeletion() = runBlocking {
        repository.seedOnce(seed)
        assertEquals(15, db.libraryDao().phrases().size)
        repository.deletePhrase("no-spice")
        repository.seedOnce(seed)
        assertEquals(14, db.libraryDao().phrases().size)
        assertNull(db.libraryDao().phrase("no-spice"))
    }
    @Test fun seedAndImportNeverOverwriteUserEdits() = runBlocking {
        repository.seedOnce(seed)
        val original = db.libraryDao().phrase("no-spice")!!.content
        repository.savePhrase(original.copy(french = "Ma traduction"))
        repository.seedOnce(seed)
        assertEquals(0, repository.importJson(seed).added)
        val actual = db.libraryDao().phrase("no-spice")!!.content
        assertEquals("Ma traduction", actual.french)
        assertEquals(original.createdAt, actual.createdAt)
        assertTrue(actual.builtIn)
    }
    @Test fun phraseCrudFavoriteAndHistoryPersist() = runBlocking {
        repository.seedOnce(seed)
        val phrase = Phrase("custom", "谢谢", "Xièxie", "Merci", categoryId = "communication")
        repository.savePhrase(phrase)
        repository.toggleFavorite("custom")
        repository.markUsed("custom")
        repository.savePhrase(phrase.copy(french = "Merci beaucoup"))
        val stored = db.libraryDao().phrase("custom")!!.content
        assertEquals("Merci beaucoup", stored.french)
        assertTrue(stored.favorite)
        assertEquals(1000L, stored.lastUsedAt)
        assertFalse(stored.builtIn)
        repository.deletePhrase("custom")
        assertNull(db.libraryDao().phrase("custom"))
    }
    @Test fun categoryCrudAndOrderingPreserveReferences() = runBlocking {
        repository.seedOnce(seed)
        val category = Category("custom", "Personnel", "#3269D8", icon = "wallet", order = 10)
        repository.saveCategory(category)
        repository.saveCategory(category.copy(name = "Mes cartes"))
        repository.savePhrase(Phrase("custom", "你好", "Nǐ hǎo", "Bonjour", categoryId = "custom"))
        try { repository.deleteCategory("custom"); fail("Nonempty category deleted") } catch (_: IllegalArgumentException) { }
        repository.reorder(db.libraryDao().categories().map { it.content }.reversed())
        assertEquals("custom", db.libraryDao().categories().first().content.id)
        repository.deletePhrase("custom")
        repository.deleteCategory("custom")
        assertEquals(10, db.libraryDao().categories().size)
    }
    @Test fun invalidImportIsAtomicAndValidImportIsIdempotent() = runBlocking {
        repository.seedOnce(seed)
        val before = repository.exportJson()
        try { repository.importJson(seed.replace("\"chinese\": \"你好\"", "\"chinese\": \"\"")); fail("Invalid import accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(before, repository.exportJson())
        val exported = LibraryCodec.decode(before)
        val new = exported.copy(phrases = listOf(exported.phrases.first().copy(id = "imported")))
        assertEquals(ImportResult(1, 0), repository.importJson(LibraryCodec.encode(new)))
        assertEquals(ImportResult(0, 1), repository.importJson(LibraryCodec.encode(new)))
    }
    @Test fun fileDatabaseSurvivesCloseAndReopen() = runBlocking {
        val name = "restart-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, ShuoDatabase::class.java, name).build()
        LibraryRepository(first).apply { seedOnce(seed); toggleFavorite("hello"); markUsed("hello") }
        first.close()
        val reopened = Room.databaseBuilder(context, ShuoDatabase::class.java, name).build()
        try {
            LibraryRepository(reopened).seedOnce(seed)
            val actual = reopened.libraryDao().phrase("hello")!!.content
            assertTrue(actual.favorite)
            assertNotNull(actual.lastUsedAt)
            assertEquals(15, reopened.libraryDao().phrases().size)
        } finally { reopened.close(); context.deleteDatabase(name) }
    }
    @Test fun everyExampleIsSearchableAcrossLanguages() = runBlocking {
        repository.seedOnce(seed)
        val library = LibraryCodec.decode(repository.exportJson())
        library.phrases.forEach { p ->
            listOf(p.chinese, p.french, SearchEngine.normalize(p.pinyin)).forEach { query ->
                assertTrue("${p.id}: $query", SearchEngine.search(library.phrases, library.categories, query).any { it.id == p.id })
            }
        }
    }
    @Test fun unsupportedSchemaChangeFailsWithoutErasingData() = runBlocking {
        val name = "migration-safety.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, ShuoDatabase::class.java, name).build()
        LibraryRepository(first).seedOnce(seed)
        first.close()
        android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, 0).use { it.version = 2 }
        val incompatible = Room.databaseBuilder(context, ShuoDatabase::class.java, name).build()
        try {
            try { incompatible.libraryDao().phrases(); fail("Missing migration accepted") } catch (_: IllegalStateException) { }
        } finally { incompatible.close() }
        android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, 0).use { raw ->
            raw.rawQuery("SELECT COUNT(*) FROM phrases", null).use { rows -> rows.moveToFirst(); assertEquals(15, rows.getInt(0)) }
        }
        context.deleteDatabase(name)
        Unit
    }
}
