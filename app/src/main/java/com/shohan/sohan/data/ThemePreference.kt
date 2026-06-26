package com.shohan.sohan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore: DataStore<Preferences>
    by preferencesDataStore(name = "sohan_theme")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class ThemePreference(private val context: Context) {

    private val KEY = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeStore.data.map { prefs ->
        when (prefs[KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK"  -> ThemeMode.DARK
            else    -> ThemeMode.SYSTEM
        }
    }

    suspend fun set(mode: ThemeMode) {
        context.themeStore.edit { it[KEY] = mode.name }
    }
}
