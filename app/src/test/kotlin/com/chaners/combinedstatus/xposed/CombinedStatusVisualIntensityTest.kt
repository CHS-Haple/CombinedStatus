package com.chaners.combinedstatus.xposed

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusVisualIntensityTest {
    @Test
    fun nativeAlphaScaleLeavesOpaqueSourceAtTintStrength() {
        assertEquals(
            1f,
            CombinedStatusVisualIntensity.resolveNativeAlphaScale(
                tint = Color.argb(255, 80, 180, 110),
                intrinsicMaxAlpha = 255,
            ),
            0.0001f,
        )
    }

    @Test
    fun nativeAlphaScaleRaisesIntrinsicHalfAlphaToFullStrength() {
        assertEquals(
            255f / 128f,
            CombinedStatusVisualIntensity.resolveNativeAlphaScale(
                tint = Color.argb(255, 80, 180, 110),
                intrinsicMaxAlpha = 128,
            ),
            0.0001f,
        )
    }

    @Test
    fun nativeAlphaScaleKeepsTintAlphaAsTheTarget() {
        assertEquals(
            128f / 64f,
            CombinedStatusVisualIntensity.resolveNativeAlphaScale(
                tint = Color.argb(128, 80, 180, 110),
                intrinsicMaxAlpha = 64,
            ),
            0.0001f,
        )
    }

    @Test
    fun nativeAlphaScalePreservesRelativeSourceCoverageUntilTargetPeak() {
        val scale =
            CombinedStatusVisualIntensity.resolveNativeAlphaScale(
                tint = Color.argb(255, 80, 180, 110),
                intrinsicMaxAlpha = 128,
            )
        val halfCoverage = 64f * scale
        val fullCoverage = 128f * scale

        assertEquals(127.5f, halfCoverage, 0.01f)
        assertEquals(255f, fullCoverage, 0.01f)
        assertTrue(halfCoverage < fullCoverage)
    }
}
