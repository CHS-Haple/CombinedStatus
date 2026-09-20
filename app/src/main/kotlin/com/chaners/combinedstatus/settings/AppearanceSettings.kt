package com.chaners.combinedstatus.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal enum class AppThemeMode {
    System,
    Light,
    Dark,
    Dynamic,
}

internal data class AppearanceSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val glassBottomBarEnabled: Boolean = true,
    val swipeBackEnabled: Boolean = true,
)

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
)

internal class AppearanceSettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val settings: Flow<AppearanceSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppearanceSettings(
                themeMode = preferences[ThemeModeKey]
                    ?.let { stored -> AppThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: AppThemeMode.System,
                glassBottomBarEnabled =
                    (preferences[GlassBottomBarEnabledKey] ?: true) &&
                        (preferences[LegacyBlurEnabledKey] ?: true),
                swipeBackEnabled = preferences[SwipeBackEnabledKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    suspend fun setGlassBottomBarEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[GlassBottomBarEnabledKey] = enabled
            preferences.remove(LegacyBlurEnabledKey)
        }
    }

    suspend fun setSwipeBackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SwipeBackEnabledKey] = enabled
        }
    }

    private companion object {
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val LegacyBlurEnabledKey = booleanPreferencesKey("blur_enabled")
        val GlassBottomBarEnabledKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val SwipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    }
}
