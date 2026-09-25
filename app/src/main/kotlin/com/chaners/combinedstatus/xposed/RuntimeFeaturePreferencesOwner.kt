package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import com.chaners.combinedstatus.settings.COMBINED_STATUS_ENABLED_KEY
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettings

internal object RuntimeFeaturePreferencesOwner {
    @Volatile
    private var current = CombinedStatusFeatureSettings(enabled = false)

    private var preferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindingToken: Any? = null

    fun currentSettings(): CombinedStatusFeatureSettings = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (CombinedStatusFeatureSettings) -> Unit,
    ): CombinedStatusFeatureSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    key == COMBINED_STATUS_ENABLED_KEY &&
                    isCurrentBinding(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != current) {
                        current = next
                        onChanged(next)
                    }
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.preferences = preferences
        this.listener = listener
        bindingToken = token
        onChanged(initial)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        current = CombinedStatusFeatureSettings(enabled = false)
    }

    private fun unbindLocked() {
        val currentPreferences = preferences
        val currentListener = listener
        preferences = null
        listener = null
        bindingToken = null
        if (currentPreferences != null && currentListener != null) {
            currentPreferences.unregisterOnSharedPreferenceChangeListener(currentListener)
        }
    }

    @Synchronized
    private fun isCurrentBinding(
        preferences: SharedPreferences,
        token: Any,
    ): Boolean =
        this.preferences === preferences &&
            bindingToken === token

    private fun resolve(preferences: SharedPreferences): CombinedStatusFeatureSettings =
        CombinedStatusFeatureSettings(
            enabled =
                preferences.getBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    true,
                ),
        )
}
