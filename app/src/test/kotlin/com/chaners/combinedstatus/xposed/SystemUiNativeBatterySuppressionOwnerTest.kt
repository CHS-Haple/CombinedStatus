package com.chaners.combinedstatus.xposed

import android.view.View
import org.junit.Assert.assertEquals
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

    @Test
    fun activeSuppressionKeepsChargingSlotButRemovesGlyph() {
        assertEquals(
            View.INVISIBLE,
            SystemUiNativeBatterySuppressionOwner.resolveChargingPresentationVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeGoneState() {
        assertEquals(
            View.GONE,
            SystemUiNativeBatterySuppressionOwner.resolveChargingPresentationVisibility(
                nativeVisibility = View.GONE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeInvisibleState() {
        assertEquals(
            View.INVISIBLE,
            SystemUiNativeBatterySuppressionOwner.resolveChargingPresentationVisibility(
                nativeVisibility = View.INVISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeVisibility() {
        assertEquals(
            View.VISIBLE,
            SystemUiNativeBatterySuppressionOwner.resolveChargingPresentationVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = false,
            ),
        )
    }
}
