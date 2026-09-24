package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import com.chaners.combinedstatus.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.combinedstatus.settings.DiagnosticsLevel

internal object RuntimeDiagnosticsPreferencesOwner {
    private var preferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    val isBound: Boolean
        @Synchronized get() = preferences != null

    internal data class BindResult(
        val detailedEnabled: Boolean,
    )

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        forceDetailed: Boolean,
        onDetailedChanged: (Boolean) -> Unit,
    ): BindResult {
        unbindLocked()

        val detailedEnabled =
            resolveDetailed(
                preferences = preferences,
                forceDetailed = forceDetailed,
            )
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (key == DIAGNOSTICS_LEVEL_KEY) {
                    onDetailedChanged(
                        resolveDetailed(
                            preferences = changed,
                            forceDetailed = forceDetailed,
                        ),
                    )
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.preferences = preferences
        this.listener = listener

        runCatching {
            onDetailedChanged(detailedEnabled)
        }.onFailure {
            unbindLocked()
            throw it
        }

        return BindResult(
            detailedEnabled = detailedEnabled,
        )
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
    }

    private fun unbindLocked() {
        val currentPreferences = preferences
        val currentListener = listener
        preferences = null
        listener = null
        if (currentPreferences != null && currentListener != null) {
            currentPreferences.unregisterOnSharedPreferenceChangeListener(currentListener)
        }
    }

    private fun resolveDetailed(
        preferences: SharedPreferences,
        forceDetailed: Boolean,
    ): Boolean =
        forceDetailed ||
            preferences.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagnosticsLevel.General.name,
            ) == DiagnosticsLevel.Detailed.name
}
