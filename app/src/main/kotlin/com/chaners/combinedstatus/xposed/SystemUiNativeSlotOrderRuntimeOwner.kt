package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiNativeSlotOrderRuntimeOwner {
    private const val STATUS_BAR_ICON_LIST =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconList"
    private const val HOOK_ID =
        "combinedstatus.nativeSlotOrder.constructor"

    private var constructorHook: HookHandle? = null

    val installedHookCount: Int
        @Synchronized get() = if (constructorHook != null) 1 else 0

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): InstallResult {
        if (constructorHook != null) {
            return InstallResult.AlreadyInstalled
        }

        val iconListClass =
            runCatching {
                Class.forName(STATUS_BAR_ICON_LIST, false, classLoader)
            }.getOrNull()
                ?: return InstallResult.Failure("status-bar-icon-list-class-missing")

        val constructor =
            iconListClass.declaredConstructors
                .firstOrNull { candidate ->
                    candidate.parameterCount == 1 &&
                        candidate.parameterTypes[0].isArray &&
                        candidate.parameterTypes[0].componentType == String::class.java
                }
                ?: return InstallResult.Failure(
                    "status-bar-icon-list-string-array-constructor-missing",
                )
        constructor.isAccessible = true

        val handle =
            runCatching {
                module
                    .hook(constructor)
                    .setId(HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            @Suppress("UNCHECKED_CAST")
                            val original =
                                chain.getArg(0) as? Array<String>
                                    ?: return@Hooker chain.proceed()

                            val existingIndex =
                                original.indexOf(SystemUiNativeCombinedParticipantOwner.SLOT)
                            if (existingIndex >= 0) {
                                onEvent?.invoke(
                                    "nativeSlotOrder predeclared " +
                                        "slot=" +
                                        SystemUiNativeCombinedParticipantOwner.SLOT +
                                        " index=" + existingIndex +
                                        " source=existing nativeGeometryWrites=0",
                                )
                                return@Hooker chain.proceed()
                            }

                            val extended =
                                java.util.Arrays.copyOf(
                                    original,
                                    original.size + 1,
                                )
                            extended[original.size] =
                                SystemUiNativeCombinedParticipantOwner.SLOT

                            val args = chain.getArgs().toTypedArray()
                            args[0] = extended

                            onEvent?.invoke(
                                "nativeSlotOrder predeclare " +
                                    "slot=" +
                                    SystemUiNativeCombinedParticipantOwner.SLOT +
                                    " original=" + original.size +
                                    " extended=" + extended.size +
                                    " targetIndex=" + original.size +
                                    " mode=constructor-append nativeGeometryWrites=0",
                            )
                            chain.proceed(args)
                        },
                    )
            }.getOrElse { error ->
                return InstallResult.Failure(
                    "constructor-hook-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        constructorHook = handle
        return InstallResult.Installed
    }

    @Synchronized
    fun resetRuntimeState() {
        constructorHook = null
    }

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult

        data class Failure(
            val reason: String,
        ) : InstallResult
    }
}
