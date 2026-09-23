package com.chaners.combinedstatus.xposed

import android.os.Bundle
import android.view.View

internal object CombinedStatusHotReloadTransfer {
    private const val VERSION = 3
    private const val PREVIOUS_VERSION = 2
    private const val LEGACY_VERSION = 1
    private const val INDEX_VERSION = 0
    private const val INDEX_HOST = 1
    private const val INDEX_STATE = 2
    private const val INDEX_BINDINGS = 3
    private const val INDEX_VISUAL = 4
    private const val INDEX_NATIVE_HOLDER = 5
    private const val LEGACY_PAYLOAD_SIZE = 4
    private const val PREVIOUS_PAYLOAD_SIZE = 5
    private const val PAYLOAD_SIZE = 6

    fun capture(
        host: Any?,
        state: Bundle,
        bindings: Any,
        visual: View?,
        nativeHolder: Any?,
    ): Any? {
        val hostView = host as? View ?: return null
        if (hostView.javaClass.name != StatusBarHostCapture.HOST_CLASS_NAME) {
            return null
        }

        return arrayOf(
            VERSION,
            hostView,
            state,
            bindings,
            visual,
            nativeHolder,
        )
    }

    fun restore(raw: Any?): Restored? {
        val payload = raw as? Array<*> ?: return null
        if (
            payload.size != PAYLOAD_SIZE &&
            payload.size != PREVIOUS_PAYLOAD_SIZE &&
            payload.size != LEGACY_PAYLOAD_SIZE
        ) {
            return null
        }

        val version = (payload.getOrNull(INDEX_VERSION) as? Number)?.toInt()
            ?: return null
        if (
            version != VERSION &&
            version != PREVIOUS_VERSION &&
            version != LEGACY_VERSION
        ) {
            return null
        }

        val host = payload.getOrNull(INDEX_HOST) as? View ?: return null
        if (host.javaClass.name != StatusBarHostCapture.HOST_CLASS_NAME) {
            return null
        }

        val state = payload.getOrNull(INDEX_STATE) as? Bundle ?: return null
        val bindings = payload.getOrNull(INDEX_BINDINGS)
        val visual =
            if (version >= PREVIOUS_VERSION) {
                payload.getOrNull(INDEX_VISUAL) as? View
            } else {
                null
            }
        val nativeHolder =
            if (version >= VERSION) {
                payload.getOrNull(INDEX_NATIVE_HOLDER)
            } else {
                null
            }

        return Restored(
            host = host,
            state = state,
            bindings = bindings,
            visual = visual,
            nativeHolder = nativeHolder,
        )
    }

    internal data class Restored(
        val host: View,
        val state: Bundle,
        val bindings: Any?,
        val visual: View?,
        val nativeHolder: Any?,
    )
}
