package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNativeNetworkSuppressionOwnerTest {
    @Test
    fun activeMobileVisualMaskMakesNativeSignalContainerTransparent() {
        assertEquals(
            0f,
            SystemUiNativeNetworkSuppressionOwner.resolveMobileVisualMaskAlpha(
                nativeAlpha = 1f,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveMobileVisualMaskPreservesNativeAlpha() {
        assertEquals(
            0.65f,
            SystemUiNativeNetworkSuppressionOwner.resolveMobileVisualMaskAlpha(
                nativeAlpha = 0.65f,
                suppressionActive = false,
            ),
        )
    }
    @Test
    fun airplaneModeKeepsNativeMobileSuppressedWhileRootsDisappear() {
        assertEquals(
            true,
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = true,
                presentation = null,
                wasSuppressed = false,
            ),
        )
    }

    @Test
    fun airplaneExitKeepsPreviousSuppressionThroughUnknownPresentationGap() {
        val unknown =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.UNKNOWN,
                boundRoots = 2,
                visibleRoots = 0,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = null,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(
            true,
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = unknown,
                wasSuppressed = true,
            ),
        )
        assertEquals(
            false,
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = unknown,
                wasSuppressed = false,
            ),
        )
    }

    @Test
    fun knownNonReplaceableMobilePresentationReleasesStickySuppression() {
        val dualSeparate =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
                boundRoots = 2,
                visibleRoots = 2,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 4,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(
            false,
            NativeNetworkSuppressionPolicy.suppressMobile(
                airplaneMode = false,
                presentation = dualSeparate,
                wasSuppressed = true,
            ),
        )
    }


    @Test
    fun staticSystemSlotsAreSuppressedOnlyWhenTheirReplacementIsReady() {
        assertEquals(
            true,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            true,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = false,
                noSimSuppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "alarm_clock",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "airplane",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldSuppressStaticSlot(
                slot = "no_sim",
                airplaneSuppressionActive = true,
                noSimSuppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
    }
    @Test
    fun peerAppliedTintWinsOverManagerAndCachedFallback() {
        assertEquals(
            0xfff2f2f2.toInt(),
            SystemUiNativeNetworkSuppressionOwner.selectStatusIconTint(
                peerAppliedTint = 0xfff2f2f2.toInt(),
                managerTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
        assertEquals(
            0xdee5e5e5.toInt(),
            SystemUiNativeNetworkSuppressionOwner.selectStatusIconTint(
                peerAppliedTint = 0x00ffffff,
                managerTint = 0xdee5e5e5.toInt(),
                fallbackTint = 0xe6ffffff.toInt(),
            ),
        )
    }

    @Test
    fun mobilePreMaskRequiresActiveHomeOwnership() {
        assertEquals(
            true,
            SystemUiNativeNetworkSuppressionOwner.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = true,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldPreMaskMobileSignal(
                suppressionActive = true,
                belongsToActiveHomeGroup = false,
            ),
        )
        assertEquals(
            false,
            SystemUiNativeNetworkSuppressionOwner.shouldPreMaskMobileSignal(
                suppressionActive = false,
                belongsToActiveHomeGroup = true,
            ),
        )
    }

}
