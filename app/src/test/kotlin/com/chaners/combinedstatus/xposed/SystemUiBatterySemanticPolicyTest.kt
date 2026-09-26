package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemUiBatterySemanticPolicyTest {
    @Test
    fun nativeChargingFamiliesResolveToChargingState() {
        listOf(
            "CHARGING",
            "QUICK_CHARGING",
            "PERF_CHARGE_MODE",
            "PERF_QC_MODE",
            "PERF_QC_MODE_DARK",
        ).forEach { name ->
            assertEquals(
                CombinedStatusBatterySemanticState.CHARGING,
                SystemUiBatterySemanticPolicy.fromNativeProgressStatus(name),
            )
        }
    }

    @Test
    fun nativeStatesMapWithoutReinterpretingPriority() {
        assertEquals(
            CombinedStatusBatterySemanticState.POWER_SAVE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("POWER_SAVE"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.PERFORMANCE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("PERFORMANCE_MODE"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.LOW,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("LOW"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.NORMAL,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("NORMAL_DARK"),
        )
        assertNull(SystemUiBatterySemanticPolicy.fromNativeProgressStatus("UNKNOWN"))
    }

    @Test
    fun fallbackMatchesExactTargetPriority() {
        assertEquals(
            CombinedStatusBatterySemanticState.CHARGING,
            SystemUiBatterySemanticPolicy.fallback(5, true, true, true),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.POWER_SAVE,
            SystemUiBatterySemanticPolicy.fallback(5, false, true, true),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.PERFORMANCE,
            SystemUiBatterySemanticPolicy.fallback(5, false, false, true),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.LOW,
            SystemUiBatterySemanticPolicy.fallback(5, false, false, false),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.NORMAL,
            SystemUiBatterySemanticPolicy.fallback(80, false, false, false),
        )
    }
}
