package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

internal enum class CombinedStatusContentLayout(
    val persistedValue: String,
) {
    NETWORK_CENTER("network_center"),
    BATTERY_CENTER("battery_center");

    companion object {
        fun fromPersisted(value: String?): CombinedStatusContentLayout =
            entries.firstOrNull { it.persistedValue == value } ?: NETWORK_CENTER
    }
}

internal data class CombinedStatusVisualSettings(
    val contentLayout: CombinedStatusContentLayout = CombinedStatusContentLayout.NETWORK_CENTER,
    val mobileFollowsBatteryColor: Boolean = false,
    val centerFollowsBatteryColor: Boolean = false,
    val batteryTopReadoutEnabled: Boolean = false,
    val batteryTopTextFollowsBatteryColor: Boolean = true,
    val batteryTopChargingIconEnabled: Boolean = true,
    val batteryTopChargingIconFollowsBatteryColor: Boolean = true,
    val batteryTopTextScale: Float = batteryTopTextScaleDefault(contentLayout),
    val batteryTopTextWeight: Int = BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
    val batteryTopVerticalOffset: Float = BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
    val batteryTopChargingIconScale: Float =
        batteryTopChargingIconScaleDefault(contentLayout),
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

internal fun isCombinedStatusVisualPreferenceKey(key: String?): Boolean {
    if (key == null) return false
    if (key == CONTENT_LAYOUT_KEY || key in PROFILE_VISUAL_BASE_KEYS) return true
    return CombinedStatusContentLayout.entries.any { layout ->
        PROFILE_VISUAL_BASE_KEYS.any { baseKey ->
            key == combinedStatusProfileKey(layout, baseKey)
        }
    }
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
        preferences.readCombinedStatusVisualSettings()

    private fun activeProfileKey(baseKey: String): String =
        combinedStatusProfileKey(
            layout = preferences.readCombinedStatusContentLayout(),
            baseKey = baseKey,
        )

    fun setContentLayout(layout: CombinedStatusContentLayout) {
        preferences
            .edit()
            .putString(CONTENT_LAYOUT_KEY, layout.persistedValue)
            .apply()
    }

    fun setMobileFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(MOBILE_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setCenterFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(CENTER_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setBatteryTopReadoutEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_READOUT_ENABLED_KEY), enabled)
            .apply()
    }

    fun setBatteryTopTextFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY), enabled)
            .apply()
    }

    fun setBatteryTopChargingIconEnabled(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_ENABLED_KEY), enabled)
            .apply()
    }

    fun setBatteryTopChargingIconFollowsBatteryColor(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(activeProfileKey(BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY), enabled)
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
                activeProfileKey(BATTERY_TOP_TEXT_SCALE_KEY),
                (uiScale * BATTERY_TOP_TEXT_UI_SCALE_REFERENCE)
                    .coerceIn(BATTERY_TOP_TEXT_SCALE_MIN, BATTERY_TOP_TEXT_SCALE_MAX),
            )
            .apply()
    }

    fun setBatteryTopTextWeight(weight: Int) {
        preferences
            .edit()
            .putInt(
                activeProfileKey(BATTERY_TOP_TEXT_WEIGHT_KEY),
                weight.coerceIn(BATTERY_TOP_TEXT_WEIGHT_MIN, BATTERY_TOP_TEXT_WEIGHT_MAX),
            )
            .apply()
    }

    fun setBatteryTopVerticalOffset(offset: Float) {
        val uiOffset =
            offset.coerceIn(
                BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
                BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
            )
        preferences
            .edit()
            .putFloat(
                activeProfileKey(BATTERY_TOP_VERTICAL_OFFSET_KEY),
                batteryTopVerticalOffsetRaw(uiOffset),
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
                activeProfileKey(BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
                (uiScale * BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE)
                    .coerceIn(
                        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
                        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
                    ),
            )
            .apply()
    }
}

internal fun SharedPreferences.readCombinedStatusContentLayout(): CombinedStatusContentLayout =
    CombinedStatusContentLayout.fromPersisted(
        getString(
            CONTENT_LAYOUT_KEY,
            CombinedStatusContentLayout.NETWORK_CENTER.persistedValue,
        ),
    )

internal fun combinedStatusProfileKey(
    layout: CombinedStatusContentLayout,
    baseKey: String,
): String = layout.persistedValue + "." + baseKey

private fun SharedPreferences.profileBoolean(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Boolean,
): Boolean {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getBoolean(profileKey, defaultValue)
    } else {
        getBoolean(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileFloat(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Float,
): Float {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getFloat(profileKey, defaultValue)
    } else {
        getFloat(baseKey, defaultValue)
    }
}

private fun SharedPreferences.profileInt(
    layout: CombinedStatusContentLayout,
    baseKey: String,
    defaultValue: Int,
): Int {
    val profileKey = combinedStatusProfileKey(layout, baseKey)
    return if (contains(profileKey)) {
        getInt(profileKey, defaultValue)
    } else {
        getInt(baseKey, defaultValue)
    }
}

internal fun SharedPreferences.readCombinedStatusVisualSettings(): CombinedStatusVisualSettings {
    val layout = readCombinedStatusContentLayout()
    return CombinedStatusVisualSettings(
        contentLayout = layout,
        mobileFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = false,
            ),
        centerFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = false,
            ),
        batteryTopReadoutEnabled =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_READOUT_ENABLED_KEY,
                defaultValue = false,
            ),
        batteryTopTextFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = true,
            ),
        batteryTopChargingIconEnabled =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
                defaultValue = true,
            ),
        batteryTopChargingIconFollowsBatteryColor =
            profileBoolean(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
                defaultValue = true,
            ),
        batteryTopTextScale =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_SCALE_KEY,
                defaultValue = batteryTopTextScaleDefault(layout),
            ),
        batteryTopTextWeight =
            profileInt(
                layout = layout,
                baseKey = BATTERY_TOP_TEXT_WEIGHT_KEY,
                defaultValue = BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
            ),
        batteryTopVerticalOffset =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_VERTICAL_OFFSET_KEY,
                defaultValue = BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
            ),
        batteryTopChargingIconScale =
            profileFloat(
                layout = layout,
                baseKey = BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                defaultValue = batteryTopChargingIconScaleDefault(layout),
            ),
    ).normalized()
}

internal fun SharedPreferences.Editor.putCombinedStatusVisualSettings(
    settings: CombinedStatusVisualSettings,
): SharedPreferences.Editor {
    val normalized = settings.normalized()
    val layout = normalized.contentLayout
    return putString(
        CONTENT_LAYOUT_KEY,
        layout.persistedValue,
    ).putBoolean(
        combinedStatusProfileKey(layout, MOBILE_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.mobileFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, CENTER_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.centerFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_READOUT_ENABLED_KEY),
        normalized.batteryTopReadoutEnabled,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.batteryTopTextFollowsBatteryColor,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_ENABLED_KEY),
        normalized.batteryTopChargingIconEnabled,
    ).putBoolean(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY),
        normalized.batteryTopChargingIconFollowsBatteryColor,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_SCALE_KEY),
        normalized.batteryTopTextScale,
    ).putInt(
        combinedStatusProfileKey(layout, BATTERY_TOP_TEXT_WEIGHT_KEY),
        normalized.batteryTopTextWeight,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_VERTICAL_OFFSET_KEY),
        normalized.batteryTopVerticalOffset,
    ).putFloat(
        combinedStatusProfileKey(layout, BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
        normalized.batteryTopChargingIconScale,
    )
}

internal const val COMBINED_STATUS_VISUAL_PREFS_NAME = "combined_status_visual"
internal const val CONTENT_LAYOUT_KEY = "content_layout"
internal const val MOBILE_FOLLOWS_BATTERY_COLOR_KEY = "mobile_follows_battery_color"
internal const val CENTER_FOLLOWS_BATTERY_COLOR_KEY = "center_follows_battery_color"
internal const val BATTERY_TOP_READOUT_ENABLED_KEY = "battery_top_readout_enabled"
internal const val BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_text_follows_battery_color"
internal const val BATTERY_TOP_CHARGING_ICON_ENABLED_KEY =
    "battery_top_charging_icon_enabled"
internal const val BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY =
    "battery_top_charging_icon_follows_battery_color"
internal const val BATTERY_TOP_TEXT_SCALE_KEY = "battery_top_text_scale"
internal const val BATTERY_TOP_TEXT_WEIGHT_KEY = "battery_top_text_weight"
internal const val BATTERY_TOP_VERTICAL_OFFSET_KEY = "battery_top_vertical_offset"
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_KEY = "battery_top_charging_icon_scale"
internal const val RUNTIME_REMOTE_PREFS_NAME = "CombinedStatusRuntimeConfig"

private val PROFILE_VISUAL_BASE_KEYS =
    setOf(
        MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
        CENTER_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_READOUT_ENABLED_KEY,
        BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
        BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
        BATTERY_TOP_TEXT_SCALE_KEY,
        BATTERY_TOP_TEXT_WEIGHT_KEY,
        BATTERY_TOP_VERTICAL_OFFSET_KEY,
        BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
    )

// Persisted text scale remains in the pre-521 physical scale.
internal const val BATTERY_TOP_TEXT_UI_SCALE_REFERENCE = 1.3f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_TEXT_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_TEXT_SCALE_DEFAULT =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_TEXT_SCALE_MIN =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MIN
internal const val BATTERY_TOP_TEXT_SCALE_MAX =
    BATTERY_TOP_TEXT_UI_SCALE_REFERENCE * BATTERY_TOP_TEXT_UI_SCALE_MAX
internal const val BATTERY_TOP_TEXT_WEIGHT_DEFAULT = 900
internal const val BATTERY_TOP_TEXT_WEIGHT_MIN = 400
internal const val BATTERY_TOP_TEXT_WEIGHT_MAX = 1400
// Runtime/persisted offset is physical canonical displacement. Device review
// established that the previous +3 position is the intended user-facing zero.
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE = 3f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MIN = -10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_UI_MAX = 10f
internal const val BATTERY_TOP_VERTICAL_OFFSET_DEFAULT =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
internal const val BATTERY_TOP_VERTICAL_OFFSET_MIN =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MIN
internal const val BATTERY_TOP_VERTICAL_OFFSET_MAX =
    BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE + BATTERY_TOP_VERTICAL_OFFSET_UI_MAX

// Runtime/persisted charging scale remains a physical multiplier.
// Build 522's user-facing 110% (1.5 × 1.10 = 1.65 physical) becomes
// Build 523's user-facing/default 100% reference.
private const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE_LEGACY = 1.5f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE = 1.65f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN = 0.4f
internal const val BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX = 1.6f
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE
internal const val BATTERY_TOP_CHARGING_ICON_SCALE_MIN =
    BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE * BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN
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
                activeProfileKey(BATTERY_TOP_CHARGING_ICON_SCALE_KEY),
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

internal fun batteryTopTextUiScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    when (layout) {
        CombinedStatusContentLayout.NETWORK_CENTER -> 1f
        CombinedStatusContentLayout.BATTERY_CENTER -> 1.2f
    }

internal fun batteryTopChargingIconUiScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    when (layout) {
        CombinedStatusContentLayout.NETWORK_CENTER -> 1f
        CombinedStatusContentLayout.BATTERY_CENTER -> 1.2f
    }

internal fun batteryTopTextScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    (
        BATTERY_TOP_TEXT_UI_SCALE_REFERENCE *
            batteryTopTextUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_TEXT_SCALE_MIN,
        BATTERY_TOP_TEXT_SCALE_MAX,
    )

internal fun batteryTopChargingIconScaleDefault(
    layout: CombinedStatusContentLayout,
): Float =
    (
        BATTERY_TOP_CHARGING_ICON_UI_SCALE_REFERENCE *
            batteryTopChargingIconUiScaleDefault(layout)
    ).coerceIn(
        BATTERY_TOP_CHARGING_ICON_SCALE_MIN,
        BATTERY_TOP_CHARGING_ICON_SCALE_MAX,
    )

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


internal fun batteryTopVerticalOffsetUi(rawOffset: Float): Float =
    (rawOffset - BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE)
        .coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        )

internal fun batteryTopVerticalOffsetRaw(uiOffset: Float): Float =
    (
        uiOffset.coerceIn(
            BATTERY_TOP_VERTICAL_OFFSET_UI_MIN,
            BATTERY_TOP_VERTICAL_OFFSET_UI_MAX,
        ) + BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE
    ).coerceIn(
        BATTERY_TOP_VERTICAL_OFFSET_MIN,
        BATTERY_TOP_VERTICAL_OFFSET_MAX,
    )
