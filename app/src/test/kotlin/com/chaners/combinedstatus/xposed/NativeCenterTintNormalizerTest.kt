package com.chaners.combinedstatus.xposed

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeCenterTintNormalizerTest {
    @Test
    fun opaqueNativeAssetKeepsSystemTintUnchanged() {
        val tint = 0xbf000000.toInt()
        assertEquals(
            tint,
            NativeCenterTintNormalizer.normalizeForIntrinsicAlpha(
                tint = tint,
                intrinsicMaxAlpha = 255,
            ),
        )
    }

    @Test
    fun intrinsicDrawableAlphaIsCompensatedWithoutChangingRgb() {
        val tint = 0xbf123456.toInt()
        val normalized =
            NativeCenterTintNormalizer.normalizeForIntrinsicAlpha(
                tint = tint,
                intrinsicMaxAlpha = 230,
            )
        assertEquals(212, Color.alpha(normalized))
        assertEquals(Color.red(tint), Color.red(normalized))
        assertEquals(Color.green(tint), Color.green(normalized))
        assertEquals(Color.blue(tint), Color.blue(normalized))
    }

    @Test
    fun compensationClampsInsteadOfOverflowing() {
        val tint = 0xe6ffffff.toInt()
        val normalized =
            NativeCenterTintNormalizer.normalizeForIntrinsicAlpha(
                tint = tint,
                intrinsicMaxAlpha = 204,
            )
        assertEquals(255, Color.alpha(normalized))
        assertEquals(0x00ffffff, normalized and 0x00ffffff)
    }
}
