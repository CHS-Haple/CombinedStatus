package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedModule

internal object SystemUiIslandMotionRuntimeOwner {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.hookCount ?: 0

    internal data class AttachResult(
        val hookCount: Int,
    ) {
        val ready: Boolean
            get() = hookCount == SystemUiIslandMotionSource.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)?,
        isProbeEnabled: () -> Boolean,
    ): AttachResult {
        current = null
        val handles =
            SystemUiIslandMotionSource.install(
                module = module,
                classLoader = classLoader,
                onEvent = onEvent,
                isProbeEnabled = isProbeEnabled,
            )
        return AttachResult(
            hookCount = handles.size,
        ).also { current = it }
    }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SystemUiIslandMotionSource.resetRuntimeState()
    }
}
