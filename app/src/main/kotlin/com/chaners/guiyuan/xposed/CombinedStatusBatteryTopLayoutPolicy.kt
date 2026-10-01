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
        contentInkHeight: Float,
        minimumSafeTopY: Float,
    ): Float =
        max(
            preferredCenterY - defaultOpticalRise.coerceAtLeast(0f),
            minimumSafeTopY +
                contentInkHeight.coerceAtLeast(0f) / 2f,
        )

    fun resolveCenterY(
        baseCenterY: Float,
        requestedOffset: Float,
        neutralOffset: Float,
        positiveLimit: Float,
        contentInkHeight: Float,
        minimumSafeTopY: Float,
    ): Float {
        if (
            !baseCenterY.isFinite() ||
            !requestedOffset.isFinite() ||
            !neutralOffset.isFinite() ||
            !positiveLimit.isFinite() ||
            !contentInkHeight.isFinite() ||
            !minimumSafeTopY.isFinite()
        ) {
            return baseCenterY
        }

        val minimumSafeCenterY =
            minimumSafeTopY +
                contentInkHeight.coerceAtLeast(0f) / 2f
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
