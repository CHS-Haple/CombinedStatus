package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryTopArcPolicyTest {
    @Test
    fun halfBatteryFillsFirstVisibleShoulderBeforeGap() {
        val result = CombinedStatusBatteryTopArcPolicy.resolve(
            batteryPercent = 50,
            startDegrees = 150f,
            maxSweep = 240f,
            gapCenterDegrees = 270f,
            gapSweepDegrees = 60f,
        )

        assertEquals(
            listOf(CombinedStatusBatteryTopArcPolicy.Arc(150f, 90f)),
            result.active,
        )
        assertEquals(
            listOf(CombinedStatusBatteryTopArcPolicy.Arc(300f, 90f)),
            result.inactive,
        )
    }

    @Test
    fun fullBatteryFillsBothSidesWithoutDrawingThroughGap() {
        val result = CombinedStatusBatteryTopArcPolicy.resolve(
            batteryPercent = 100,
            startDegrees = 150f,
            maxSweep = 240f,
            gapCenterDegrees = 270f,
            gapSweepDegrees = 60f,
        )

        assertEquals(
            listOf(
                CombinedStatusBatteryTopArcPolicy.Arc(150f, 90f),
                CombinedStatusBatteryTopArcPolicy.Arc(300f, 90f),
            ),
            result.active,
        )
        assertTrue(result.inactive.isEmpty())
    }

    @Test
    fun widerReadoutRequestsWiderButBoundedGap() {
        val narrow = CombinedStatusBatteryTopArcPolicy.gapSweepDegrees(
            groupWidth = 18f,
            ringRadius = 50f,
            horizontalPadding = 4f,
        )
        val wide = CombinedStatusBatteryTopArcPolicy.gapSweepDegrees(
            groupWidth = 48f,
            ringRadius = 50f,
            horizontalPadding = 4f,
        )

        assertTrue(wide > narrow)
        assertTrue(wide <= 118f)
    }

    @Test
    fun opticalPaddingCanExceedLegacyEightyTwoDegreeCap() {
        val gap = CombinedStatusBatteryTopArcPolicy.gapSweepDegrees(
            groupWidth = 48f,
            ringRadius = 50f,
            horizontalPadding = 12f,
        )

        assertTrue(gap > 82f)
        assertTrue(gap <= 118f)
    }
}
