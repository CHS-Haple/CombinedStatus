package com.chaners.guiyuan.xposed

import kotlin.math.min

internal object CombinedStatusBatteryRingTransitionPolicy {
    internal data class Segments(
        val background: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val active: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val remainingFraction: Float,
    )

    fun remainingFraction(progress: Float): Float {
        val p =
            progress
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f
        val eased = p * p * (3f - 2f * p)
        return 1f - eased
    }

    fun resolve(
        drawableArcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        batteryPercent: Int,
        progress: Float,
    ): Segments {
        if (drawableArcs.isEmpty()) {
            return Segments(
                background = emptyList(),
                active = emptyList(),
                remainingFraction = remainingFraction(progress),
            )
        }

        val remaining = remainingFraction(progress)
        val totalSweep =
            drawableArcs
                .sumOf { arc -> arc.sweepDegrees.coerceAtLeast(0f).toDouble() }
                .toFloat()
        if (totalSweep <= 0f || remaining <= 0f) {
            return Segments(
                background = emptyList(),
                active = emptyList(),
                remainingFraction = remaining,
            )
        }

        val backgroundBudget = totalSweep * remaining
        val activeBudget =
            totalSweep *
                batteryPercent.coerceIn(0, 100) /
                100f *
                remaining

        return Segments(
            background = prefix(drawableArcs, backgroundBudget),
            active = prefix(drawableArcs, activeBudget),
            remainingFraction = remaining,
        )
    }

    private fun prefix(
        arcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        sweepBudget: Float,
    ): List<CombinedStatusBatteryTopArcPolicy.Arc> {
        var remaining = sweepBudget.coerceAtLeast(0f)
        if (remaining <= 0f) return emptyList()

        val result = ArrayList<CombinedStatusBatteryTopArcPolicy.Arc>(arcs.size)
        arcs.forEach { arc ->
            if (remaining <= 0f) return@forEach
            val sweep = arc.sweepDegrees.coerceAtLeast(0f)
            if (sweep <= 0f) return@forEach
            val visible = min(sweep, remaining)
            if (visible > 0f) {
                result +=
                    CombinedStatusBatteryTopArcPolicy.Arc(
                        startDegrees = arc.startDegrees,
                        sweepDegrees = visible,
                    )
            }
            remaining -= visible
        }
        return result
    }
}
