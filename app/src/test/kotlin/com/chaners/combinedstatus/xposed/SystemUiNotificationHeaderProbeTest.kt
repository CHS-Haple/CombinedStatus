package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNotificationHeaderProbeTest {
    @Test
    fun captureOnlyOccursWhenEnteringBoundaryBucket() {
        assertEquals(
            true,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(null, 0),
        )
        assertEquals(
            false,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(0, 0),
        )
        assertEquals(
            true,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(0, 1),
        )
        assertEquals(
            false,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(1, 2),
        )
        assertEquals(
            true,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(6, 7),
        )
        assertEquals(
            true,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(7, 8),
        )
        assertEquals(
            false,
            SystemUiNotificationHeaderProbe.shouldCaptureBoundary(8, null),
        )
    }
}
