package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardHostResolverTest {
    @Test
    fun steadyKeyguardAcceptsKeyguardAndShadeLockedOnly() {
        assertTrue(SystemUiKeyguardHostResolver.isSteadyKeyguardSurface(SystemUiSceneStateSource.Surface.KEYGUARD))
        assertTrue(SystemUiKeyguardHostResolver.isSteadyKeyguardSurface(SystemUiSceneStateSource.Surface.SHADE_LOCKED))
        assertFalse(SystemUiKeyguardHostResolver.isSteadyKeyguardSurface(SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR))
        assertFalse(SystemUiKeyguardHostResolver.isSteadyKeyguardSurface(SystemUiSceneStateSource.Surface.UNKNOWN))
    }

    @Test
    fun resolverAcceptsOnlyPinnedKeyguardHostClass() {
        assertTrue(SystemUiKeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"))
        assertFalse(SystemUiKeyguardHostResolver.isKeyguardHostClassName("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"))
    }
    @Test
    fun keyguardStatusPresentationVisibilityUsesNativeVisualState() {
        assertTrue(
            SystemUiKeyguardHostResolver.resolveStatusPresentationVisible(
                hostShown = true,
                statusIconsShown = true,
                statusIconsAlpha = 1f,
                batteryShown = true,
                batteryAlpha = 0f,
            ),
        )
        assertTrue(
            SystemUiKeyguardHostResolver.resolveStatusPresentationVisible(
                hostShown = true,
                statusIconsShown = true,
                statusIconsAlpha = 0f,
                batteryShown = true,
                batteryAlpha = 1f,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostResolver.resolveStatusPresentationVisible(
                hostShown = true,
                statusIconsShown = true,
                statusIconsAlpha = 0f,
                batteryShown = true,
                batteryAlpha = 0f,
            ),
        )
        assertFalse(
            SystemUiKeyguardHostResolver.resolveStatusPresentationVisible(
                hostShown = false,
                statusIconsShown = true,
                statusIconsAlpha = 1f,
                batteryShown = true,
                batteryAlpha = 1f,
            ),
        )
    }

}
