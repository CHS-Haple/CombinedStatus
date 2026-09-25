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
    fun sourceAlphaIsBoundedByObservedMaximum() {
        assertEquals(255, CombinedStatusVisualIntensity.normalizeSourceAlpha(200, 128))
    }
}
