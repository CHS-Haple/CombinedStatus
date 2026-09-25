package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedModule

internal object SystemUiBatteryRuntimeOwner {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.hooks ?: 0

    internal data class AttachResult(
        val hooks: Int,
    ) {
        val ready: Boolean
            get() = hooks == SystemUiBatteryStateSource.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onBatteryPresentationApplied: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): AttachResult =
        AttachResult(
            hooks =
                SystemUiBatteryStateSource.install(
                    module = module,
                    classLoader = classLoader,
                    onBatteryState = onBatteryState,
                    onBatteryPresentationApplied = onBatteryPresentationApplied,
                    onEvent = onEvent,
                ).size,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SystemUiBatteryStateSource.resetRuntimeState()
    }
}
