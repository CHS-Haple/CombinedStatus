package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeCenterTintNormalizerTest {
    @Test
    fun fullStrengthCanvasUsesSystemTintAlpha() {
        assertEquals(
            191,
            CombinedStatusVisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 255,
                opacity = 1f,
            ),
        )
    }

    @Test
    fun semanticDimmingMultipliesSystemTintInsteadOfReplacingIt() {
        assertEquals(
            35,
            CombinedStatusVisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 48,
                opacity = 1f,
            ),
        )
    }

    @Test
    fun transitionOpacityIsIndependentFromSemanticIntensity() {
        assertEquals(
            95,
            CombinedStatusVisualIntensity.resolveCanvasAlpha(
                color = 0xbf123456.toInt(),
                semanticAlpha = 255,
                opacity = 0.5f,
            ),
        )
        assertEquals(
            128,
            CombinedStatusVisualIntensity.resolveDrawableAlpha(0.5f),
        )
    }

    @Test
    fun opaqueNativeMaskKeepsSourceAlphaUnchanged() {
        assertEquals(
            191,
            CombinedStatusVisualIntensity.normalizeSourceAlpha(
                sourceAlpha = 191,
                sourcePlateauAlpha = 255,
            ),
        )
    }

    @Test
    fun intrinsicDrawableCeilingIsNormalizedAtMaskLayer() {
        assertEquals(
            255,
            CombinedStatusVisualIntensity.normalizeSourceAlpha(
                sourceAlpha = 230,
                sourcePlateauAlpha = 230,
            ),
        )
        assertEquals(
            128,
            CombinedStatusVisualIntensity.normalizeSourceAlpha(
                sourceAlpha = 115,
                sourcePlateauAlpha = 230,
            ),
        )
    }

    @Test
    fun normalizationClampsUnexpectedSourceAlphaAboveObservedPlateau() {
        assertEquals(
            255,
            CombinedStatusVisualIntensity.normalizeSourceAlpha(
                sourceAlpha = 230,
                sourcePlateauAlpha = 204,
            ),
        )
    }
}
