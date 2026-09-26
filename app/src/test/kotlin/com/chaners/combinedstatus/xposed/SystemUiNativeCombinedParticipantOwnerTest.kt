package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiNativeCombinedParticipantOwnerTest {
    @Test
    fun compensatedBridgeAcceptsNativeWidthWithZeroEffectiveOccupancy() {
        assertTrue(
            SystemUiNativeCombinedParticipantOwner.isCompensatedSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                rootPaddingStart = 0,
                rootPaddingEnd = -105,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootAnchorScreenX = 1242f,
                batteryAnchorScreenX = 1242f,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun compensatedBridgeRejectsDuplicateNativeOccupancy() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isCompensatedSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                rootPaddingStart = 0,
                rootPaddingEnd = 0,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootAnchorScreenX = 1242f,
                batteryAnchorScreenX = 1242f,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun compensatedBridgeRejectsLegacyZeroWidthShell() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isCompensatedSlotHandoffReady(
                rootMeasuredWidth = 0,
                rootMeasuredHeight = 108,
                rootPaddingStart = 0,
                rootPaddingEnd = 0,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootAnchorScreenX = 1242f,
                batteryAnchorScreenX = 1242f,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
    }

    @Test
    fun compensatedBridgeRejectsClippedOrMisalignedGeometry() {
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isCompensatedSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                rootPaddingStart = 0,
                rootPaddingEnd = -105,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = true,
                rootAnchorScreenX = 1242f,
                batteryAnchorScreenX = 1242f,
                renderLeft = 0,
                renderRight = 105,
            ),
        )
        assertFalse(
            SystemUiNativeCombinedParticipantOwner.isCompensatedSlotHandoffReady(
                rootMeasuredWidth = 105,
                rootMeasuredHeight = 108,
                rootPaddingStart = 0,
                rootPaddingEnd = -105,
                renderMeasuredWidth = 105,
                renderMeasuredHeight = 108,
                expectedVisualWidth = 105,
                expectedVisualHeight = 108,
                parentClipsChildren = false,
                rootAnchorScreenX = 1243f,
                batteryAnchorScreenX = 1242f,
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
    fun visibleBatteryUsesFullShellWithZeroEffectiveOccupancy() {
        val geometry =
            SystemUiNativeCombinedParticipantOwner.resolveNativeShellGeometry(
                nativeBatteryHidden = false,
                visualWidth = 105,
            )

        assertEquals(105, geometry?.width)
        assertEquals(0, geometry?.paddingStart)
        assertEquals(-105, geometry?.paddingEnd)
        assertEquals(0, geometry?.effectiveOccupancy)
    }

    @Test
    fun hiddenBatteryLetsFullShellConsumeNativeSlot() {
        val geometry =
            SystemUiNativeCombinedParticipantOwner.resolveNativeShellGeometry(
                nativeBatteryHidden = true,
                visualWidth = 105,
            )

        assertEquals(105, geometry?.width)
        assertEquals(0, geometry?.paddingStart)
        assertEquals(0, geometry?.paddingEnd)
        assertEquals(105, geometry?.effectiveOccupancy)
    }

    @Test
    fun shellGeometryFailsClosedWithoutVisualWidth() {
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeShellGeometry(
                nativeBatteryHidden = false,
                visualWidth = 0,
            ),
        )
    }

    @Test
    fun nativeBindingTintBecomesSingleResolvedTintAuthority() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                        statusIconTint = 0xff202020.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xff303030.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun nativeBindingTintDoesNotDependOnBatteryAnchor() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                    ),
                nativeTint = 0xff303030.toInt(),
            )

        assertEquals(0xff303030.toInt(), merged.appliedTint)
        assertEquals(0xff303030.toInt(), merged.statusIconTint)
    }

    @Test
    fun missingNativeTintDropsLegacyStatusIconFallbackAndUsesBatteryAnchor() {
        val merged =
            SystemUiNativeCombinedParticipantOwner.mergeNativeParticipantTint(
                batteryTint =
                    CombinedStatusTintState(
                        appliedTint = 0xbf101010.toInt(),
                        statusIconTint = 0xff202020.toInt(),
                    ),
                nativeTint = null,
            )

        assertEquals(0xbf101010.toInt(), merged.appliedTint)
        assertEquals(null, merged.statusIconTint)
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

    @Test
    fun masterSwitchBlocksHomeOverlayRegardlessOfSceneOrHandoffState() {
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                sceneAllowsOverlay = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                sceneAllowsOverlay = true,
                nativeHandoffActive = true,
            ),
        )
    }

    @Test
    fun enabledMasterSwitchStillDefersToSceneAndNativeHandoff() {
        assertTrue(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = false,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                nativeHandoffActive = true,
            ),
        )
    }


    @Test
    fun nativeIconStateShowsCombinedRendererAndHidesDot() {
        val visibility =
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
                state = 7,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.VISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.GONE, visibility?.dotVisibility)
    }

    @Test
    fun nativeDotStateUsesSystemDotWithoutCombinedRenderer() {
        val visibility =
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
                state = 8,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.INVISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.VISIBLE, visibility?.dotVisibility)
    }

    @Test
    fun nativeHiddenStateKeepsShellButDrawsNoCombinedContent() {
        val visibility =
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
                state = 9,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            )

        assertEquals(android.view.View.INVISIBLE, visibility?.renderVisibility)
        assertEquals(android.view.View.INVISIBLE, visibility?.dotVisibility)
    }

    @Test
    fun unknownNativeVisibleStateFailsClosed() {
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeContentVisibility(
                state = 99,
                iconState = 7,
                dotState = 8,
                hiddenState = 9,
            ),
        )
    }


    @Test
    fun validatedMasterSwitchUsesNativeRemoveLifecycle() {
        assertEquals(
            false,
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = true,
                handoffValidated = true,
            ),
        )
        assertEquals(
            true,
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = true,
            ),
        )
    }

    @Test
    fun unvalidatedMasterSwitchStaysOnBootstrapFallback() {
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeFeatureRemoveFlag(
                featureEnabled = false,
                handoffValidated = false,
            ),
        )
    }

    @Test
    fun nativeVisibleStateNamesResolveWithoutAssumingNumericOrder() {
        val states =
            SystemUiNativeCombinedParticipantOwner.resolveNativeVisibilityStates { candidate ->
                when (candidate) {
                    2 -> "ICON"
                    4 -> "DOT"
                    7 -> "HIDDEN"
                    else -> "UNKNOWN"
                }
            }

        assertEquals(2, states?.icon)
        assertEquals(4, states?.dot)
        assertEquals(7, states?.hidden)
    }

    @Test
    fun missingNativeVisibleStateFailsClosed() {
        assertEquals(
            null,
            SystemUiNativeCombinedParticipantOwner.resolveNativeVisibilityStates { candidate ->
                when (candidate) {
                    0 -> "ICON"
                    1 -> "DOT"
                    else -> "UNKNOWN"
                }
            },
        )
    }
}