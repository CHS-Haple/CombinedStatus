package com.chaners.guiyuan.xposed

internal sealed interface CombinedStatusBatteryColorSource {
    data object SystemDefault : CombinedStatusBatteryColorSource
    data object FollowStatusIcon : CombinedStatusBatteryColorSource
    data class Custom(val color: Int) : CombinedStatusBatteryColorSource
}

internal data class CombinedStatusBatteryColorPreferences(
    val normal: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val charging: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val powerSave: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val performance: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val low: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
) {
    fun sourceFor(
        state: CombinedStatusBatterySemanticState,
    ): CombinedStatusBatteryColorSource =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL -> normal
            CombinedStatusBatterySemanticState.CHARGING -> charging
            CombinedStatusBatterySemanticState.POWER_SAVE -> powerSave
            CombinedStatusBatterySemanticState.PERFORMANCE -> performance
            CombinedStatusBatterySemanticState.LOW -> low
        }
}

internal object CombinedStatusBatteryColorPolicy {
    fun resolve(
        state: CombinedStatusBatterySemanticState,
        systemSemanticColor: Int?,
        statusIconTint: Int,
        preferences: CombinedStatusBatteryColorPreferences =
            CombinedStatusBatteryColorPreferences(),
    ): Int {
        val systemDefault =
            if (state == CombinedStatusBatterySemanticState.NORMAL) {
                statusIconTint
            } else {
                systemSemanticColor
                    ?.takeIf(::isVisibleColor)
                    ?: statusIconTint
            }
        return when (val source = preferences.sourceFor(state)) {
            CombinedStatusBatteryColorSource.SystemDefault -> systemDefault
            CombinedStatusBatteryColorSource.FollowStatusIcon -> statusIconTint
            is CombinedStatusBatteryColorSource.Custom ->
                source.color
                    .takeIf(::isVisibleColor)
                    ?: systemDefault
        }
    }

    private fun isVisibleColor(color: Int): Boolean =
        color ushr 24 != 0
}
