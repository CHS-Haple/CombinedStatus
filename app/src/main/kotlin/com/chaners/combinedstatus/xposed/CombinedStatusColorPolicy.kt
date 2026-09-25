package com.chaners.combinedstatus.xposed

internal data class CombinedStatusColors(
    val primaryTint: Int,
    val batteryTint: Int,
)

internal object CombinedStatusColorPolicy {
    fun resolve(
        model: CombinedStatusRenderModel,
        tintState: CombinedStatusTintState,
    ): CombinedStatusColors =
        CombinedStatusColors(
            primaryTint =
                tintState.statusIconTint
                    ?.takeIf { color -> (color ushr 24) != 0 }
                    ?: tintState.appliedTint,
            batteryTint =
                if (model.charging) {
                    CHARGING_TINT
                } else {
                    tintState.appliedTint
                },
        )

    internal const val CHARGING_TINT = 0xff1cb753.toInt()
}
