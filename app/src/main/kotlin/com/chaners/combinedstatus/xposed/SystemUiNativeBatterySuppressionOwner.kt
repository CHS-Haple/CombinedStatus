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
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val SET_HIDE_METHOD_NAME = "setIsHideBattery"
    private const val HIDE_FIELD_NAME = "mIsHideBattery"
    private const val HOOK_ID =
        "combinedstatus.nativeBatterySuppression.setIsHideBattery"

    private var hookHandle: HookHandle? = null
    private var setHideMethod: Method? = null
    private var hideField: Field? = null
    private var activeContainer: WeakReference<Any>? = null
    private var activeBatteryView: WeakReference<View>? = null
    private var latestNativeHideRequest: Boolean? = null
    private var nativeBatteryVisibilityBeforeSuppression: Int? = null
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
        if (hookHandle == null || setHideMethod == null || hideField == null) {
            return StateResult.Failure("hook-not-ready")
        }

        val hostView =
            host as? ViewGroup
                ?: return StateResult.Failure("host-not-view-group")
        val container =
            hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
                ?: return StateResult.Failure("battery-container-missing")
        val batteryView =
            (container as? ViewGroup)?.directChild(BATTERY_VIEW_CLASS_NAME)
                ?: return StateResult.Failure("battery-view-missing")

        val existingContainer = activeContainer?.get()
        val existingBatteryView = activeBatteryView?.get()
        if (
            suppressionActive &&
            existingContainer === container &&
            existingBatteryView === batteryView
        ) {
            val nativeRequested =
                latestNativeHideRequest
                    ?: readNativeHideLocked(container)
                    ?: return StateResult.Failure("native-hide-state-unavailable")
            val effective =
                readNativeHideLocked(container)
                    ?: return StateResult.Failure("effective-hide-state-unavailable")
            val beforeVisibility = batteryView.visibility
            if (!applyVisibilityLocked(batteryView, View.INVISIBLE)) {
                return StateResult.Failure("battery-visual-hide-failed")
            }
            return StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                effectiveVisibility = batteryView.visibility,
                layoutChanged = false,
                visualChanged = beforeVisibility != batteryView.visibility,
            )
        }

        if (existingContainer != null || existingBatteryView != null) {
            restorePreviousLocked(
                container = existingContainer,
                batteryView = existingBatteryView,
            )
        }

        val nativeRequested =
            readNativeHideLocked(container)
                ?: return StateResult.Failure("native-hide-state-unavailable")
        val nativeVisibility = batteryView.visibility

        activeContainer = WeakReference(container)
        activeBatteryView = WeakReference(batteryView)
        latestNativeHideRequest = nativeRequested
        nativeBatteryVisibilityBeforeSuppression = nativeVisibility
        suppressionActive = true

        val targetHide =
            resolveEffectiveHide(
                nativeRequestedHide = nativeRequested,
                suppressionActive = true,
            )
        val layoutApplied =
            if (nativeRequested == targetHide) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = targetHide,
                )
            }
        if (!layoutApplied) {
            clearOwnedStateLocked()
            return StateResult.Failure("native-hide-apply-failed")
        }

        val beforeVisibility = batteryView.visibility
        if (!applyVisibilityLocked(batteryView, View.INVISIBLE)) {
            suppressionActive = false
            runCatching { applyHideLocked(container, nativeRequested) }
            runCatching { applyVisibilityLocked(batteryView, nativeVisibility) }
            clearOwnedStateLocked()
            return StateResult.Failure("battery-visual-hide-failed")
        }

        val effective =
            readNativeHideLocked(container)
                ?: run {
                    suppressionActive = false
                    runCatching { applyHideLocked(container, nativeRequested) }
                    runCatching { applyVisibilityLocked(batteryView, nativeVisibility) }
                    clearOwnedStateLocked()
                    return StateResult.Failure("effective-hide-state-unavailable")
                }
        val result =
            StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                effectiveVisibility = batteryView.visibility,
                layoutChanged = nativeRequested != effective,
                visualChanged = beforeVisibility != batteryView.visibility,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val container = activeContainer?.get()
        val batteryView = activeBatteryView?.get()
        val restoreHide = latestNativeHideRequest
        val restoreVisibility = nativeBatteryVisibilityBeforeSuppression

        suppressionActive = false
        clearOwnedStateLocked(keepSuppressionFlag = true)

        if (
            container == null ||
            batteryView == null ||
            restoreHide == null ||
            restoreVisibility == null
        ) {
            return StateResult.Inactive(
                source = source,
                restoredNativeHide = null,
                restoredVisibility = null,
                layoutChanged = false,
                visualChanged = false,
            )
        }

        val beforeHide = readNativeHideLocked(container)
        val hideRestored =
            if (beforeHide == restoreHide) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = restoreHide,
                )
            }
        if (!hideRestored) {
            return StateResult.Failure("native-hide-restore-failed")
        }

        val beforeVisibility = batteryView.visibility
        if (!applyVisibilityLocked(batteryView, restoreVisibility)) {
            return StateResult.Failure("battery-visual-restore-failed")
        }

        val afterHide = readNativeHideLocked(container)
        val result =
            StateResult.Inactive(
                source = source,
                restoredNativeHide = afterHide,
                restoredVisibility = batteryView.visibility,
                layoutChanged = beforeHide != afterHide,
                visualChanged = beforeVisibility != batteryView.visibility,
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
        clearOwnedStateLocked()
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
    private fun restorePreviousLocked(
        container: Any?,
        batteryView: View?,
    ) {
        val restoreHide = latestNativeHideRequest
        val restoreVisibility = nativeBatteryVisibilityBeforeSuppression
        suppressionActive = false
        clearOwnedStateLocked(keepSuppressionFlag = true)

        if (container != null && restoreHide != null) {
            applyHideLocked(
                container = container,
                hidden = restoreHide,
            )
        }
        if (batteryView != null && restoreVisibility != null) {
            applyVisibilityLocked(
                view = batteryView,
                visibility = restoreVisibility,
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

    private fun applyVisibilityLocked(
        view: View,
        visibility: Int,
    ): Boolean =
        runCatching {
            if (view.visibility != visibility) {
                view.visibility = visibility
            }
            view.visibility == visibility
        }.getOrDefault(false)

    private fun readNativeHideLocked(container: Any): Boolean? =
        runCatching {
            hideField?.getBoolean(container)
        }.getOrNull()

    private fun clearOwnedStateLocked(keepSuppressionFlag: Boolean = false) {
        activeContainer = null
        activeBatteryView = null
        latestNativeHideRequest = null
        nativeBatteryVisibilityBeforeSuppression = null
        if (!keepSuppressionFlag) {
            suppressionActive = false
        }
        applyingOverride = false
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

    internal fun resolveEffectiveHide(
        nativeRequestedHide: Boolean,
        suppressionActive: Boolean,
    ): Boolean =
        nativeRequestedHide || suppressionActive

    internal fun resolveEffectiveVisibility(
        nativeVisibility: Int,
        suppressionActive: Boolean,
    ): Int =
        if (suppressionActive) {
            View.INVISIBLE
        } else {
            nativeVisibility
        }

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
            val effectiveVisibility: Int,
            val layoutChanged: Boolean,
            val visualChanged: Boolean,
        ) : StateResult {
            override val summary: String
                get() =
                    "active:nativeRequestedHide=" + nativeRequestedHide +
                        ",effectiveHide=" + effectiveHide +
                        ",effectiveVisibility=" + effectiveVisibility +
                        ",layoutChanged=" + layoutChanged +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression active source=" + source +
                        " nativeRequestedHide=" + nativeRequestedHide +
                        " effectiveHide=" + effectiveHide +
                        " effectiveVisibility=" + effectiveVisibility +
                        " layoutChanged=" + layoutChanged +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery+MiuiBatteryMeterView.INVISIBLE" +
                        " nativeGeometryWrites=" + if (layoutChanged) 1 else 0 +
                        " nativeVisibilityWrites=" + if (visualChanged) 1 else 0
        }

        data class Inactive(
            val source: String,
            val restoredNativeHide: Boolean?,
            val restoredVisibility: Int?,
            val layoutChanged: Boolean,
            val visualChanged: Boolean,
        ) : StateResult {
            override val summary: String
                get() =
                    "inactive:restoredNativeHide=" + restoredNativeHide +
                        ",restoredVisibility=" + restoredVisibility +
                        ",layoutChanged=" + layoutChanged +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression inactive source=" + source +
                        " restoredNativeHide=" + restoredNativeHide +
                        " restoredVisibility=" + restoredVisibility +
                        " layoutChanged=" + layoutChanged +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery+MiuiBatteryMeterView.INVISIBLE" +
                        " nativeGeometryWrites=" + if (layoutChanged) 1 else 0 +
                        " nativeVisibilityWrites=" + if (visualChanged) 1 else 0
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason

            override val logLine: String
                get() =
                    "nativeBatterySuppression unavailable reason=" + reason +
                        " nativeGeometryWrites=0 nativeVisibilityWrites=0"
        }
    }
}
