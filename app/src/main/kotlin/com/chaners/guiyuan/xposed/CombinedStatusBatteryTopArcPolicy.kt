package com.chaners.guiyuan.xposed

import kotlin.math.asin
import kotlin.math.min

internal object CombinedStatusBatteryTopArcPolicy {
    internal data class Arc(
        val startDegrees: Float,
        val sweepDegrees: Float,
    )

    internal data class Segments(
        val active: List<Arc>,
        val inactive: List<Arc>,
    )

    fun gapSweepDegrees(
        groupWidth: Float,
        ringRadius: Float,
        horizontalPadding: Float,
    ): Float {
        if (groupWidth <= 0f || ringRadius <= 0f) return 0f
        val halfChord = (groupWidth / 2f + horizontalPadding).coerceAtLeast(0f)
        val ratio = (halfChord / ringRadius).coerceIn(0f, MAX_GAP_CHORD_RATIO)
        return Math.toDegrees((2f * asin(ratio)).toDouble())
            .toFloat()
            .coerceAtMost(MAX_GAP_SWEEP_DEGREES)
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

    private const val MAX_GAP_CHORD_RATIO = 0.92f
    // 82° was sufficient for the original small readout, but clips the
    // measured optical request of the rebased three-digit/charging group.
    // Keep a bounded shoulder on both sides while allowing optical clearance.
    private const val MAX_GAP_SWEEP_DEGREES = 118f
}
