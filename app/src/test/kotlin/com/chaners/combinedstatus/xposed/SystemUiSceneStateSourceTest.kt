package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiSceneStateSourceTest {
    @Test
    fun exactStatusBarStatesMapToExpectedSurfaces() {
        assertEquals(
            SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            SystemUiSceneStateSource.classifyRawState(0),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.KEYGUARD,
            SystemUiSceneStateSource.classifyRawState(1),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.SHADE_LOCKED,
            SystemUiSceneStateSource.classifyRawState(2),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.UNKNOWN,
            SystemUiSceneStateSource.classifyRawState(99),
        )
    }

    @Test
    fun runtimeKeyguardRequiresPlatformKeyguardProof() {
        assertEquals(
            SystemUiSceneStateSource.Surface.KEYGUARD,
            SystemUiSceneStateSource.resolveSurface(1, true),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.TRANSIENT_PANEL,
            SystemUiSceneStateSource.resolveSurface(1, false),
        )
        assertEquals(
            SystemUiSceneStateSource.Surface.UNKNOWN,
            SystemUiSceneStateSource.resolveSurface(1, null),
        )
    }

    @Test
    fun homeOverlayAllowsUnlockedAndTransientPanelOnly() {
        listOf(
            SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            SystemUiSceneStateSource.Surface.TRANSIENT_PANEL,
        ).forEach { surface ->
            assertTrue(SystemUiSceneStateSource.allowsHomeOverlay(surface))
        }
        listOf(
            SystemUiSceneStateSource.Surface.KEYGUARD,
            SystemUiSceneStateSource.Surface.SHADE_LOCKED,
            SystemUiSceneStateSource.Surface.UNKNOWN,
        ).forEach { surface ->
            assertFalse(SystemUiSceneStateSource.allowsHomeOverlay(surface))
        }
    }
}
