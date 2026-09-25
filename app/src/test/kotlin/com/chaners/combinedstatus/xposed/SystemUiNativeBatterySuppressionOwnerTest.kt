package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeBatterySuppressionOwnerTest {
    @Test
    fun activeSuppressionHidesBatteryLayoutRegardlessOfNativeRequest() {
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveHide(
                nativeRequestedHide = false,
                suppressionActive = true,
            ),
        )
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveHide(
                nativeRequestedHide = true,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeHideRequest() {
        assertFalse(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveHide(
                nativeRequestedHide = false,
                suppressionActive = false,
            ),
        )
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveHide(
                nativeRequestedHide = true,
                suppressionActive = false,
            ),
        )
    }

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
