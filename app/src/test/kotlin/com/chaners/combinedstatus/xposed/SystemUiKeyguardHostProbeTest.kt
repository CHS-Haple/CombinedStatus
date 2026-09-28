package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardHostProbeTest {
    @Test
    fun probeRunsOnlyForKeyguardSurface() {
        assertTrue(
            SystemUiKeyguardHostProbe.shouldProbe(
                SystemUiSceneStateSource.Surface.KEYGUARD,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldProbe(
                SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldProbe(
                SystemUiSceneStateSource.Surface.SHADE_LOCKED,
            ),
        )
    }

    @Test
    fun hostGuardAcceptsOnlyPinnedMiuiKeyguardHost() {
        assertTrue(
            SystemUiKeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            ),
        )
    }

    @Test
    fun sampleFreezesOnlyAfterPositiveReadyTopology() {
        assertTrue(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
                selectedAsRealSystemIcons = true,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
                selectedAsRealSystemIcons = false,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 0,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 105,
                selectedAsRealSystemIcons = true,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = false,
                batteryCarrierWidthPx = 105,
                selectedAsRealSystemIcons = true,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostProbe.shouldFreezeSample(
                hostAttached = true,
                systemIconsAttached = true,
                systemIconsWidth = 105,
                batteryMatchesSceneSource = true,
                batteryCarrierWidthPx = 0,
                selectedAsRealSystemIcons = true,
            ),
        )
    }
}
