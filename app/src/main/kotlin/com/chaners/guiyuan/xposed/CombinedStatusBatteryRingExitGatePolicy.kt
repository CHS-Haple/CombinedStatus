package com.chaners.guiyuan.xposed

import kotlin.math.asin

internal object CombinedStatusBatteryRingExitGatePolicy {
    private const val EARLY_OPEN_WIDTH_FRACTION = 0.65f

    fun minimumConsumedSweep(
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
        exitDirection: CombinedStatusBatteryRingTransitionPolicy.ExitDirection,
    ): Float {
        if (exitDirection != CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT) {
            return 0f
        }
        if (
            !contentLeft.isFinite() ||
            !contentTop.isFinite() ||
            !contentRight.isFinite() ||
            !contentBottom.isFinite() ||
            !ringCenterX.isFinite() ||
            !ringCenterY.isFinite() ||
            !ringRadius.isFinite() ||
            ringRadius <= 0f ||
            !ringStroke.isFinite() ||
            !visualClearance.isFinite() ||
            !startDegrees.isFinite() ||
            !maxSweep.isFinite() ||
            maxSweep <= 0f
        ) {
            return 0f
        }

        val clearance =
            visualClearance.coerceAtLeast(0f) +
                ringStroke.coerceAtLeast(0f) / 2f
        val left = contentLeft - clearance
        val top = contentTop - clearance
        val right = contentRight + clearance
        val bottom = contentBottom + clearance
        val width = right - left
        val height = bottom - top
        if (width <= 0f || height <= 0f) return 0f

        val innerRadius = (ringRadius - clearance).coerceAtLeast(0f)
        if (innerRadius <= 0f) return 0f

        // Start opening before the optical leading edge reaches the inner ring.
        // The lead distance scales with the live CENTER width, so Wi-Fi, 5G/5GA,
        // airplane and no-SIM do not share one hard-coded timing window.
        val innerLeft = ringCenterX - innerRadius
        val leadDistance =
            (width * EARLY_OPEN_WIDTH_FRACTION)
                .coerceAtLeast(0.001f)
        val gateStartLeft = innerLeft + leadDistance
        val rawProgress =
            ((gateStartLeft - left) / leadDistance)
                .coerceIn(0f, 1f)
        if (rawProgress <= 0f) return 0f

        // A fast ease-out makes the portal yield early, while the normal
        // Build-543/544 retract remains the authority for the rest of the ring.
        val gateProgress = 1f - (1f - rawProgress) * (1f - rawProgress)

        // Keep a stable side portal after CENTER has crossed the ring. Derive
        // its angular size from the live optical height rather than current Y,
        // so the opening cannot shrink again while CENTER continues down-left.
        val halfHeight = height / 2f
        val halfAngle =
            Math.toDegrees(
                asin((halfHeight / ringRadius).coerceIn(0f, 1f)).toDouble(),
            ).toFloat()
        val portalEndDegrees = 180f + halfAngle
        val fullConsumedSweep =
            (portalEndDegrees - startDegrees)
                .coerceIn(0f, maxSweep)

        return fullConsumedSweep * gateProgress
    }
}
