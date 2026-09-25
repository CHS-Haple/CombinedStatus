package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryVisualModePolicyTest {
    @Test
    fun chargingHasHighestPriority() {
        assertEquals(
            BatteryVisualMode.CHARGING,
            resolve(
                charging = true,
                powerSave = true,
                extremePowerSave = true,
                performanceMode = true,
            ),
        )
    }

    @Test
    fun extremePowerSavePrecedesPowerSaveAndPerformance() {
        assertEquals(
            BatteryVisualMode.EXTREME_POWER_SAVE,
            resolve(
                charging = false,
                powerSave = true,
                extremePowerSave = true,
                performanceMode = true,
            ),
        )
    }

    @Test
    fun powerSavePrecedesPerformance() {
        assertEquals(
            BatteryVisualMode.POWER_SAVE,
            resolve(
                charging = false,
                powerSave = true,
                extremePowerSave = false,
                performanceMode = true,
            ),
        )
    }

    @Test
    fun performanceIsUsedWhenSaverModesAreInactive() {
        assertEquals(
            BatteryVisualMode.PERFORMANCE,
            resolve(
                charging = false,
                powerSave = false,
                extremePowerSave = false,
                performanceMode = true,
            ),
        )
    }

    @Test
    fun unknownModeCallbacksRemainNormalUntilEvidenceArrives() {
        assertEquals(
            BatteryVisualMode.NORMAL,
            resolve(
                charging = false,
                powerSave = null,
                extremePowerSave = null,
                performanceMode = null,
            ),
        )
    }

    private fun resolve(
        charging: Boolean,
        powerSave: Boolean?,
        extremePowerSave: Boolean?,
        performanceMode: Boolean?,
    ): BatteryVisualMode =
        CombinedStatusRenderModel.resolveBatteryVisualMode(
            CombinedStatusStateStore.BatteryState(
                percent = 80,
                charging = charging,
                powerSave = powerSave,
                extremePowerSave = extremePowerSave,
                performanceMode = performanceMode,
            ),
        )
}
