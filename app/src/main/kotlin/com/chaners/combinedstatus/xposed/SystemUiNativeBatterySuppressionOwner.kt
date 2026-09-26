package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object SystemUiNativeBatterySuppressionOwner {
    const val HOOK_COUNT = 1

    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val SET_HIDE_METHOD_NAME = "setIsHideBattery"
    private const val HIDE_FIELD_NAME = "mIsHideBattery"
    private const val HIDE_HOOK_ID =
        "combinedstatus.nativeBatterySuppression.setIsHideBattery"

    private var hideHookHandle: HookHandle? = null
    private var hideField: Field? = null
    private var hideOriginInvoker: XposedInterface.Invoker<*, Method>? = null
    private var activeContainer: WeakReference<Any>? = null
    private var latestNativeHideRequest: Boolean? = null
    private var suppressionActive = false
    private var eventSink: ((String) -> Unit)? = null

    val installedHookCount: Int
        @Synchronized get() = if (hideHookHandle != null) 1 else 0

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): InstallResult {
        if (installedHookCount == HOOK_COUNT) {
            eventSink = onEvent
            return InstallResult.AlreadyInstalled
        }

        return runCatching {
            val containerClass =
                Class.forName(
                    BATTERY_CONTAINER_CLASS_NAME,
                    false,
                    classLoader,
                )
            val hideMethod =
                containerClass.declaredMethods
                    .firstOrNull { candidate ->
                        candidate.name == SET_HIDE_METHOD_NAME &&
                            candidate.parameterCount == 1 &&
                            candidate.parameterTypes[0] == Boolean::class.javaObjectType
                    }
                    ?: error("battery-hide-method-missing")
            check(hideMethod.returnType == Void.TYPE) {
                "battery-hide-return-type-mismatch"
            }
            hideMethod.isAccessible = true

            val field =
                containerClass
                    .getDeclaredField(HIDE_FIELD_NAME)
                    .apply {
                        check(type == java.lang.Boolean.TYPE) {
                            "battery-hide-field-type-mismatch"
                        }
                        isAccessible = true
                    }

            val originInvoker =
                module
                    .getInvoker(hideMethod)
                    .setType(XposedInterface.Invoker.Type.ORIGIN)

            val hideHandle =
                module
                    .hook(hideMethod)
                    .setId(HIDE_HOOK_ID)
                    .intercept(hideRequestHooker())

            hideField = field
            hideOriginInvoker = originInvoker
            hideHookHandle = hideHandle
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            runCatching { hideHookHandle?.unhook() }
            hideHookHandle = null
            hideField = null
            hideOriginInvoker = null
            clearOwnedStateLocked()
            eventSink = onEvent
            InstallResult.Failure(
                error.message ?: error.javaClass.simpleName,
            )
        }
    }

    @Synchronized
    fun activate(
        host: Any,
        source: String,
    ): StateResult {
        if (
            installedHookCount != HOOK_COUNT ||
            hideField == null ||
            hideOriginInvoker == null
        ) {
            return StateResult.Failure("hook-not-ready")
        }

        val hostView =
            host as? ViewGroup
                ?: return StateResult.Failure("host-not-view-group")
        val container =
            hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
                ?: return StateResult.Failure("battery-container-missing")

        val sameSession =
            suppressionActive &&
                activeContainer?.get() === container

        if (!sameSession && activeContainer?.get() != null) {
            if (!restorePreviousLocked()) {
                return StateResult.Failure("previous-session-restore-failed")
            }
        }

        val nativeRequestedHide =
            if (sameSession) {
                latestNativeHideRequest
                    ?: readNativeHideLocked(container)
            } else {
                readNativeHideLocked(container)
            } ?: return StateResult.Failure("native-hide-state-unavailable")

        activeContainer = WeakReference(container)
        latestNativeHideRequest = nativeRequestedHide
        suppressionActive = true

        val effectiveHide =
            resolveEffectiveBatteryHide(
                nativeRequestedHide = nativeRequestedHide,
                replacementActive = true,
            )
        val beforeHide = readNativeHideLocked(container)
        if (!invokeOriginHideLocked(container, effectiveHide)) {
            runCatching {
                invokeOriginHideLocked(container, nativeRequestedHide)
            }
            clearOwnedStateLocked()
            return StateResult.Failure("effective-hide-commit-failed")
        }

        val result =
            StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequestedHide,
                effectiveHide = effectiveHide,
                layoutChanged = beforeHide != effectiveHide,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val container = activeContainer?.get()
        val nativeRequestedHide = latestNativeHideRequest

        if (!suppressionActive || container == null || nativeRequestedHide == null) {
            clearOwnedStateLocked()
            return StateResult.Inactive(
                source = source,
                restoredNativeHide = nativeRequestedHide,
                layoutChanged = false,
            )
        }

        val beforeHide = readNativeHideLocked(container)
        if (!invokeOriginHideLocked(container, nativeRequestedHide)) {
            return StateResult.Failure("native-hide-restore-failed")
        }

        val result =
            StateResult.Inactive(
                source = source,
                restoredNativeHide = nativeRequestedHide,
                layoutChanged = beforeHide != nativeRequestedHide,
            )
        clearOwnedStateLocked()
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        runCatching { hideHookHandle?.unhook() }
        hideHookHandle = null
        hideField = null
        hideOriginInvoker = null
        clearOwnedStateLocked()
        eventSink = null
    }

    private fun hideRequestHooker(): Hooker =
        Hooker { chain ->
            val container = chain.thisObject
            val requested = chain.getArg(0) as? Boolean
                ?: return@Hooker chain.proceed()

            val replacementOwnsHide =
                synchronized(this) {
                    val active =
                        suppressionActive &&
                            activeContainer?.get() === container
                    if (active) {
                        latestNativeHideRequest = requested
                    }
                    active
                }
            if (!replacementOwnsHide) {
                return@Hooker chain.proceed()
            }

            val effectiveHide =
                resolveEffectiveBatteryHide(
                    nativeRequestedHide = requested,
                    replacementActive = true,
                )
            val result =
                if (effectiveHide == requested) {
                    chain.proceed()
                } else {
                    chain.proceed(arrayOf(effectiveHide))
                }

            val verified =
                synchronized(this) {
                    readNativeHideLocked(container) == effectiveHide
                }
            eventSink?.invoke(
                "nativeBatterySuppression compose " +
                    "nativeRequestedHide=" + requested +
                    " effectiveHide=" + effectiveHide +
                    " verified=" + verified +
                    " contract=MiuiStatusBatteryContainer.setIsHideBattery(composed-owner) " +
                    "moduleLayoutWrites=0 nativeGeometryWrites=1",
            )
            result
        }

    @Synchronized
    private fun restorePreviousLocked(): Boolean {
        val container = activeContainer?.get()
        val nativeRequestedHide = latestNativeHideRequest
        if (container == null || nativeRequestedHide == null) {
            clearOwnedStateLocked()
            return true
        }
        if (!invokeOriginHideLocked(container, nativeRequestedHide)) {
            return false
        }
        clearOwnedStateLocked()
        return true
    }

    private fun invokeOriginHideLocked(
        container: Any,
        hide: Boolean,
    ): Boolean {
        val invoker = hideOriginInvoker ?: return false
        return runCatching {
            invoker.invoke(container, hide)
            readNativeHideLocked(container) == hide
        }.getOrDefault(false)
    }

    private fun readNativeHideLocked(container: Any): Boolean? =
        runCatching {
            hideField?.getBoolean(container)
        }.getOrNull()

    private fun clearOwnedStateLocked() {
        activeContainer = null
        latestNativeHideRequest = null
        suppressionActive = false
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child
            }
        }
        return null
    }

    internal fun resolveEffectiveBatteryHide(
        nativeRequestedHide: Boolean,
        replacementActive: Boolean,
    ): Boolean =
        nativeRequestedHide || replacementActive

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult

        data class Failure(
            val reason: String,
        ) : InstallResult
    }

    internal sealed interface StateResult {
        val summary: String
        val logLine: String

        data class Active(
            val source: String,
            val nativeRequestedHide: Boolean,
            val effectiveHide: Boolean,
            val layoutChanged: Boolean,
        ) : StateResult {
            val changed: Boolean
                get() = layoutChanged

            override val summary: String
                get() =
                    "active:nativeRequestedHide=" + nativeRequestedHide +
                        ",effectiveHide=" + effectiveHide +
                        ",layoutChanged=" + layoutChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression active source=" + source +
                        " nativeRequestedHide=" + nativeRequestedHide +
                        " effectiveHide=" + effectiveHide +
                        " layoutChanged=" + layoutChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery(composed-owner) " +
                        "moduleLayoutWrites=0 rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=" + if (layoutChanged) 1 else 0
        }

        data class Inactive(
            val source: String,
            val restoredNativeHide: Boolean?,
            val layoutChanged: Boolean,
        ) : StateResult {
            val changed: Boolean
                get() = layoutChanged

            override val summary: String
                get() =
                    "inactive:restoredNativeHide=" + restoredNativeHide +
                        ",layoutChanged=" + layoutChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression inactive source=" + source +
                        " restoredNativeHide=" + restoredNativeHide +
                        " layoutChanged=" + layoutChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery(composed-owner) " +
                        "moduleLayoutWrites=0 rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=" + if (layoutChanged) 1 else 0
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason

            override val logLine: String
                get() =
                    "nativeBatterySuppression unavailable reason=" + reason +
                        " moduleLayoutWrites=0 nativeGeometryWrites=0 " +
                        "rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0"
        }
    }
}
