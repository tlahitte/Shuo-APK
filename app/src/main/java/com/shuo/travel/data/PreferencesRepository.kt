package com.shuo.travel.data

import android.content.Context
import com.shuo.travel.domain.Language
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

private val Context.settings by preferencesDataStore("settings")
enum class Appearance { LIGHT, DARK, SYSTEM }
data class Preferences(val appearance: Appearance = Appearance.SYSTEM, val textScale: Float = 1f, val language: Language = Language.FRENCH)

class PreferencesRepository(private val context: Context) {
    // The locale is needed before Activity creation, including platform dialogs.
    // This small preference can be read without blocking on a coroutine/DataStore.
    private val languageStore = context.getSharedPreferences("app-language", Context.MODE_PRIVATE)
    private val language = MutableStateFlow(savedLanguage(context))
    private val themeKey = stringPreferencesKey("appearance")
    private val scaleKey = floatPreferencesKey("text-scale")
    val preferences = combine(context.settings.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }, language) { values, selected ->
        Preferences(runCatching { Appearance.valueOf(values[themeKey].orEmpty()) }.getOrDefault(Appearance.SYSTEM),
            (values[scaleKey] ?: 1f).coerceIn(0.75f, 1.5f), selected)
    }
    suspend fun language(value: Language) {
        languageStore.edit().putString("language", value.tag).apply()
        language.value = value
    }
    suspend fun appearance(value: Appearance) { context.settings.edit { it[themeKey] = value.name } }
    suspend fun scale(value: Float) { context.settings.edit { it[scaleKey] = value.coerceIn(0.75f, 1.5f) } }
    companion object {
        fun savedLanguage(context: Context): Language = Language.fromTag(
            context.getSharedPreferences("app-language", Context.MODE_PRIVATE).getString("language", null))
    }
}
