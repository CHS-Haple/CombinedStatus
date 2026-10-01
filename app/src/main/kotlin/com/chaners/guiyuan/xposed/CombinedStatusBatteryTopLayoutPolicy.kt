package com.chaners.guiyuan.xposed

import kotlin.math.max

internal object CombinedStatusBatteryTopLayoutPolicy {
    fun resolveMinimumSafeTopY(
        transformScale: Float,
        transformOffsetY: Float,
        viewTopY: Float = 0f,
    ): Float {
        if (
            !transformScale.isFinite() ||
            transformScale <= 0f ||
            !transformOffsetY.isFinite() ||
            !viewTopY.isFinite()
        ) {
            return 0f
        }
        return (viewTopY - transformOffsetY) / transformScale
    }

    fun resolveOpticalBaseCenterY(
        preferredCenterY: Float,
        defaultOpticalRise: Float,
    ): Float {
        if (!preferredCenterY.isFinite()) return 0f
        val rise =
            defaultOpticalRise
                .takeIf(Float::isFinite)
                ?.coerceAtLeast(0f)
                ?: 0f
        return preferredCenterY - rise
    }

    fun resolveCenterY(
        baseCenterY: Float,
        requestedOffset: Float,
        neutralOffset: Float,
        positiveLimit: Float,
        contentTopExtent: Float,
        minimumSafeTopY: Float,
    ): Float {
        if (
            !baseCenterY.isFinite() ||
            !requestedOffset.isFinite() ||
            !neutralOffset.isFinite() ||
            !positiveLimit.isFinite() ||
            !contentTopExtent.isFinite() ||
            !minimumSafeTopY.isFinite()
        ) {
            return baseCenterY
        }

        val minimumSafeCenterY =
            minimumSafeTopY +
                contentTopExtent.coerceAtLeast(0f)
        val neutralCenterY =
            max(
                baseCenterY - neutralOffset,
                minimumSafeCenterY,
            )

        if (requestedOffset <= neutralOffset) {
            return max(
                baseCenterY - requestedOffset,
                minimumSafeCenterY,
            )
        }
        if (positiveLimit <= neutralOffset) {
            return neutralCenterY
        }

        val normalizedPositive =
            (
                (requestedOffset - neutralOffset) /
                    (positiveLimit - neutralOffset)
            ).coerceIn(0f, 1f)
        val availableRise =
            (neutralCenterY - minimumSafeCenterY).coerceAtLeast(0f)
        return neutralCenterY - availableRise * normalizedPositive
    }
}
