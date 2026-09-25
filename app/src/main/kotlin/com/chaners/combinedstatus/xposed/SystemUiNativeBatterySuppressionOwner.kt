package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
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
    private const val HOOK_ID =
        "combinedstatus.nativeBatterySuppression.setIsHideBattery"

    private var hookHandle: HookHandle? = null
    private var setHideMethod: Method? = null
    private var hideField: Field? = null
    private var activeContainer: WeakReference<Any>? = null
    private var latestNativeHideRequest: Boolean? = null
    private var suppressionActive = false
    private var applyingOverride = false
    private var eventSink: ((String) -> Unit)? = null

    val installedHookCount: Int
        @Synchronized get() = if (hookHandle != null) 1 else 0

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): InstallResult {
        if (hookHandle != null) {
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
            val method =
                containerClass.declaredMethods
                    .firstOrNull { candidate ->
                        candidate.name == SET_HIDE_METHOD_NAME &&
                            candidate.parameterCount == 1 &&
                            candidate.parameterTypes[0] == Boolean::class.javaObjectType
                    }
                    ?: error("battery-hide-method-missing")
            check(method.returnType == Void.TYPE) {
                "battery-hide-return-type-mismatch"
            }
            method.isAccessible = true

            val field =
                containerClass
                    .getDeclaredField(HIDE_FIELD_NAME)
                    .apply {
                        check(type == java.lang.Boolean.TYPE) {
                            "battery-hide-field-type-mismatch"
                        }
                        isAccessible = true
                    }

            val handle =
                module
                    .hook(method)
                    .setId(HOOK_ID)
                    .intercept(hideRequestHooker())

            setHideMethod = method
            hideField = field
            hookHandle = handle
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            hookHandle = null
            setHideMethod = null
            hideField = null
            activeContainer = null
            latestNativeHideRequest = null
            suppressionActive = false
            applyingOverride = false
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
        if (hookHandle == null || setHideMethod == null || hideField == null) {
            return StateResult.Failure("hook-not-ready")
        }

        val hostView =
            host as? ViewGroup
                ?: return StateResult.Failure("host-not-view-group")
        val container =
            hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
                ?: return StateResult.Failure("battery-container-missing")

        val existing = activeContainer?.get()
        if (suppressionActive && existing === container) {
            val nativeRequested =
                latestNativeHideRequest
                    ?: readNativeHideLocked(container)
                    ?: return StateResult.Failure("native-hide-state-unavailable")
            val effective =
                readNativeHideLocked(container)
                    ?: return StateResult.Failure("effective-hide-state-unavailable")
            return StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                changed = false,
            )
        }

        if (existing != null && existing !== container) {
            restorePreviousLocked(existing)
        }

        val nativeRequested =
            readNativeHideLocked(container)
                ?: return StateResult.Failure("native-hide-state-unavailable")

        activeContainer = WeakReference(container)
        latestNativeHideRequest = nativeRequested
        suppressionActive = true

        val before = nativeRequested
        val target = resolveEffectiveHide(
            nativeRequestedHide = nativeRequested,
            suppressionActive = true,
        )
        val applied =
            if (before == target) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = target,
                )
            }
        if (!applied) {
            suppressionActive = false
            activeContainer = null
            latestNativeHideRequest = null
            return StateResult.Failure("native-hide-apply-failed")
        }

        val effective =
            readNativeHideLocked(container)
                ?: run {
                    suppressionActive = false
                    activeContainer = null
                    latestNativeHideRequest = null
                    return StateResult.Failure("effective-hide-state-unavailable")
                }
        val result =
            StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                changed = before != effective,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val container = activeContainer?.get()
        val restore = latestNativeHideRequest
        suppressionActive = false
        activeContainer = null
        latestNativeHideRequest = null

        if (container == null || restore == null) {
            return StateResult.Inactive(
                source = source,
                restoredNativeHide = null,
                changed = false,
            )
        }

        val before = readNativeHideLocked(container)
        val restored =
            if (before == restore) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = restore,
                )
            }
        if (!restored) {
            return StateResult.Failure("native-hide-restore-failed")
        }

        val after = readNativeHideLocked(container)
        val result =
            StateResult.Inactive(
                source = source,
                restoredNativeHide = after,
                changed = before != after,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        hookHandle = null
        setHideMethod = null
        hideField = null
        activeContainer = null
        latestNativeHideRequest = null
        suppressionActive = false
        applyingOverride = false
        eventSink = null
    }

    private fun hideRequestHooker(): Hooker =
        Hooker { chain ->
            val container = chain.thisObject
            val requested = chain.getArg(0) as? Boolean
            val overrideWithHidden =
                synchronized(this) {
                    if (
                        applyingOverride ||
                        !suppressionActive ||
                        activeContainer?.get() !== container ||
                        requested == null
                    ) {
                        false
                    } else {
                        latestNativeHideRequest = requested
                        !requested
                    }
                }

            if (!overrideWithHidden) {
                return@Hooker chain.proceed()
            }

            val result =
                chain.proceed(
                    arrayOf(
                        java.lang.Boolean.TRUE,
                    ),
                )
            eventSink?.invoke(
                "nativeBatterySuppression override " +
                    "nativeRequestedHide=false " +
                    "effectiveHide=true " +
                    "contract=MiuiStatusBatteryContainer.setIsHideBattery " +
                    "nativeGeometryWrites=1",
            )
            result
        }

    @Synchronized
    private fun restorePreviousLocked(container: Any) {
        val restore = latestNativeHideRequest
        suppressionActive = false
        activeContainer = null
        latestNativeHideRequest = null
        if (restore != null) {
            applyHideLocked(
                container = container,
                hidden = restore,
            )
        }
    }

    private fun applyHideLocked(
        container: Any,
        hidden: Boolean,
    ): Boolean {
        val method = setHideMethod ?: return false
        applyingOverride = true
        return try {
            method.invoke(
                container,
                java.lang.Boolean.valueOf(hidden),
            )
            readNativeHideLocked(container) == hidden
        } catch (_: Throwable) {
            false
        } finally {
            applyingOverride = false
        }
    }

    private fun readNativeHideLocked(container: Any): Boolean? =
        runCatching {
            hideField?.getBoolean(container)
        }.getOrNull()

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child
            }
        }
        return null
    }

    internal fun resolveEffectiveHide(
        nativeRequestedHide: Boolean,
        suppressionActive: Boolean,
    ): Boolean =
        nativeRequestedHide || suppressionActive

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
            val changed: Boolean,
        ) : StateResult {
            override val summary: String
                get() =
                    "active:nativeRequestedHide=" + nativeRequestedHide +
                        ",effectiveHide=" + effectiveHide +
                        ",changed=" + changed

            override val logLine: String
                get() =
                    "nativeBatterySuppression active source=" + source +
                        " nativeRequestedHide=" + nativeRequestedHide +
                        " effectiveHide=" + effectiveHide +
                        " changed=" + changed +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery" +
                        " nativeGeometryWrites=" + if (changed) 1 else 0
        }

        data class Inactive(
            val source: String,
            val restoredNativeHide: Boolean?,
            val changed: Boolean,
        ) : StateResult {
            override val summary: String
                get() =
                    "inactive:restoredNativeHide=" + restoredNativeHide +
                        ",changed=" + changed

            override val logLine: String
                get() =
                    "nativeBatterySuppression inactive source=" + source +
                        " restoredNativeHide=" + restoredNativeHide +
                        " changed=" + changed +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery" +
                        " nativeGeometryWrites=" + if (changed) 1 else 0
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason

            override val logLine: String
                get() =
                    "nativeBatterySuppression unavailable reason=" + reason +
                        " nativeGeometryWrites=0"
        }
    }
}
