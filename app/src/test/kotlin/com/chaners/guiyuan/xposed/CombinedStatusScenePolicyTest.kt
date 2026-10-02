package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusScenePolicyTest {
    @Test
    fun everySceneHasExactlyOneCapability() {
        val capabilities = CombinedStatusScenePolicy.all()

        assertEquals(CombinedStatusScene.entries.size, capabilities.size)
        assertEquals(
            CombinedStatusScene.entries.toSet(),
            capabilities.map { it.scene }.toSet(),
        )
    }

    @Test
    fun homeStableUsesProjectedOverlayWithoutNativeSlotMutation() {
        val home = CombinedStatusScenePolicy.capability(CombinedStatusScene.HOME_STABLE)

        assertEquals(CombinedStatusRenderMode.PROJECTED, home.renderMode)
        assertEquals(CombinedStatusMotionOwnership.NONE, home.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.RUNTIME_VERIFIED, home.evidence)
    }

    @Test
    fun systemUiOwnedTransitionsDoNotRequestCombinedSlotMutation() {
        val systemUiOwned =
            CombinedStatusScenePolicy.all()
                .filter { it.motionOwnership == CombinedStatusMotionOwnership.SYSTEM_UI }

        assertTrue(systemUiOwned.isNotEmpty())
        systemUiOwned.forEach { capability ->
            assertTrue(capability.renderMode in CombinedStatusRenderMode.entries)
        }
    }

    @Test
    fun chargingIsNotModeledAsAnIndependentScene() {
        assertTrue(
            CombinedStatusScene.entries.none {
                it.name.contains("CHARG", ignoreCase = true)
            },
        )
    }

    @Test
    fun retainedTransitionSourceWitnessSurvivesPresentationHandoff() {
        assertTrue(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 0,
                heightPx = 169,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                widthPx = 105,
                heightPx = 169,
                hostAttached = false,
            ),
        )
    }

    @Test
    fun keyguardAndStableAodAreIndependentProjectedCandidates() {
        val keyguard = CombinedStatusScenePolicy.capability(CombinedStatusScene.KEYGUARD)
        assertEquals(CombinedStatusRenderMode.PROJECTED, keyguard.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, keyguard.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.STATIC_VERIFIED, keyguard.evidence)

        val aod = CombinedStatusScenePolicy.capability(CombinedStatusScene.AOD)
        assertEquals(CombinedStatusRenderMode.PROJECTED, aod.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, aod.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.STATIC_VERIFIED, aod.evidence)

        assertTrue(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = false,
                aodEnabled = true,
                stableAod = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
            ),
        )
    }

    @Test
    fun keyguardAndAodProjectionMatrixKeepsChildPreferencesIndependent() {
        fun resolveStable(
            feature: Boolean,
            keyguard: Boolean,
            aod: Boolean,
            toAod: Boolean,
            source: CombinedStatusSourceScene,
        ) =
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = feature,
                keyguardEnabled = keyguard,
                aodEnabled = aod,
                toAod = toAod,
                isAodAnimate = false,
                steadySourceScene = source,
            )

        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            resolveStable(true, true, false, false, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, false, true, false, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            resolveStable(true, false, true, true, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, false, true, CombinedStatusSourceScene.KEYGUARD),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(true, true, true, false, CombinedStatusSourceScene.HOME),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            resolveStable(false, true, true, false, CombinedStatusSourceScene.KEYGUARD),
        )
    }

    @Test
    fun aodAnimationRoutesByVisiblePresentationOwnershipNotDirectionFields() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                aodPresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.UNKNOWN,
                aodPresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.HOME,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = true,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationOwned = true,
            ),
        )
    }

    @Test
    fun singleEnabledFamilyUsesSteadyKeyguardBoundaryInsteadOfAodAnimationTail() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.KEYGUARD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = true,
                aodEnabled = false,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardPresentationOwned = false,
                aodPresentationOwned = false,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = false,
                aodPresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = true,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = true,
                aodPresentationOwned = true,
            ),
        )
    }

    @Test
    fun aodOnlyPrearmsAcrossTransientKeyguardSourceWhileHomeStillOwnsPresentation() {
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.AOD,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = true,
            ),
        )
        assertEquals(
            CombinedStatusScenePolicy.KeyguardAodProjection.NATIVE,
            CombinedStatusScenePolicy.resolveKeyguardAodProjection(
                featureEnabled = true,
                keyguardEnabled = false,
                aodEnabled = true,
                toAod = false,
                isAodAnimate = false,
                steadySourceScene = CombinedStatusSourceScene.KEYGUARD,
                homePresentationOwned = false,
            ),
        )
    }

    @Test
    fun aodPrearmRequiresAodFeatureAndVisibleHomeOwnership() {
        assertTrue(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = true,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.aodProjectionEligible(
                featureEnabled = true,
                aodEnabled = false,
                stableAod = false,
                homeTransitionPrearm = true,
            ),
        )
    }

    @Test
    fun controlCenterProjectionInheritsVerifiedSourceSceneCapability() {
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = true,
                sourceScene = CombinedStatusSourceScene.UNKNOWN,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                featureEnabled = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseRejectsEveryIndependentInvalidBoundary() {
        val base =
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            )
        assertTrue(base)

        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = false,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = false,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = false,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = false,
                aodBlocked = false,
                nativeFraction = 0.5f,
            ),
        )
    }

    @Test
    fun hiddenControlCenterIgnoresKeyguardLifecycleChurnUntilItActuallyOpens() {
        assertFalse(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = true,
                nativeFraction = 0f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0.1f,
                leaseActive = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.shouldReconcileControlCenterForKeyguardLifecycle(
                controlCenterVisible = false,
                nativeFraction = 0f,
                leaseActive = true,
            ),
        )
    }

    @Test
    fun keyguardControlCenterLeaseExistsOnlyInsideVerifiedNativeTransitionLifetime() {
        assertTrue(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardRuntimeReady = true,
                nativeFraction = 0.5f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldAcquireKeyguardControlCenterLease(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardRuntimeReady = true,
                nativeFraction = 0f,
            ),
        )

        assertTrue(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.HOME,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = true,
                nativeFraction = 1f,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.shouldRetainKeyguardControlCenterLease(
                leaseActive = true,
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                featureEnabled = true,
                keyguardEnabled = true,
                hostAttached = true,
                aodBlocked = false,
                nativeFraction = 0f,
            ),
        )
    }
}
