package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusVisualSettingsTest {
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
