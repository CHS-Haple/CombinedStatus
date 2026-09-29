package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusKeyguardRenderSessionTest {
    @Test
    fun overlayRequiresFeatureAndCompletedNativeHandoff() {
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(false, false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, false, true))
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, false, false))
    }

    @Test
    fun readinessRequiresCompleteAttachedKeyguardSurface() {
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, false, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, true))
    }
}
