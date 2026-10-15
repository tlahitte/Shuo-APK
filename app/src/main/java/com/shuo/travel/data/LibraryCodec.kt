package com.shuo.travel.data

import com.shuo.travel.domain.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LibraryCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(library: Library): String { validate(library); return json.encodeToString(library) }
    fun decode(text: String): Library {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Fichier trop volumineux (5 Mo maximum)." }
        return json.decodeFromString<Library>(text).also(::validate)
    }
    fun validate(library: Library) {
        require(library.version in 1..2) { "Version de bibliothèque non prise en charge." }
        require(library.categories.size <= 200 && library.phrases.size <= 10_000) { "Bibliothèque trop volumineuse." }
        require(library.categories.map { it.id }.distinct().size == library.categories.size) { "Identifiants de catégorie en double." }
        require(library.phrases.map { it.id }.distinct().size == library.phrases.size) { "Identifiants de carte en double." }
        library.categories.forEach(::validateCategory)
        library.phrases.forEach { phrase ->
            validatePhrase(phrase)
            require(library.categories.any { it.id == phrase.categoryId }) { "Catégorie introuvable : ${phrase.categoryId}" }
        }
    }
    private fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_-]{1,100}"))
    fun validateCategory(category: Category) {
        require(validId(category.id)) { "Identifiant de catégorie invalide." }
        require(category.name.isNotBlank() && category.name.length <= 80) { "Nom de catégorie requis (80 caractères maximum)." }
        require(listOf(category.color, category.darkColor).all { it.matches(Regex("#[0-9a-fA-F]{6}")) }) { "Couleur invalide : utilisez #RRGGBB." }
        require(category.icon in CategoryIcons) { "Icône inconnue." }
        require(category.order in 0..100_000) { "Ordre de catégorie invalide." }
    }
    fun validatePhrase(phrase: Phrase) {
        require(validId(phrase.id) && validId(phrase.categoryId)) { "Identifiant de carte invalide." }
        require(listOf(phrase.chinese, phrase.pinyin, phrase.french).all { it.isNotBlank() && it.length <= 2000 }) { "Renseignez le français, le chinois et le pinyin (2 000 caractères maximum)." }
        require(listOf(phrase.english, phrase.spanish, phrase.italian).all { (it?.length ?: 0) <= 2000 } && phrase.keywords.size <= 100 && phrase.keywords.all { it.length <= 100 }) { "Traduction ou mots-clés trop longs." }
        require(phrase.createdAt >= 0 && phrase.modifiedAt >= phrase.createdAt && (phrase.lastUsedAt ?: 0) >= 0) { "Dates invalides." }
    }
}
