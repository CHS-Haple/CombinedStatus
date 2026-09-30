package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

internal data class CombinedStatusVisualSettings(
    val mobileFollowsBatteryColor: Boolean = false,
    val centerFollowsBatteryColor: Boolean = false,
    val batteryTopReadoutEnabled: Boolean = false,
    val batteryTopTextScale: Float = BATTERY_TOP_TEXT_SCALE_DEFAULT,
    val batteryTopTextWeight: Int = BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
    val batteryTopVerticalOffset: Float = BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
    val batteryTopChargingIconScale: Float = BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT,
)

internal fun CombinedStatusVisualSettings.normalized(): CombinedStatusVisualSettings =
    copy(
        batteryTopTextScale =
            batteryTopTextScale.coerceIn(
                BATTERY_TOP_TEXT_SCALE_MIN,
                BATTERY_TOP_TEXT_SCALE_MAX,
            ),
        batteryTopTextWeight =
            batteryTopTextWeight.coerceIn(
                BATTERY_TOP_TEXT_WEIGHT_MIN,
                BATTERY_TOP_TEXT_WEIGHT_MAX,
            ),
        batteryTopVerticalOffset =
            batteryTopVerticalOffset.coerceIn(
                BATTERY_TOP_VERTICAL_OFFSET_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_MAX,
            ),
        batteryTopChargingIconScale =
            batteryTopChargingIconScale.coerceIn(
                BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
            ),
    )

internal fun isCombinedStatusVisualPreferenceKey(key: String?): Boolean =
    when (key) {
        MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
        CENTER_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_READOUT_ENABLED_KEY,
        BATTERY_TOP_TEXT_SCALE_KEY,
        BATTERY_TOP_TEXT_WEIGHT_KEY,
        BATTERY_TOP_VERTICAL_OFFSET_KEY,
        BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
        -> true
        else -> false
    }

internal class CombinedStatusVisualSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_VISUAL_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        migrateBatteryTopChargingScaleReferenceIfNeeded(preferences)
    }

    val settings: Flow<CombinedStatusVisualSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isCombinedStatusVisualPreferenceKey(key)) {
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
            batteryTopReadoutEnabled =
                preferences.getBoolean(
                    BATTERY_TOP_READOUT_ENABLED_KEY,
                    false,
                ),
            batteryTopTextScale =
                preferences.getFloat(
                    BATTERY_TOP_TEXT_SCALE_KEY,
                    BATTERY_TOP_TEXT_SCALE_DEFAULT,
                ),
            batteryTopTextWeight =
                preferences.getInt(
                    BATTERY_TOP_TEXT_WEIGHT_KEY,
                    BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
                ),
            batteryTopVerticalOffset =
                preferences.getFloat(
                    BATTERY_TOP_VERTICAL_OFFSET_KEY,
                    BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
                ),
            batteryTopChargingIconScale =
                preferences.getFloat(
                    BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                    BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT,
                ),
        ).normalized()

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

    fun setBatteryTopReadoutEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(BATTERY_TOP_READOUT_ENABLED_KEY, enabled)
            .apply()
    }

    fun setBatteryTopTextScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                BATTERY_TOP_TEXT_UI_SCALE_MIN,
                BATTERY_TOP_TEXT_UI_SCALE_MAX,
            )
        preferences
            .edit()
            .putFloat(
                BATTERY_TOP_TEXT_SCALE_KEY,
                (uiScale * BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
                    .coerceIn(BATTERY_TOP_TEXT_SCALE_MIN, BATTERY_TOP_TEXT_SCALE_MAX),
            )
            .apply()
    }

    fun setBatteryTopTextWeight(weight: Int) {
        preferences
            .edit()
            .putInt(
                BATTERY_TOP_TEXT_WEIGHT_KEY,
                weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
            )
            .apply()
    }

    fun setBatteryTopVerticalOffset(offset: Float) {
        preferences
            .edit()
            .putFloat(
                BATTERY_TOP_VERTICAL_OFFSET_KEY,
                offset.coerceIn(BATTERY_TOP_VERTICAL_OFFSET_MIN, BATTERY_TOP_VERTICAL_OFFSET_MAX),
            )
            .apply()
    }

    fun setBatteryTopChargingIconScale(scale: Float) {
        val uiScale =
            scale.coerceIn(
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
            )
        preferences
            .edit()
            .putFloat(
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                (uiScale * BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
                    .coerceIn(
                        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
                    ),
            )
            .apply()
    }
}

internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val BATTERY_TOP_READOUT_ENABLED_KEY = "battery_top_readout_enabled"
internal const val BATTERY_TOP_TEXT_SCALE_KEY = "battery_top_text_scale"
internal const val BATTERY_TOP_TEXT_WEIGHT_KEY = "battery_top_text_weight"
internal const val BATTERY_TOP_VERTICAL_OFFSET_KEY = "battery_top_vertical_offset"
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_KEY = "battery_top_charging_icon_scale"
internal const val RUNTIME_REMOTE_PREFS_NAME = "CombinedStatusRuntimeConfig"

// Persisted text scale remains in the pre-521 physical scale.
internal const val BATTERY_TOP_TEXT_UI_SCALE_REFERENCE = 1.3f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MIN = 0f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MAX = 2f
internal const val BATTERY_TOP_TEXT_SCALE_DEFAULT =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_TEXT_SCALE_MIN = 0f
internal const val BATTERY_TOP_TEXT_SCALE_MAX =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MAX
internal const val BATTERY_TOP_TEXT_WEIGHT_DEFAULT = 900
internal const val BATTERY_TOP_TEXT_WEIGHT_MIN = 400
internal const val BATTERY_TOP_TEXT_WEIGHT_MAX = 1400
internal const val BATTERY_TOP_VERTICAL_OFFSET_DEFAULT = 0f
internal const val BATTERY_TOP_VERTICAL_OFFSET_MIN = -30f
internal const val BATTERY_TOP_VERTICAL_OFFSET_MAX = 30f

// Runtime/persisted charging scale remains a physical multiplier.
// Build 522's user-facing 110% (1.5 × 1.10 = 1.65 physical) becomes
// Build 523's user-facing/default 100% reference.
private const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY = 1.5f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE = 1.65f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN = 0f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX = 2f
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MIN = 0f
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MAX =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX
private const val BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY =
    "battery_top_charging_scale_schema"
private const val BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT = 2
private const val BATTERY_TOP_SCALE_EPSILON = 0.0001f

internal fun migrateBatteryTopChargingScaleReferenceIfNeeded(
    preferences: SharedPreferences,
) {
    if (
        preferences.getInt(BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY, 1) >=
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT
    ) {
        return
    }

    val editor = preferences.edit()
    if (preferences.contains(BATTERY_TOP_CHARGING_ICON_SCALE_KEY)) {
        val raw =
            preferences.getFloat(
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY,
            )
        if (
            abs(raw - BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY) <=
                BATTERY_TOP_SCALE_EPSILON
        ) {
            editor.putFloat(
                BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE,
            )
        }
    }
    editor
        .putInt(
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_KEY,
            BATTERY_TOP_CHARGING_SCALE_SCHEMA_CURRENT,
        )
        .apply()
}

internal fun batteryTopTextUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_TEXT_UI_SCALE_MIN,
            BATTERY_TOP_TEXT_UI_SCALE_MAX,
        )

internal fun batteryTopChargingIconUiScale(rawScale: Float): Float =
    (rawScale / BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
        .coerceIn(
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN,
            BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX,
        )
