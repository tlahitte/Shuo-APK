package com.shuo.travel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import com.shuo.travel.R
import com.shuo.travel.data.Appearance

@OptIn(ExperimentalTextApi::class)
val Inter = FontFamily(*listOf(400, 500, 600, 700).map {
    Font(R.font.inter, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
}.toTypedArray())
@OptIn(ExperimentalTextApi::class)
val ChineseFont = FontFamily(Font(R.font.noto_sans_sc, FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500))))
val LocalDark = staticCompositionLocalOf { false }
private val Light = lightColorScheme(primary = Color(0xFF365FDC), onPrimary = Color.White,
    background = Color(0xFFF5F6F8), surface = Color.White, surfaceVariant = Color(0xFFEDEFF3),
    onSurface = Color(0xFF18212F), onBackground = Color(0xFF18212F), onSurfaceVariant = Color(0xFF667184), outline = Color(0xFFDDE2E9))
private val Dark = darkColorScheme(primary = Color(0xFFA4BBFF), onPrimary = Color(0xFF10151D),
    background = Color(0xFF10151D), surface = Color(0xFF1B222D), surfaceVariant = Color(0xFF262F3D),
    onSurface = Color(0xFFF0F3F9), onBackground = Color(0xFFF0F3F9), onSurfaceVariant = Color(0xFFA9B4C6), outline = Color(0xFF354050))

@Composable fun ShuoTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance) { Appearance.DARK -> true; Appearance.LIGHT -> false; Appearance.SYSTEM -> isSystemInDarkTheme() }
    val typography = Typography().let { base -> base.copy(
        bodyLarge = base.bodyLarge.copy(fontFamily = Inter), bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
        bodySmall = base.bodySmall.copy(fontFamily = Inter), labelLarge = base.labelLarge.copy(fontFamily = Inter),
        labelMedium = base.labelMedium.copy(fontFamily = Inter), titleLarge = base.titleLarge.copy(fontFamily = Inter),
        titleMedium = base.titleMedium.copy(fontFamily = Inter), headlineSmall = base.headlineSmall.copy(fontFamily = Inter)) }
    CompositionLocalProvider(LocalDark provides dark) { MaterialTheme(colorScheme = if (dark) Dark else Light, typography = typography, content = content) }
}
