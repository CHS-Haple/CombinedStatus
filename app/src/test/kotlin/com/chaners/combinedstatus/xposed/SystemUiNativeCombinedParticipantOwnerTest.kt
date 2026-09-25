package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
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
    fun handoffModeAllowsVisibleHome() {
        assertEquals(
            SystemUiNativeCombinedParticipantOwner.HandoffMode.VISIBLE_HOME,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModePrearmsHiddenHomeParticipantOnKeyguard() {
        assertEquals(
            SystemUiNativeCombinedParticipantOwner.HandoffMode.PREARMED_KEYGUARD,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                rootShown = false,
            ),
        )
    }

    @Test
    fun handoffModeDoesNotPrearmVisibleParticipantOnKeyguard() {
        assertEquals(
            SystemUiNativeCombinedParticipantOwner.HandoffMode.BLOCKED,
            SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                surface = SystemUiSceneStateSource.Surface.KEYGUARD,
                rootShown = true,
            ),
        )
    }

    @Test
    fun handoffModeFailsClosedForUnknownAndShadeLocked() {
        listOf(
            SystemUiSceneStateSource.Surface.UNKNOWN,
            SystemUiSceneStateSource.Surface.SHADE_LOCKED,
        ).forEach { surface ->
            assertEquals(
                SystemUiNativeCombinedParticipantOwner.HandoffMode.BLOCKED,
                SystemUiNativeCombinedParticipantOwner.resolveHandoffMode(
                    surface = surface,
                    rootShown = false,
                ),
            )
        }
    }


    @Test
    fun islandSlotTakesOverBatteryOccupancyOnlyWhileNativeBatteryIsHidden() {
        assertEquals(
            105,
            SystemUiNativeCombinedParticipantOwner.resolveIslandSlotWidth(
                nativeBatteryHidden = true,
                visualWidth = 105,
            ),
        )
        assertEquals(
            0,
            SystemUiNativeCombinedParticipantOwner.resolveIslandSlotWidth(
                nativeBatteryHidden = false,
                visualWidth = 105,
            ),
        )
    }

    @Test
    fun islandSlotFailsClosedWhenVisualWidthIsUnavailable() {
        assertEquals(
            0,
            SystemUiNativeCombinedParticipantOwner.resolveIslandSlotWidth(
                nativeBatteryHidden = true,
                visualWidth = 0,
            ),
        )
    }

    @Test
    fun nativeBindingTintWinsOverLegacyVisualSlotTint() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                        statusIconTint = 0xff202020.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xbf000000.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun nativeBindingTintRemainsAuthorityWhenVisualSlotTintIsUnavailable() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xbf000000.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun transparentNativeBindingTintFallsBackWithoutOverwritingAnchor() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                    ),
                nativeTint = 0x00303030,
            )

        assertEquals(0xbf000000.toInt(), merged.appliedTint)
        assertEquals(null, merged.statusIconTint)
    }

}