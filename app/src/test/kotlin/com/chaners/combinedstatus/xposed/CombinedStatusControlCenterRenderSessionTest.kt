package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterRenderSessionTest {
    @Test
    fun projectionReadinessRequiresPreparedFakeRootAndNativeMask() {
        assertTrue(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                maskReady = true,
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                maskReady = false,
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = false,
                layoutReady = true,
                hostAttached = true,
                maskReady = true,
            ),
        )
    }
}
