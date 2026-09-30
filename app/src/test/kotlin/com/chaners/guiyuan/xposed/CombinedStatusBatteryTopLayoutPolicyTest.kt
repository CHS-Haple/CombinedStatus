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
    fun manualPositiveOffsetIsLiteralAndNotClampedByHiddenHeadroom() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 13f,
                positiveLimit = 13f,
            )

        assertEquals(1.5f, center, 0.0001f)
    }

    @Test
    fun manualNegativeOffsetIsLiteral() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = -7f,
                positiveLimit = 13f,
            )

        assertEquals(21.5f, center, 0.0001f)
    }

    @Test
    fun nonFiniteManualOffsetFallsBackToAutomaticBase() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = Float.NaN,
                positiveLimit = 13f,
            )

        assertEquals(14.5f, center, 0.0001f)
    }

    @Test
    fun positiveOffsetStillHonorsExplicitConfiguredRange() {
        val center =
            CombinedStatusBatteryTopLayoutPolicy.resolveCenterY(
                baseCenterY = 14.5f,
                requestedOffset = 100f,
                positiveLimit = 13f,
            )

        assertEquals(1.5f, center, 0.0001f)
        assertTrue(center < 14.5f)
    }
}
