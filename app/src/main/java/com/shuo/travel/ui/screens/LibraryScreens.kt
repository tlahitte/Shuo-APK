package com.shuo.travel.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import com.shuo.travel.domain.*
import com.shuo.travel.ui.UiState
import com.shuo.travel.ui.components.*
import com.shuo.travel.ui.theme.Inter

@Composable private fun Card(phrase: Phrase, state: UiState, minHeight: Dp = 148.dp, open: (Phrase) -> Unit) {
    state.categories.find { it.id == phrase.categoryId }?.let { category ->
        PhraseCard(phrase, category, Modifier.testTag("phrase-${phrase.id}"), scale = state.preferences.textScale, minHeight = minHeight) { open(phrase) }
    }
}

@Composable fun HomeScreen(state: UiState, add: () -> Unit, search: () -> Unit, category: (Category) -> Unit,
    favorites: () -> Unit, history: () -> Unit, open: (Phrase) -> Unit) {
    val favorite = state.phrases.firstOrNull { it.favorite }
    val recent = state.phrases.filter { it.lastUsedAt != null }.maxByOrNull { it.lastUsedAt!! }
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header("Bonjour, la Chine !", "Vos mots, toujours à portée de main.", action = { Action("plus", "Ajouter une carte", true, onClick = add) }) }
        item {
            Surface(onClick = search, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Symbol("search", size = 22, tint = MaterialTheme.colorScheme.primary)
                    Label("Chercher une phrase en français", 15, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Section("À chaque situation, sa carte") }
        item { CategoryGrid(state.categories, onClick = category) }
        item { Section("Vos favoris", "Tout voir · ${state.phrases.count { it.favorite }}", favorites) }
        item { if (favorite == null) EmptyState("Touchez l’étoile d’une carte pour la retrouver ici.") else Card(favorite, state, open = open) }
        item { Section("Dernière carte utilisée", "Historique", history) }
        item { if (recent == null) EmptyState("Les cartes que vous montrez apparaîtront ici.") else Card(recent, state, 126.dp, open) }
        item { OfflineFooter(state.phrases.size) }
    }
}

@Composable fun SearchInput(value: String, onChange: (String) -> Unit) {
    BasicTextField(value, onChange, Modifier.fillMaxWidth().testTag("search-input"), singleLine = true,
        textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("search", size = 22, tint = MaterialTheme.colorScheme.primary)
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (value.isEmpty()) Label("Français, 中文, pinyin…", 15, MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
                if (value.isNotEmpty()) IconButton({ onChange("") }, Modifier.size(24.dp)) { Symbol("x", "Effacer la recherche", 20) }
            }
        })
}

@Composable fun Filters(categories: List<Category>, selected: String?, select: (String?) -> Unit,
    favorites: Boolean? = null, toggleFavorites: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill("Tout", selected == null && favorites != true, MaterialTheme.colorScheme.primary) { select(null) }
        categories.forEach { category ->
            FilterPill(category.name, selected == category.id, categoryColor(category)) { select(if (selected == category.id) null else category.id) }
        }
        if (favorites != null) FilterPill("Favoris", favorites, MaterialTheme.colorScheme.primary, toggleFavorites)
    }
}

@Composable private fun FilterPill(label: String, selected: Boolean, accent: androidx.compose.ui.graphics.Color, click: () -> Unit) {
    Surface(onClick = click, shape = RoundedCornerShape(100.dp),
        color = if (selected) accent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outline)) {
        Label(label, 12, if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable fun SearchScreen(state: UiState, initialCategory: String?, history: Boolean, back: () -> Unit, open: (Phrase) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(true) }
    val results = remember(state.phrases, state.categories, query, category, favorites, history) {
        SearchEngine.search(state.phrases.filter { !history || it.lastUsedAt != null }, state.categories, query, category, favorites)
            .let { if (history) it.sortedByDescending { phrase -> phrase.lastUsedAt } else it }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("search-list"), contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header(if (history) "Historique" else "Rechercher", back = back,
            action = { Action("sliders-horizontal", "Filtres", true) { filters = !filters } }) }
        item { SearchInput(query) { query = it } }
        if (filters) item {
            val matchingCategories = SearchEngine.search(state.phrases, state.categories, query).map { it.categoryId }.toSet()
            Filters(state.categories.filter { it.id in matchingCategories || it.id == category }, category, { category = it }, favorites) { favorites = !favorites }
        }
        item { Section("${results.size} cartes" + if (query.isNotBlank()) " pour « $query »" else "") }
        if (results.isEmpty()) item { EmptyState(if (history) "Aucune carte montrée pour le moment." else "Aucune carte trouvée. Essayez un autre mot ou une autre catégorie.") }
        items(results, key = { it.id }) { Card(it, state, open = open) }
        item { Label("Touchez une carte pour la montrer en grand.", 12, MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OfflineFooter() }
    }
}

@Composable fun FavoritesScreen(state: UiState, add: () -> Unit, history: () -> Unit, open: (Phrase) -> Unit) {
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val favorites = state.phrases.filter { it.favorite && (category == null || it.categoryId == category) }
    val recent = state.phrases.filter { it.lastUsedAt != null }.maxByOrNull { it.lastUsedAt!! }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header("Vos favoris", "${state.phrases.count { it.favorite }} cartes à retrouver en un geste.", action = { Action("plus", "Ajouter une carte", true, onClick = add) }) }
        item { Filters(state.categories.filter { cat -> state.phrases.any { it.favorite && it.categoryId == cat.id } }, category, { category = it }) }
        item { Section("À garder sous la main") }
        if (favorites.isEmpty()) item { EmptyState("Touchez l’étoile d’une carte pour l’ajouter aux favoris.") }
        items(favorites, key = { it.id }) { Card(it, state, open = open) }
        item { Section("Récemment utilisées", "Tout voir", history) }
        item { if (recent == null) EmptyState("Aucune carte montrée pour le moment.") else Card(recent, state, 126.dp, open) }
        item { OfflineFooter() }
    }
}
