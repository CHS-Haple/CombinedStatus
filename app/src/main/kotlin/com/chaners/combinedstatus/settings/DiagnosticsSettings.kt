package com.chaners.combinedstatus.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class DiagnosticsLevel {
    General,
    Detailed,
}

internal data class DiagnosticsSettings(
    val level: DiagnosticsLevel = DiagnosticsLevel.General,
)

internal fun decodeDiagnosticsLevel(storedValue: String?): DiagnosticsLevel =
    DiagnosticsLevel.entries.firstOrNull { it.name == storedValue }
        ?: DiagnosticsLevel.General

internal class DiagnosticsSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            DIAGNOSTICS_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<DiagnosticsSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(DiagnosticsSettings(level = currentLevel()))
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == DIAGNOSTICS_LEVEL_KEY) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun currentLevel(): DiagnosticsLevel =
        decodeDiagnosticsLevel(
            preferences.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagnosticsLevel.General.name,
            ),
        )

    fun setLevel(level: DiagnosticsLevel) {
        preferences
            .edit()
            .putString(DIAGNOSTICS_LEVEL_KEY, level.name)
            .apply()
    }
}

internal const val DIAGNOSTICS_PREFS_NAME = "diagnostics"
internal const val DIAGNOSTICS_LEVEL_KEY = "diagnostics_level"
