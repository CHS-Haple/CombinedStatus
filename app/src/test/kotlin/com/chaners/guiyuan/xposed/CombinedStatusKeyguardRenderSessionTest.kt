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
    fun dualEnabledTransitionContinuityKeepsOutgoingSceneUntilStableBoundary() {
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveTransitionSceneEligible(
                sceneIsAod = false,
                stableSceneEligible = false,
                transitionContinuityEnabled = true,
                toAod = true,
                isAodAnimate = true,
            ),
        )
        assertTrue(
            CombinedStatusKeyguardRenderSession.resolveTransitionSceneEligible(
                sceneIsAod = true,
                stableSceneEligible = false,
                transitionContinuityEnabled = true,
                toAod = false,
                isAodAnimate = true,
            ),
        )
        assertFalse(
            CombinedStatusKeyguardRenderSession.resolveTransitionSceneEligible(
                sceneIsAod = false,
                stableSceneEligible = false,
                transitionContinuityEnabled = false,
                toAod = true,
                isAodAnimate = true,
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
