package com.chaners.guiyuan.xposed

import kotlin.math.max

internal object CombinedStatusBatteryTopLayoutPolicy {
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
        return baseCenterY - maximumSafeRise * normalized
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
