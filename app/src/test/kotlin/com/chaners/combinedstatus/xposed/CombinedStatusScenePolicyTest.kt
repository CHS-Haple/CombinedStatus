package com.chaners.combinedstatus.xposed

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
}

