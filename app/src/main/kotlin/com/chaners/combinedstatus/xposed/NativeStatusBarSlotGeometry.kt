package com.chaners.combinedstatus.xposed

internal object NativeStatusBarSlotGeometry {
    internal data class Resolved(
        val containerWidth: Int,
        val statusIconsMeasuredWidth: Int,
        val privacyMeasuredWidth: Int,
        val slotWidth: Int,
        val slotHeight: Int,
    )

    internal fun resolve(
        containerWidth: Int,
        containerPaddingStart: Int,
        containerPaddingEnd: Int,
        statusIconsMeasuredWidth: Int,
        privacyMeasuredWidth: Int,
        containerHeight: Int,
    ): Resolved? {
        if (
            containerWidth <= 0 ||
            containerHeight <= 0 ||
            containerPaddingStart < 0 ||
            containerPaddingEnd < 0 ||
            statusIconsMeasuredWidth < 0 ||
            privacyMeasuredWidth < 0
        ) {
            return null
        }

        val contentWidth =
            containerWidth - containerPaddingStart - containerPaddingEnd
        val slotWidth =
            contentWidth -
                statusIconsMeasuredWidth -
                privacyMeasuredWidth
        if (slotWidth <= 0) {
            return null
        }

        return Resolved(
            containerWidth = containerWidth,
            statusIconsMeasuredWidth = statusIconsMeasuredWidth,
            privacyMeasuredWidth = privacyMeasuredWidth,
            slotWidth = slotWidth,
            slotHeight = containerHeight,
        )
    }
}
