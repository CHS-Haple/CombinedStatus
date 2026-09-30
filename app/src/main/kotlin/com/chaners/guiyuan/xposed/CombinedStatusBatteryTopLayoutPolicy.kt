package com.chaners.guiyuan.xposed

import kotlin.math.max

internal object CombinedStatusBatteryTopLayoutPolicy {
    fun resolveOpticalBaseCenterY(
        preferredCenterY: Float,
        defaultOpticalRise: Float,
        contentInkHeight: Float,
        topSafeInset: Float,
    ): Float =
        max(
            preferredCenterY - defaultOpticalRise.coerceAtLeast(0f),
            contentInkHeight.coerceAtLeast(0f) / 2f +
                topSafeInset.coerceAtLeast(0f),
        )

    fun resolveCenterY(
        baseCenterY: Float,
        requestedOffset: Float,
        positiveLimit: Float,
        contentInkHeight: Float,
        topSafeInset: Float,
    ): Float {
        if (requestedOffset <= 0f || positiveLimit <= 0f) {
            return baseCenterY - requestedOffset
        }

        val minimumSafeCenterY =
            contentInkHeight.coerceAtLeast(0f) / 2f +
                topSafeInset.coerceAtLeast(0f)
        val maximumSafeRise =
            (baseCenterY - minimumSafeCenterY).coerceAtLeast(0f)
        val normalized =
            (requestedOffset / positiveLimit).coerceIn(0f, 1f)
        // The top surface has limited physical headroom. Use an ease-out
        // response so small/medium positive values visibly move instead of
        // spending most of the slider near the neutral position, while +max
        // still lands exactly on the safe top boundary.
        val responsive =
            1f - (1f - normalized) * (1f - normalized)
        return baseCenterY - maximumSafeRise * responsive
    }

    fun resolveRingGapPadding(
        contentInkHeight: Float,
        ringStroke: Float,
        basePadding: Float,
        inkHeightRatio: Float,
        ringStrokeRatio: Float,
    ): Float =
        max(
            0f,
            basePadding +
                contentInkHeight.coerceAtLeast(0f) * inkHeightRatio.coerceAtLeast(0f) +
                ringStroke.coerceAtLeast(0f) * ringStrokeRatio.coerceAtLeast(0f),
        )
}
