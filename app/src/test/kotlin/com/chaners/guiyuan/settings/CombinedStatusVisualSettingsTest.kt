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
}
