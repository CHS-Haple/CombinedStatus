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
}
