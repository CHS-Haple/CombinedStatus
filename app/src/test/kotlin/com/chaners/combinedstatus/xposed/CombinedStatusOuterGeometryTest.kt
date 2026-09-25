package com.chaners.combinedstatus.xposed

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusOuterGeometryTest {
    @Test
    fun fourDotsRemainMirrorSymmetric() {
        val angles =
            (0 until 4)
                .map(CombinedStatusOuterGeometry::bottomDotAngle)
                .map(Math::toDegrees)

        assertEquals(180.0, angles[0] + angles[3], 0.0001)
        assertEquals(180.0, angles[1] + angles[2], 0.0001)
    }

    @Test
    fun ringAndDotEdgeGapsRemainVisuallyBalanced() {
        val ascendingAngles =
            (3 downTo 0)
                .map(CombinedStatusOuterGeometry::bottomDotAngle)

        val leftRingGap =
            ringToDotGap(
                ringEndAngle = Math.toRadians(30.0),
                dotAngle = ascendingAngles.first(),
            )
        val rightRingGap =
            ringToDotGap(
                ringEndAngle = Math.toRadians(150.0),
                dotAngle = ascendingAngles.last(),
            )
        val dotGaps =
            ascendingAngles
                .zipWithNext()
                .map { (left, right) ->
                    dotToDotGap(left, right)
                }

        val gaps = listOf(leftRingGap) + dotGaps + rightRingGap
        val spread = gaps.maxOrNull()!! - gaps.minOrNull()!!

        assertTrue("edge-gap spread=$spread gaps=$gaps", spread < 0.01f)
    }

    private fun ringToDotGap(
        ringEndAngle: Double,
        dotAngle: Double,
    ): Float {
        val ringX =
            cos(ringEndAngle).toFloat() * CombinedStatusOuterGeometry.RING_RADIUS
        val ringY =
            sin(ringEndAngle).toFloat() * CombinedStatusOuterGeometry.RING_RADIUS
        val dotX =
            cos(dotAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val dotY =
            sin(dotAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val centerDistance =
            sqrt(
                (dotX - ringX) * (dotX - ringX) +
                    (dotY - ringY) * (dotY - ringY),
            )
        return centerDistance -
            CombinedStatusOuterGeometry.RING_STROKE / 2f -
            CombinedStatusOuterGeometry.MOBILE_DOT_RADIUS
    }

    private fun dotToDotGap(
        firstAngle: Double,
        secondAngle: Double,
    ): Float {
        val firstX =
            cos(firstAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val firstY =
            sin(firstAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val secondX =
            cos(secondAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val secondY =
            sin(secondAngle).toFloat() * CombinedStatusOuterGeometry.MOBILE_ORBIT_RADIUS
        val centerDistance =
            sqrt(
                (secondX - firstX) * (secondX - firstX) +
                    (secondY - firstY) * (secondY - firstY),
            )
        return centerDistance - 2f * CombinedStatusOuterGeometry.MOBILE_DOT_RADIUS
    }
}
