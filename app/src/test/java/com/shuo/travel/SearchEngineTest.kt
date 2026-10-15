package com.shuo.travel

import com.shuo.travel.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SearchEngineTest {
    private val category = Category("restaurant", "Restaurants", "#D56A24", icon = "utensils", order = 0)
    private fun phrase(id: String, french: String, pinyin: String = "Bú yào là", chinese: String = "不要辣") =
        Phrase(id, chinese, pinyin, french, keywords = listOf("piment", "épice"), categoryId = "restaurant")

    @Test fun accentsAndTonesAreNormalized() {
        assertEquals("vegetarien lu oeuf", SearchEngine.normalize("VÉGÉTARIEN Lǜ Œuf"))
        assertEquals("bu yao la", SearchEngine.normalize("Bú yào là"))
        assertEquals("cafe", SearchEngine.normalize("cafe\u0301"))
    }
    @Test fun casingIsIndependentOfDeviceLocale() {
        val previous = Locale.getDefault()
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR")); assertEquals("itineraire", SearchEngine.normalize("ITINÉRAIRE")) }
        finally { Locale.setDefault(previous) }
    }
    @Test fun matchesFrenchChinesePinyinKeywordsAndCategory() {
        val p = phrase("one", "Pas de piment, s’il vous plaît.")
        for (query in listOf("PIMENT", "不要", "bu yao", "yao", "epice", "restaurant", "s il")) {
            assertEquals(query, listOf(p), SearchEngine.search(listOf(p), listOf(category), query))
        }
    }
    @Test fun ranksExactThenPrefixThenSubstring() {
        val phrases = listOf(phrase("weak", "Un menu"), phrase("prefix", "Menu enfant"), phrase("exact", "Menu"))
        assertEquals(listOf("exact", "prefix", "weak"), SearchEngine.search(phrases, listOf(category), "menu").map { it.id })
    }
    @Test fun allPartialTokensMustMatch() {
        val p = phrase("one", "Station de métro")
        assertEquals(listOf(p), SearchEngine.search(listOf(p), listOf(category), "sta metr"))
        assertTrue(SearchEngine.search(listOf(p), listOf(category), "sta vélo").isEmpty())
    }
    @Test fun categoryAndFavoriteFiltersComposeWithSearch() {
        val favorite = phrase("fav", "Piment").copy(favorite = true)
        val plain = phrase("plain", "Piment")
        val other = phrase("other", "Piment").copy(favorite = true, categoryId = "other")
        assertEquals(listOf(favorite), SearchEngine.search(listOf(plain, other, favorite), listOf(category), "pim", "restaurant", true))
    }
    @Test fun emptyQueryAndUserCreatedPhraseWork() {
        val p = phrase("user", "Nouvelle expression", "xīn", "新")
        assertEquals(listOf(p), SearchEngine.search(listOf(p), listOf(category), ""))
        assertEquals(listOf(p), SearchEngine.search(listOf(p), listOf(category), "xin"))
    }
}
