package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeCombinedParticipantOwnerTest {
    @Test
    fun zeroSlotBridgeAcceptsPreservedBatteryGeometry() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                batteryScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsDuplicateLayoutWidth() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                batteryScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsCenteredChildOverflow() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1242,
                batteryScreenX = 1242,
                renderLeft = -52,
                renderRight = 53,
            ),
        )
    }

    @Test
    fun zeroSlotBridgeRejectsClippedOrMisalignedOverflow() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = true,
                rootScreenX = 1242,
                batteryScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isZeroSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootScreenX = 1238,
                batteryScreenX = 1242,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }
    @Test
    fun committedParticipantStaysLogicallyVisibleAcrossKeyguard() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.resolveSceneBindingVisibility(
                handoffCommitted = true,
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
            ),
        )
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.resolveSceneBindingVisibility(
                handoffCommitted = true,
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
    }

    @Test
    fun uncommittedParticipantStillRequiresUnlockedHome() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.resolveSceneBindingVisibility(
                handoffCommitted = false,
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
            ),
        )
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.resolveSceneBindingVisibility(
                handoffCommitted = false,
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
            ),
        )
    }

}
