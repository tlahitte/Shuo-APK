package com.shuo.travel.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shuo.travel.domain.Phrase
import com.shuo.travel.ui.*
import com.shuo.travel.ui.components.*
import com.shuo.travel.ui.theme.ChineseFont
import com.shuo.travel.ui.theme.Inter

@Composable fun PresentationScreen(phrase: Phrase, state: UiState, model: LibraryViewModel, back: () -> Unit, edit: () -> Unit, next: () -> Unit) {
    var revealed by rememberSaveable(phrase.id) { mutableStateOf(false) }
    var rotated by rememberSaveable(phrase.id) { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    LaunchedEffect(phrase.id) { model.used(phrase.id) }
    val category = state.categories.firstOrNull { it.id == phrase.categoryId } ?: return
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val textWidth = with(density) { (maxWidth - 96.dp).roundToPx() }.coerceAtLeast(1)
        val chineseHeight = measurer.measure(phrase.chinese, TextStyle(fontFamily = ChineseFont,
            fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 34.8.sp), constraints = Constraints(maxWidth = textWidth)).size.height
        val pinyinHeight = measurer.measure(phrase.pinyin, TextStyle(fontFamily = Inter,
            fontSize = 23.sp, lineHeight = 32.2.sp), constraints = Constraints(maxWidth = textWidth)).size.height
        val minimumCardHeight = with(density) { (chineseHeight + pinyinHeight).toDp() } + 174.dp
        val short = maxHeight < 550.dp || minimumCardHeight > maxHeight - 240.dp
        Column(Modifier.fillMaxSize().then(if (short) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(16.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Action("arrow-left", "Retour", onClick = back)
                Label("MONTRER LA CARTE", 12, MaterialTheme.colorScheme.onSurfaceVariant)
                Action("star", if (phrase.favorite) "Retirer des favoris" else "Ajouter aux favoris", true,
                    tint = if (phrase.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) { model.favorite(phrase.id) }
            }
            PhraseCard(phrase, category,
                (if (short) Modifier.height(maxOf(400.dp, minimumCardHeight)) else Modifier.weight(1f)).rotate(if (rotated) 180f else 0f),
                presentation = true, scale = state.preferences.textScale, minHeight = 180.dp)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (revealed) {
                    Label(phrase.french, 19)
                    phrase.english?.let { Label(it, 13, MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    TextButton({ revealed = true }, contentPadding = PaddingValues(0.dp)) { Text("Afficher la traduction") }
                }
                Label("Carte ${state.phrases.indexOfFirst { it.id == phrase.id } + 1} sur ${state.phrases.size} · Votre bibliothèque", 12, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(edit) { Symbol("pencil", size = 22); Spacer(Modifier.width(8.dp)); Label("Modifier", 13, MaterialTheme.colorScheme.onSurfaceVariant) }
                Row {
                    if (revealed) Action("eye-off", "Masquer la traduction") { revealed = false }
                    Action("rotate-ccw", "Tourner la carte à 180 degrés") { rotated = !rotated }
                    Action("chevron-right", "Carte suivante", true, onClick = next)
                }
            }
        }
    }
}
