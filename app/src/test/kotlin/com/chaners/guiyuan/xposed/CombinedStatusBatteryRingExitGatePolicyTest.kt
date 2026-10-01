package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryRingExitGatePolicyTest {
    @Test
    fun leftGateStaysClosedBeforeOpticalEnvelopeApproachesRing() {
        val sweep =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = 40f,
                contentTop = 45f,
                contentRight = 80f,
                contentBottom = 75f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        assertEquals(0f, sweep, 0.0001f)
    }

    @Test
    fun leftGateIsFullyOpenWhenLeadingEdgeReachesInnerRing() {
        val sweep =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = 10f,
                contentTop = 50f,
                contentRight = 30f,
                contentBottom = 70f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        assertTrue(sweep > 40f)
        assertTrue(sweep < 45f)
    }

    @Test
    fun widerEnvelopeStartsYieldingEarlier() {
        val narrow =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = 24f,
                contentTop = 50f,
                contentRight = 44f,
                contentBottom = 70f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val wide =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = 24f,
                contentTop = 50f,
                contentRight = 64f,
                contentBottom = 70f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        assertTrue(wide > narrow)
    }

    @Test
    fun gateRemainsFullyOpenAfterEnvelopePassesRing() {
        val sweep =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = -20f,
                contentTop = 50f,
                contentRight = 0f,
                contentBottom = 70f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        assertTrue(sweep > 40f)
    }

    @Test
    fun nonLeftDirectionsDoNotInjectGateTiming() {
        val right =
            CombinedStatusBatteryRingExitGatePolicy.minimumConsumedSweep(
                contentLeft = 10f,
                contentTop = 50f,
                contentRight = 30f,
                contentBottom = 70f,
                ringCenterX = 60f,
                ringCenterY = 60f,
                ringRadius = 50f,
                ringStroke = 0f,
                visualClearance = 0f,
                startDegrees = 150f,
                maxSweep = 240f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.RIGHT,
            )
        assertEquals(0f, right, 0.0001f)
    }
}
