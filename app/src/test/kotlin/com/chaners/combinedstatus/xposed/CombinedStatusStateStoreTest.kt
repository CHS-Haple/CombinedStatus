package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusStateStoreTest {
    @Test
    fun airplaneExitStartsFreshMobileRecoveryAndClearsCachedSignal() {
        CombinedStatusStateStore.restoreHotReloadState(null)
        CombinedStatusStateStore.updateAirplaneMode(true)
        CombinedStatusStateStore.updateMobile(
            CombinedStatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = CombinedStatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 1,
                signal = SignalStrength.Level(4),
            ),
        )

        val snapshot =
            CombinedStatusStateStore.updateAirplaneMode(false)
                ?: error("expected airplane transition")

        assertTrue(snapshot.mobileRecoveryPending)
        assertTrue(snapshot.mobile[4]?.signal is SignalStrength.Unknown)
        assertNull(snapshot.mobile[4]?.signalResId)
    }

    @Test
    fun unavailableSignalDoesNotFinishAirplaneRecovery() {
        CombinedStatusStateStore.restoreHotReloadState(null)
        CombinedStatusStateStore.updateAirplaneMode(true)
        CombinedStatusStateStore.updateAirplaneMode(false)
        CombinedStatusStateStore.updateMobile(
            CombinedStatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = CombinedStatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 2,
                signal = SignalStrength.Unavailable,
            ),
        )

        val completed =
            CombinedStatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = 4,
                mobileTypeReady = true,
                mobileDataEnabled = true,
            )

        assertNull(completed)
        assertTrue(CombinedStatusStateStore.snapshot().mobileRecoveryPending)
    }

    @Test
    fun freshSignalAndMobileTypeFinishAirplaneRecovery() {
        CombinedStatusStateStore.restoreHotReloadState(null)
        CombinedStatusStateStore.updateAirplaneMode(true)
        CombinedStatusStateStore.updateAirplaneMode(false)
        CombinedStatusStateStore.updateMobile(
            CombinedStatusStateStore.MobileIconUpdate(
                subscriptionId = 4,
                kind = CombinedStatusStateStore.MobileIconKind.SIGNAL,
                resourceId = 3,
                signal = SignalStrength.Level(3),
            ),
        )

        val completed =
            CombinedStatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = 4,
                mobileTypeReady = true,
                mobileDataEnabled = true,
            ) ?: error("expected recovery completion")

        assertFalse(completed.mobileRecoveryPending)
    }

    @Test
    fun partialBatteryUpdateKeepsKnownHyperOsModeState() {
        CombinedStatusStateStore.restoreHotReloadState(null)
        CombinedStatusStateStore.updateBattery(
            CombinedStatusStateStore.BatteryState(
                percent = 80,
                charging = false,
                powerSave = true,
                extremePowerSave = false,
                performanceMode = false,
            ),
        )

        val updated =
            CombinedStatusStateStore.updateBattery(
                CombinedStatusStateStore.BatteryState(
                    percent = 79,
                    charging = false,
                ),
            ) ?: error("expected battery level update")

        assertTrue(updated.battery?.powerSave == true)
        assertTrue(updated.battery?.extremePowerSave == false)
        assertTrue(updated.battery?.performanceMode == false)
    }

}
