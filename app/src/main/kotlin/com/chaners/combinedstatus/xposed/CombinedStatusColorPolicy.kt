package com.chaners.combinedstatus.xposed

import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings

internal data class CombinedStatusColors(
    val centerTint: Int,
    val mobileTint: Int,
    val batteryTint: Int,
)

internal object CombinedStatusColorPolicy {
    fun resolve(
        model: CombinedStatusRenderModel,
        tintState: CombinedStatusTintState,
        visualSettings: CombinedStatusVisualSettings = CombinedStatusVisualSettings(),
    ): CombinedStatusColors {
        val statusIconTint =
            tintState.statusIconTint
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?: tintState.appliedTint
        val batteryTint =
            if (model.charging) {
                CHARGING_TINT
            } else {
                tintState.appliedTint
            }

        return CombinedStatusColors(
            centerTint =
                if (visualSettings.centerFollowsBatteryColor) {
                    batteryTint
                } else {
                    statusIconTint
                },
            mobileTint =
                if (visualSettings.mobileFollowsBatteryColor) {
                    batteryTint
                } else {
                    statusIconTint
                },
            batteryTint = batteryTint,
        )
    }

    internal const val CHARGING_TINT = 0xff1cb753.toInt()
}
