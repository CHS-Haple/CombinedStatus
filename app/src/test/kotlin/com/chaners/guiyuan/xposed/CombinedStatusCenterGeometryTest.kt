package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusCenterGeometryTest {
    @Test
    fun wifiSizeChangesWithoutResizingMobileTypeOrNativePeers() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1.2f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.wifiMaxWidth * 1.2f, enlarged.wifiMaxWidth, 0.0001f)
        assertEquals(base.wifiMaxHeight * 1.2f, enlarged.wifiMaxHeight, 0.0001f)
        assertEquals(base.mobileTypeTextSize, enlarged.mobileTypeTextSize, 0f)
        assertEquals(base.mobileTypeSuffixSize, enlarged.mobileTypeSuffixSize, 0f)
        assertEquals(base.airplaneMaxSize, enlarged.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlarged.noSimMaxSize, 0f)
    }

    @Test
    fun mobileTypeSizeChangesWithoutResizingWifiOrNativePeers() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlarged =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1.2f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.mobileTypeTextSize * 1.2f, enlarged.mobileTypeTextSize, 0.0001f)
        assertEquals(base.mobileTypeSuffixSize * 1.2f, enlarged.mobileTypeSuffixSize, 0.0001f)
        assertEquals(base.mobileTypeSuffixRise * 1.2f, enlarged.mobileTypeSuffixRise, 0.0001f)
        assertEquals(base.wifiMaxWidth, enlarged.wifiMaxWidth, 0f)
        assertEquals(base.airplaneMaxSize, enlarged.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlarged.noSimMaxSize, 0f)
    }

    @Test
    fun mobileTypeWeightChangesIndependentlyFromAllDrawableSizes() {
        val light =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 600,
            )
        val heavy =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 900,
            )

        assertEquals(light.wifiMaxWidth, heavy.wifiMaxWidth, 0f)
        assertEquals(light.airplaneMaxSize, heavy.airplaneMaxSize, 0f)
        assertEquals(light.mobileTypeTextSize, heavy.mobileTypeTextSize, 0f)
        assertTrue(heavy.mobileTypeWeight > light.mobileTypeWeight)
    }

    @Test
    fun invalidAndOutOfRangeValuesAreClampedSafely() {
        val fallback =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = Float.NaN,
                mobileTypeSizeScale = Float.NaN,
                mobileTypeWeight = Int.MIN_VALUE,
            )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_WIFI_SIZE_SCALE,
            fallback.wifiSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.DEFAULT_MOBILE_TYPE_SIZE_SCALE,
            fallback.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MIN_MOBILE_TYPE_WEIGHT,
            fallback.mobileTypeWeight,
        )

        val clamped =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 5f,
                mobileTypeSizeScale = 5f,
                mobileTypeWeight = 5000,
            )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_WIFI_SIZE_SCALE,
            clamped.wifiSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_MOBILE_TYPE_SIZE_SCALE,
            clamped.mobileTypeSizeScale,
            0f,
        )
        assertEquals(
            CombinedStatusCenterGeometry.MAX_MOBILE_TYPE_WEIGHT,
            clamped.mobileTypeWeight,
        )
    }

    @Test
    fun fiveGaAccessSuffixUsesLowerRightVerticalDirection() {
        assertEquals(
            8f,
            CombinedStatusMobileTypeSuffixPolicy.verticalOffset(
                suffix = "A",
                magnitude = 8f,
            ),
            0f,
        )
        assertEquals(
            -8f,
            CombinedStatusMobileTypeSuffixPolicy.verticalOffset(
                suffix = "++",
                magnitude = 8f,
            ),
            0f,
        )
    }

    @Test
    fun nativePeerDefaultsRemainOpticallyMatchedButDoNotFollowWifiScaling() {
        val base =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )
        val enlargedWifi =
            CombinedStatusCenterGeometry.resolve(
                wifiSizeScale = 1.2f,
                mobileTypeSizeScale = 1f,
                mobileTypeWeight = 800,
            )

        assertEquals(base.wifiMaxWidth, base.airplaneMaxSize, 0f)
        assertEquals(base.airplaneMaxSize, enlargedWifi.airplaneMaxSize, 0f)
        assertEquals(base.noSimMaxSize, enlargedWifi.noSimMaxSize, 0f)
    }
}
