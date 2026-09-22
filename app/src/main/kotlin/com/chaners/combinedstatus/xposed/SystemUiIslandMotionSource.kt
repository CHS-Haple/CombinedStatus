package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiIslandMotionSource {
    const val HOOK_COUNT = 1

    private const val ISLAND_LISTENER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector\$islandListener\$1"
    private const val ISLAND_STATUS_METHOD_NAME = "onIslandStatusChanged"
    private const val HOOK_ID = "combinedstatus.island.home.status"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onIslandStatusChanged: (IslandStatus) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val listenerClass =
            Class.forName(ISLAND_LISTENER_CLASS_NAME, false, classLoader)
        val method =
            listenerClass.getDeclaredMethod(
                ISLAND_STATUS_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val showing = chain.getArg(0) as? Boolean ?: false
                        val secondary = chain.getArg(1) as? Boolean ?: false
                        val animate = chain.getArg(2) as? Boolean ?: false
                        val result = chain.proceed()

                        val status =
                            IslandStatus(
                                showing = showing,
                                secondary = secondary,
                                animate = animate,
                            )
                        onIslandStatusChanged(status)
                        onEvent?.invoke(
                            "islandMotion nativeStatus showing=" + showing +
                                " secondary=" + secondary +
                                " animate=" + animate +
                                " follow=nativeAnchor",
                        )
                        result
                    },
                )

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    internal data class IslandStatus(
        val showing: Boolean,
        val secondary: Boolean,
        val animate: Boolean,
    )
}
