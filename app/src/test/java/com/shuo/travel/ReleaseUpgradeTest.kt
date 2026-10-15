package com.shuo.travel

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shuo.travel.data.*
import com.shuo.travel.domain.*
import com.shuo.travel.ui.inLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ReleaseUpgradeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
    private val current get() = asset("starter-library.json")
    private val legacy get() = asset("starter-library-v1.json")

    @Test fun upgradePreservesEditsDeletionsFavoritesHistoryAndCustomCategories() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ShuoDatabase::class.java).build()
        try {
            val repo = LibraryRepository(db) { 2000L }
            repo.seedOnce(legacy)
            repo.deletePhrase("thanks")
            repo.savePhrase(db.libraryDao().phrase("no-spice")!!.content.copy(french = "Mon texte personnel"))
            repo.toggleFavorite("hello")
            repo.markUsed("hello")
            repo.saveCategory(db.libraryDao().categories().first { it.content.id == "hotel" }.content.copy(name = "Mes hôtels"))
            repo.savePhrase(Phrase("custom", "好", "Hǎo", "Bien", categoryId = "hotel"))
            repo.seedBundled({ current }, { legacy })
            assertEquals(313, db.libraryDao().phrases().size) // 310 + 3 unmatched legacy + 1 custom - 1 deletion
            assertNull(db.libraryDao().phrase("thanks"))
            assertEquals("Mon texte personnel", db.libraryDao().phrase("no-spice")!!.content.french)
            val hello = db.libraryDao().phrase("hello")!!.content
            assertEquals("你好。", hello.chinese)
            assertEquals("Hola.", hello.spanish)
            assertTrue(hello.favorite)
            assertEquals(2000L, hello.lastUsedAt)
            assertEquals("Un bicchiere d'acqua, per favore.", db.libraryDao().phrase("water")!!.content.italian)
            assertEquals("Mes hôtels", db.libraryDao().categories().first { it.content.id == "hotel" }.content.name)
            repo.seedBundled({ error("Warm startup must not read assets") }, { error("No legacy read") })
            assertEquals(313, db.libraryDao().phrases().size)
            repo.deletePhrase("SHUO-0307")
            repo.seedBundled({ current }, { legacy })
            assertNull(db.libraryDao().phrase("SHUO-0307"))
        } finally { db.close() }
    }

    @Test fun realSchemaOneMigratesWithoutDataLoss() = runBlocking {
        val name = "v1-upgrade-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = JSONObject(javaClass.classLoader!!.getResource("schema-v1.json")!!.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { raw ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indexes = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indexes.length()) raw.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            raw.execSQL("INSERT INTO categories VALUES ('personal','Personnel','#3269D8','#85AFFF','wallet',0)")
            raw.execSQL("INSERT INTO phrases (id,chinese,pinyin,french,english,keywords,categoryId,favorite,createdAt,modifiedAt,lastUsedAt,builtIn) VALUES ('mine','好','Hǎo','Bien',NULL,'[]','personal',1,100,100,200,0)")
            raw.version = 1
        }
        val upgraded = Room.databaseBuilder(context, ShuoDatabase::class.java, name).addMigrations(ShuoDatabase.MIGRATION_1_2).build()
        try {
            val card = upgraded.libraryDao().phrase("mine")!!.content
            assertEquals("Bien", card.french)
            assertTrue(card.favorite)
            assertEquals(200L, card.lastUsedAt)
            assertNull(card.spanish)
            LibraryRepository(upgraded).seedBundled({ current }, { legacy })
            assertEquals(311, upgraded.libraryDao().phrases().size)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }

    @Test fun frenchDefaultAndSavedLanguageAreIndependentOfDeviceLocale() = runBlocking {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            assertEquals(Language.FRENCH, Preferences().language)
            assertEquals(Language.FRENCH, Language.fromTag("unsupported"))
            val prefs = PreferencesRepository(context)
            for (language in Language.entries) {
                prefs.language(language)
                assertEquals(language, PreferencesRepository(context).preferences.first().language)
                val localized = context.inLanguage(language)
                assertEquals(listOf("Bonjour, la Chine !", "Hello, China!", "¡Hola, China!", "Ciao, Cina!")[language.ordinal], localized.getString(R.string.greeting))
            }
            prefs.language(Language.FRENCH)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun translatedCategoriesAndPersonalFallbackPreserveUserData() {
        val category = Category("hotel", "Hôtels", "#000000", icon = "bed-double", order = 0)
        assertEquals("Hotels", category.localizedName(Language.ENGLISH))
        assertEquals("Mes hôtels", category.copy(name = "Mes hôtels").localizedName(Language.ENGLISH))
        val phrase = Phrase("mine", "好", "Hǎo", "Bien", categoryId = "hotel")
        assertEquals("Bien", phrase.translation(Language.ITALIAN))
    }

    @Test fun multilingualIndexFindsAccentsCompactPinyinAndLiveEdits() {
        val library = LibraryCodec.decode(current)
        assertEquals(310, library.phrases.size)
        assertEquals(10, library.categories.size)
        assertTrue(library.phrases.all { !it.english.isNullOrBlank() && !it.spanish.isNullOrBlank() && !it.italian.isNullOrBlank() })
        val index = SearchEngine.index(library.phrases, library.categories, Language.ITALIAN)
        for (query in listOf("toilettes", "toilets", "banos", "bagni", "xishoujian", "洗手间")) {
            assertTrue(query, index.search(query).any { it.id == "toilets" })
        }
        assertTrue(index.search("zhifubao").any { it.id == "SHUO-0205" })
        assertTrue(index.search("allergico arachidi", "restaurant").any { it.id == "allergy" })
        assertTrue(index.search("allergico arachidi", "hotel").isEmpty())
        assertTrue(index.search("zzzz-no-match").isEmpty())
        val edited = library.phrases.map { if (it.id == "hello") it.copy(italian = "Salve viaggiatore") else it }
        assertEquals("hello", SearchEngine.index(edited, library.categories).search("Salve viaggiatore").single().id)
        val queries = listOf("", "metro", "sin chile", "hotel", "banos", "zhifubao", "allergico", "wi fi", "洗手间", "zzzzz")
        repeat(20) { queries.forEach { index.search(it) } }
        val samples = List(100) { i -> val start = System.nanoTime(); index.search(queries[i % queries.size]); (System.nanoTime() - start) / 1_000_000.0 }.sorted()
        println("310-card indexed search: median=${samples[50]} ms, p95=${samples[95]} ms (JVM host)")
    }
}
