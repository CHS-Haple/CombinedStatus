package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemUiPanelTransitionSourceTest {
    @Test
    fun nativeFractionIsClampedWithoutInventingInvalidState() {
        assertEquals(0f, SystemUiPanelTransitionSource.normalizeFraction(-0.2f))
        assertEquals(0.5f, SystemUiPanelTransitionSource.normalizeFraction(0.5f))
        assertEquals(1f, SystemUiPanelTransitionSource.normalizeFraction(1.4f))
        assertNull(SystemUiPanelTransitionSource.normalizeFraction(Float.NaN))
        assertNull(SystemUiPanelTransitionSource.normalizeFraction(Float.POSITIVE_INFINITY))
    }

    @Test
    fun diagnosticsUseBoundedExpansionBuckets() {
        assertEquals(0, SystemUiPanelTransitionSource.diagnosticBucket(0f))
        assertEquals(1, SystemUiPanelTransitionSource.diagnosticBucket(0.125f))
        assertEquals(4, SystemUiPanelTransitionSource.diagnosticBucket(0.5f))
        assertEquals(7, SystemUiPanelTransitionSource.diagnosticBucket(0.99f))
        assertEquals(8, SystemUiPanelTransitionSource.diagnosticBucket(1f))
        assertNull(SystemUiPanelTransitionSource.diagnosticBucket(null))
    }
}
