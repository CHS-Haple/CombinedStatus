package com.chaners.combinedstatus.xposed

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusOuterGeometryTest {
    @Test
    fun ringAndDotsScaleProportionally() {
        for (scale in TEST_SCALES) {
            val geometry = CombinedStatusOuterGeometry.resolve(scale)

            assertEquals(
                CombinedStatusOuterGeometry.BASE_RING_STROKE * scale,
                geometry.ringStroke,
                0.0001f,
            )
            assertEquals(
                CombinedStatusOuterGeometry.BASE_MOBILE_DOT_RADIUS * scale,
                geometry.mobileDotRadius,
                0.0001f,
            )
        }
    }

    @Test
    fun unavailableMarkScalesWithOuterWeightWithoutChangingItsIndependentProportions() {
        for (scale in TEST_SCALES) {
            val geometry = CombinedStatusOuterGeometry.resolve(scale)

            assertEquals(
                CombinedStatusOuterGeometry.BASE_UNAVAILABLE_MARK_STROKE * scale,
                geometry.unavailableMarkStroke,
                0.0001f,
            )
            assertEquals(
                CombinedStatusOuterGeometry.BASE_UNAVAILABLE_MARK_HALF_EXTENT * scale,
                geometry.unavailableMarkHalfExtent,
                0.0001f,
            )
        }
    }

    @Test
    fun fourDotsRemainMirrorSymmetricAcrossSupportedScales() {
        for (scale in TEST_SCALES) {
            val geometry = CombinedStatusOuterGeometry.resolve(scale)
            val angles =
                (0 until 4)
                    .map(geometry::bottomDotAngle)
                    .map(Math::toDegrees)

            assertEquals(
                "outer symmetry scale=$scale",
                180.0,
                angles[0] + angles[3],
                0.0001,
            )
            assertEquals(
                "inner symmetry scale=$scale",
                180.0,
                angles[1] + angles[2],
                0.0001,
            )
        }
    }

    @Test
    fun fiveVisualEdgeGapsStayBalancedAcrossSupportedScales() {
        for (scale in TEST_SCALES) {
            val geometry = CombinedStatusOuterGeometry.resolve(scale)
            val ascendingAngles =
                (3 downTo 0)
                    .map(geometry::bottomDotAngle)

            val leftRingGap =
                ringToDotGap(
                    ringEndAngle = Math.toRadians(30.0),
                    dotAngle = ascendingAngles.first(),
                    geometry = geometry,
                )
            val rightRingGap =
                ringToDotGap(
                    ringEndAngle = Math.toRadians(150.0),
                    dotAngle = ascendingAngles.last(),
                    geometry = geometry,
                )
            val dotGaps =
                ascendingAngles
                    .zipWithNext()
                    .map { (left, right) ->
                        dotToDotGap(
                            firstAngle = left,
                            secondAngle = right,
                            geometry = geometry,
                        )
                    }

            val gaps = listOf(leftRingGap) + dotGaps + rightRingGap
            val spread = gaps.maxOrNull()!! - gaps.minOrNull()!!

            assertTrue(
                "edge-gap spread=$spread scale=$scale gaps=$gaps",
                spread < 0.01f,
            )
            assertTrue(
                "edge gaps must stay positive scale=$scale gaps=$gaps",
                gaps.all { it > 0f },
            )
        }
    }

    @Test
    fun invalidOrOutOfRangeWeightScaleFallsBackOrClampsSafely() {
        assertEquals(
            CombinedStatusOuterGeometry.DEFAULT_WEIGHT_SCALE,
            CombinedStatusOuterGeometry.normalizeWeightScale(Float.NaN),
            0f,
        )
        assertEquals(
            CombinedStatusOuterGeometry.MIN_WEIGHT_SCALE,
            CombinedStatusOuterGeometry.normalizeWeightScale(0f),
            0f,
        )
        assertEquals(
            CombinedStatusOuterGeometry.MAX_WEIGHT_SCALE,
            CombinedStatusOuterGeometry.normalizeWeightScale(5f),
            0f,
        )
    }

    private fun ringToDotGap(
        ringEndAngle: Double,
        dotAngle: Double,
        geometry: CombinedStatusOuterGeometry.Resolved,
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
            geometry.ringStroke / 2f -
            geometry.mobileDotRadius
    }

    private fun dotToDotGap(
        firstAngle: Double,
        secondAngle: Double,
        geometry: CombinedStatusOuterGeometry.Resolved,
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
        return centerDistance - 2f * geometry.mobileDotRadius
    }

    private companion object {
        val TEST_SCALES =
            listOf(
                0.60f,
                1.00f,
                CombinedStatusOuterGeometry.DEFAULT_WEIGHT_SCALE,
                1.50f,
                2.00f,
            )
    }
}
