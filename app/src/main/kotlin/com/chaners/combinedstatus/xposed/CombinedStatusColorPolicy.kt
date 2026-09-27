package com.chaners.combinedstatus.xposed

import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings

internal data class CombinedStatusColors(
    val centerTint: Int,
    val mobileTint: Int,
    val batteryTint: Int,
    val centerUsesBatteryTint: Boolean = false,
)

internal object CombinedStatusColorPolicy {
    fun resolve(
        model: CombinedStatusRenderModel,
        tintState: CombinedStatusTintState,
        visualSettings: CombinedStatusVisualSettings = CombinedStatusVisualSettings(),
        batteryColorPreferences: CombinedStatusBatteryColorPreferences =
            CombinedStatusBatteryColorPreferences(),
    ): CombinedStatusColors {
        val nativeParticipantTint =
            tintState.statusIconTint
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?: tintState.appliedTint
        val batteryTint =
            CombinedStatusBatteryColorPolicy.resolve(
                state = model.batterySemanticState,
                systemSemanticColor = model.batterySystemSemanticColor,
                statusIconTint = nativeParticipantTint,
                preferences = batteryColorPreferences,
            )

        return CombinedStatusColors(
            centerTint =
                if (visualSettings.centerFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            mobileTint =
                if (visualSettings.mobileFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            batteryTint = batteryTint,
            centerUsesBatteryTint = visualSettings.centerFollowsBatteryColor,
        )
    }
}
