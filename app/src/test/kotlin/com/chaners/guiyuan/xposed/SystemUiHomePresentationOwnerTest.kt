package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun persistentIgnoredSlotRestoreRemovesOnlySessionOwnedDelta() {
        val existing = listOf("alarm_clock", "wifi")
        val requested = listOf("wifi", "mobile", "no_sim")
        val owned =
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy.ownedDelta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf("mobile", "no_sim"), owned)

        val live = listOf("alarm_clock", "wifi", "mobile", "no_sim", "vpn")
        assertEquals(
            listOf("alarm_clock", "wifi", "vpn"),
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }

    @Test
    fun persistentIgnoredSlotRestoreAvoidsNativeSetterDuringContinuousHandoff() {
        assertFalse(
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy
                .shouldUseNativeSetterOnRestore(
                    requestLayout = false,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy
                .shouldUseNativeSetterOnRestore(
                    requestLayout = true,
                ),
        )
    }

    @Test
    fun endReservationKeepsOneResolvedEndBoundaryAcrossBatteryStates() {
        assertEquals(
            0,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 105,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            -30,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            105,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
    }

    @Test
    fun deferredControlCenterCutoverPreservesNativeVisualsUntilCompactLayout() {
        assertTrue(
            SystemUiHomePresentationOwner.VisualMaskPolicy
                .shouldPreserveNativeBeforeCompactCutover(
                    deferVisualMaskUntilLayout = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy
                .shouldPreserveNativeBeforeCompactCutover(
                    deferVisualMaskUntilLayout = false,
                ),
        )
    }

    @Test
    fun lateEligibleControlCenterCanAdoptAlreadyCompletedNativeLayout() {
        assertTrue(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = false,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 0,
                height = 108,
            ),
        )
    }

    @Test
    fun continuousHotReloadHandoffSuppressesIntermediateLayoutRequest() {
        assertFalse(
            SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = true,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = false,
                ),
        )
    }

    @Test
    fun transientLiveBatteryWidthLossIsDeferredOnlyAfterControlCenterCutover() {
        assertTrue(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = true,
                    compactLayoutReady = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = true,
                    compactLayoutReady = false,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = false,
                    compactLayoutReady = true,
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
