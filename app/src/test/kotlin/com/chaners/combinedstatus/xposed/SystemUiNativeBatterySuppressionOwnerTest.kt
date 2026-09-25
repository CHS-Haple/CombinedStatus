package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNativeBatterySuppressionOwnerTest {
    @Test
    fun activePresentationMaskMakesChildTransparent() {
        assertEquals(
            0f,
            SystemUiNativeBatterySuppressionOwner.resolvePresentationChildAlpha(
                nativeAlpha = 1f,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactivePresentationMaskPreservesNativeAlpha() {
        assertEquals(
            0.7f,
            SystemUiNativeBatterySuppressionOwner.resolvePresentationChildAlpha(
                nativeAlpha = 0.7f,
                suppressionActive = false,
            ),
        )
    }
}
