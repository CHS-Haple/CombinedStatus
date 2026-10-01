package com.chaners.guiyuan.xposed

import kotlin.math.max
import kotlin.math.min

internal object CombinedStatusBatteryRingTransitionPolicy {
    internal enum class ExitDirection { NONE, LEFT, RIGHT }

    internal data class Segments(
        val background: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val active: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        val remainingFraction: Float,
    )

    fun remainingFraction(progress: Float): Float {
        val p = progress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
        val eased = p * p * (3f - 2f * p)
        return 1f - eased
    }

    fun resolve(
        drawableArcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        batteryPercent: Int,
        progress: Float,
        exitDirection: ExitDirection = ExitDirection.NONE,
    ): Segments {
        val remaining = remainingFraction(progress)
        val totalSweep = drawableArcs.sumOf { it.sweepDegrees.coerceAtLeast(0f).toDouble() }.toFloat()
        if (totalSweep <= 0f || remaining <= 0f) {
            return Segments(emptyList(), emptyList(), remaining)
        }

        val retainedSweep = totalSweep * remaining
        val retainedStart =
            when (exitDirection) {
                ExitDirection.LEFT -> totalSweep - retainedSweep
                ExitDirection.NONE, ExitDirection.RIGHT -> 0f
            }
        val retainedEnd = retainedStart + retainedSweep
        val activeEnd = totalSweep * batteryPercent.coerceIn(0, 100) / 100f
        return Segments(
            background = slice(drawableArcs, retainedStart, retainedEnd),
            active = slice(drawableArcs, max(retainedStart, 0f), min(retainedEnd, activeEnd)),
            remainingFraction = remaining,
        )
    }

    private fun slice(
        arcs: List<CombinedStatusBatteryTopArcPolicy.Arc>,
        rangeStart: Float,
        rangeEnd: Float,
    ): List<CombinedStatusBatteryTopArcPolicy.Arc> {
        if (rangeEnd <= rangeStart) return emptyList()
        var cursor = 0f
        val result = ArrayList<CombinedStatusBatteryTopArcPolicy.Arc>(arcs.size)
        arcs.forEach { arc ->
            val sweep = arc.sweepDegrees.coerceAtLeast(0f)
            val arcStart = cursor
            val arcEnd = cursor + sweep
            val visibleStart = max(arcStart, rangeStart)
            val visibleEnd = min(arcEnd, rangeEnd)
            if (visibleEnd > visibleStart) {
                result += CombinedStatusBatteryTopArcPolicy.Arc(
                    startDegrees = arc.startDegrees + visibleStart - arcStart,
                    sweepDegrees = visibleEnd - visibleStart,
                )
            }
            cursor = arcEnd
        }
        return result
    }
}
