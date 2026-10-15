package com.shuo.travel.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settings by preferencesDataStore("settings")
enum class Appearance { LIGHT, DARK, SYSTEM }
data class Preferences(val appearance: Appearance = Appearance.SYSTEM, val textScale: Float = 1f)

class PreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("appearance")
    private val scaleKey = floatPreferencesKey("text-scale")
    val preferences = context.settings.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map {
        Preferences(runCatching { Appearance.valueOf(it[themeKey].orEmpty()) }.getOrDefault(Appearance.SYSTEM),
            (it[scaleKey] ?: 1f).coerceIn(0.75f, 1.5f))
    }
    suspend fun appearance(value: Appearance) { context.settings.edit { it[themeKey] = value.name } }
    suspend fun scale(value: Float) { context.settings.edit { it[scaleKey] = value.coerceIn(0.75f, 1.5f) } }
}
