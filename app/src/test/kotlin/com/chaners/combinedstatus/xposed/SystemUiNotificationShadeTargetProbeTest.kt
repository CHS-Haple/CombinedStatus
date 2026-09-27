package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNotificationShadeTargetProbeTest {
    @Test
    fun captureOnlyOccursWhenEnteringBoundaryBucket() {
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(null, 0),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(0, 0),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(0, 1),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(1, 2),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(6, 7),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(7, 8),
        )
    }

    @Test
    fun candidateSelectionIsNarrowAndSemantic() {
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "statusIconsController",
                "java.lang.Object",
            ),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "owner",
                "com.android.systemui.SomeHeaderController",
            ),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "configurationController",
                "com.android.systemui.statusbar.policy.ConfigurationController",
            ),
        )
    }
}
