package com.shuo.travel.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.shuo.travel.domain.*
import com.shuo.travel.ui.theme.*

@Composable fun PhraseCard(phrase: Phrase, category: Category, modifier: Modifier = Modifier,
    presentation: Boolean = false, scale: Float = 1f, minHeight: Dp = 148.dp, onClick: (() -> Unit)? = null) {
    val color = categoryColor(category)
    val tabText = if (LocalDark.current) Color(0xFF10151D) else Color.White
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape)
        .background(MaterialTheme.colorScheme.surface).border(1.dp, color.copy(alpha = .4f), shape)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Montrer la carte", onClick = onClick) else Modifier)) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        Canvas(Modifier.matchParentSize().semantics { contentDescription = "Catégorie : ${category.name}" }) {
            drawRect(color, size = androidx.compose.ui.geometry.Size(30.dp.toPx(), size.height))
            val layout = textMeasurer.measure(category.name, TextStyle(fontFamily = Inter, fontSize = 11.sp, color = tabText),
                maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (size.height - 24.dp.toPx()).toInt().coerceAtLeast(1)))
            val pivot = Offset(15.dp.toPx(), size.height / 2)
            rotate(90f, pivot) { drawText(layout, topLeft = Offset(pivot.x - layout.size.width / 2, pivot.y - layout.size.height / 2)) }
        }
        if (presentation) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(start = 48.dp, end = 16.dp, top = 24.dp, bottom = 24.dp), contentAlignment = Alignment.Center) {
                val pinyinStyle = TextStyle(fontFamily = Inter, fontSize = 23.sp, lineHeight = 32.2.sp)
                val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
                val textHeight = (maxHeight - 104.dp).coerceAtLeast(1.dp)
                val height = with(density) { textHeight.roundToPx() }
                val pinyinHeight = textMeasurer.measure(phrase.pinyin, pinyinStyle, constraints = Constraints(maxWidth = width)).size.height
                val textSize = remember(phrase.chinese, width, height, scale, density.fontScale) {
                    var value = 68f * scale
                    while (value > 24f) {
                        val measured = textMeasurer.measure(phrase.chinese,
                            TextStyle(fontFamily = ChineseFont, fontSize = value.sp, lineHeight = (value * 1.45f).sp, fontWeight = FontWeight.Medium),
                            constraints = Constraints(maxWidth = width))
                        if (measured.size.height + pinyinHeight + with(density) { 18.dp.roundToPx() } <= height) break
                        value -= 2f
                    }
                    value.coerceAtLeast(24f)
                }
                Column(Modifier.heightIn(max = textHeight).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    androidx.compose.material3.Text(phrase.chinese, Modifier.fillMaxWidth(), fontFamily = ChineseFont,
                        fontWeight = FontWeight.Medium, fontSize = textSize.sp, lineHeight = (textSize * 1.45f).sp, textAlign = TextAlign.Center)
                    androidx.compose.material3.Text(phrase.pinyin, Modifier.fillMaxWidth(), style = pinyinStyle,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().heightIn(min = minHeight).padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)) {
                androidx.compose.material3.Text(phrase.chinese, Modifier.fillMaxWidth(), fontFamily = ChineseFont, fontWeight = FontWeight.Medium,
                    fontSize = (30 * scale).sp, lineHeight = (43.5f * scale).sp, textAlign = TextAlign.Center)
                Label(phrase.pinyin, 14, MaterialTheme.colorScheme.onSurfaceVariant, align = TextAlign.Center)
                Label(phrase.french, 12, align = TextAlign.Center)
            }
        }
        Symbol(category.icon, size = if (presentation) 52 else 26, tint = color,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = if (presentation) 24.dp else 12.dp, bottom = if (presentation) 26.dp else 10.dp))
    }
}
