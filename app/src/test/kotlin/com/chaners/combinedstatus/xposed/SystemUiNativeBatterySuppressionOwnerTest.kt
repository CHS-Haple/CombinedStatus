package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeBatterySuppressionOwnerTest {
    @Test
    fun activeSuppressionHidesBatteryRegardlessOfNativeRequest() {
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
    fun inactiveSuppressionPreservesNativeRequest() {
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
}
