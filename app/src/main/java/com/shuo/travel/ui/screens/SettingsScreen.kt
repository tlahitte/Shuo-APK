package com.shuo.travel.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shuo.travel.BuildConfig
import com.shuo.travel.data.Appearance
import com.shuo.travel.domain.*
import com.shuo.travel.ui.*
import com.shuo.travel.ui.components.*
import java.util.UUID

@Composable fun SettingsScreen(state: UiState, model: LibraryViewModel, browse: (Category) -> Unit) {
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(model::export) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(model::import) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Header("Votre bibliothèque", "${state.phrases.size} cartes · ${state.categories.size} catégories", action = {
            Action("plus", "Créer une catégorie", true) { creating = true }
        })
        Section("Catégories", if (reorder) "Terminé" else "Réorganiser") { reorder = !reorder }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { category ->
                        Surface(onClick = { editId = category.id }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Column {
                                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Symbol(category.icon, size = 23, tint = categoryColor(category))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Label(category.name, 11, weight = FontWeight.SemiBold)
                                        Label("${state.phrases.count { it.categoryId == category.id }} cartes", 10, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Symbol("chevron-right", size = 13, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (reorder) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    val index = state.categories.indexOf(category)
                                    IconButton({ if (index > 0) model.reorder(state.categories.toMutableList().apply { add(index-1, removeAt(index)) }) }, enabled = index > 0) { Symbol("arrow-up", "Monter ${category.name}", 18) }
                                    IconButton({ if (index < state.categories.lastIndex) model.reorder(state.categories.toMutableList().apply { add(index+1, removeAt(index)) }) }, enabled = index < state.categories.lastIndex) { Symbol("arrow-down", "Descendre ${category.name}", 18) }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Section("Apparence")
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Triple(Appearance.LIGHT, "Clair", "sun"), Triple(Appearance.DARK, "Sombre", "moon"), Triple(Appearance.SYSTEM, "Système", "smartphone")).forEach { (value, title, icon) ->
                        val selected = state.preferences.appearance == value
                        OutlinedButton({ model.appearance(value) }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(5.dp), border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Symbol(icon, size = 16, tint = LocalContentColor.current)
                            Spacer(Modifier.width(5.dp)); Label(title, 11, LocalContentColor.current, FontWeight.Medium)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Label("Taille du chinois", 13, modifier = Modifier.weight(1f))
                    Action("minus", "Réduire le chinois") { model.scale(state.preferences.textScale - .125f) }
                    Label("${(state.preferences.textScale * 100).toInt()} %", 12, MaterialTheme.colorScheme.primary)
                    Action("plus", "Agrandir le chinois") { model.scale(state.preferences.textScale + .125f) }
                }
            }
        }
        Surface(shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("shield-check", size = 28, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Label("Prêt, même sans réseau", 13, weight = FontWeight.SemiBold)
                    Label("Cartes et favoris sur cet appareil. Aucun compte requis.", 11, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ export.launch("shuo-cartes.json") }, contentPadding = PaddingValues(0.dp)) { Label("Exporter mes cartes", 12, MaterialTheme.colorScheme.primary, FontWeight.SemiBold) }
            TextButton({ import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, contentPadding = PaddingValues(0.dp)) { Label("Importer des cartes", 12, MaterialTheme.colorScheme.primary, FontWeight.SemiBold) }
        }
        Label("Version ${BuildConfig.VERSION_NAME} · FR → 简体中文", 11, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (creating || editId != null) {
        val existing = state.categories.firstOrNull { it.id == editId }
        CategoryEditor(existing, state.categories.size, model,
            dismiss = { creating = false; editId = null },
            browse = { if (existing != null) { creating = false; editId = null; browse(existing) } })
    }
}

@Composable private fun CategoryEditor(existing: Category?, order: Int, model: LibraryViewModel, dismiss: () -> Unit, browse: () -> Unit) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var color by rememberSaveable(existing?.id) { mutableStateOf(existing?.color ?: "#3269D8") }
    var darkColor by rememberSaveable(existing?.id) { mutableStateOf(existing?.darkColor ?: "#85AFFF") }
    var icon by rememberSaveable(existing?.id) { mutableStateOf(existing?.icon ?: "train-front") }
    val id = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var deleting by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) "Nouvelle catégorie" else "Modifier la catégorie") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text("Nom") }, singleLine = true)
                OutlinedTextField(color, { color = it.take(7) }, label = { Text("Couleur claire · #RRGGBB") }, singleLine = true)
                OutlinedTextField(darkColor, { darkColor = it.take(7) }, label = { Text("Couleur sombre · #RRGGBB") }, singleLine = true)
                CategoryIcons.chunked(5).forEach { row -> Row {
                    row.forEach { option -> IconButton({ icon = option }, Modifier.weight(1f)) {
                        Symbol(option, option, tint = if (icon == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                } }
                if (existing != null) {
                    TextButton(browse) { Text("Voir les cartes") }
                    TextButton({ deleting = true }) { Text("Supprimer la catégorie", color = MaterialTheme.colorScheme.error) }
                }
            }
        }, confirmButton = { TextButton({ model.saveCategory(Category(id, name, color, darkColor, icon, existing?.order ?: order), dismiss) },
            enabled = name.isNotBlank() && listOf(color, darkColor).all { it.matches(Regex("#[0-9a-fA-F]{6}")) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(dismiss) { Text("Annuler") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Supprimer cette catégorie ?") },
        text = { Text("La catégorie doit être vide. Déplacez d’abord ses cartes dans une autre catégorie.") },
        confirmButton = { TextButton({ model.deleteCategory(id, dismiss); deleting = false }) { Text("Supprimer") } },
        dismissButton = { TextButton({ deleting = false }) { Text("Annuler") } })
}
