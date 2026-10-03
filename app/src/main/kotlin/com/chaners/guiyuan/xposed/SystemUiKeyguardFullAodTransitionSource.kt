package com.chaners.guiyuan.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiKeyguardFullAodTransitionSource {
    const val HOOK_COUNT = 1

    private const val CONTROLLER_CLASS =
        "com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject"
    private const val ANIMATE_FULL_AOD_METHOD = "animateFullAod"
    private const val HOOK_ID =
        "combinedstatus.keyguardAod.animateFullAod"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTransition: () -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val controllerClass = Class.forName(CONTROLLER_CLASS, false, classLoader)
        val candidates =
            controllerClass.declaredMethods.filter { method ->
                method.name == ANIMATE_FULL_AOD_METHOD &&
                    matchesAnimateFullAodSignature(
                        parameterTypes = method.parameterTypes,
                        returnType = method.returnType,
                    )
            }
        val method =
            candidates.singleOrNull()
                ?.apply { isAccessible = true }
                ?: error(
                    "full-aod-animation-method-contract-" +
                        if (candidates.isEmpty()) "missing" else "ambiguous",
                )

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val rawArg0 = chain.getArg(0) as? Boolean
                        val rawArg1 = chain.getArg(1) as? Boolean
                        val result = chain.proceed()

                        // HyperOS has already committed the Keyguard-family target
                        // when animateFullAod returns. Consume that native boundary
                        // read-only; do not infer semantics from the raw booleans.
                        onTransition()
                        onEvent?.invoke(
                            "keyguardFullAod source=animateFullAod" +
                                " arg0=" + (rawArg0 ?: "unavailable") +
                                " arg1=" + (rawArg1 ?: "unavailable") +
                                " eventDriven=true readOnly=true nativeGeometryWrites=0",
                        )
                        result
                    },
                )
        return listOf(handle)
    }

    internal fun matchesAnimateFullAodSignature(
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    ): Boolean =
        parameterTypes.size == 2 &&
            isBooleanType(parameterTypes[0]) &&
            isBooleanType(parameterTypes[1]) &&
            returnType == Void.TYPE

    private fun isBooleanType(type: Class<*>): Boolean =
        type == Boolean::class.javaPrimitiveType ||
            type == Boolean::class.javaObjectType
}
