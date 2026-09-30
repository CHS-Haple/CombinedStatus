package com.chaners.guiyuan.xposed

import kotlin.math.max
import kotlin.math.min

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
        positiveLimit: Float,
        contentInkHeight: Float,
        minimumSafeTopY: Float,
    ): Float {
        if (requestedOffset <= 0f || positiveLimit <= 0f) {
            return baseCenterY - requestedOffset
        }

        val minimumSafeCenterY =
            minimumSafeTopY +
                contentInkHeight.coerceAtLeast(0f) / 2f
        val maximumSafeRise =
            (baseCenterY - minimumSafeCenterY).coerceAtLeast(0f)
        val requestedRise =
            requestedOffset.coerceIn(0f, positiveLimit)
        return baseCenterY - min(requestedRise, maximumSafeRise)
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
