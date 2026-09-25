package com.chaners.combinedstatus.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class CombinedStatusVisualSettings(
    val mobileFollowsBatteryColor: Boolean = false,
    val centerFollowsBatteryColor: Boolean = false,
)

internal class CombinedStatusVisualSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<CombinedStatusVisualSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (
                        key == MOBILE_FOLLOWS_BATTERY_COLOR_KEY ||
                        key == CENTER_FOLLOWS_BATTERY_COLOR_KEY
                    ) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): CombinedStatusVisualSettings =
        CombinedStatusVisualSettings(
            mobileFollowsBatteryColor =
                preferences.getBoolean(
                    MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                    false,
                ),
            centerFollowsBatteryColor =
                preferences.getBoolean(
                    CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                    false,
                ),
        )

    fun setMobileFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(MOBILE_FOLLOWS_BATTERY_COLOR_KEY, enabled)
            .apply()
    }

    fun setCenterFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(CENTER_FOLLOWS_BATTERY_COLOR_KEY, enabled)
            .apply()
    }
}

internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val RUNTIME_REMOTE_PREFS_NAME = "CombinedStatusRuntimeConfig"
