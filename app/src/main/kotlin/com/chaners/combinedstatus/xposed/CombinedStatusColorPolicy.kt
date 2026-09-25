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
        val nativeParticipantTint =
            tintState.statusIconTint
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?: tintState.appliedTint
        val batteryTint =
            when (model.batteryVisualMode) {
                BatteryVisualMode.CHARGING,
                BatteryVisualMode.POWER_SAVE,
                BatteryVisualMode.PERFORMANCE,
                BatteryVisualMode.EXTREME_POWER_SAVE ->
                    model.batteryModeTint
                        ?.takeIf { color -> (color ushr 24) != 0 }
                        ?: nativeParticipantTint
                BatteryVisualMode.NORMAL -> nativeParticipantTint
            }

        return CombinedStatusColors(
            centerTint =
                if (visualSettings.centerFollowsBatteryColor) {
                    batteryTint
                } else {
                    nativeParticipantTint
                },
            mobileTint =
                if (visualSettings.mobileFollowsBatteryColor) {
                    batteryTint
                } else {
                    nativeParticipantTint
                },
            batteryTint = batteryTint,
        )
    }
}
