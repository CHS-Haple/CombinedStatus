package com.chaners.combinedstatus.xposed

import java.lang.ref.WeakReference

internal object SystemUiHostRegistry {
    private var statusHost = WeakReference<Any>(null)

    @Synchronized
    fun captureStatusHost(host: Any): Capture? {
        val previous = statusHost.get()
        if (previous === host) {
            return null
        }

        statusHost = WeakReference(host)
        return Capture(
            className = host.javaClass.name,
            identity = System.identityHashCode(host),
            replacement = previous != null,
        )
    }

    internal data class Capture(
        val className: String,
        val identity: Int,
        val replacement: Boolean,
    )
}
