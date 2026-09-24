package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal object SystemUiHotspotSlotOwner {
    const val SLOT = "hotspot"
    const val HOOK_COUNT = 2

    private const val CONTROLLER_IMPL =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val SET_ICON_HOOK_ID =
        "combinedstatus.hotspot.setIcon"
    private const val SET_VISIBILITY_HOOK_ID =
        "combinedstatus.hotspot.setVisibility"

    private val installedHandles = mutableListOf<HookHandle>()
    private var visibilityMethod: Method? = null
    private var controllerRef: WeakReference<Any>? = null
    private var currentState = CombinedStatusStateStore.HotspotState()
    private var replacementActive = false
    private var internalVisibilityWrite = false
    private var stateSink: ((CombinedStatusStateStore.HotspotState) -> Unit)? = null
    private var eventSink: ((String) -> Unit)? = null

    val installedHookCount: Int
        @Synchronized get() = installedHandles.size

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onState: (CombinedStatusStateStore.HotspotState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): InstallResult {
        if (installedHandles.isNotEmpty()) {
            stateSink = onState
            eventSink = onEvent
            return InstallResult.AlreadyInstalled
        }

        val controllerClass =
            runCatching {
                Class.forName(CONTROLLER_IMPL, false, classLoader)
            }.getOrElse { error ->
                return InstallResult.Failure(
                    "controller-class-" + (error.message ?: error.javaClass.simpleName),
                )
            }

        val setIcon =
            controllerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "setIcon" &&
                        method.parameterTypes.toList() ==
                            listOf(
                                CharSequence::class.java,
                                String::class.java,
                                Int::class.javaPrimitiveType,
                            )
                }
                ?: return InstallResult.Failure("resource-setIcon-contract-missing")
        val setVisibility =
            controllerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "setIconVisibility" &&
                        method.parameterTypes.toList() ==
                            listOf(
                                String::class.java,
                                Boolean::class.javaPrimitiveType,
                            )
                }
                ?: return InstallResult.Failure("setIconVisibility-contract-missing")
        setIcon.isAccessible = true
        setVisibility.isAccessible = true
        visibilityMethod = setVisibility

        val created = mutableListOf<HookHandle>()
        return runCatching {
            created +=
                module
                    .hook(setIcon)
                    .setId(SET_ICON_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val result = chain.proceed()
                            val slot = chain.getArg(1) as? String
                            if (slot == SLOT) {
                                val controller = chain.thisObject
                                val resourceId =
                                    (chain.getArg(2) as? Number)
                                        ?.toInt()
                                        ?.takeIf { it != 0 }
                                if (controller != null) {
                                    controllerRef = WeakReference(controller)
                                }
                                publishResource(resourceId)
                            }
                            result
                        },
                    )
            created +=
                module
                    .hook(setVisibility)
                    .setId(SET_VISIBILITY_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val slot = chain.getArg(0) as? String
                            if (slot != SLOT || internalVisibilityWrite) {
                                return@Hooker chain.proceed()
                            }

                            val controller = chain.thisObject
                            val requestedVisible =
                                chain.getArg(1) as? Boolean
                                    ?: return@Hooker chain.proceed()
                            if (controller != null) {
                                controllerRef = WeakReference(controller)
                            }
                            publishVisibility(requestedVisible)

                            val suppress =
                                synchronized(this) {
                                    replacementActive && requestedVisible
                                }
                            if (suppress) {
                                null
                            } else {
                                chain.proceed()
                            }
                        },
                    )

            installedHandles.clear()
            installedHandles.addAll(created)
            stateSink = onState
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            created.forEach { handle -> runCatching { handle.unhook() } }
            installedHandles.clear()
            visibilityMethod = null
            controllerRef = null
            stateSink = onState
            eventSink = onEvent
            InstallResult.Failure(error.message ?: error.javaClass.simpleName)
        }
    }

    fun setReplacementActive(
        host: Any?,
        state: CombinedStatusStateStore.HotspotState,
        active: Boolean,
        source: String,
    ): String {
        val controller =
            synchronized(this) {
                if (currentState.visible == null && state.visible != null) {
                    currentState = currentState.copy(visible = state.visible)
                }
                if (currentState.iconResId == null && state.iconResId != null) {
                    currentState = currentState.copy(iconResId = state.iconResId)
                }
                replacementActive = active
                controllerRef?.get()
                    ?: resolveController(host)?.also { resolved ->
                        controllerRef = WeakReference(resolved)
                    }
            }

        val requestedVisible =
            synchronized(this) {
                currentState.visible
            }
        val actualVisible =
            requestedVisible?.let { requested ->
                requested && !active
            }
        val applied =
            if (controller != null && actualVisible != null) {
                writeNativeVisibility(controller, actualVisible)
            } else {
                false
            }

        val summary =
            "hotspotReplacement source=" + source +
                " active=" + active +
                " requestedVisible=" + (requestedVisible?.toString() ?: "unknown") +
                " actualVisible=" + (actualVisible?.toString() ?: "unknown") +
                " resId=" + (state.iconResId ?: currentState.iconResId ?: 0) +
                " applied=" + applied +
                " nativeGeometryWrites=0"
        eventSink?.invoke(summary)
        return summary
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        val controller = controllerRef?.get()
        val requestedVisible = currentState.visible
        if (replacementActive && controller != null && requestedVisible != null) {
            writeNativeVisibility(controller, requestedVisible)
        }
        if (replacementActive || currentState.visible != null || currentState.iconResId != null) {
            eventSink?.invoke(
                "hotspotReplacement reset source=" + source +
                    " restoredVisible=" + (requestedVisible?.toString() ?: "unknown") +
                    " nativeGeometryWrites=0",
            )
        }
        installedHandles.clear()
        visibilityMethod = null
        controllerRef = null
        currentState = CombinedStatusStateStore.HotspotState()
        replacementActive = false
        internalVisibilityWrite = false
        stateSink = null
        eventSink = null
    }

    private fun publishResource(resourceId: Int?) {
        val update =
            synchronized(this) {
                val next = currentState.copy(iconResId = resourceId)
                if (next == currentState) {
                    null
                } else {
                    currentState = next
                    next
                }
            }
        update?.let { state ->
            stateSink?.invoke(state)
            eventSink?.invoke(
                "hotspotState source=setIcon visible=" +
                    (state.visible?.toString() ?: "unknown") +
                    " resId=" + (state.iconResId ?: 0) +
                    " eventDriven=true",
            )
        }
    }

    private fun publishVisibility(visible: Boolean) {
        val update =
            synchronized(this) {
                val next = currentState.copy(visible = visible)
                if (next == currentState) {
                    null
                } else {
                    currentState = next
                    next
                }
            }
        update?.let { state ->
            stateSink?.invoke(state)
            eventSink?.invoke(
                "hotspotState source=setIconVisibility visible=" + visible +
                    " resId=" + (state.iconResId ?: 0) +
                    " eventDriven=true",
            )
        }
    }

    private fun writeNativeVisibility(
        controller: Any,
        visible: Boolean,
    ): Boolean {
        val method = synchronized(this) { visibilityMethod } ?: return false
        return runCatching {
            synchronized(this) {
                internalVisibilityWrite = true
            }
            method.invoke(controller, SLOT, visible)
            true
        }.getOrDefault(false).also {
            synchronized(this) {
                internalVisibilityWrite = false
            }
        }
    }

    private fun resolveController(host: Any?): Any? {
        if (host == null) {
            return null
        }
        return when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
            is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                resolution.handles.controller
            is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                null
        }
    }

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult

        data class Failure(
            val reason: String,
        ) : InstallResult
    }
}
