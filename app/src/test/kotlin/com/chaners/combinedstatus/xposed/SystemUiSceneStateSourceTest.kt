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
    fun homeOverlayFailsClosedOutsideUnlockedStatusBar() {
        assertTrue(
            SystemUiSceneStateSource.allowsHomeOverlay(
                SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
        listOf(
            SystemUiSceneStateSource.Surface.KEYGUARD,
            SystemUiSceneStateSource.Surface.SHADE_LOCKED,
            SystemUiSceneStateSource.Surface.UNKNOWN,
        ).forEach { surface ->
            assertFalse(SystemUiSceneStateSource.allowsHomeOverlay(surface))
        }
    }
}
