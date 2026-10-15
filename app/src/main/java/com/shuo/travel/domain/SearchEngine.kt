package com.shuo.travel.domain

import java.text.Normalizer
import java.util.Locale

object SearchEngine {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace("œ", "oe").replace("æ", "ae")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
        .replace(Regex("\\s+"), " ")

    fun search(phrases: List<Phrase>, categories: List<Category>, query: String,
               categoryId: String? = null, favoritesOnly: Boolean = false): List<Phrase> {
        val normalized = normalize(query)
        val tokens = normalized.split(' ').filter { it.isNotEmpty() }
        val categoryNames = categories.associate { it.id to normalize(it.name) }
        return phrases.asSequence()
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { !favoritesOnly || it.favorite }
            .mapNotNull { phrase ->
                val fields = listOf(phrase.chinese to 40, phrase.french to 30,
                    phrase.pinyin to 20, phrase.english.orEmpty() to 10,
                    phrase.keywords.joinToString(" ") to 0)
                    .map { normalize(it.first) to it.second } + (categoryNames[phrase.categoryId].orEmpty() to 0)
                if (!tokens.all { token -> fields.any { token in it.first } }) null
                else phrase to if (normalized.isEmpty()) 0 else fields.maxOf { (field, weight) ->
                    when {
                        field == normalized -> 300 + weight
                        field.startsWith(normalized) -> 200 + weight
                        normalized in field -> 100 + weight
                        else -> 0
                    }
                }
            }.sortedWith(compareByDescending<Pair<Phrase, Int>> { it.second }
                .thenBy { normalize(it.first.french) }.thenBy { it.first.id })
            .map { it.first }.toList()
    }
}
