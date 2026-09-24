package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedModule

internal object SystemUiNetworkRuntimeOwner {
    private var current: SystemUiNetworkStateSource.InstallResult? = null

    val installedHookCount: Int
        @Synchronized get() = current?.handles?.size ?: 0

    val wifiReady: Boolean
        @Synchronized get() = current?.wifiReady == true

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (CombinedStatusStateStore.WifiState) -> Unit,
        onMobileIcon: (CombinedStatusStateStore.MobileIconUpdate) -> Unit,
        onPresentationChanged: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): SystemUiNetworkStateSource.InstallResult =
        SystemUiNetworkStateSource.install(
            module = module,
            classLoader = classLoader,
            onWifiState = onWifiState,
            onMobileIcon = onMobileIcon,
            onPresentationChanged = onPresentationChanged,
            onEvent = onEvent,
        ).also { current = it }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SystemUiNetworkStateSource.resetEventState()
    }
}
