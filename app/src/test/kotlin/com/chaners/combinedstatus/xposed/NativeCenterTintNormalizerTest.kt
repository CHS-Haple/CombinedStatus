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
    fun opaqueNativeAssetKeepsSystemTintUnchanged() {
        val tint = 0xbf000000.toInt()
        assertEquals(
            tint,
            CombinedStatusVisualIntensity.resolveNativeFullStrengthTint(
                tint = tint,
                intrinsicMaxAlpha = 255,
            ),
        )
    }

    @Test
    fun intrinsicDrawableAlphaIsCompensatedWithoutChangingRgb() {
        val tint = 0xbf123456.toInt()
        val normalized =
            CombinedStatusVisualIntensity.resolveNativeFullStrengthTint(
                tint = tint,
                intrinsicMaxAlpha = 230,
            )
        assertEquals(212, normalized ushr 24)
        assertEquals(tint and 0x00ffffff, normalized and 0x00ffffff)
    }

    @Test
    fun compensationClampsInsteadOfOverflowing() {
        val tint = 0xe6ffffff.toInt()
        val normalized =
            CombinedStatusVisualIntensity.resolveNativeFullStrengthTint(
                tint = tint,
                intrinsicMaxAlpha = 204,
            )
        assertEquals(255, normalized ushr 24)
        assertEquals(0x00ffffff, normalized and 0x00ffffff)
    }
}
