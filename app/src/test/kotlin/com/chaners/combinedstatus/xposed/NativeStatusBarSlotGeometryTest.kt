package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeStatusBarSlotGeometryTest {
    @Test
    fun stableHomeMeasurementResolvesNativeBatteryOccupancy() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 478,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNotNull(resolved)
        assertEquals(105, resolved?.slotWidth)
        assertEquals(108, resolved?.slotHeight)
    }

    @Test
    fun transientBatteryViewExpansionDoesNotParticipateInSlotWidth() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 478,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertEquals(105, resolved?.slotWidth)
    }

    @Test
    fun visiblePrivacyOccupancyIsExcludedFromBatterySlot() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 448,
                privacyMeasuredWidth = 30,
                containerHeight = 108,
            )

        assertEquals(105, resolved?.slotWidth)
    }

    @Test
    fun unavailableSlotFailsClosed() {
        val resolved =
            NativeStatusBarSlotGeometry.resolve(
                containerWidth = 587,
                containerPaddingStart = 4,
                containerPaddingEnd = 0,
                statusIconsMeasuredWidth = 583,
                privacyMeasuredWidth = 0,
                containerHeight = 108,
            )

        assertNull(resolved)
    }
}
