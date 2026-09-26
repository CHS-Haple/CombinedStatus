package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeBatterySuppressionOwnerTest {
    @Test
    fun replacementOwnsEffectiveHideWhileNativeWantsBatteryVisible() {
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveBatteryHide(
                nativeRequestedHide = false,
                replacementActive = true,
            ),
        )
    }

    @Test
    fun nativeHideRemainsAuthoritativeWhenReplacementIsInactive() {
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveBatteryHide(
                nativeRequestedHide = true,
                replacementActive = false,
            ),
        )
        assertFalse(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveBatteryHide(
                nativeRequestedHide = false,
                replacementActive = false,
            ),
        )
    }

    @Test
    fun nativeHideAndReplacementComposeWithoutClearingEitherRequest() {
        assertTrue(
            SystemUiNativeBatterySuppressionOwner.resolveEffectiveBatteryHide(
                nativeRequestedHide = true,
                replacementActive = true,
            ),
        )
    }
}
