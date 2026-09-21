package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedModule

internal object StatusBarHostCapture {
    const val HOST_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer"
    const val HOST_READY_METHOD_NAME = "onFinishInflate"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onCaptured: (SystemUiHostRegistry.Capture) -> Unit,
    ) {
        val hostClass = Class.forName(HOST_CLASS_NAME, false, classLoader)
        val hostReadyMethod = hostClass.getDeclaredMethod(HOST_READY_METHOD_NAME)

        module.hook(hostReadyMethod).intercept { chain ->
            val result = chain.proceed()

            chain.thisObject?.let { host ->
                SystemUiHostRegistry.captureStatusHost(host)?.let(onCaptured)
            }

            result
        }
    }
}
