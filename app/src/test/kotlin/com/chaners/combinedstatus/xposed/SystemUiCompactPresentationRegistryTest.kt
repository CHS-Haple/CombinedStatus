package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiCompactPresentationRegistryTest {
    @Test
    fun immediateSurfaceDoesNotRequireNativeLayoutGate() {
        assertTrue(
            SystemUiCompactPresentationRegistry.resolveCutoverReady(
                deferVisualMaskUntilLayout = false,
                compactLayoutReady = false,
            ),
        )
    }

    @Test
    fun deferredSurfaceRequiresCompletedNativeCompactLayout() {
        assertFalse(
            SystemUiCompactPresentationRegistry.resolveCutoverReady(
                deferVisualMaskUntilLayout = true,
                compactLayoutReady = false,
            ),
        )
        assertTrue(
            SystemUiCompactPresentationRegistry.resolveCutoverReady(
                deferVisualMaskUntilLayout = true,
                compactLayoutReady = true,
            ),
        )
    }
}
