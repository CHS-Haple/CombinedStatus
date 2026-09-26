package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiHomePresentationOwnerTest {
    @Test
    fun temporaryEntriesPreserveExistingAndRestoreOnlyOwnedEntries() {
        val slots = mutableListOf("alarm_clock", "wifi")
        SystemUiHomePresentationOwner.OwnedListEntries.withTemporaryEntries(
            target = slots,
            entries = listOf("wifi", "mobile", "no_sim"),
        ) {
            assertEquals(
                listOf("alarm_clock", "wifi", "mobile", "no_sim"),
                slots,
            )
        }
        assertEquals(listOf("alarm_clock", "wifi"), slots)
    }

    @Test
    fun carrierReservationPreservesNativeStateOutsideOwnedLayoutScope() {
        assertEquals(
            false,
            SystemUiHomePresentationOwner.CarrierReservationPolicy.resolveLayoutHide(
                nativeHide = true,
                presentationActive = true,
            ),
        )
        assertEquals(
            true,
            SystemUiHomePresentationOwner.CarrierReservationPolicy.resolveLayoutHide(
                nativeHide = true,
                presentationActive = false,
            ),
        )
        assertEquals(
            false,
            SystemUiHomePresentationOwner.CarrierReservationPolicy.resolveLayoutHide(
                nativeHide = false,
                presentationActive = true,
            ),
        )
    }

    @Test
    fun temporaryEntriesRestoreAfterFailure() {
        val slots = mutableListOf("alarm_clock")
        runCatching {
            SystemUiHomePresentationOwner.OwnedListEntries.withTemporaryEntries(
                target = slots,
                entries = listOf("wifi", "mobile"),
            ) {
                error("expected")
            }
        }
        assertEquals(listOf("alarm_clock"), slots)
        assertTrue("wifi" !in slots && "mobile" !in slots)
    }
}
