package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterTransitionOwnerTest {
    @Test
    fun shrinkOnlyScalePolicyNeverEnlargesSemanticGlyphs() {
        val source = geometry(width = 10f, height = 10f)
        val target = geometry(width = 30f, height = 20f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = CombinedStatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )

        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun targetScalePolicyStillAllowsWifiOpticalConvergence() {
        val source = geometry(width = 10f, height = 10f)
        val target = geometry(width = 20f, height = 20f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
            )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
    }

    @Test
    fun unmatchedComponentsExitFasterThanLinearWithoutASeparateTimeline() {
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(0f),
            0.0001f,
        )
        assertEquals(
            0.25f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(1f),
            0.0001f,
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitScale(0.5f) > 0.95f,
        )
    }

    @Test
    fun nativeTargetHeightCanBoundLocalShapeWithoutOwningItsExactScale() {
        val ratio =
            CombinedStatusControlCenterTransitionOwner.Policy.relativeGeometryHeight(
                target = geometry(width = 20f, height = 50f),
                current = geometry(width = 10f, height = 20f),
            )

        assertEquals(2.5f, ratio ?: -1f, 0.0001f)
    }

    private fun geometry(
        width: Float,
        height: Float,
    ): FloatArray =
        floatArrayOf(
            0f,
            0f,
            width,
            0f,
            0f,
            height,
        )

    @Test
    fun sourceGeometryKeepsNativePositionButUsesStableRenderBasis() {
        val nativePosition = floatArrayOf(100f, 200f, 60f, 0f, 0f, 40f)
        val stableRenderBasis = floatArrayOf(900f, 900f, 105f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.composeSourceGeometry(
                positionAuthority = nativePosition,
                basisAuthority = stableRenderBasis,
            )

        assertEquals(100f, result[0], 0.0001f)
        assertEquals(200f, result[1], 0.0001f)
        assertEquals(105f, result[2], 0.0001f)
        assertEquals(169f, result[5], 0.0001f)
    }

    @Test
    fun semanticFallbackSeparatesMobileTypeAndSignalInsteadOfSharingSlotCenter() {
        val type =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type_single", "mobile_type"),
                isRtl = false,
            )
        val signal =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_signal"),
                isRtl = false,
            )
        requireNotNull(type)
        requireNotNull(signal)

        assertTrue(type.right < signal.left)

        val rtlType =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type"),
                isRtl = true,
            )
        val rtlSignal =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_signal"),
                isRtl = true,
            )
        requireNotNull(rtlType)
        requireNotNull(rtlSignal)
        assertTrue(rtlSignal.right < rtlType.left)
    }

    @Test
    fun hyperCeilerDualSignalCompatibilityRequiresItsStructuralSignature() {
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = false,
                candidateVisible = true,
                candidateHasResourceEntry = false,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = true,
                candidateVisible = true,
                candidateHasResourceEntry = false,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = false,
                candidateVisible = true,
                candidateHasResourceEntry = true,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
    }
}
