package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusVisualSettings

internal data class CombinedStatusColors(
    val centerTint: Int,
    val mobileTint: Int,
    val batteryTint: Int,
    val batteryTextTint: Int,
    val chargingIconTint: Int,
)

internal object CombinedStatusColorPolicy {
    fun resolve(
        model: CombinedStatusRenderModel,
        tintState: CombinedStatusTintState,
        visualSettings: CombinedStatusVisualSettings = CombinedStatusVisualSettings(),
        batteryColorPreferences: CombinedStatusBatteryColorPreferences? = null,
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
                preferences =
                    batteryColorPreferences
                        ?: CombinedStatusBatteryColorPolicy.preferencesFor(visualSettings),
            )

        return CombinedStatusColors(
            centerTint =
                if (visualSettings.centerFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            mobileTint =
                if (visualSettings.mobileFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            batteryTint = batteryTint,
            batteryTextTint =
                if (visualSettings.batteryTopTextFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            chargingIconTint =
                if (visualSettings.batteryTopChargingIconFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
        )
    }
}
