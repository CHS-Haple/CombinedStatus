package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNativeNetworkSuppressionOwnerTest {
    @Test
    fun activeSuppressionForcesHiddenVisibilityState() {
        assertEquals(
            2,
            SystemUiNativeNetworkSuppressionOwner.resolveEffectiveVisibilityState(
                nativeVisibilityState = 0,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeVisibilityState() {
        assertEquals(
            1,
            SystemUiNativeNetworkSuppressionOwner.resolveEffectiveVisibilityState(
                nativeVisibilityState = 1,
                suppressionActive = false,
            ),
        )
    }
}
