package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusLayoutPolicyTest {
    @Test
    fun scalingKeepsTheEndAnchorFixed() {
        val small = resolve(scale = 1f)
        val large = resolve(scale = 1.2f)

        assertEquals(587f, small.visualRightPx, 0.001f)
        assertEquals(587f, large.visualRightPx, 0.001f)
        assertTrue(large.visualLeftPx < small.visualLeftPx)
        assertTrue(large.requestedSlotWidthPx > small.requestedSlotWidthPx)
        assertTrue(large.neighborGapPx > small.neighborGapPx)
    }

    @Test
    fun ownedSlotAppliesTheSharedRequestedWidth() {
        val layout = resolve(
            scale = 1.2f,
            renderMode = CombinedStatusRenderMode.OWNED_SLOT,
        )

        assertEquals(layout.requestedSlotWidthPx, layout.appliedSlotWidthPx, 0.001f)
        assertEquals(
            layout.visualSidePx + layout.neighborGapPx,
            layout.appliedSlotWidthPx,
            0.001f,
        )
    }

    @Test
    fun projectedSceneKeepsNativeSlotButReusesTheSameVisualRule() {
        val owned = resolve(
            scale = 1.2f,
            renderMode = CombinedStatusRenderMode.OWNED_SLOT,
        )
        val projected = resolve(
            scale = 1.2f,
            renderMode = CombinedStatusRenderMode.PROJECTED,
        )

        assertEquals(owned.visualSidePx, projected.visualSidePx, 0.001f)
        assertEquals(owned.neighborGapPx, projected.neighborGapPx, 0.001f)
        assertEquals(owned.requestedSlotWidthPx, projected.requestedSlotWidthPx, 0.001f)
        assertEquals(105f, projected.appliedSlotWidthPx, 0.001f)
        assertEquals(owned.visualRightPx, projected.visualRightPx, 0.001f)
    }

    @Test
    fun nativeOnlySceneDoesNotRenderCombinedVisuals() {
        val layout = resolve(
            scale = 1f,
            renderMode = CombinedStatusRenderMode.NATIVE_ONLY,
            motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
        )

        assertFalse(layout.renderCombined)
        assertEquals(105f, layout.appliedSlotWidthPx, 0.001f)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, layout.motionOwnership)
    }

    @Test
    fun sharedPolicyDoesNotChangeIdealGeometryBySceneCapability() {
        val owned = resolve(
            scale = 0.9f,
            renderMode = CombinedStatusRenderMode.OWNED_SLOT,
        )
        val projected = resolve(
            scale = 0.9f,
            renderMode = CombinedStatusRenderMode.PROJECTED,
            motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
        )
        val nativeOnly = resolve(
            scale = 0.9f,
            renderMode = CombinedStatusRenderMode.NATIVE_ONLY,
            motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
        )

        listOf(projected, nativeOnly).forEach { other ->
            assertEquals(owned.visualSidePx, other.visualSidePx, 0.001f)
            assertEquals(owned.neighborGapPx, other.neighborGapPx, 0.001f)
            assertEquals(owned.requestedSlotWidthPx, other.requestedSlotWidthPx, 0.001f)
            assertEquals(owned.visualLeftPx, other.visualLeftPx, 0.001f)
            assertEquals(owned.visualRightPx, other.visualRightPx, 0.001f)
        }
        assertTrue(owned.renderCombined)
        assertTrue(projected.renderCombined)
    }

    private fun resolve(
        scale: Float,
        renderMode: CombinedStatusRenderMode = CombinedStatusRenderMode.OWNED_SLOT,
        motionOwnership: CombinedStatusMotionOwnership =
            CombinedStatusMotionOwnership.COMBINED_STATUS,
    ): CombinedStatusResolvedLayout =
        CombinedStatusLayoutPolicy.resolve(
            settings = CombinedStatusLayoutSettings(
                baseVisualSidePx = 105f,
                baseNeighborGapPx = 6f,
                userScale = scale,
            ),
            host = CombinedStatusHostLayout(
                hostHeightPx = 108f,
                endAnchorPx = 587f,
                nativeSlotWidthPx = 105f,
                renderMode = renderMode,
                motionOwnership = motionOwnership,
            ),
        )
}
