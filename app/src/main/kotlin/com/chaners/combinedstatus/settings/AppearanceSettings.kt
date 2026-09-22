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
}

internal data class AppearanceSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColorEnabled: Boolean = false,
    val floatingNavigationBlurEnabled: Boolean = true,
    val swipeBackEnabled: Boolean = true,
)

internal data class ThemeSelection(
    val mode: AppThemeMode,
    val dynamicColorEnabled: Boolean,
)

internal fun decodeThemeSelection(
    storedMode: String?,
    storedDynamicColorEnabled: Boolean?,
): ThemeSelection {
    val legacyDynamic = storedMode == LEGACY_DYNAMIC_THEME_MODE
    val mode =
        AppThemeMode.entries.firstOrNull { it.name == storedMode }
            ?: AppThemeMode.System
    return ThemeSelection(
        mode = mode,
        dynamicColorEnabled = storedDynamicColorEnabled ?: legacyDynamic,
    )
}

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
            val themeSelection =
                decodeThemeSelection(
                    storedMode = preferences[ThemeModeKey],
                    storedDynamicColorEnabled = preferences[DynamicColorEnabledKey],
                )
            AppearanceSettings(
                themeMode = themeSelection.mode,
                dynamicColorEnabled = themeSelection.dynamicColorEnabled,
                floatingNavigationBlurEnabled =
                    preferences[FloatingNavigationBlurEnabledKey]
                        ?: (
                            (preferences[LegacyGlassBottomBarEnabledKey] ?: true) &&
                                (preferences[LegacyBlurEnabledKey] ?: true)
                            ),
                swipeBackEnabled = preferences[SwipeBackEnabledKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (preferences[ThemeModeKey] == LEGACY_DYNAMIC_THEME_MODE) {
                preferences[ThemeModeKey] = AppThemeMode.System.name
            }
            preferences[DynamicColorEnabledKey] = enabled
        }
    }

    suspend fun setFloatingNavigationBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FloatingNavigationBlurEnabledKey] = enabled
            preferences.remove(LegacyGlassBottomBarEnabledKey)
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
        val DynamicColorEnabledKey = booleanPreferencesKey("dynamic_color_enabled")
        val LegacyBlurEnabledKey = booleanPreferencesKey("blur_enabled")
        val LegacyGlassBottomBarEnabledKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val FloatingNavigationBlurEnabledKey =
            booleanPreferencesKey("floating_navigation_blur_enabled")
        val SwipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
