package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterRenderSessionTest {
    @Test
    fun projectionReadinessRequiresSingleOwnedNativeCarrier() {
        assertTrue(CombinedStatusControlCenterRenderSession.resolveProjectionReady(true, true, true, true, true, true))
        assertFalse(CombinedStatusControlCenterRenderSession.resolveProjectionReady(true, true, true, true, true, false))
        assertFalse(CombinedStatusControlCenterRenderSession.resolveProjectionReady(true, true, false, true, true, true))
    }
}
