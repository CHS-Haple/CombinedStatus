package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusVisualIntensityTest {
    @Test
    fun fullStrengthSourceIsUnchanged() {
        assertEquals(0, CombinedStatusVisualIntensity.normalizeSourceAlpha(0, 255))
        assertEquals(64, CombinedStatusVisualIntensity.normalizeSourceAlpha(64, 255))
        assertEquals(255, CombinedStatusVisualIntensity.normalizeSourceAlpha(255, 255))
    }

    @Test
    fun intrinsicAlphaCeilingIsNormalizedToFullStrength() {
        assertEquals(255, CombinedStatusVisualIntensity.normalizeSourceAlpha(128, 128))
        assertEquals(128, CombinedStatusVisualIntensity.normalizeSourceAlpha(64, 128))
        assertEquals(0, CombinedStatusVisualIntensity.normalizeSourceAlpha(0, 128))
    }

    @Test
    fun normalizationPreservesRelativeAlphaOrdering() {
        val values = listOf(0, 16, 32, 64, 96, 128)
            .map { CombinedStatusVisualIntensity.normalizeSourceAlpha(it, 128) }
        assertTrue(values.zipWithNext().all { (left, right) -> left <= right })
        assertEquals(255, values.last())
    }

    @Test
    fun sourceAlphaIsBoundedByObservedCeiling() {
        assertEquals(255, CombinedStatusVisualIntensity.normalizeSourceAlpha(200, 128))
    }

    @Test
    fun robustCeilingIgnoresSparseHigherAlphaPixels() {
        val alphas =
            intArrayOf(
                0, 0, 4, 8,
                64, 128, 191, 191, 191, 191, 191, 191,
                255,
            )
        assertEquals(
            191,
            CombinedStatusVisualIntensity.resolveSourceCeilingAlpha(
                sourceAlphas = alphas,
                minVisibleAlpha = 8,
            ),
        )
    }

    @Test
    fun upperVisibleDistributionDefinesCeiling() {
        val alphas = intArrayOf(96, 96, 192, 192)
        assertEquals(
            192,
            CombinedStatusVisualIntensity.resolveSourceCeilingAlpha(
                sourceAlphas = alphas,
                minVisibleAlpha = 8,
            ),
        )
    }

    @Test
    fun noVisiblePixelsHasNoCeiling() {
        assertEquals(
            0,
            CombinedStatusVisualIntensity.resolveSourceCeilingAlpha(
                sourceAlphas = intArrayOf(0, 1, 4, 8),
                minVisibleAlpha = 8,
            ),
        )
    }

    @Test
    fun thinAntialiasGradientDoesNotNormalizeFromLowAlphaMode() {
        val alphas =
            intArrayOf(
                0, 4, 8,
                32, 32, 32,
                64, 96, 128, 160, 192, 224, 255,
            )
        assertEquals(
            192,
            CombinedStatusVisualIntensity.resolveSourceCeilingAlpha(
                sourceAlphas = alphas,
                minVisibleAlpha = 8,
            ),
        )
    }
}
