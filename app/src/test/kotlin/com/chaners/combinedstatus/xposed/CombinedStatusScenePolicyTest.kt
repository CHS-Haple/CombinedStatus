package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
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
    fun homeStableIsTheOnlyInitialOwnedSlotCandidate() {
        val owned =
            CombinedStatusScenePolicy.all()
                .filter { it.renderMode == CombinedStatusRenderMode.OWNED_SLOT }

        assertEquals(1, owned.size)
        assertEquals(CombinedStatusScene.HOME_STABLE, owned.single().scene)
        assertEquals(CombinedStatusMotionOwnership.NONE, owned.single().motionOwnership)
        assertEquals(
            CombinedStatusSceneEvidence.RUNTIME_VERIFIED,
            owned.single().evidence,
        )
    }

    @Test
    fun systemUiOwnedTransitionsDoNotRequestCombinedSlotMutation() {
        val systemUiOwned =
            CombinedStatusScenePolicy.all()
                .filter { it.motionOwnership == CombinedStatusMotionOwnership.SYSTEM_UI }

        assertTrue(systemUiOwned.isNotEmpty())
        systemUiOwned.forEach { capability ->
            assertTrue(capability.renderMode != CombinedStatusRenderMode.OWNED_SLOT)
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
    fun keyguardAndAodReuseProjectionInsteadOfOwningAnotherGeometryFormula() {
        listOf(
            CombinedStatusScene.KEYGUARD,
            CombinedStatusScene.AOD,
        ).forEach { scene ->
            val capability = CombinedStatusScenePolicy.capability(scene)
            assertEquals(CombinedStatusRenderMode.PROJECTED, capability.renderMode)
            assertEquals(
                CombinedStatusMotionOwnership.SYSTEM_UI,
                capability.motionOwnership,
            )
        }
    }
}
