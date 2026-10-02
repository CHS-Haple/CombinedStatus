package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemUiBatterySemanticPolicyTest {
    @Test
    fun mapsNativeProgressStatusesWithoutReconstructingPriority() {
        assertEquals(
            CombinedStatusBatterySemanticState.CHARGING,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("QUICK_CHARGING"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.CHARGING,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("PERF_CHARGE_MODE"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.POWER_SAVE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("POWER_SAVE"),
        )
        assertEquals(
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE,
            SystemUiBatterySemanticPolicy.fromNativeProgressStatus("SUPER_POWER_SAVE"),
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
}
