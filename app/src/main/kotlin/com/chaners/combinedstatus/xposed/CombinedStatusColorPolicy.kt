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
                BatteryVisualMode.CHARGING -> CHARGING_TINT
                BatteryVisualMode.POWER_SAVE -> POWER_SAVE_TINT
                BatteryVisualMode.PERFORMANCE -> PERFORMANCE_TINT
                BatteryVisualMode.EXTREME_POWER_SAVE ->
                    // Exact target color semantics are not yet runtime-verified.
                    nativeParticipantTint
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

    // Values are fingerprint-scoped to the verified HyperOS SystemUI target,
    // not generic MIUI theme constants.
    internal const val CHARGING_TINT = 0xff1cb753.toInt()
    internal const val POWER_SAVE_TINT = 0xffffb300.toInt()
    internal const val PERFORMANCE_TINT = 0xff2f80ed.toInt()
}
