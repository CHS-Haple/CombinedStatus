package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusKeyguardRenderSessionTest {
    @Test
    fun overlayRequiresFeatureAndCompletedNativeHandoff() {
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, true))
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOverlayVisible(true, false))
    }

    @Test
    fun readinessRequiresCompleteAttachedKeyguardSurface() {
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, false, true))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, false))
    }
}
