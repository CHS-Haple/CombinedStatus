package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryRingTransitionPolicyTest {
    @Test
    fun transitionProgressFinishesRingAtFortyFivePercentWithoutJump() {
        assertEquals(
            0f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.225f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.45f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.6f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun remainingFractionUsesContinuousFrontLoadedCurve() {
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0f),
            0.0001f,
        )
        assertEquals(
            0.615319f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.25f),
            0.0001f,
        )
        assertEquals(
            0.179334f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(1f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(Float.NaN),
            0.0001f,
        )
    }

    @Test
    fun globalCurveKeepsBuild550EarlyPaceAndExtendsTailContinuously() {
        fun remainingAtGlobal(progress: Float): Float =
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(
                CombinedStatusBatteryRingTransitionPolicy.transitionProgress(progress),
            )

        assertEquals(0.7337591f, remainingAtGlobal(0.0875f), 0.0001f)
        assertEquals(0.34119043f, remainingAtGlobal(0.175f), 0.0001f)
        assertEquals(0.01148136f, remainingAtGlobal(0.35f), 0.0001f)
        assertEquals(0f, remainingAtGlobal(0.45f), 0.0001f)
    }

    @Test
    fun retractKeepsGrayPathAndBatteryFillOnSamePrefix() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 240f,
                        ),
                    ),
                batteryPercent = 75,
                progress = 0.5f,
            )

        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(1, result.background.size)
        assertEquals(150f, result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(1, result.active.size)
        assertEquals(150f, result.active.single().startDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun retractConsumesOrderedPathAcrossTopGap() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 90f,
                        ),
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 300f,
                            sweepDegrees = 90f,
                        ),
                    ),
                batteryPercent = 100,
                progress = 0.25f,
            )

        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.25f)
        assertEquals(2, result.background.size)
        assertEquals(90f, result.background[0].sweepDegrees, 0.0001f)
        assertEquals(180f * remaining - 90f, result.background[1].sweepDegrees, 0.0001f)
        assertEquals(result.background, result.active)
    }

    @Test
    fun nonePreservesBuild543ActiveLengthSemantics() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.NONE,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun rightExitPreservesBuild543ActiveLengthSemantics() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.RIGHT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f, result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(150f, result.active.single().startDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitClearsLeftSideFirst() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 100,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f + 240f * (1f - remaining), result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitPreservesBatterySemanticsByIntersection() {
        val progress = 0.35f
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(progress)
        val retainedStart = 240f * (1f - remaining)
        assertEquals(150f + retainedStart, result.active.single().startDegrees, 0.0001f)
        assertEquals((180f - retainedStart).coerceAtLeast(0f), result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun completedRetractLeavesNoTransitionRing() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 240f,
                        ),
                    ),
                batteryPercent = 63,
                progress = 1f,
            )

        assertTrue(result.background.isEmpty())
        assertTrue(result.active.isEmpty())
        assertEquals(0f, result.remainingFraction, 0.0001f)
    }
}
