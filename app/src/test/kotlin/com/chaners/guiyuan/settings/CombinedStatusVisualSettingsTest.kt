package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusVisualSettingsTest {
    @Test
    fun batteryTopVerticalOffsetUiZeroMapsToFormerPhysicalPlusThree() {
        assertEquals(
            BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE,
            batteryTopVerticalOffsetRaw(0f),
            0.0001f,
        )
        assertEquals(
            0f,
            batteryTopVerticalOffsetUi(BATTERY_TOP_VERTICAL_OFFSET_UI_REFERENCE),
            0.0001f,
        )
    }

    @Test
    fun batteryTopVerticalOffsetPositiveUiRangeRemainsLiteralAfterRebase() {
        assertEquals(
            13f,
            batteryTopVerticalOffsetRaw(10f),
            0.0001f,
        )
        assertEquals(
            33f,
            batteryTopVerticalOffsetRaw(30f),
            0.0001f,
        )
    }

    @Test
    fun batteryTopVerticalOffsetKeepsFormerPersistedLowerBoundReadable() {
        val normalized =
            CombinedStatusVisualSettings(
                batteryTopVerticalOffset = -30f,
            ).normalized()

        assertEquals(-30f, normalized.batteryTopVerticalOffset, 0.0001f)
    }
}
