package com.chaners.combinedstatus.xposed

internal enum class CombinedStatusRenderMode {
    PROJECTED,
    NATIVE_ONLY,
}

internal enum class CombinedStatusMotionOwnership {
    NONE,
    COMBINED_STATUS,
    SYSTEM_UI,
}

internal data class CombinedStatusLayoutSettings(
    val baseVisualSidePx: Float,
    val baseNeighborGapPx: Float,
    val userScale: Float,
) {
    init {
        require(baseVisualSidePx > 0f && baseVisualSidePx.isFinite())
        require(baseNeighborGapPx >= 0f && baseNeighborGapPx.isFinite())
        require(userScale > 0f && userScale.isFinite())
    }
}

internal data class CombinedStatusHostLayout(
    val hostHeightPx: Float,
    val endAnchorPx: Float,
    val nativeSlotWidthPx: Float,
    val renderMode: CombinedStatusRenderMode,
    val motionOwnership: CombinedStatusMotionOwnership,
) {
    init {
        require(hostHeightPx > 0f && hostHeightPx.isFinite())
        require(endAnchorPx.isFinite())
        require(nativeSlotWidthPx >= 0f && nativeSlotWidthPx.isFinite())
    }
}

internal data class CombinedStatusResolvedLayout(
    val renderCombined: Boolean,
    val visualSidePx: Float,
    val neighborGapPx: Float,
    val requestedSlotWidthPx: Float,
    val appliedSlotWidthPx: Float,
    val visualLeftPx: Float,
    val visualTopPx: Float,
    val visualRightPx: Float,
    val visualBottomPx: Float,
    val slotLeftPx: Float,
    val slotRightPx: Float,
    val motionOwnership: CombinedStatusMotionOwnership,
)

internal object CombinedStatusLayoutPolicy {
    fun resolve(
        settings: CombinedStatusLayoutSettings,
        host: CombinedStatusHostLayout,
    ): CombinedStatusResolvedLayout {
        val visualSide = settings.baseVisualSidePx * settings.userScale
        val neighborGap = settings.baseNeighborGapPx * settings.userScale
        val requestedSlotWidth = visualSide + neighborGap

        val appliedSlotWidth = host.nativeSlotWidthPx

        val visualRight = host.endAnchorPx
        val visualLeft = visualRight - visualSide
        val visualTop = (host.hostHeightPx - visualSide) / 2f
        val visualBottom = visualTop + visualSide
        val slotRight = host.endAnchorPx
        val slotLeft = slotRight - appliedSlotWidth

        return CombinedStatusResolvedLayout(
            renderCombined = host.renderMode != CombinedStatusRenderMode.NATIVE_ONLY,
            visualSidePx = visualSide,
            neighborGapPx = neighborGap,
            requestedSlotWidthPx = requestedSlotWidth,
            appliedSlotWidthPx = appliedSlotWidth,
            visualLeftPx = visualLeft,
            visualTopPx = visualTop,
            visualRightPx = visualRight,
            visualBottomPx = visualBottom,
            slotLeftPx = slotLeft,
            slotRightPx = slotRight,
            motionOwnership = host.motionOwnership,
        )
    }
}

internal object CombinedStatusHomeLayoutResolver {
    fun resolve(
        hostWidthPx: Int,
        hostHeightPx: Int,
        nativeCarrierWidthPx: Int,
        isRtl: Boolean,
    ): CombinedStatusResolvedLayout? {
        if (hostWidthPx <= 0 || hostHeightPx <= 0 || nativeCarrierWidthPx <= 0) {
            return null
        }
        val carrierWidth = nativeCarrierWidthPx.coerceAtMost(hostWidthPx)
        return CombinedStatusLayoutPolicy.resolve(
            settings =
                CombinedStatusLayoutSettings(
                    baseVisualSidePx = minOf(carrierWidth, hostHeightPx).toFloat(),
                    baseNeighborGapPx = 0f,
                    userScale = 1f,
                ),
            host =
                CombinedStatusHostLayout(
                    hostHeightPx = hostHeightPx.toFloat(),
                    endAnchorPx = if (isRtl) carrierWidth.toFloat() else hostWidthPx.toFloat(),
                    nativeSlotWidthPx = carrierWidth.toFloat(),
                    renderMode = CombinedStatusRenderMode.PROJECTED,
                    motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
                ),
        )
    }
}
