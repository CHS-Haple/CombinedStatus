package com.chaners.combinedstatus.xposed

import android.view.View

internal object SystemUiHomeCarrierMetrics {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val BASE_SLOT_WIDTH_RESOURCE = "battery_meter_width"

    fun resolveBaseSlotWidthPx(anchor: View): Int? {
        val resources = anchor.resources
        val resourceId =
            resources.getIdentifier(
                BASE_SLOT_WIDTH_RESOURCE,
                "dimen",
                SYSTEM_UI_PACKAGE,
            )
        if (resourceId == 0) {
            return null
        }
        return runCatching {
            resources.getDimensionPixelSize(resourceId)
        }.getOrNull()?.takeIf { width -> width > 0 }
    }
}
