package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusCenterGeometryTest {
    @Test
    fun nativeCenterFamiliesScaleFromOneSharedSizeParameter() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = 1f,
                textWeightScale = 1f,
            )
        val enlarged =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = 1.2f,
                textWeightScale = 1f,
            )

        assertEquals(base.wifiMaxWidth * 1.2f, enlarged.wifiMaxWidth, 0.0001f)
        assertEquals(base.wifiMaxHeight * 1.2f, enlarged.wifiMaxHeight, 0.0001f)
        assertEquals(base.airplaneMaxSize * 1.2f, enlarged.airplaneMaxSize, 0.0001f)
        assertEquals(base.noSimMaxSize * 1.2f, enlarged.noSimMaxSize, 0.0001f)
        assertEquals(base.mobileTypeTextSize * 1.2f, enlarged.mobileTypeTextSize, 0.0001f)
        assertEquals(base.mobileTypeSuffixSize * 1.2f, enlarged.mobileTypeSuffixSize, 0.0001f)
    }

    @Test
    fun textWeightChangesIndependentlyFromNativeDrawableSize() {
        val light =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = 1f,
                textWeightScale = 0.8f,
            )
        val heavy =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = 1f,
                textWeightScale = 1.1f,
            )

        assertEquals(light.wifiMaxWidth, heavy.wifiMaxWidth, 0f)
        assertEquals(light.airplaneMaxSize, heavy.airplaneMaxSize, 0f)
        assertTrue(heavy.mobileTypeWeight > light.mobileTypeWeight)
    }

    @Test
    fun invalidAndOutOfRangeValuesAreClampedSafely() {
        val fallback =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = Float.NaN,
                textWeightScale = Float.NaN,
            )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_SIZE_SCALE,
            fallback.sizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_TEXT_WEIGHT_SCALE,
            fallback.textWeightScale,
            0f,
        )

        val clamped =
            CombinedStatusCenterGeometry.resolve(
                sizeScale = 5f,
                textWeightScale = 5f,
            )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_SIZE_SCALE,
            clamped.sizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_TEXT_WEIGHT_SCALE,
            clamped.textWeightScale,
            0f,
        )
    }
}
