package com.shuo.travel.ui

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.shuo.travel.domain.Language
import java.util.Locale

val LocalLanguage = staticCompositionLocalOf { Language.FRENCH }

fun Context.inLanguage(language: Language): Context = ContextThemeWrapper(this, 0).apply {
    // Keep the Activity in the wrapper chain for file pickers and back handling.
    applyOverrideConfiguration(Configuration(this@inLanguage.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) })
}

@Composable fun ShuoLocale(language: Language, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLanguage provides language, content = content)
}

@Composable fun countText(id: Int, count: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, count, count, *args)
