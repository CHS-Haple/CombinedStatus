package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusVisualIntensityTest {
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
}
