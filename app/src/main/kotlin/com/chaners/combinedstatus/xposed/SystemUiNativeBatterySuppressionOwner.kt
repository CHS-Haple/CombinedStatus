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
    private var activeBatteryView: WeakReference<ViewGroup>? = null
    private var presentationMasks: Array<PresentationMaskState> = emptyArray()
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
            (container as? ViewGroup)
                ?.directChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
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
            val mask =
                applyPresentationMaskLocked(
                    batteryView = batteryView,
                    preserveExistingNativeAlpha = true,
                )
            if (mask.failureReason != null) {
                return StateResult.Failure(mask.failureReason)
            }
            return StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                maskedChildren = mask.maskedChildren,
                layoutChanged = false,
                visualChanged = mask.alphaWrites > 0,
            )
        }

        if (existingContainer != null || existingBatteryView != null) {
            restorePreviousLocked(
                container = existingContainer,
            )
        }

        val nativeRequested =
            readNativeHideLocked(container)
                ?: return StateResult.Failure("native-hide-state-unavailable")

        activeContainer = WeakReference(container)
        activeBatteryView = WeakReference(batteryView)
        latestNativeHideRequest = nativeRequested
        suppressionActive = true

        val target =
            resolveEffectiveHide(
                nativeRequestedHide = nativeRequested,
                suppressionActive = true,
            )
        val layoutApplied =
            if (nativeRequested == target) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = target,
                )
            }
        if (!layoutApplied) {
            clearOwnedStateLocked()
            return StateResult.Failure("native-hide-apply-failed")
        }

        val mask =
            applyPresentationMaskLocked(
                batteryView = batteryView,
                preserveExistingNativeAlpha = false,
            )
        if (mask.failureReason != null) {
            suppressionActive = false
            runCatching { applyHideLocked(container, nativeRequested) }
            restorePresentationMasksLocked()
            clearOwnedStateLocked()
            return StateResult.Failure(mask.failureReason)
        }

        val effective =
            readNativeHideLocked(container)
                ?: run {
                    suppressionActive = false
                    runCatching { applyHideLocked(container, nativeRequested) }
                    restorePresentationMasksLocked()
                    clearOwnedStateLocked()
                    return StateResult.Failure("effective-hide-state-unavailable")
                }

        val result =
            StateResult.Active(
                source = source,
                nativeRequestedHide = nativeRequested,
                effectiveHide = effective,
                maskedChildren = mask.maskedChildren,
                layoutChanged = nativeRequested != effective,
                visualChanged = mask.alphaWrites > 0,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val container = activeContainer?.get()
        val restoreHide = latestNativeHideRequest

        suppressionActive = false

        val restoredChildren = restorePresentationMasksLocked()
        activeBatteryView = null

        if (container == null || restoreHide == null) {
            activeContainer = null
            latestNativeHideRequest = null
            return StateResult.Inactive(
                source = source,
                restoredNativeHide = null,
                restoredChildren = restoredChildren,
                layoutChanged = false,
                visualChanged = restoredChildren > 0,
            )
        }

        val beforeHide = readNativeHideLocked(container)
        val restored =
            if (beforeHide == restoreHide) {
                true
            } else {
                applyHideLocked(
                    container = container,
                    hidden = restoreHide,
                )
            }

        activeContainer = null
        latestNativeHideRequest = null

        if (!restored) {
            return StateResult.Failure("native-hide-restore-failed")
        }

        val afterHide = readNativeHideLocked(container)
        val result =
            StateResult.Inactive(
                source = source,
                restoredNativeHide = afterHide,
                restoredChildren = restoredChildren,
                layoutChanged = beforeHide != afterHide,
                visualChanged = restoredChildren > 0,
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
    ) {
        val restoreHide = latestNativeHideRequest
        suppressionActive = false
        restorePresentationMasksLocked()
        activeBatteryView = null
        activeContainer = null
        latestNativeHideRequest = null

        if (container != null && restoreHide != null) {
            applyHideLocked(
                container = container,
                hidden = restoreHide,
            )
        }
    }

    private fun applyPresentationMaskLocked(
        batteryView: ViewGroup,
        preserveExistingNativeAlpha: Boolean,
    ): PresentationMaskSnapshot {
        if (batteryView.childCount <= 0) {
            return PresentationMaskSnapshot.failure(
                reason = "battery-presentation-children-missing",
            )
        }

        val previous = presentationMasks
        val next = mutableListOf<PresentationMaskState>()

        for (index in 0 until batteryView.childCount) {
            val child = batteryView.getChildAt(index)
            val existing =
                previous.firstOrNull { state ->
                    state.view.get() === child
                }
            next +=
                PresentationMaskState(
                    view = WeakReference(child),
                    nativeAlpha =
                        if (preserveExistingNativeAlpha && existing != null) {
                            existing.nativeAlpha
                        } else {
                            child.alpha
                        },
                )
        }

        val nextViews =
            next
                .mapNotNull { state -> state.view.get() }
                .toSet()
        previous
            .filter { state ->
                val view = state.view.get()
                view != null && view !in nextViews
            }
            .forEach { state ->
                restorePresentationMaskLocked(state)
            }

        presentationMasks =
            next
                .distinctBy { state ->
                    state.view.get()?.let(System::identityHashCode)
                }
                .toTypedArray()

        var masked = 0
        var writes = 0
        presentationMasks.forEach { state ->
            val child = state.view.get() ?: return@forEach
            val targetAlpha =
                resolvePresentationChildAlpha(
                    nativeAlpha = state.nativeAlpha,
                    suppressionActive = true,
                )
            if (child.alpha != targetAlpha) {
                child.alpha = targetAlpha
                writes += 1
            }
            if (child.alpha == targetAlpha) {
                masked += 1
            }
        }

        return PresentationMaskSnapshot.ready(
            maskedChildren = masked,
            alphaWrites = writes,
        )
    }

    private fun restorePresentationMasksLocked(): Int {
        val states = presentationMasks
        presentationMasks = emptyArray()
        var restored = 0
        states.forEach { state ->
            if (restorePresentationMaskLocked(state)) {
                restored += 1
            }
        }
        return restored
    }

    private fun restorePresentationMaskLocked(state: PresentationMaskState): Boolean {
        val child = state.view.get() ?: return false
        return runCatching {
            if (child.alpha != state.nativeAlpha) {
                child.alpha = state.nativeAlpha
            }
            child.alpha == state.nativeAlpha
        }.getOrDefault(false)
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

    private fun clearOwnedStateLocked() {
        presentationMasks = emptyArray()
        activeContainer = null
        activeBatteryView = null
        latestNativeHideRequest = null
        suppressionActive = false
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

    internal fun resolvePresentationChildAlpha(
        nativeAlpha: Float,
        suppressionActive: Boolean,
    ): Float =
        if (suppressionActive) {
            0f
        } else {
            nativeAlpha
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
            val maskedChildren: Int,
            val layoutChanged: Boolean,
            val visualChanged: Boolean,
        ) : StateResult {
            val changed: Boolean
                get() = layoutChanged

            override val summary: String
                get() =
                    "active:nativeRequestedHide=" + nativeRequestedHide +
                        ",effectiveHide=" + effectiveHide +
                        ",maskedChildren=" + maskedChildren +
                        ",layoutChanged=" + layoutChanged +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression active source=" + source +
                        " nativeRequestedHide=" + nativeRequestedHide +
                        " effectiveHide=" + effectiveHide +
                        " maskedChildren=" + maskedChildren +
                        " layoutChanged=" + layoutChanged +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery+MiuiBatteryMeterView.children.alpha " +
                        "rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=" + (if (layoutChanged) 1 else 0)
        }

        data class Inactive(
            val source: String,
            val restoredNativeHide: Boolean?,
            val restoredChildren: Int,
            val layoutChanged: Boolean,
            val visualChanged: Boolean,
        ) : StateResult {
            val changed: Boolean
                get() = layoutChanged

            override val summary: String
                get() =
                    "inactive:restoredNativeHide=" + restoredNativeHide +
                        ",restoredChildren=" + restoredChildren +
                        ",layoutChanged=" + layoutChanged +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression inactive source=" + source +
                        " restoredNativeHide=" + restoredNativeHide +
                        " restoredChildren=" + restoredChildren +
                        " layoutChanged=" + layoutChanged +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery+MiuiBatteryMeterView.children.alpha " +
                        "rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=" + (if (layoutChanged) 1 else 0)
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason

            override val logLine: String
                get() =
                    "nativeBatterySuppression unavailable reason=" + reason +
                        " nativeGeometryWrites=0 rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0"
        }
    }

    private data class PresentationMaskState(
        val view: WeakReference<View>,
        val nativeAlpha: Float,
    )

    private data class PresentationMaskSnapshot(
        val maskedChildren: Int,
        val alphaWrites: Int,
        val failureReason: String?,
    ) {
        companion object {
            fun ready(
                maskedChildren: Int,
                alphaWrites: Int,
            ): PresentationMaskSnapshot =
                PresentationMaskSnapshot(
                    maskedChildren = maskedChildren,
                    alphaWrites = alphaWrites,
                    failureReason = null,
                )

            fun failure(
                reason: String,
            ): PresentationMaskSnapshot =
                PresentationMaskSnapshot(
                    maskedChildren = 0,
                    alphaWrites = 0,
                    failureReason = reason,
                )
        }
    }
}
