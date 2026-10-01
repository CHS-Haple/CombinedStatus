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
    fun opticalDefaultStillClampsOnlyTheAutomaticBasePlacement() {
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
    fun uiZeroKeepsAcceptedNeutralPositionWhenItIsSafe() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentInkHeight = 20f,
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
                contentInkHeight = 20f,
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
                contentInkHeight = 20f,
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
                contentInkHeight = 20f,
                minimumSafeTopY = -2f,
            )

        assertEquals(21.5f, center, 0.0001f)
    }

    @Test
    fun unsafeNeutralIsClampedOnlyAtRealPhysicalTop() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 3f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentInkHeight = 30f,
                minimumSafeTopY = 0f,
            )

        assertEquals(15f, center, 0.0001f)
    }

    @Test
    fun requestPastPositiveLimitStillStopsAtPhysicalTop() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 100f,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentInkHeight = 20f,
                minimumSafeTopY = -2f,
            )

        assertEquals(8f, center, 0.0001f)
        assertTrue(center < 11.5f)
    }

    @Test
    fun nonFiniteManualOffsetFallsBackToAutomaticBase() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = Float.NaN,
                neutralOffset = 3f,
                positiveLimit = 13f,
                contentInkHeight = 20f,
                minimumSafeTopY = -2f,
            )

        assertEquals(14.5f, center, 0.0001f)
    }
}
