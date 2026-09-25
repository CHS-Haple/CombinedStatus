package com.chaners.combinedstatus.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class CombinedStatusFeatureSettings(
    val enabled: Boolean = true,
)

internal class CombinedStatusFeatureSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_FEATURE_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<CombinedStatusFeatureSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == COMBINED_STATUS_ENABLED_KEY) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): CombinedStatusFeatureSettings =
        CombinedStatusFeatureSettings(
            enabled =
                preferences.getBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    true,
                ),
        )

    fun setEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(COMBINED_STATUS_ENABLED_KEY, enabled)
            .apply()
    }
}

internal const val COMBINED_STATUS_FEATURE_PREFS_NAME = "combined_status_feature"
internal const val COMBINED_STATUS_ENABLED_KEY = "combined_status_enabled"
