package com.shuo.travel.domain

enum class Language(val tag: String, val nativeName: String) {
    FRENCH("fr", "Français"), ENGLISH("en", "English"), SPANISH("es", "Español"), ITALIAN("it", "Italiano");
    companion object { fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag } ?: FRENCH }
}

fun Phrase.translation(language: Language): String = when (language) {
    Language.FRENCH -> french
    Language.ENGLISH -> english?.takeIf { it.isNotBlank() } ?: french
    Language.SPANISH -> spanish?.takeIf { it.isNotBlank() } ?: french
    Language.ITALIAN -> italian?.takeIf { it.isNotBlank() } ?: french
}

private val categoryNames = mapOf(
    "transport" to listOf("Transports", "Transport", "Transporte", "Trasporti"),
    "restaurant" to listOf("Restaurants", "Restaurants", "Restaurantes", "Ristoranti"),
    "hotel" to listOf("Hôtels", "Hotels", "Hoteles", "Hotel"),
    "shopping" to listOf("Achats", "Shopping", "Compras", "Acquisti"),
    "directions" to listOf("Itinéraires", "Directions", "Direcciones", "Indicazioni"),
    "emergency" to listOf("Urgences", "Emergencies", "Urgencias", "Emergenze"),
    "communication" to listOf("Communication", "Communication", "Comunicación", "Comunicazione"),
    "payment" to listOf("Paiements", "Payments", "Pagos", "Pagamenti"),
    "tourism" to listOf("Tourisme", "Sightseeing", "Turismo", "Turismo"),
    "other" to listOf("Divers", "Everyday", "Vida diaria", "Quotidiano"),
)

fun Category.localizedName(language: Language): String {
    val names = categoryNames[id] ?: return name
    // User-authored category names remain exactly as entered.
    return if (name == names[0]) names[language.ordinal] else name
}
