package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusVisualSettingsTest {
    @Test
    fun newGeometryControlsUseBoundedDefaults() {
        val settings = CombinedStatusVisualSettings()
        assertEquals(1f, settings.combinedScale, 0.0001f)
        assertEquals(1f, settings.ringStrokeScale, 0.0001f)
        assertEquals(1f, settings.wifiSizeScale, 0.0001f)
        assertEquals(1f, settings.mobileTypeSizeScale, 0.0001f)
        assertEquals(800, settings.mobileTypeWeight)

        val normalized =
            settings.copy(
                combinedScale = 9f,
                ringStrokeScale = 9f,
                wifiSizeScale = 9f,
                mobileTypeSizeScale = 9f,
                mobileTypeWeight = 5000,
            ).normalized()
        assertEquals(COMBINED_SCALE_MAX, normalized.combinedScale, 0.0001f)
        assertEquals(RING_STROKE_SCALE_MAX, normalized.ringStrokeScale, 0.0001f)
        assertEquals(WIFI_SIZE_SCALE_MAX, normalized.wifiSizeScale, 0.0001f)
        assertEquals(MOBILE_TYPE_SIZE_SCALE_MAX, normalized.mobileTypeSizeScale, 0.0001f)
        assertEquals(MOBILE_TYPE_WEIGHT_MAX, normalized.mobileTypeWeight)
    }

    @Test
    fun iosStylePaletteUsesExpectedSemanticDefaults() {
        assertEquals(0xFF34C759.toInt(), CombinedStatusIosStyleBatteryPalette.CHARGING)
        assertEquals(0xFFFFCC00.toInt(), CombinedStatusIosStyleBatteryPalette.POWER_SAVE)
        assertEquals(0xFF007AFF.toInt(), CombinedStatusIosStyleBatteryPalette.PERFORMANCE)
        assertEquals(0xFFFF9500.toInt(), CombinedStatusIosStyleBatteryPalette.SUPER_POWER_SAVE)
        assertEquals(0xFFFF3B30.toInt(), CombinedStatusIosStyleBatteryPalette.LOW)
        assertEquals(
            null,
            CombinedStatusIosStyleBatteryPalette.colorFor(CombinedStatusBatteryColorSlot.NORMAL),
        )
    }

    @Test
    fun customColorOverridesAreForcedOpaque() {
        val overrides =
            CombinedStatusBatteryColorOverrides().withColor(
                CombinedStatusBatteryColorSlot.CHARGING,
                0x0034C759,
            )
        assertEquals(0xFF34C759.toInt(), overrides.charging)
    }

    @Test
    fun previousPhysicalPlusThreeIsTheNewUserFacingZero() {
        assertEquals(
            0f,
            batteryTopVerticalOffsetUi(3f),
            0.0001f,
        )
        assertEquals(
            3f,
            batteryTopVerticalOffsetRaw(0f),
            0.0001f,
        )
    }

    @Test
    fun userFacingOffsetRangeIsPlusMinusTenAroundPhysicalReference() {
        assertEquals(-10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MIN, 0.0001f)
        assertEquals(10f, BATTERY_TOP_VERTICAL_OFFSET_UI_MAX, 0.0001f)
        assertEquals(
            -7f,
            batteryTopVerticalOffsetRaw(-10f),
            0.0001f,
        )
        assertEquals(
            13f,
            batteryTopVerticalOffsetRaw(10f),
            0.0001f,
        )
    }

    @Test
    fun offsetMappingClampsOnlyAtVisibleSliderEnds() {
        assertEquals(
            -10f,
            batteryTopVerticalOffsetUi(-30f),
            0.0001f,
        )
        assertEquals(
            10f,
            batteryTopVerticalOffsetUi(30f),
            0.0001f,
        )
    }

    @Test
    fun normalizedRuntimeOffsetUsesThePhysicalRangeBehindTheVisibleSlider() {
        val high =
            CombinedStatusVisualSettings(
                batteryTopVerticalOffset = 30f,
            ).normalized()
        val low =
            CombinedStatusVisualSettings(
                batteryTopVerticalOffset = -30f,
            ).normalized()

        assertEquals(13f, high.batteryTopVerticalOffset, 0.0001f)
        assertEquals(-7f, low.batteryTopVerticalOffset, 0.0001f)
    }


    @Test
    fun clearNotificationParticipatesInVisualRuntimeSync() {
        assertEquals(true, isCombinedStatusVisualPreferenceKey(null))
    }

    @Test
    fun allNewVisualKeysParticipateInRuntimeSync() {
        val keys =
            listOf(
                CONTENT_LAYOUT_KEY,
                BATTERY_TOP_TEXT_FOLLOWS_BATTERY_COLOR_KEY,
                BATTERY_TOP_CHARGING_ICON_ENABLED_KEY,
                BATTERY_TOP_CHARGING_ICON_FOLLOWS_BATTERY_COLOR_KEY,
            )

        keys.forEach { key ->
            assertEquals(true, isCombinedStatusVisualPreferenceKey(key))
        }
    }


    @Test
    fun layoutProfilesUseIndependentPersistedKeys() {
        assertEquals(
            "network_center.battery_top_text_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.NETWORK_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
        assertEquals(
            "battery_center.battery_top_text_scale",
            combinedStatusProfileKey(
                CombinedStatusContentLayout.BATTERY_CENTER,
                BATTERY_TOP_TEXT_SCALE_KEY,
            ),
        )
    }

    @Test
    fun profileKeysParticipateInRuntimeSync() {
        CombinedStatusContentLayout.entries.forEach { layout ->
            listOf(
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
                COMBINED_SCALE_KEY,
                RING_STROKE_SCALE_KEY,
                WIFI_SIZE_SCALE_KEY,
                MOBILE_TYPE_SIZE_SCALE_KEY,
                MOBILE_TYPE_WEIGHT_KEY,
            ).forEach { baseKey ->
                assertEquals(
                    true,
                    isCombinedStatusVisualPreferenceKey(
                        combinedStatusProfileKey(layout, baseKey),
                    ),
                )
            }
        }
    }

    @Test
    fun globalBatteryColorKeysParticipateInRuntimeSync() {
        listOf(
            BATTERY_COLOR_PRESET_KEY,
            BATTERY_COLOR_NORMAL_KEY,
            BATTERY_COLOR_POWER_SAVE_KEY,
            BATTERY_COLOR_PERFORMANCE_KEY,
            BATTERY_COLOR_SUPER_POWER_SAVE_KEY,
            BATTERY_COLOR_CHARGING_KEY,
            BATTERY_COLOR_LOW_KEY,
        ).forEach { key ->
            assertEquals(true, isCombinedStatusVisualPreferenceKey(key))
        }
    }

    @Test
    fun newBatteryVisualControlsKeepRequestedDefaults() {
        val settings = CombinedStatusVisualSettings()

        assertEquals(CombinedStatusContentLayout.NETWORK_CENTER, settings.contentLayout)
        assertEquals(true, settings.batteryTopTextFollowsBatteryColor)
        assertEquals(true, settings.batteryTopChargingIconEnabled)
        assertEquals(true, settings.batteryTopChargingIconFollowsBatteryColor)
    }

    @Test
    fun persistedLayoutFallsBackToNetworkCenter() {
        assertEquals(
            CombinedStatusContentLayout.NETWORK_CENTER,
            CombinedStatusContentLayout.fromPersisted("unknown"),
        )
        assertEquals(
            CombinedStatusContentLayout.BATTERY_CENTER,
            CombinedStatusContentLayout.fromPersisted("battery_center"),
        )
    }


    @Test
    fun batteryCenteredProfileDefaultsBothBatteryScalesToOneHundredTwentyPercent() {
        assertEquals(
            1.2f,
            batteryTopTextUiScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopTextUiScale(
                batteryTopTextScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(
                batteryTopChargingIconScaleDefault(CombinedStatusContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
    }


    @Test
    fun directBatteryCenteredSettingsConstructionAlsoUsesOneHundredTwentyPercentDefaults() {
        val settings =
            CombinedStatusVisualSettings(
                contentLayout = CombinedStatusContentLayout.BATTERY_CENTER,
            )

        assertEquals(
            1.2f,
            batteryTopTextUiScale(settings.batteryTopTextScale),
            0.0001f,
        )
        assertEquals(
            1.2f,
            batteryTopChargingIconUiScale(settings.batteryTopChargingIconScale),
            0.0001f,
        )
    }

    @Test
    fun networkCenteredProfileKeepsOneHundredPercentScaleDefaults() {
        assertEquals(
            1f,
            batteryTopTextUiScaleDefault(CombinedStatusContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            batteryTopChargingIconUiScaleDefault(CombinedStatusContentLayout.NETWORK_CENTER),
            0.0001f,
        )
    }

    @Test
    fun batteryTopScaleRangesAreFortyToOneHundredSixtyPercent() {
        assertEquals(0.4f, BATTERY_TOP_TEXT_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_TEXT_UI_SCALE_MAX, 0.0001f)
        assertEquals(0.4f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX, 0.0001f)

        assertEquals(
            0.4f,
            batteryTopTextUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            batteryTopTextUiScale(Float.MAX_VALUE),
            0.0001f,
        )
        assertEquals(
            0.4f,
            batteryTopChargingIconUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            batteryTopChargingIconUiScale(Float.MAX_VALUE),
            0.0001f,
        )
    }
}
