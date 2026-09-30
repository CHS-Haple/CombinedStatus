package com.chaners.guiyuan.xposed

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.min

internal object CombinedStatusBatteryTopArcPolicy {
    internal data class Arc(
        val startDegrees: Float,
        val sweepDegrees: Float,
    )

    internal data class Gap(
        val centerDegrees: Float,
        val sweepDegrees: Float,
    )

    internal data class Segments(
        val active: List<Arc>,
        val inactive: List<Arc>,
    )

    fun resolveGap(
        contentLeft: Float,
        contentTop: Float,
        contentRight: Float,
        contentBottom: Float,
        ringCenterX: Float,
        ringCenterY: Float,
        ringRadius: Float,
        ringStroke: Float,
        visualClearance: Float,
        startDegrees: Float,
        maxSweep: Float,
    ): Gap {
        if (
            !contentLeft.isFinite() ||
            !contentTop.isFinite() ||
            !contentRight.isFinite() ||
            !contentBottom.isFinite() ||
            !ringCenterX.isFinite() ||
            !ringCenterY.isFinite() ||
            !ringRadius.isFinite() ||
            ringRadius <= 0f ||
            !startDegrees.isFinite() ||
            !maxSweep.isFinite() ||
            maxSweep <= 0f
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        val clearance =
            visualClearance.coerceAtLeast(0f) +
                ringStroke.coerceAtLeast(0f) / 2f
        val left = contentLeft - clearance
        val top = contentTop - clearance
        val right = contentRight + clearance
        val bottom = contentBottom + clearance
        val ringTop = ringCenterY - ringRadius
        val ringBottom = ringCenterY + ringRadius

        // If the visible content envelope cannot intersect the ring at all,
        // no ring cutout is needed.
        if (
            top > bottom ||
            bottom <= ringTop ||
            top >= ringBottom ||
            right <= ringCenterX ||
            left >= ringCenterX
        ) {
            return Gap(TOP_DEGREES, 0f)
        }

        val availableLeft =
            (TOP_DEGREES - startDegrees)
                .coerceAtLeast(0f)
        val availableRight =
            (startDegrees + maxSweep - TOP_DEGREES)
                .coerceAtLeast(0f)

        val leftRatio =
            ((ringCenterX - left) / ringRadius)
                .coerceIn(0f, 1f)
        val rightRatio =
            ((right - ringCenterX) / ringRadius)
                .coerceIn(0f, 1f)
        val verticalRatio =
            ((ringCenterY - bottom) / ringRadius)
                .coerceIn(-1f, 1f)

        val verticalHalf =
            Math.toDegrees(acos(verticalRatio).toDouble()).toFloat()
        val leftHalf =
            min(
                Math.toDegrees(asin(leftRatio).toDouble()).toFloat(),
                verticalHalf,
            ).coerceAtMost(availableLeft)
        val rightHalf =
            min(
                Math.toDegrees(asin(rightRatio).toDouble()).toFloat(),
                verticalHalf,
            ).coerceAtMost(availableRight)

        if (leftHalf <= 0f && rightHalf <= 0f) {
            return Gap(TOP_DEGREES, 0f)
        }

        val gapStart = TOP_DEGREES - leftHalf
        val gapEnd = TOP_DEGREES + rightHalf
        return Gap(
            centerDegrees = (gapStart + gapEnd) / 2f,
            sweepDegrees = (gapEnd - gapStart).coerceAtLeast(0f),
        )
    }

    fun resolve(
        batteryPercent: Int,
        startDegrees: Float,
        maxSweep: Float,
        gapCenterDegrees: Float,
        gapSweepDegrees: Float,
    ): Segments {
        if (maxSweep <= 0f) return Segments(emptyList(), emptyList())

        val endDegrees = startDegrees + maxSweep
        val halfGap = gapSweepDegrees.coerceAtLeast(0f) / 2f
        val gapStart = (gapCenterDegrees - halfGap).coerceIn(startDegrees, endDegrees)
        val gapEnd = (gapCenterDegrees + halfGap).coerceIn(startDegrees, endDegrees)

        val drawable = ArrayList<Arc>(2)
        if (gapStart > startDegrees) drawable += Arc(startDegrees, gapStart - startDegrees)
        if (gapEnd < endDegrees) drawable += Arc(gapEnd, endDegrees - gapEnd)
        if (drawable.isEmpty()) return Segments(emptyList(), emptyList())

        val totalVisibleSweep = drawable.sumOf { it.sweepDegrees.toDouble() }.toFloat()
        var activeRemaining = totalVisibleSweep * batteryPercent.coerceIn(0, 100) / 100f
        val active = ArrayList<Arc>(2)
        val inactive = ArrayList<Arc>(2)

        drawable.forEach { arc ->
            val activeSweep = min(arc.sweepDegrees, activeRemaining.coerceAtLeast(0f))
            if (activeSweep > 0f) active += Arc(arc.startDegrees, activeSweep)
            val inactiveSweep = arc.sweepDegrees - activeSweep
            if (inactiveSweep > 0f) {
                inactive += Arc(arc.startDegrees + activeSweep, inactiveSweep)
            }
            activeRemaining -= activeSweep
        }

        return Segments(active = active, inactive = inactive)
    }

    private const val TOP_DEGREES = 270f
}
