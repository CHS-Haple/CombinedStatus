package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
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
    fun steadySourceAuthorityRequiresMatchingStructuralHost() {
        assertEquals(
            CombinedStatusSourceScene.HOME,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiNotificationStatusContainer",
                ),
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView",
                ),
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiSceneStateSource.classifySteadySourceAncestors(
                listOf(
                    "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                ),
            ),
        )
    }
    @Test
    fun visibleSteadySourceRejectsHiddenStructuralHosts() {
        assertEquals(
            CombinedStatusSourceScene.HOME,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.HOME,
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
                hostShown = true,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.HOME,
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
                hostShown = false,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.KEYGUARD,
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                hostShown = true,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.KEYGUARD,
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                hostShown = false,
            ),
        )
    }

    @Test
    fun visibleSteadySourceStillRequiresMatchingSurface() {
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.HOME,
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                hostShown = true,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiSceneStateSource.qualifySteadySourceScene(
                structuralScene = CombinedStatusSourceScene.KEYGUARD,
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
                hostShown = true,
            ),
        )
    }

}
