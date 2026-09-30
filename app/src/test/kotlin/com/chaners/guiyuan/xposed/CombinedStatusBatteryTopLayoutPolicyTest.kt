package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryTopLayoutPolicyTest {
    @Test
    fun positiveOffsetUsesWholeSliderRangeWithoutCrossingTopSafeBoundary() {
        val base = 13.5f
        val contentHeight = 18f
        val inset = 1.5f

        val zero =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = base,
                requestedOffset = 0f,
                positiveLimit = 30f,
                contentInkHeight = contentHeight,
                topSafeInset = inset,
            )
        val halfway =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = base,
                requestedOffset = 15f,
                positiveLimit = 30f,
                contentInkHeight = contentHeight,
                topSafeInset = inset,
            )
        val maximum =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = base,
                requestedOffset = 30f,
                positiveLimit = 30f,
                contentInkHeight = contentHeight,
                topSafeInset = inset,
            )

        assertEquals(base, zero, 0.0001f)
        assertTrue(halfway < zero)
        assertTrue(maximum < halfway)
        assertEquals(contentHeight / 2f + inset, maximum, 0.0001f)
    }

    @Test
    fun negativeOffsetKeepsRequestedDownwardDistance() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 13.5f,
                requestedOffset = -30f,
                positiveLimit = 30f,
                contentInkHeight = 18f,
                topSafeInset = 1.5f,
            )

        assertEquals(43.5f, center, 0.0001f)
    }

    @Test
    fun ringGapPaddingGrowsWithOpticalInkHeight() {
        val small =
            CombinedStatusBatteryTopLayoutPolicy.resolveRingGapPadding(
                contentInkHeight = 12f,
                ringStroke = 4f,
                basePadding = 3f,
                inkHeightRatio = 0.08f,
                ringStrokeRatio = 0.25f,
            )
        val large =
            CombinedStatusBatteryTopLayoutPolicy.resolveRingGapPadding(
                contentInkHeight = 24f,
                ringStroke = 4f,
                basePadding = 3f,
                inkHeightRatio = 0.08f,
                ringStrokeRatio = 0.25f,
            )

        assertTrue(large > small)
    }
}
