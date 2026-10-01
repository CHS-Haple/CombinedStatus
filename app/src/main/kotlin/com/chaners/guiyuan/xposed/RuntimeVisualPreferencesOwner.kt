package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import com.chaners.guiyuan.settings.isCombinedStatusVisualPreferenceKey
import com.chaners.guiyuan.settings.readCombinedStatusVisualSettings

internal object RuntimeVisualPreferencesOwner {
    @Volatile
    private var current = CombinedStatusVisualSettings()

    private var preferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindingToken: Any? = null

    fun currentSettings(): CombinedStatusVisualSettings = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (CombinedStatusVisualSettings) -> Unit,
    ): CombinedStatusVisualSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isCombinedStatusVisualPreferenceKey(key) &&
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
        current = CombinedStatusVisualSettings()
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

    private fun resolve(preferences: SharedPreferences): CombinedStatusVisualSettings =
        preferences.readCombinedStatusVisualSettings()
}
