package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiKeyguardAodStateSourceTest {
    @Test
    fun pinnedToggleAodContractRequiresSingleBooleanParameter() {
        assertTrue(
            SystemUiKeyguardAodStateSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Boolean::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SystemUiKeyguardAodStateSource.matchesToggleAodSignature(
                parameterTypes = emptyArray(),
                returnType = Void.TYPE,
            ),
        )
        assertFalse(
            SystemUiKeyguardAodStateSource.matchesToggleAodSignature(
                parameterTypes = arrayOf<Class<*>>(Int::class.javaPrimitiveType!!),
                returnType = Void.TYPE,
            ),
        )
    }

    @Test
    fun anyNativeAodSignalBlocksKeyguardProjection() {
        assertFalse(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, false))
        assertTrue(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(true, false, false))
        assertTrue(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, true, false))
        assertFalse(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, true))
        assertFalse(SystemUiKeyguardAodStateSource.blocksKeyguardProjection(false, false, null))
    }
}
