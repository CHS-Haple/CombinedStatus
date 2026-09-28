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
}
