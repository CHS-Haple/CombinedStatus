package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiSceneStateSourceTest {
    @Test
    fun batteryStatusStatesRemainReadOnlyClassifications() {
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
    fun controlCenterProjectionFollowsSourceScenePolicy() {
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                surface = SystemUiSceneStateSource.Surface.SHADE_LOCKED,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                surface = SystemUiSceneStateSource.Surface.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
    }
}

