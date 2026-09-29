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
    fun targetAspectRatioComesFromNativeWitnessGeometry() {
        val ratio =
            CombinedStatusControlCenterTransitionOwner.Policy.geometryAspectRatio(
                geometry(width = 20f, height = 8f),
            )

        assertEquals(0.4f, ratio ?: -1f, 0.0001f)
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
}
