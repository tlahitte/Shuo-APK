package com.shuo.travel.ui.screens

import com.shuo.travel.R
import androidx.compose.ui.res.stringResource
import com.shuo.travel.ui.countText
import com.shuo.travel.ui.LocalLanguage
import com.shuo.travel.domain.localizedName
import com.shuo.travel.domain.translation

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        item { Header(stringResource(R.string.greeting), stringResource(R.string.tagline), action = { Action("plus", stringResource(R.string.add_card), true, onClick = add) }) }
        item {
            Surface(onClick = search, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Symbol("search", size = 22, tint = MaterialTheme.colorScheme.primary)
                    Label(stringResource(R.string.search_hint), 15, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Section(stringResource(R.string.by_situation)) }
        item { CategoryGrid(state.categories, onClick = category) }
        item { Section(stringResource(R.string.your_favorites), stringResource(R.string.see_all_count, state.phrases.count { it.favorite }), favorites) }
        item { if (favorite == null) EmptyState(stringResource(R.string.favorites_empty)) else Card(favorite, state, open = open) }
        item { Section(stringResource(R.string.last_used), stringResource(R.string.history), history) }
        item { if (recent == null) EmptyState(stringResource(R.string.history_hint)) else Card(recent, state, 126.dp, open) }
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
                    if (value.isEmpty()) Label(stringResource(R.string.search_languages), 15, MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
                if (value.isNotEmpty()) IconButton({ onChange("") }, Modifier.size(24.dp)) { Symbol("x", stringResource(R.string.clear_search), 20) }
            }
        })
}

@Composable fun Filters(categories: List<Category>, selected: String?, select: (String?) -> Unit,
    favorites: Boolean? = null, toggleFavorites: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill(stringResource(R.string.all), selected == null && favorites != true, MaterialTheme.colorScheme.primary) { select(null) }
        categories.forEach { category ->
            FilterPill(category.localizedName(LocalLanguage.current), selected == category.id, categoryColor(category)) { select(if (selected == category.id) null else category.id) }
        }
        if (favorites != null) FilterPill(stringResource(R.string.favorites), favorites, MaterialTheme.colorScheme.primary, toggleFavorites)
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
    val matches by produceState(emptyList<Phrase>(), state.searchIndex, query) {
        value = withContext(Dispatchers.Default) { state.searchIndex.search(query) }
    }
    val results = remember(matches, category, favorites, history) {
        matches.filter { (category == null || it.categoryId == category) && (!favorites || it.favorite) && (!history || it.lastUsedAt != null) }
            .let { if (history) it.sortedByDescending { phrase -> phrase.lastUsedAt } else it }
    }
    val matchingCategories = remember(matches) { matches.map { it.categoryId }.toSet() }
    LazyColumn(Modifier.fillMaxSize().testTag("search-list"), contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header(if (history) stringResource(R.string.history) else stringResource(R.string.search), back = back,
            action = { Action("sliders-horizontal", stringResource(R.string.filters), true) { filters = !filters } }) }
        item { SearchInput(query) { query = it } }
        if (filters) item {
            Filters(state.categories.filter { it.id in matchingCategories || it.id == category }, category, { category = it }, favorites) { favorites = !favorites }
        }
        item { Section(if (query.isNotBlank()) countText(R.plurals.search_count, results.size, query) else countText(R.plurals.cards_count, results.size)) }
        if (results.isEmpty()) item { EmptyState(if (history) stringResource(R.string.history_empty) else stringResource(R.string.search_empty)) }
        items(results, key = { it.id }) { Card(it, state, open = open) }
        item { Label(stringResource(R.string.tap_to_show), 12, MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OfflineFooter() }
    }
}

@Composable fun FavoritesScreen(state: UiState, add: () -> Unit, history: () -> Unit, open: (Phrase) -> Unit) {
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val favorites = state.phrases.filter { it.favorite && (category == null || it.categoryId == category) }
    val recent = state.phrases.filter { it.lastUsedAt != null }.maxByOrNull { it.lastUsedAt!! }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header(stringResource(R.string.your_favorites), countText(R.plurals.favorites_count, state.phrases.count { it.favorite }), action = { Action("plus", stringResource(R.string.add_card), true, onClick = add) }) }
        item { Filters(state.categories.filter { cat -> state.phrases.any { it.favorite && it.categoryId == cat.id } }, category, { category = it }) }
        item { Section(stringResource(R.string.keep_handy)) }
        if (favorites.isEmpty()) item { EmptyState(stringResource(R.string.add_favorite_hint)) }
        items(favorites, key = { it.id }) { Card(it, state, open = open) }
        item { Section(stringResource(R.string.recent), stringResource(R.string.see_all), history) }
        item { if (recent == null) EmptyState(stringResource(R.string.history_empty)) else Card(recent, state, 126.dp, open) }
        item { OfflineFooter() }
    }
}
