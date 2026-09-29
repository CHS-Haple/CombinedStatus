package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemUiPanelTransitionSourceTest {
    @Test
    fun nativeFractionPreservesFiniteHyperOsPayload() {
        assertEquals(-0.2f, SystemUiPanelTransitionSource.nativeFraction(-0.2f))
        assertEquals(0.5f, SystemUiPanelTransitionSource.nativeFraction(0.5f))
        assertEquals(1.4f, SystemUiPanelTransitionSource.nativeFraction(1.4f))
        assertNull(SystemUiPanelTransitionSource.nativeFraction(Float.NaN))
        assertNull(SystemUiPanelTransitionSource.nativeFraction(Float.POSITIVE_INFINITY))
    }

    @Test
    fun controlCenterHomeEligibilityRequiresNativeInvisibleSemantics() {
        assertEquals(true, SystemUiPanelTransitionSource.controlCenterAllowsHome(false))
        assertEquals(false, SystemUiPanelTransitionSource.controlCenterAllowsHome(true))
        assertEquals(false, SystemUiPanelTransitionSource.controlCenterAllowsHome(null))
    }

    @Test
    fun controlCenterEligibilitySnapshotCanSeedHotReloadGeneration() {
        SystemUiPanelTransitionSource.resetRuntimeState()
        assertNull(SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(false)
        assertEquals(false, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(true)
        assertEquals(true, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())

        // A v5 or older payload has no Control Center field; do not erase the
        // successfully installed generation's current/bootstrap eligibility.
        SystemUiPanelTransitionSource.restoreControlCenterHomeEligibility(null)
        assertEquals(true, SystemUiPanelTransitionSource.currentControlCenterHomeEligibility())
    }

    @Test
    fun runtimeHookCountIncludesFakeLifecyclePrearmAndOptionalDiagnostics() {
        assertEquals(2, SystemUiPanelTransitionSource.expectedHookCount(false))
        assertEquals(4, SystemUiPanelTransitionSource.expectedHookCount(true))
    }

    @Test
    fun appearanceDiagnosticPreservesNativeBooleanPayloadWithoutInterpretation() {
        assertEquals(
            "controlCenterAppearance first=true second=false expanding=unknown " +
                "addBatteryIsland=unknown batteryWidthDiff=unknown " +
                "fakePresentation=unknown readOnly=true nativeGeometryWrites=0",
            SystemUiPanelTransitionSource.appearanceDiagnostic(
                first = true,
                second = false,
                snapshot = null,
            ),
        )
    }

    @Test
    fun appearanceDiagnosticIncludesTopLevelFakePresentationSnapshot() {
        val fake =
            SystemUiPanelTransitionSource.ControlCenterFakePresentationSnapshot(
                rootClassName =
                    "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons",
                rootVisibility = 0,
                rootAlpha = 1f,
                rootWidth = 1440,
                rootHeight = 108,
                statusBarAreaClassName =
                    "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer",
                statusBarAreaVisibility = 0,
                statusBarAreaAlpha = 1f,
                statusBarAreaWidth = 587,
                statusBarAreaHeight = 108,
            )
        assertEquals(
            "controlCenterAppearance first=true second=true expanding=unknown " +
                "addBatteryIsland=unknown batteryWidthDiff=unknown " +
                "fakePresentation={root=" +
                "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons" +
                "(v=0,a=1.0,w=1440,h=108),statusBarArea=" +
                "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer" +
                "(v=0,a=1.0,w=587,h=108)} readOnly=true nativeGeometryWrites=0",
            SystemUiPanelTransitionSource.appearanceDiagnostic(
                first = true,
                second = true,
                snapshot = null,
                fakePresentation = fake,
            ),
        )
    }

    @Test
    fun controlAnchorProbeOnlyUsesTransitionBoundaryBuckets() {
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(0))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(1))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(2))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(6))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(7))
        assertEquals(true, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(8))
        assertEquals(false, SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(null))
    }

    @Test
    fun homeMotionSnapshotKeepsBatteryWrapperAndBatteryDistinct() {
        val snapshot =
            SystemUiIslandMotionSource.OwnerSnapshot(
                views =
                    mapOf(
                        "mBatteryContainer" to
                            SystemUiIslandMotionSource.MotionViewSnapshot(
                                className = "FrameLayout",
                                screenX = 1242,
                                width = 105,
                                translationX = 0f,
                                alpha = 1f,
                                visibility = 0,
                            ),
                        "mBatteryView" to
                            SystemUiIslandMotionSource.MotionViewSnapshot(
                                className = "MiuiBatteryMeterView",
                                screenX = 1242,
                                width = 105,
                                translationX = 0f,
                                alpha = 1f,
                                visibility = 0,
                            ),
                    ),
            )

        assertEquals(
            "{mBatteryContainer=FrameLayout(x=1242,w=105,tx=0.0,a=1.0,v=0)," +
                "mBatteryView=MiuiBatteryMeterView(x=1242,w=105,tx=0.0,a=1.0,v=0)}",
            snapshot.summary,
        )
    }

    @Test
    fun diagnosticsUseBoundedExpansionBuckets() {
        assertEquals(0, SystemUiPanelTransitionSource.diagnosticBucket(0f))
        assertEquals(1, SystemUiPanelTransitionSource.diagnosticBucket(0.125f))
        assertEquals(4, SystemUiPanelTransitionSource.diagnosticBucket(0.5f))
        assertEquals(7, SystemUiPanelTransitionSource.diagnosticBucket(0.99f))
        assertEquals(8, SystemUiPanelTransitionSource.diagnosticBucket(1f))
        assertEquals(0, SystemUiPanelTransitionSource.diagnosticBucket(-0.2f))
        assertEquals(8, SystemUiPanelTransitionSource.diagnosticBucket(1.4f))
        assertNull(SystemUiPanelTransitionSource.diagnosticBucket(null))
    }

    @Test
    fun controlCenterSourceUsesHomeCarrierIdentityBeforeStructuralFallback() {
        assertEquals(
            CombinedStatusSourceScene.HOME,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = true,
                structuralScene = CombinedStatusSourceScene.UNKNOWN,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.KEYGUARD,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = CombinedStatusSourceScene.KEYGUARD,
            ),
        )
        assertEquals(
            CombinedStatusSourceScene.UNKNOWN,
            SystemUiPanelTransitionSource.classifyControlCenterSourceScene(
                homeIdentityMatches = false,
                structuralScene = CombinedStatusSourceScene.UNKNOWN,
            ),
        )
    }

    @Test
    fun controlCenterUpdateCarriesNativeSelectedSourceScene() {
        val update =
            SystemUiPanelTransitionSource.Update(
                source = SystemUiPanelTransitionSource.Source.CONTROL_CENTER,
                fraction = null,
                expanded = null,
                tracking = null,
                visible = true,
                controlCenterSourceScene = CombinedStatusSourceScene.KEYGUARD,
            )
        assertEquals(CombinedStatusSourceScene.KEYGUARD, update.controlCenterSourceScene)
    }
}
