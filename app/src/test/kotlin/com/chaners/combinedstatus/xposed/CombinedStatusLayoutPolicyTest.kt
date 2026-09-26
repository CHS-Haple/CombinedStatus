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
    fun projectedSceneKeepsNativeSlotButReusesTheSameVisualRule() {
        val projected = resolve(scale = 1.2f)

        assertEquals(105f, projected.appliedSlotWidthPx, 0.001f)
        assertTrue(projected.requestedSlotWidthPx > projected.appliedSlotWidthPx)
        assertEquals(587f, projected.visualRightPx, 0.001f)
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
        val projected = resolve(scale = 0.9f)
        val nativeOnly = resolve(
            scale = 0.9f,
            renderMode = CombinedStatusRenderMode.NATIVE_ONLY,
            motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
        )

        assertEquals(projected.visualSidePx, nativeOnly.visualSidePx, 0.001f)
        assertEquals(projected.neighborGapPx, nativeOnly.neighborGapPx, 0.001f)
        assertEquals(projected.requestedSlotWidthPx, nativeOnly.requestedSlotWidthPx, 0.001f)
        assertEquals(projected.visualLeftPx, nativeOnly.visualLeftPx, 0.001f)
        assertEquals(projected.visualRightPx, nativeOnly.visualRightPx, 0.001f)
        assertTrue(projected.renderCombined)
    }

    @Test
    fun homeResolverKeepsCurrentCarrierWidthAndHostHeightSeparated() {
        val layout =
            requireNotNull(
                CombinedStatusHomeLayoutResolver.resolve(
                    hostWidthPx = 587,
                    hostHeightPx = 108,
                    nativeCarrierWidthPx = 105,
                    isRtl = false,
                ),
            )

        assertEquals(105f, layout.requestedSlotWidthPx, 0.001f)
        assertEquals(105f, layout.appliedSlotWidthPx, 0.001f)
        assertEquals(482f, layout.slotLeftPx, 0.001f)
        assertEquals(587f, layout.slotRightPx, 0.001f)
        assertEquals(CombinedStatusMotionOwnership.SYSTEM_UI, layout.motionOwnership)
    }

    private fun resolve(
        scale: Float,
        renderMode: CombinedStatusRenderMode = CombinedStatusRenderMode.PROJECTED,
        motionOwnership: CombinedStatusMotionOwnership =
            CombinedStatusMotionOwnership.NONE,
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
