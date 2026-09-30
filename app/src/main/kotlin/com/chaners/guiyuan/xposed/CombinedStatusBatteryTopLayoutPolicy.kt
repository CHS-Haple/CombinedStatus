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
        positiveLimit: Float,
    ): Float {
        if (!requestedOffset.isFinite()) return baseCenterY
        if (requestedOffset <= 0f || positiveLimit <= 0f) {
            return baseCenterY - requestedOffset
        }

        // Default placement is already bounded by resolveOpticalBaseCenterY().
        // A user-requested offset must stay literal instead of being silently
        // collapsed by a second clip-safety ceiling. The View/display clip is
        // the physical boundary and remains observable to the user.
        return baseCenterY -
            requestedOffset.coerceIn(0f, positiveLimit)
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
