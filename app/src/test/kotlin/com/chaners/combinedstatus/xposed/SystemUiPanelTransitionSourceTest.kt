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
