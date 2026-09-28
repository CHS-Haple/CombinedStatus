package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardSceneSourceTest {
    @Test
    fun hostClassGuardAcceptsOnlyPinnedMiuiKeyguardHost() {
        assertTrue(
            SystemUiKeyguardSceneSource.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
            ),
        )
        assertFalse(
            SystemUiKeyguardSceneSource.isKeyguardHostClassName(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
            ),
        )
    }
}
