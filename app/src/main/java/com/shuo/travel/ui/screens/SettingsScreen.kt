package com.shuo.travel.ui.screens

import com.shuo.travel.R
import androidx.compose.ui.res.stringResource
import com.shuo.travel.ui.countText
import com.shuo.travel.ui.LocalLanguage
import com.shuo.travel.domain.localizedName
import com.shuo.travel.domain.translation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
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
        Header(stringResource(R.string.library), stringResource(R.string.library_count, state.phrases.size, state.categories.size), action = {
            Action("plus", stringResource(R.string.add_category), true) { creating = true }
        })
        UpdateSection(model.updates)
        Section(stringResource(R.string.categories), if (reorder) stringResource(R.string.done) else stringResource(R.string.reorder)) { reorder = !reorder }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { category ->
                        Surface(onClick = { editId = category.id }, Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                            Column {
                                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Symbol(category.icon, size = 23, tint = categoryColor(category))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Label(category.localizedName(LocalLanguage.current), 11, weight = FontWeight.SemiBold)
                                        Label(countText(R.plurals.cards_count, state.phrases.count { it.categoryId == category.id }), 10, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Symbol("chevron-right", size = 13, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (reorder) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    val index = state.categories.indexOf(category)
                                    IconButton({ if (index > 0) model.reorder(state.categories.toMutableList().apply { add(index-1, removeAt(index)) }) }, enabled = index > 0) { Symbol("arrow-up", stringResource(R.string.move_up, category.localizedName(LocalLanguage.current)), 18) }
                                    IconButton({ if (index < state.categories.lastIndex) model.reorder(state.categories.toMutableList().apply { add(index+1, removeAt(index)) }) }, enabled = index < state.categories.lastIndex) { Symbol("arrow-down", stringResource(R.string.move_down, category.localizedName(LocalLanguage.current)), 18) }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Section(stringResource(R.string.language))
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(stringResource(R.string.language_hint), 12, MaterialTheme.colorScheme.onSurfaceVariant)
                Language.entries.forEach { language ->
                    val selected = language == state.preferences.language
                    Row(Modifier.fillMaxWidth().testTag("language-${language.tag}")
                        .selectable(selected, role = Role.RadioButton) { model.language(language) }
                        .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Label(language.nativeName, 14)
                    }
                }
                Label(stringResource(R.string.fallback_hint), 11, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Section(stringResource(R.string.appearance))
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Triple(Appearance.LIGHT, stringResource(R.string.light), "sun"), Triple(Appearance.DARK, stringResource(R.string.dark), "moon"), Triple(Appearance.SYSTEM, stringResource(R.string.system), "smartphone")).forEach { (value, title, icon) ->
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
                    Label(stringResource(R.string.chinese_size), 13, modifier = Modifier.weight(1f))
                    Action("minus", stringResource(R.string.smaller)) { model.scale(state.preferences.textScale - .125f) }
                    Label("${(state.preferences.textScale * 100).toInt()} %", 12, MaterialTheme.colorScheme.primary)
                    Action("plus", stringResource(R.string.larger)) { model.scale(state.preferences.textScale + .125f) }
                }
            }
        }
        Surface(shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("shield-check", size = 28, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Label(stringResource(R.string.offline_ready), 13, weight = FontWeight.SemiBold)
                    Label(stringResource(R.string.offline_body), 11, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ export.launch("shuo-cartes.json") }, contentPadding = PaddingValues(0.dp)) { Label(stringResource(R.string.export_cards), 12, MaterialTheme.colorScheme.primary, FontWeight.SemiBold) }
            TextButton({ import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, contentPadding = PaddingValues(0.dp)) { Label(stringResource(R.string.import_cards), 12, MaterialTheme.colorScheme.primary, FontWeight.SemiBold) }
        }
        Label(stringResource(R.string.version_label, BuildConfig.VERSION_NAME, state.preferences.language.tag.uppercase()), 11, MaterialTheme.colorScheme.onSurfaceVariant)
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
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) stringResource(R.string.new_category) else stringResource(R.string.edit_category)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
                OutlinedTextField(color, { color = it.take(7) }, label = { Text(stringResource(R.string.light_color)) }, singleLine = true)
                OutlinedTextField(darkColor, { darkColor = it.take(7) }, label = { Text(stringResource(R.string.dark_color)) }, singleLine = true)
                CategoryIcons.chunked(5).forEach { row -> Row {
                    row.forEach { option -> IconButton({ icon = option }, Modifier.weight(1f)) {
                        Symbol(option, stringResource(when (option) {
                        "train-front" -> R.string.icon_train; "utensils" -> R.string.icon_food
                        "bed-double" -> R.string.icon_bed; "shopping-bag" -> R.string.icon_shop
                        "signpost" -> R.string.icon_sign; "cross" -> R.string.icon_cross
                        "messages-square" -> R.string.icon_chat; "wallet" -> R.string.icon_wallet
                        "landmark" -> R.string.icon_landmark; else -> R.string.icon_other
                    }), tint = if (icon == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                } }
                if (existing != null) {
                    TextButton(browse) { Text(stringResource(R.string.view_cards)) }
                    TextButton({ deleting = true }) { Text(stringResource(R.string.delete_category), color = MaterialTheme.colorScheme.error) }
                }
            }
        }, confirmButton = { TextButton({ model.saveCategory(Category(id, name, color, darkColor, icon, existing?.order ?: order), dismiss) },
            enabled = name.isNotBlank() && listOf(color, darkColor).all { it.matches(Regex("#[0-9a-fA-F]{6}")) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.delete_category_title)) },
        text = { Text(stringResource(R.string.delete_category_body)) },
        confirmButton = { TextButton({ model.deleteCategory(id, dismiss); deleting = false }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } })
}
