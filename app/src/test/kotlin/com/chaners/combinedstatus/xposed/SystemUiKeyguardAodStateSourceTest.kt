package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardAodStateSourceTest {
    @Test
    fun anyNativeAodSignalBlocksKeyguardProjection() {
        assertFalse(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, false))
        assertTrue(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(true, false, false))
        assertTrue(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, true, false))
        assertTrue(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, true))
        assertFalse(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, null))
    }
}
