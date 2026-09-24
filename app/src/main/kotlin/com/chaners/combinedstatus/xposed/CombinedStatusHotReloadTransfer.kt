package com.chaners.combinedstatus.xposed

import android.os.Bundle
import android.view.View

internal object CombinedStatusHotReloadTransfer {
    private const val VERSION = 4
    private const val NATIVE_TRANSFER_VERSION = 3
    private const val VISUAL_TRANSFER_VERSION = 2
    private const val LEGACY_VERSION = 1
    private const val INDEX_VERSION = 0
    private const val INDEX_HOST = 1
    private const val INDEX_STATE = 2
    private const val INDEX_BINDINGS = 3
    private const val CURRENT_PAYLOAD_SIZE = 4
    private const val LEGACY_PAYLOAD_SIZE = 4
    private const val VISUAL_PAYLOAD_SIZE = 5
    private const val NATIVE_PAYLOAD_SIZE = 6

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
        val version = (payload.getOrNull(INDEX_VERSION) as? Number)?.toInt()
            ?: return null
        val expectedSize =
            when (version) {
                VERSION -> CURRENT_PAYLOAD_SIZE
                NATIVE_TRANSFER_VERSION -> NATIVE_PAYLOAD_SIZE
                VISUAL_TRANSFER_VERSION -> VISUAL_PAYLOAD_SIZE
                LEGACY_VERSION -> LEGACY_PAYLOAD_SIZE
                else -> return null
            }
        if (payload.size != expectedSize) {
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
