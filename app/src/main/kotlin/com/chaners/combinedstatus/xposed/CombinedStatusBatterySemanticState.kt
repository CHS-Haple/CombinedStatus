package com.chaners.combinedstatus.xposed

internal enum class CombinedStatusBatterySemanticState {
    NORMAL,
    CHARGING,
    POWER_SAVE,
    PERFORMANCE,
    LOW,
}

internal object SystemUiBatterySemanticPolicy {
    fun fromNativeProgressStatus(
        statusName: String?,
    ): CombinedStatusBatterySemanticState? {
        val normalized = statusName?.removeSuffix("_DARK") ?: return null
        return when (normalized) {
            "CHARGING",
            "QUICK_CHARGING",
            "PERF_CHARGE_MODE",
            "PERF_QC_MODE",
            -> CombinedStatusBatterySemanticState.CHARGING
            "POWER_SAVE" -> CombinedStatusBatterySemanticState.POWER_SAVE
            "PERFORMANCE_MODE" -> CombinedStatusBatterySemanticState.PERFORMANCE
            "LOW" -> CombinedStatusBatterySemanticState.LOW
            "NORMAL" -> CombinedStatusBatterySemanticState.NORMAL
            else -> null
        }
    }
}
