package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryTopLayoutPolicyTest {
    @Test
    fun canonicalZeroIsNotTreatedAsPhysicalViewTop() {
        val minimumTop =
            CombinedStatusBatteryTopLayoutPolicy.resolveMinimumSafeTopY(
                transformScale = 0.875f,
                transformOffsetY = 32f,
            )

        assertEquals(-36.57143f, minimumTop, 0.0001f)
    }

    @Test
    fun zeroUsesUpwardOpticalDefaultWithoutArtificialHeadroomShift() {
        val base =
            CombinedStatusBatteryTopLayoutPolicy.resolveOpticalBaseCenterY(
                preferredCenterY = 16f,
                defaultOpticalRise = 1.5f,
                contentInkHeight = 18f,
                minimumSafeTopY = -36f,
            )

        assertEquals(14.5f, base, 0.0001f)
    }

    @Test
    fun opticalDefaultClampsOnlyAtActualPhysicalTopSafety() {
        val base =
            CombinedStatusBatteryTopLayoutPolicy.resolveOpticalBaseCenterY(
                preferredCenterY = 16f,
                defaultOpticalRise = 1.5f,
                contentInkHeight = 30f,
                minimumSafeTopY = 1f,
            )

        assertEquals(16f, base, 0.0001f)
    }

    @Test
    fun positiveOffsetUsesRequestedDistanceWhileRealHeadroomExists() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 20f,
                positiveLimit = 30f,
                contentInkHeight = 18f,
                minimumSafeTopY = -36f,
            )

        assertEquals(-5.5f, center, 0.0001f)
    }

    @Test
    fun positiveMaximumCanTravelAboveCanonicalZeroWithoutClippingView() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 30f,
                positiveLimit = 30f,
                contentInkHeight = 18f,
                minimumSafeTopY = -36f,
            )

        assertEquals(-15.5f, center, 0.0001f)
        assertTrue(center < 0f)
    }

    @Test
    fun positiveOffsetStopsOnlyWhenActualViewTopWouldClipContent() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 30f,
                positiveLimit = 30f,
                contentInkHeight = 18f,
                minimumSafeTopY = -10f,
            )

        assertEquals(-1f, center, 0.0001f)
    }

    @Test
    fun negativeOffsetKeepsRequestedDownwardDistance() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 13.5f,
                requestedOffset = -30f,
                positiveLimit = 30f,
                contentInkHeight = 18f,
                minimumSafeTopY = -36f,
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
