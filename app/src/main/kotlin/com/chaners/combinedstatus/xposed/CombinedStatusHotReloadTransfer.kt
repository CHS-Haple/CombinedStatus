package com.chaners.combinedstatus.xposed

import android.os.Bundle
import android.view.View

internal object CombinedStatusHotReloadTransfer {
    private const val VERSION = 1
    private const val INDEX_VERSION = 0
    private const val INDEX_HOST = 1
    private const val INDEX_STATE = 2
    private const val INDEX_BINDINGS = 3
    private const val PAYLOAD_SIZE = 4

    fun capture(
        host: Any?,
        state: Bundle,
        bindings: Any,
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
        )
    }

    fun restore(raw: Any?): Restored? {
        val payload = raw as? Array<*> ?: return null
        if (payload.size != PAYLOAD_SIZE) {
            return null
        }

        val version = (payload.getOrNull(INDEX_VERSION) as? Number)?.toInt()
            ?: return null
        if (version != VERSION) {
            return null
        }

        val host = payload.getOrNull(INDEX_HOST) as? View ?: return null
        if (host.javaClass.name != StatusBarHostCapture.HOST_CLASS_NAME) {
            return null
        }

        val state = payload.getOrNull(INDEX_STATE) as? Bundle ?: return null
        val bindings = payload.getOrNull(INDEX_BINDINGS)

        return Restored(
            host = host,
            state = state,
            bindings = bindings,
        )
    }

    internal data class Restored(
        val host: View,
        val state: Bundle,
        val bindings: Any?,
    )
}
