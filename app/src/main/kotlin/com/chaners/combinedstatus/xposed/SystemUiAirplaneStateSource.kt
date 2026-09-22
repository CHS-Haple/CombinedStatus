package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiAirplaneStateSource {
    const val HOOK_COUNT = 1

    private const val REPOSITORY_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.airplane.data.repository.impl.AirplaneModeRepositoryImpl"
    private const val SET_AIRPLANE_MODE_METHOD_NAME = "setIsAirplaneMode"
    private const val HOOK_ID = "combinedstatus.airplane.repository.set"

    @Volatile
    private var lastRequestedState: Boolean? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onAirplaneMode: (Boolean) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val repositoryClass =
            Class.forName(REPOSITORY_CLASS_NAME, false, classLoader)
        val continuationClass =
            Class.forName("kotlin.coroutines.Continuation", false, classLoader)
        val method =
            repositoryClass.getDeclaredMethod(
                SET_AIRPLANE_MODE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                continuationClass,
            ).apply { isAccessible = true }

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val enabled = chain.getArg(0) as? Boolean
                        if (enabled != null) {
                            val changed =
                                synchronized(this) {
                                    val previous = lastRequestedState
                                    lastRequestedState = enabled
                                    previous != enabled
                                }
                            if (changed) {
                                onAirplaneMode(enabled)
                                onEvent?.invoke(
                                    "airplaneState repository phase=beforeProceed " +
                                        "enabled=" + enabled +
                                        " source=setIsAirplaneMode",
                                )
                            }
                        }
                        chain.proceed()
                    },
                )

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID
}
