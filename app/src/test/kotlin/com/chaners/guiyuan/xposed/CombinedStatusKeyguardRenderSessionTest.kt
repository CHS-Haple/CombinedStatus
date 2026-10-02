package com.chaners.guiyuan.xposed

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
    fun keyguardAndAodFamilyKeepChildFeatureGatesIndependent() {
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = false,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = false,
            ),
        )
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                sceneIsAod = true,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveFamilyFeatureEnabled(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                sceneIsAod = true,
            ),
        )
    }

    @Test
    fun keyguardFamilyAlphaResetsAfterLeavingAod() {
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyChildAlpha(
                sceneIsAod = true,
                batteryAlpha = 0.42f,
            ) < 1f,
        )
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveFamilyChildAlpha(
                sceneIsAod = false,
                batteryAlpha = 0.42f,
            ) == 1f,
        )
    }

    @Test
    fun aodOverlayRequiresIndependentFeatureAndStableAod() {
        assertFalse(CombinedStatusKeyguardRenderSession.resolveAodOverlayVisible(false, false, true))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveAodOverlayVisible(true, true, true))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveAodOverlayVisible(true, false, false))
        assertTrue(CombinedStatusKeyguardRenderSession.resolveAodOverlayVisible(true, false, true))
    }

    @Test
    fun aodReadinessRejectsTransitionStateWithoutAffectingKeyguardContract() {
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveAodOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveAodOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                stableAod = false,
            ),
        )
    }

    @Test
    fun readinessRequiresCompleteAttachedKeyguardSurface() {
        assertTrue(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, false, true, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, false, false))
        assertFalse(CombinedStatusKeyguardRenderSession.resolveOwnerReady(true, true, true, true, true, true))
    }
}
