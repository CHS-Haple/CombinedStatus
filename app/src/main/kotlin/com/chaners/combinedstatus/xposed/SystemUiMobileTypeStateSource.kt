package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiMobileTypeStateSource {
    const val CLASS_NAME = "com.miui.systemui.statusbar.views.MobileTypeDrawable"
    const val METHOD_NAME = "measure"
    const val HOOK_COUNT = 1
    private const val HOOK_ID = "combinedstatus.mobileType.measure"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onChanged: () -> Unit,
    ): List<HookHandle> {
        val clazz = Class.forName(CLASS_NAME, false, classLoader)
        val method =
            clazz.getDeclaredMethod(METHOD_NAME)
                .apply { isAccessible = true }

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        onChanged()
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID
}
