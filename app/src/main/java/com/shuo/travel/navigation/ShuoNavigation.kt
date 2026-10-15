package com.shuo.travel.navigation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.shuo.travel.ui.*
import com.shuo.travel.ui.components.*
import com.shuo.travel.ui.screens.*

@Composable fun ShuoNavigation(model: LibraryViewModel, state: UiState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    val back: () -> Unit = { nav.popBackStack(); Unit }
    val open: (com.shuo.travel.domain.Phrase) -> Unit = { nav.navigate("present/${it.id}") }
    val add = { nav.navigate("add") }
    var dirtyEdit by remember { mutableStateOf(false) }
    var pendingTab by remember { mutableStateOf<String?>(null) }
    val switchTab: (String) -> Unit = { target ->
        nav.navigate(target) { popUpTo("home"); launchSingleTop = true }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!route.startsWith("present") && state.ready) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).heightIn(min = 80.dp).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(Triple("home", "Accueil", "house"), Triple("favorites", "Favoris", "star"), Triple("settings", "Catégories", "layout-grid")).forEach { (target, title, icon) ->
                        val selected = route == target || (target == "home" && route !in listOf("favorites", "settings"))
                        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(Modifier.weight(1f).clip(CircleShape).testTag("nav-$target").clickable(role = Role.Tab) {
                            if (dirtyEdit) pendingTab = target else switchTab(target)
                        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(64.dp, 32.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent), contentAlignment = Alignment.Center) { Symbol(icon, size = 23, tint = color) }
                            Label(title, 12, color)
                        }
                    }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.startupError != null -> Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                    Text(state.startupError); Button(model::initialize) { Text("Réessayer") }
                }
                !state.ready -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> NavHost(nav, "home") {
                    composable("home") { HomeScreen(state, add, { nav.navigate("search") }, { nav.navigate("search?category=${it.id}") }, { nav.navigate("favorites") }, { nav.navigate("history") }, open) }
                    composable("search?category={category}", arguments = listOf(navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null })) {
                        SearchScreen(state, it.arguments?.getString("category"), false, back, open)
                    }
                    composable("history") { SearchScreen(state, null, true, back, open) }
                    composable("favorites") { FavoritesScreen(state, add, { nav.navigate("history") }, open) }
                    composable("settings") { SettingsScreen(state, model) { nav.navigate("search?category=${it.id}") } }
                    composable("add") { EditScreen(null, state, model, back) { dirtyEdit = it } }
                    composable("edit/{id}") {
                        val id = it.arguments?.getString("id")
                        val phrase = state.phrases.find { p -> p.id == id }
                        if (phrase != null) EditScreen(phrase, state, model, back) { dirtyEdit = it }
                        else EmptyState("Cette carte n’existe plus.")
                    }
                    composable("present/{id}") {
                        val phrase = state.phrases.find { p -> p.id == it.arguments?.getString("id") }
                        if (phrase != null) PresentationScreen(phrase, state, model, back, { nav.navigate("edit/${phrase.id}") }) {
                            val next = state.phrases[(state.phrases.indexOf(phrase) + 1) % state.phrases.size]
                            nav.navigate("present/${next.id}") { popUpTo(it.destination.id) { inclusive = true } }
                        } else Column(Modifier.padding(20.dp)) {
                            EmptyState("Cette carte n’existe plus.")
                            TextButton(back) { Text("Retour à la bibliothèque") }
                        }
                    }
                }
            }
        }
    }
    if (pendingTab != null) AlertDialog(onDismissRequest = { pendingTab = null },
        title = { Text("Quitter sans enregistrer ?") }, text = { Text("Vos modifications ne seront pas conservées.") },
        confirmButton = { TextButton({ val target = pendingTab!!; pendingTab = null; switchTab(target) }) { Text("Quitter") } },
        dismissButton = { TextButton({ pendingTab = null }) { Text("Continuer") } })
}
