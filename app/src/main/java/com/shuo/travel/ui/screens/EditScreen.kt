package com.shuo.travel.ui.screens

import com.shuo.travel.R
import androidx.compose.ui.res.stringResource
import com.shuo.travel.ui.LocalLanguage
import com.shuo.travel.domain.localizedName
import com.shuo.travel.domain.translation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import com.shuo.travel.domain.*
import com.shuo.travel.ui.*
import com.shuo.travel.ui.components.*
import com.shuo.travel.ui.theme.*
import java.util.UUID

@Composable fun EditScreen(existing: Phrase?, state: UiState, model: LibraryViewModel, back: () -> Unit, onDirtyChanged: (Boolean) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var french by rememberSaveable { mutableStateOf(existing?.french.orEmpty()) }
    var chinese by rememberSaveable { mutableStateOf(existing?.chinese.orEmpty()) }
    var pinyin by rememberSaveable { mutableStateOf(existing?.pinyin.orEmpty()) }
    var english by rememberSaveable { mutableStateOf(existing?.english.orEmpty()) }
    var spanish by rememberSaveable { mutableStateOf(existing?.spanish.orEmpty()) }
    var italian by rememberSaveable { mutableStateOf(existing?.italian.orEmpty()) }
    var keywords by rememberSaveable { mutableStateOf(existing?.keywords?.joinToString(", ").orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.categoryId ?: state.categories.firstOrNull()?.id) }
    var extras by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val dirty = french != existing?.french.orEmpty() || chinese != existing?.chinese.orEmpty() || pinyin != existing?.pinyin.orEmpty() ||
        english != existing?.english.orEmpty() || spanish != existing?.spanish.orEmpty() || italian != existing?.italian.orEmpty() || keywords != existing?.keywords?.joinToString(", ").orEmpty() ||
        category != (existing?.categoryId ?: state.categories.firstOrNull()?.id)
    val close = { if (dirty) confirmDiscard = true else back() }
    DisposableEffect(dirty) {
        onDirtyChanged(dirty)
        onDispose { onDirtyChanged(false) }
    }
    BackHandler { close() }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Header(if (existing == null) stringResource(R.string.new_card) else stringResource(R.string.edit_card), back = close,
                action = { Action("x", stringResource(R.string.close), true, onClick = close) })
            PhraseInput(stringResource(R.string.french_input), french, "french-input") { french = it }
            when (state.preferences.language) {
                Language.ENGLISH -> PhraseInput(stringResource(R.string.english), english, "english-input") { english = it }
                Language.SPANISH -> PhraseInput(stringResource(R.string.spanish), spanish, "spanish-input") { spanish = it }
                Language.ITALIAN -> PhraseInput(stringResource(R.string.italian), italian, "italian-input") { italian = it }
                Language.FRENCH -> Unit
            }
            PhraseInput(stringResource(R.string.chinese_input), chinese, "chinese-input", true) { chinese = it }
            PhraseInput(stringResource(R.string.pinyin_input), pinyin, "pinyin-input") { pinyin = it }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Symbol("info", size = 18, tint = MaterialTheme.colorScheme.primary)
                Label(stringResource(R.string.manual_translation), 12, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section(stringResource(R.string.choose_category))
            CategoryGrid(state.categories, category, true) { category = it.id }
            TextButton({ extras = true }) { Label(stringResource(R.string.extra_translations), 12, MaterialTheme.colorScheme.primary) }
            if (existing != null) TextButton({ confirmDelete = true }) { Label(stringResource(R.string.delete_card), 12, MaterialTheme.colorScheme.error) }
        }
        Button(onClick = {
            submitting = true
            model.save(Phrase(id, chinese, pinyin, french, english.ifBlank { null },
                keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }, category!!, spanish = spanish.ifBlank { null }, italian = italian.ifBlank { null }), done = back, finished = { submitting = false })
        }, enabled = !submitting && french.isNotBlank() && chinese.isNotBlank() && pinyin.isNotBlank() && category != null,
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp).heightIn(min = 52.dp).testTag("save-phrase"), shape = RoundedCornerShape(16.dp)) {
            Symbol("check", tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_card))
        }
    }
    if (extras) AlertDialog(onDismissRequest = { extras = false }, title = { Text(stringResource(R.string.optional_fields)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(english, { if (it.length <= 2000) english = it }, label = { Text(stringResource(R.string.english)) })
            OutlinedTextField(spanish, { if (it.length <= 2000) spanish = it }, label = { Text(stringResource(R.string.spanish)) })
            OutlinedTextField(italian, { if (it.length <= 2000) italian = it }, label = { Text(stringResource(R.string.italian)) })
            OutlinedTextField(keywords, { if (it.length <= 5000) keywords = it }, label = { Text(stringResource(R.string.keywords)) })
        }
    }, confirmButton = { TextButton({ extras = false }) { Text(stringResource(R.string.done)) } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.delete_card_title)) },
        text = { Text(stringResource(R.string.delete_card_body)) },
        confirmButton = { TextButton({ model.delete(id, back); confirmDelete = false }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = { TextButton(back) { Text(stringResource(R.string.leave)) } }, dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.continue_edit)) } })
}

@Composable private fun PhraseInput(label: String, value: String, tag: String, chinese: Boolean = false, change: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(label, 13, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        OutlinedTextField(value, { if (it.length <= 2000) change(it) }, Modifier.fillMaxWidth().testTag(tag),
            shape = RoundedCornerShape(16.dp), textStyle = TextStyle(fontFamily = if (chinese) ChineseFont else Inter, fontSize = if (chinese) 26.sp else 14.sp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline))
    }
}
