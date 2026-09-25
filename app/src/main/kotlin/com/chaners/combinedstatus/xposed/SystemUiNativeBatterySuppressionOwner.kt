package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field

internal object SystemUiNativeBatterySuppressionOwner {
    const val HOOK_COUNT = 2

    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val SET_HIDE_METHOD_NAME = "setIsHideBattery"
    private const val UPDATE_CHARGE_METHOD_NAME = "updateChargeAndText"
    private const val HIDE_FIELD_NAME = "mIsHideBattery"
    private const val CHARGING_VIEW_FIELD_NAME = "mBatteryChargingView"
    private const val HIDE_HOOK_ID =
        "combinedstatus.nativeBatterySuppression.setIsHideBattery"
    private const val CHARGE_REFRESH_HOOK_ID =
        "combinedstatus.nativeBatterySuppression.updateChargeAndText"

    private var hideHookHandle: HookHandle? = null
    private var chargeRefreshHookHandle: HookHandle? = null
    private var hideField: Field? = null
    private var chargingViewField: Field? = null
    private var activeContainer: WeakReference<Any>? = null
    private var activeBatteryView: WeakReference<ViewGroup>? = null
    private var presentationMasks: Array<PresentationMaskState> = emptyArray()
    private var latestNativeHideRequest: Boolean? = null
    private var suppressionActive = false
    private var eventSink: ((String) -> Unit)? = null
    private var nativeHideSink: ((Boolean) -> Unit)? = null

    val installedHookCount: Int
        @Synchronized get() =
            listOf(hideHookHandle, chargeRefreshHookHandle).count { it != null }

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onNativeHideChanged: ((Boolean) -> Unit)? = null,
    ): InstallResult {
        if (installedHookCount == HOOK_COUNT) {
            eventSink = onEvent
            nativeHideSink = onNativeHideChanged
            return InstallResult.AlreadyInstalled
        }

        val createdHandles = mutableListOf<HookHandle>()
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

            val batteryClass =
                Class.forName(
                    BATTERY_VIEW_CLASS_NAME,
                    false,
                    classLoader,
                )
            val updateChargeMethod =
                batteryClass
                    .getDeclaredMethod(UPDATE_CHARGE_METHOD_NAME)
                    .apply {
                        check(returnType == Void.TYPE) {
                            "battery-charge-refresh-return-type-mismatch"
                        }
                        isAccessible = true
                    }
            val chargingField =
                batteryClass
                    .getDeclaredField(CHARGING_VIEW_FIELD_NAME)
                    .apply {
                        check(View::class.java.isAssignableFrom(type)) {
                            "battery-charging-view-field-type-mismatch"
                        }
                        isAccessible = true
                    }

            val hideHandle =
                module
                    .hook(hideMethod)
                    .setId(HIDE_HOOK_ID)
                    .intercept(hideRequestHooker())
                    .also(createdHandles::add)
            val refreshHandle =
                module
                    .hook(updateChargeMethod)
                    .setId(CHARGE_REFRESH_HOOK_ID)
                    .intercept(chargePresentationHooker())
                    .also(createdHandles::add)

            hideField = field
            chargingViewField = chargingField
            hideHookHandle = hideHandle
            chargeRefreshHookHandle = refreshHandle
            eventSink = onEvent
            nativeHideSink = onNativeHideChanged
            InstallResult.Installed
        }.getOrElse { error ->
            createdHandles.forEach { handle ->
                runCatching { handle.unhook() }
            }
            runCatching { hideHookHandle?.unhook() }
            runCatching { chargeRefreshHookHandle?.unhook() }
            hideHookHandle = null
            chargeRefreshHookHandle = null
            hideField = null
            chargingViewField = null
            clearOwnedStateLocked()
            eventSink = onEvent
            nativeHideSink = onNativeHideChanged
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
            chargingViewField == null
        ) {
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

        val sameSession =
            suppressionActive &&
                activeContainer?.get() === container &&
                activeBatteryView?.get() === batteryView

        if (!sameSession && (activeContainer?.get() != null || activeBatteryView?.get() != null)) {
            restorePreviousLocked()
        }

        val nativeHide =
            readNativeHideLocked(container)
                ?: return StateResult.Failure("native-hide-state-unavailable")

        activeContainer = WeakReference(container)
        activeBatteryView = WeakReference(batteryView)
        latestNativeHideRequest = nativeHide
        suppressionActive = true

        val mask =
            applyPresentationMaskLocked(
                batteryView = batteryView,
                preserveExistingNativeAlpha = sameSession,
            )
        if (mask.failureReason != null) {
            restorePresentationMasksLocked()
            clearOwnedStateLocked()
            return StateResult.Failure(mask.failureReason)
        }

        val effectiveHide =
            readNativeHideLocked(container)
                ?: run {
                    restorePresentationMasksLocked()
                    clearOwnedStateLocked()
                    return StateResult.Failure("effective-hide-state-unavailable")
                }

        val result =
            StateResult.Active(
                source = source,
                nativeRequestedHide = latestNativeHideRequest ?: effectiveHide,
                effectiveHide = effectiveHide,
                maskedChildren = mask.maskedChildren,
                layoutChanged = false,
                visualChanged = mask.alphaWrites > 0 || mask.visibilityWrites > 0,
            )
        eventSink?.invoke(result.logLine)
        nativeHideSink?.invoke(effectiveHide)
        return result
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val container = activeContainer?.get()
        val beforeHide = container?.let(::readNativeHideLocked)
        suppressionActive = false

        val restoredChildren = restorePresentationMasksLocked()
        activeContainer = null
        activeBatteryView = null
        latestNativeHideRequest = null

        val afterHide = container?.let(::readNativeHideLocked)
        val result =
            StateResult.Inactive(
                source = source,
                restoredNativeHide = afterHide ?: beforeHide,
                restoredChildren = restoredChildren,
                layoutChanged = false,
                visualChanged = restoredChildren > 0,
            )
        eventSink?.invoke(result.logLine)
        return result
    }

    @Synchronized
    fun revalidate(source: String): StateResult? {
        if (!suppressionActive) {
            return null
        }
        val batteryView =
            activeBatteryView?.get()
                ?: return StateResult.Failure("active-battery-view-missing")
        val snapshot =
            applyPresentationMaskLocked(
                batteryView = batteryView,
                preserveExistingNativeAlpha = true,
            )
        if (snapshot.failureReason != null) {
            return StateResult.Failure(snapshot.failureReason)
        }
        if (snapshot.alphaWrites > 0 || snapshot.visibilityWrites > 0) {
            eventSink?.invoke(
                "nativeBatterySuppression revalidate " +
                    "source=" + source +
                    " maskedChildren=" + snapshot.maskedChildren +
                    " alphaWrites=" + snapshot.alphaWrites +
                    " visibilityWrites=" + snapshot.visibilityWrites +
                    " visualChanged=true moduleLayoutWrites=0 nativeGeometryWrites=0",
            )
        }
        return StateResult.Active(
            source = source,
            nativeRequestedHide = latestNativeHideRequest ?: false,
            effectiveHide = latestNativeHideRequest ?: false,
            maskedChildren = snapshot.maskedChildren,
            layoutChanged = false,
            visualChanged =
                snapshot.alphaWrites > 0 ||
                    snapshot.visibilityWrites > 0,
        )
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        runCatching { hideHookHandle?.unhook() }
        runCatching { chargeRefreshHookHandle?.unhook() }
        hideHookHandle = null
        chargeRefreshHookHandle = null
        hideField = null
        chargingViewField = null
        clearOwnedStateLocked()
        eventSink = null
        nativeHideSink = null
    }

    private fun hideRequestHooker(): Hooker =
        Hooker { chain ->
            val container = chain.thisObject
            val requested = chain.getArg(0) as? Boolean
            val observed =
                synchronized(this) {
                    if (
                        suppressionActive &&
                        activeContainer?.get() === container &&
                        requested != null
                    ) {
                        latestNativeHideRequest = requested
                        true
                    } else {
                        false
                    }
                }
            val result = chain.proceed()
            if (observed && requested != null) {
                nativeHideSink?.invoke(requested)
                eventSink?.invoke(
                    "nativeBatterySuppression observe " +
                        "nativeRequestedHide=" + requested +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery(observe-only) " +
                        "moduleLayoutWrites=0 nativeGeometryWrites=0",
                )
            }
            result
        }

    private fun chargePresentationHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            val batteryView = chain.thisObject as? ViewGroup
                ?: return@Hooker result
            val snapshot =
                synchronized(this) {
                    if (
                        !suppressionActive ||
                        activeBatteryView?.get() !== batteryView
                    ) {
                        null
                    } else {
                        applyPresentationMaskLocked(
                            batteryView = batteryView,
                            preserveExistingNativeAlpha = true,
                        )
                    }
                }
            if (
                snapshot != null &&
                snapshot.failureReason == null &&
                (snapshot.alphaWrites > 0 || snapshot.visibilityWrites > 0)
            ) {
                eventSink?.invoke(
                    "nativeBatterySuppression revalidate " +
                        "source=" + UPDATE_CHARGE_METHOD_NAME +
                        " maskedChildren=" + snapshot.maskedChildren +
                        " chargingTarget=" + snapshot.chargingTargetPresent +
                        " chargingNativeVisibility=" +
                        visibilityName(snapshot.chargingNativeVisibility) +
                        " chargingAppliedVisibility=" +
                        visibilityName(snapshot.chargingAppliedVisibility) +
                        " alphaWrites=" + snapshot.alphaWrites +
                        " visibilityWrites=" + snapshot.visibilityWrites +
                        " visualChanged=true moduleLayoutWrites=0 nativeGeometryWrites=0",
                )
            }
            result
        }

    @Synchronized
    private fun restorePreviousLocked() {
        suppressionActive = false
        restorePresentationMasksLocked()
        activeBatteryView = null
        activeContainer = null
        latestNativeHideRequest = null
    }

    private fun applyPresentationMaskLocked(
        batteryView: ViewGroup,
        preserveExistingNativeAlpha: Boolean,
    ): PresentationMaskSnapshot {
        val chargingView =
            runCatching {
                chargingViewField?.get(batteryView) as? View
            }.getOrNull()
        if (batteryView.childCount <= 0 && chargingView == null) {
            return PresentationMaskSnapshot.failure(
                reason = "battery-presentation-children-missing",
            )
        }

        val previous = presentationMasks
        val next = mutableListOf<PresentationMaskState>()
        val targets = mutableListOf<View>()

        for (index in 0 until batteryView.childCount) {
            targets += batteryView.getChildAt(index)
        }
        if (
            chargingView != null &&
            targets.none { child -> child === chargingView }
        ) {
            targets += chargingView
        }

        targets.forEach { child ->
            val existing =
                previous.firstOrNull { state ->
                    state.view.get() === child
                }
            val controlsChargingVisibility = child === chargingView
            val currentVisibility = child.visibility
            val nativeVisibility =
                if (!controlsChargingVisibility) {
                    null
                } else if (
                    existing != null &&
                    existing.appliedVisibility != null &&
                    currentVisibility == existing.appliedVisibility
                ) {
                    existing.nativeVisibility
                } else {
                    currentVisibility
                }
            val appliedVisibility =
                nativeVisibility?.let { visibility ->
                    resolveChargingPresentationVisibility(
                        nativeVisibility = visibility,
                        suppressionActive = true,
                    )
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
                    nativeVisibility = nativeVisibility,
                    appliedVisibility = appliedVisibility,
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
        var alphaWrites = 0
        var visibilityWrites = 0
        presentationMasks.forEach { state ->
            val child = state.view.get() ?: return@forEach
            val targetAlpha =
                resolvePresentationChildAlpha(
                    nativeAlpha = state.nativeAlpha,
                    suppressionActive = true,
                )
            if (child.alpha != targetAlpha) {
                child.alpha = targetAlpha
                alphaWrites += 1
            }

            val targetVisibility = state.appliedVisibility
            if (
                targetVisibility != null &&
                child.visibility != targetVisibility
            ) {
                child.visibility = targetVisibility
                visibilityWrites += 1
            }

            if (
                child.alpha == targetAlpha &&
                (targetVisibility == null || child.visibility == targetVisibility)
            ) {
                masked += 1
            }
        }

        val chargingState =
            presentationMasks.firstOrNull { state ->
                state.view.get() === chargingView
            }
        return PresentationMaskSnapshot.ready(
            maskedChildren = masked,
            alphaWrites = alphaWrites,
            visibilityWrites = visibilityWrites,
            chargingTargetPresent = chargingView != null,
            chargingNativeVisibility = chargingState?.nativeVisibility,
            chargingAppliedVisibility = chargingState?.appliedVisibility,
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

            val nativeVisibility = state.nativeVisibility
            val appliedVisibility = state.appliedVisibility
            val ownsVisibility =
                nativeVisibility != null &&
                    appliedVisibility != null &&
                    appliedVisibility != nativeVisibility &&
                    child.visibility == appliedVisibility
            if (ownsVisibility) {
                child.visibility = nativeVisibility
            }

            child.alpha == state.nativeAlpha &&
                (!ownsVisibility || child.visibility == nativeVisibility)
        }.getOrDefault(false)
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

    internal fun resolvePresentationChildAlpha(
        nativeAlpha: Float,
        suppressionActive: Boolean,
    ): Float =
        if (suppressionActive) {
            0f
        } else {
            nativeAlpha
        }

    internal fun resolveChargingPresentationVisibility(
        nativeVisibility: Int,
        suppressionActive: Boolean,
    ): Int =
        if (
            suppressionActive &&
            nativeVisibility == View.VISIBLE
        ) {
            View.INVISIBLE
        } else {
            nativeVisibility
        }

    private fun visibilityName(visibility: Int?): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            null -> "none"
            else -> visibility.toString()
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
                        ",layoutChanged=false" +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression active source=" + source +
                        " nativeRequestedHide=" + nativeRequestedHide +
                        " effectiveHide=" + effectiveHide +
                        " maskedChildren=" + maskedChildren +
                        " layoutChanged=false" +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery(observe-only)+MiuiBatteryMeterView.children.alpha+mBatteryChargingView.visibility " +
                        "moduleLayoutWrites=0 rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=0"
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
                    "inactive:observedNativeHide=" + restoredNativeHide +
                        ",restoredChildren=" + restoredChildren +
                        ",layoutChanged=false" +
                        ",visualChanged=" + visualChanged

            override val logLine: String
                get() =
                    "nativeBatterySuppression inactive source=" + source +
                        " observedNativeHide=" + restoredNativeHide +
                        " restoredChildren=" + restoredChildren +
                        " layoutChanged=false" +
                        " visualChanged=" + visualChanged +
                        " contract=MiuiStatusBatteryContainer.setIsHideBattery(observe-only)+MiuiBatteryMeterView.children.alpha+mBatteryChargingView.visibility " +
                        "moduleLayoutWrites=0 rootVisibilityWrites=0 rootAlphaWrites=0 rootTranslationWrites=0 " +
                        "nativeGeometryWrites=0"
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

    private data class PresentationMaskState(
        val view: WeakReference<View>,
        val nativeAlpha: Float,
        val nativeVisibility: Int?,
        val appliedVisibility: Int?,
    )

    private data class PresentationMaskSnapshot(
        val maskedChildren: Int,
        val alphaWrites: Int,
        val visibilityWrites: Int,
        val chargingTargetPresent: Boolean,
        val chargingNativeVisibility: Int?,
        val chargingAppliedVisibility: Int?,
        val failureReason: String?,
    ) {
        companion object {
            fun ready(
                maskedChildren: Int,
                alphaWrites: Int,
                visibilityWrites: Int,
                chargingTargetPresent: Boolean,
                chargingNativeVisibility: Int?,
                chargingAppliedVisibility: Int?,
            ): PresentationMaskSnapshot =
                PresentationMaskSnapshot(
                    maskedChildren = maskedChildren,
                    alphaWrites = alphaWrites,
                    visibilityWrites = visibilityWrites,
                    chargingTargetPresent = chargingTargetPresent,
                    chargingNativeVisibility = chargingNativeVisibility,
                    chargingAppliedVisibility = chargingAppliedVisibility,
                    failureReason = null,
                )

            fun failure(
                reason: String,
            ): PresentationMaskSnapshot =
                PresentationMaskSnapshot(
                    maskedChildren = 0,
                    alphaWrites = 0,
                    visibilityWrites = 0,
                    chargingTargetPresent = false,
                    chargingNativeVisibility = null,
                    chargingAppliedVisibility = null,
                    failureReason = reason,
                )
        }
    }
}
