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
    fun keyguardIsProjectedCandidateWhileAodRemainsNativeOnly() {
        val keyguard = CombinedStatusScenePolicy.capability(CombinedStatusScene.KEYGUARD)
        assertEquals(CombinedStatusRenderMode.PROJECTED, keyguard.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, keyguard.motionOwnership)
        assertEquals(CombinedStatusSceneEvidence.STATIC_VERIFIED, keyguard.evidence)

        val aod = CombinedStatusScenePolicy.capability(CombinedStatusScene.AOD)
        assertEquals(CombinedStatusRenderMode.NATIVE_ONLY, aod.renderMode)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, aod.motionOwnership)
    }

    @Test
    fun controlCenterProjectionInheritsVerifiedSourceSceneCapability() {
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                sourceScene = CombinedStatusSourceScene.HOME,
                keyguardEnabled = false,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = false,
            ),
        )
        assertTrue(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                sourceScene = CombinedStatusSourceScene.KEYGUARD,
                keyguardEnabled = true,
            ),
        )
        assertFalse(
            CombinedStatusScenePolicy.controlCenterProjectionEligible(
                sourceScene = CombinedStatusSourceScene.UNKNOWN,
                keyguardEnabled = true,
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
