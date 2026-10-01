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
    fun opticalBaseDoesNotConsumeManualPositiveHeadroom() {
        val base =
            CombinedStatusBatteryTopLayoutPolicy.resolveOpticalBaseCenterY(
                preferredCenterY = 16f,
                defaultOpticalRise = 1.5f,
            )

        assertEquals(14.5f, base, 0.0001f)
    }

    @Test
    fun uiZeroKeepsAcceptedNeutralPositionWhenItIsSafe() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(11.5f, center, 0.0001f)
    }

    @Test
    fun positiveUiRangeConsumesAllRemainingPhysicalHeadroom() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 13f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(8f, center, 0.0001f)
    }

    @Test
    fun positiveUiMidpointMapsToHalfRemainingPhysicalHeadroom() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 8f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(9.75f, center, 0.0001f)
    }

    @Test
    fun negativeUiRangeKeepsLiteralDownwardTravel() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = -7f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(21.5f, center, 0.0001f)
    }

    @Test
    fun oversizedContentMovesNeutralDownToTheRealSafeTop() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 15f,
                minimumSafeTopY = 0f,
            )

        assertEquals(15f, center, 0.0001f)
    }

    @Test
    fun largerChargingTopExtentReducesPositiveTravelInsteadOfClipping() {
        val compact =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 20f,
                requestedOffset = 13f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )
        val enlargedCharging =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 20f,
                requestedOffset = 13f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 14f,
                minimumSafeTopY = -2f,
            )

        assertEquals(8f, compact, 0.0001f)
        assertEquals(12f, enlargedCharging, 0.0001f)
        assertTrue(enlargedCharging > compact)
    }

    @Test
    fun requestPastPositiveLimitStillStopsAtPhysicalTop() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 100f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(8f, center, 0.0001f)
        assertTrue(center < 11.5f)
    }

    @Test
    fun nonFiniteManualOffsetFallsBackToOpticalBase() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = Float.NaN,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentTopExtent = 10f,
                minimumSafeTopY = -2f,
            )

        assertEquals(14.5f, center, 0.0001f)
    }
}
