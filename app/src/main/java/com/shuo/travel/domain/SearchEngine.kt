package com.shuo.travel.domain

import java.text.Normalizer
import java.util.Locale

object SearchEngine {
    private val marks = Regex("\\p{M}+")
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val spaces = Regex("\\s+")
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae")
        .replace(punctuation, " ").trim().replace(spaces, " ")

    private data class Entry(val phrase: Phrase, val fields: List<Pair<String, Int>>, val sortKey: String)
    class Index internal constructor(phrases: List<Phrase>, categories: List<Category>, language: Language) {
        private val categoryNames = categories.associate { category ->
            category.id to Language.entries.joinToString(" ") { category.localizedName(it) }.let(::normalize)
        }
        private val entries = phrases.map { p ->
            val fields = listOf(p.chinese to 40, p.translation(language) to 35, p.french to 30,
                p.pinyin to 20, p.english.orEmpty() to 10, p.spanish.orEmpty() to 10,
                p.italian.orEmpty() to 10, p.keywords.joinToString(" ") to 0)
                .map { normalize(it.first) to it.second }
            // A compact pinyin field also matches "xishoujian" and "zhifubao".
            Entry(p, fields + (normalize(p.pinyin).replace(" ", "") to 15) +
                (categoryNames[p.categoryId].orEmpty() to 0), normalize(p.translation(language)))
        }.sortedWith(compareBy<Entry> { it.sortKey }.thenBy { it.phrase.id })

        fun search(query: String, categoryId: String? = null, favoritesOnly: Boolean = false, history: Boolean = false): List<Phrase> {
            val normalized = normalize(query)
            val tokens = normalized.split(' ').filter(String::isNotEmpty)
            val candidates = entries.asSequence().filter {
                (categoryId == null || it.phrase.categoryId == categoryId) &&
                (!favoritesOnly || it.phrase.favorite) && (!history || it.phrase.lastUsedAt != null)
            }
            val matches = if (tokens.isEmpty()) candidates.map { it.phrase }.toList() else candidates.mapNotNull { entry ->
                if (!tokens.all { token -> entry.fields.any { token in it.first } }) null
                else entry to entry.fields.maxOf { (field, weight) -> when {
                    field == normalized -> 300 + weight
                    field.startsWith(normalized) -> 200 + weight
                    normalized in field -> 100 + weight
                    else -> 0
                } }
            }.sortedByDescending { it.second }.map { it.first.phrase }.toList()
            return if (history) matches.sortedByDescending { it.lastUsedAt } else matches
        }
    }
    fun index(phrases: List<Phrase>, categories: List<Category>, language: Language = Language.FRENCH) = Index(phrases, categories, language)
    fun search(phrases: List<Phrase>, categories: List<Category>, query: String,
               categoryId: String? = null, favoritesOnly: Boolean = false): List<Phrase> =
        index(phrases, categories).search(query, categoryId, favoritesOnly)
}
