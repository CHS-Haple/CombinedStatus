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
    fun notificationShadeHomeEligibilityRequiresNativeClosedSemantics() {
        assertEquals(true, SystemUiPanelTransitionSource.notificationShadeAllowsHome(false, false))
        assertEquals(false, SystemUiPanelTransitionSource.notificationShadeAllowsHome(false, true))
        assertEquals(false, SystemUiPanelTransitionSource.notificationShadeAllowsHome(true, false))
        assertEquals(false, SystemUiPanelTransitionSource.notificationShadeAllowsHome(true, true))
        assertEquals(false, SystemUiPanelTransitionSource.notificationShadeAllowsHome(null, false))
        assertEquals(false, SystemUiPanelTransitionSource.notificationShadeAllowsHome(false, null))
    }

    @Test
    fun notificationShadeEligibilitySnapshotCanSeedHotReloadGeneration() {
        SystemUiPanelTransitionSource.resetRuntimeState()
        assertNull(SystemUiPanelTransitionSource.currentNotificationShadeHomeEligibility())

        SystemUiPanelTransitionSource.restoreNotificationShadeHomeEligibility(false)
        assertEquals(false, SystemUiPanelTransitionSource.currentNotificationShadeHomeEligibility())

        SystemUiPanelTransitionSource.restoreNotificationShadeHomeEligibility(true)
        assertEquals(true, SystemUiPanelTransitionSource.currentNotificationShadeHomeEligibility())

        SystemUiPanelTransitionSource.restoreNotificationShadeHomeEligibility(null)
        assertNull(SystemUiPanelTransitionSource.currentNotificationShadeHomeEligibility())
    }

    @Test
    fun runtimeHookCountKeepsControlCenterDiagnosticsOptional() {
        assertEquals(1, SystemUiPanelTransitionSource.expectedHookCount(false))
        assertEquals(3, SystemUiPanelTransitionSource.expectedHookCount(true))
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
}
