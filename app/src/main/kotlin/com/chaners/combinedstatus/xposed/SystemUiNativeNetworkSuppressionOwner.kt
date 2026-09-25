package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal object SystemUiNativeNetworkSuppressionOwner {
    private const val MODERN_STATUS_BAR_VIEW_CLASS =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    private const val HOME_MANAGER_CLASS =
        "com.android.systemui.statusbar.phone.ui.DarkIconManager"

    private const val ICON_VISIBLE_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.iconVisible"
    private const val VISIBILITY_STATE_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.visibilityState"
    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"

    private const val VISIBILITY_STATE_HIDDEN = 2

    private val noTargetSlots = emptySet<String>()
    private val wifiOnlyTargetSlots = setOf("wifi")
    private val mobileOnlyTargetSlots = setOf("mobile")
    private val wifiAndMobileTargetSlots = setOf("wifi", "mobile")
    private val observableTargetSlots = wifiAndMobileTargetSlots

    private val installedHandles = mutableListOf<HookHandle>()
    private var setVisibleStateMethod: Method? = null
    private var getVisibleStateMethod: Method? = null
    private var activeManager: Any? = null
    private var activeGroup: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null
    private var applyingVisibilityOverride = false

    @Volatile
    private var suppressedTargets: Array<SuppressedTargetState> = emptyArray()

    @Volatile
    private var wifiSuppressionEnabled = false

    @Volatile
    private var mobileSuppressionEnabled = false

    val installedHookCount: Int
        @Synchronized get() = installedHandles.size

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): InstallResult {
        if (installedHandles.isNotEmpty()) {
            eventSink = onEvent
            return InstallResult.AlreadyInstalled
        }

        val created = mutableListOf<HookHandle>()
        return runCatching {
            val modernViewClass =
                Class.forName(
                    MODERN_STATUS_BAR_VIEW_CLASS,
                    false,
                    classLoader,
                )
            val iconVisible =
                modernViewClass
                    .getDeclaredMethod("isIconVisible")
                    .apply {
                        check(returnType == java.lang.Boolean.TYPE) {
                            "icon-visible-return-type-mismatch"
                        }
                        isAccessible = true
                    }
            val setVisibleState =
                modernViewClass
                    .getDeclaredMethod(
                        "setVisibleState",
                        Integer.TYPE,
                        java.lang.Boolean.TYPE,
                    ).apply {
                        check(returnType == Void.TYPE) {
                            "set-visible-state-return-type-mismatch"
                        }
                        isAccessible = true
                    }
            val getVisibleState =
                modernViewClass
                    .getDeclaredMethod("getVisibleState")
                    .apply {
                        check(returnType == Integer.TYPE) {
                            "get-visible-state-return-type-mismatch"
                        }
                        isAccessible = true
                    }

            val darkIconManager =
                Class.forName(
                    HOME_MANAGER_CLASS,
                    false,
                    classLoader,
                )
            val onIconAdded =
                darkIconManager.declaredMethods
                    .firstOrNull { method ->
                        method.name == "onIconAdded" &&
                            method.parameterCount == 4 &&
                            method.parameterTypes.getOrNull(0) == Integer.TYPE &&
                            method.parameterTypes.getOrNull(1) == String::class.java &&
                            method.parameterTypes.getOrNull(2) == java.lang.Boolean.TYPE
                    }
                    ?: error("home-icon-manager-onIconAdded-missing")
            onIconAdded.isAccessible = true

            created +=
                module
                    .hook(iconVisible)
                    .setId(ICON_VISIBLE_HOOK_ID)
                    .intercept(iconVisibleHooker())
            created +=
                module
                    .hook(setVisibleState)
                    .setId(VISIBILITY_STATE_HOOK_ID)
                    .intercept(visibilityStateHooker())
            created +=
                module
                    .hook(onIconAdded)
                    .setId(HOME_ICON_ADDED_HOOK_ID)
                    .intercept(homeIconAddedHooker())

            setVisibleStateMethod = setVisibleState
            getVisibleStateMethod = getVisibleState
            installedHandles.clear()
            installedHandles.addAll(created)
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            created.forEach { handle ->
                runCatching { handle.unhook() }
            }
            installedHandles.clear()
            setVisibleStateMethod = null
            getVisibleStateMethod = null
            clearSessionLocked(requestLayout = false)
            eventSink = onEvent
            InstallResult.Failure(
                error.message ?: error.javaClass.simpleName,
            )
        }
    }

    @Synchronized
    fun activate(
        host: Any,
        suppressWifi: Boolean,
        suppressMobile: Boolean,
    ): StateResult {
        if (installedHandles.size != EXPECTED_HOOK_COUNT) {
            return StateResult.Failure("hooks-not-ready")
        }

        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return StateResult.Failure(resolution.reason)
            }

        if (handles.manager.javaClass.name != HOME_MANAGER_CLASS) {
            return StateResult.Failure("home-manager-mismatch")
        }

        activeManager = handles.manager
        activeGroup = WeakReference(handles.group)
        wifiSuppressionEnabled = suppressWifi
        mobileSuppressionEnabled = suppressMobile

        val snapshot = refreshTargetsLocked("handoff")
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return StateResult.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return StateResult.Active(
            targets = snapshot.targetCount,
            slots = snapshot.slots,
            wifiSuppressed = snapshot.wifiSuppressed,
            mobileSuppressed = snapshot.mobileSuppressed,
            hiddenTargets = snapshot.hiddenTargetCount,
        )
    }

    @Synchronized
    fun updatePolicy(
        suppressWifi: Boolean,
        suppressMobile: Boolean,
        source: String,
    ): StateResult? {
        if (
            wifiSuppressionEnabled == suppressWifi &&
            mobileSuppressionEnabled == suppressMobile
        ) {
            return null
        }

        wifiSuppressionEnabled = suppressWifi
        mobileSuppressionEnabled = suppressMobile
        if (activeManager == null || activeGroup?.get() == null) {
            return null
        }

        val snapshot = refreshTargetsLocked(source)
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return StateResult.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return StateResult.Active(
            targets = snapshot.targetCount,
            slots = snapshot.slots,
            wifiSuppressed = snapshot.wifiSuppressed,
            mobileSuppressed = snapshot.mobileSuppressed,
            hiddenTargets = snapshot.hiddenTargetCount,
        )
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val group = activeGroup?.get()
        val previousCount = suppressedTargets.count { state -> state.view.get() != null }
        val restoredCount = restoreSuppressedTargetsLocked()

        activeManager = null
        activeGroup = null
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        applyingVisibilityOverride = false

        group?.requestLayout()
        if (previousCount > 0) {
            eventSink?.invoke(
                "nativeNetworkSuppression inactive source=" + source +
                    " restoredTargets=" + restoredCount +
                    " nativeGeometryWrites=0",
            )
        }
        return StateResult.Inactive(restoredCount)
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        installedHandles.clear()
        setVisibleStateMethod = null
        getVisibleStateMethod = null
        eventSink = null
    }

    private fun iconVisibleHooker(): Hooker =
        Hooker { chain ->
            if (isSuppressedView(chain.thisObject)) {
                false
            } else {
                chain.proceed()
            }
        }

    private fun visibilityStateHooker(): Hooker =
        Hooker { chain ->
            val view = chain.thisObject as? View
            val requestedState = (chain.getArg(0) as? Number)?.toInt()
            val effectiveState =
                synchronized(this) {
                    val target =
                        suppressedTargets.firstOrNull { state ->
                            state.view.get() === view
                        }
                    if (
                        applyingVisibilityOverride ||
                        target == null ||
                        requestedState == null
                    ) {
                        null
                    } else {
                        target.latestNativeVisibilityState = requestedState
                        resolveEffectiveVisibilityState(
                            nativeVisibilityState = requestedState,
                            suppressionActive = true,
                        )
                    }
                }

            if (effectiveState == null) {
                chain.proceed()
            } else {
                chain.proceed(
                    arrayOf(
                        Integer.valueOf(effectiveState),
                        chain.getArg(1),
                    ),
                )
            }
        }

    private fun homeIconAddedHooker(): Hooker =
        Hooker { chain ->
            val result = chain.proceed()
            val manager = chain.thisObject
            val slot = chain.getArg(1) as? String
            if (
                manager === activeManager &&
                slot != null &&
                slot in observableTargetSlots
            ) {
                synchronized(this) {
                    if (manager === activeManager) {
                        val snapshot = refreshTargetsLocked("iconAdded:" + slot)
                        eventSink?.invoke(snapshot.logLine)
                    }
                }
            }
            result
        }

    private fun isSuppressedView(candidate: Any): Boolean =
        suppressedTargets.any { state ->
            state.view.get() === candidate
        }

    private fun refreshTargetsLocked(source: String): TargetSnapshot {
        val group =
            activeGroup?.get()
                ?: return TargetSnapshot.failure(
                    source = source,
                    reason = "home-status-icon-group-missing",
                )

        val targetSlots =
            when {
                wifiSuppressionEnabled && mobileSuppressionEnabled ->
                    wifiAndMobileTargetSlots
                wifiSuppressionEnabled ->
                    wifiOnlyTargetSlots
                mobileSuppressionEnabled ->
                    mobileOnlyTargetSlots
                else ->
                    noTargetSlots
            }

        val targetViews = mutableListOf<Pair<String, View>>()
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val slot = NativeParticipantRuntimeAccess.slotOf(child) ?: continue
            if (slot in targetSlots) {
                targetViews += slot to child
            }
        }

        val oldTargets = suppressedTargets
        val newTargets = mutableListOf<SuppressedTargetState>()
        val resolvedSlots = mutableListOf<String>()

        targetViews.forEach { (slot, view) ->
            if (!isModernStatusBarView(view)) {
                return TargetSnapshot.failure(
                    source = source,
                    reason = "target-not-modern-status-bar-view",
                    targets = newTargets.size,
                    slots = resolvedSlots,
                )
            }

            val previous =
                oldTargets.firstOrNull { state ->
                    state.view.get() === view
                }
            val nativeVisibilityState =
                previous?.latestNativeVisibilityState
                    ?: readVisibleStateLocked(view)
                    ?: return TargetSnapshot.failure(
                        source = source,
                        reason = "native-visibility-state-unavailable",
                        targets = newTargets.size,
                        slots = resolvedSlots,
                    )

            newTargets +=
                SuppressedTargetState(
                    view = WeakReference(view),
                    latestNativeVisibilityState = nativeVisibilityState,
                )
            resolvedSlots += slot
        }

        suppressedTargets =
            newTargets
                .distinctBy { state ->
                    state.view.get()?.let(System::identityHashCode)
                }
                .toTypedArray()

        val currentViews =
            suppressedTargets
                .mapNotNull { state -> state.view.get() }
                .toSet()
        oldTargets
            .filter { state ->
                val view = state.view.get()
                view != null && view !in currentViews
            }
            .forEach { state ->
                restoreTargetVisibilityLocked(state)
            }

        var hiddenTargets = 0
        suppressedTargets.forEach { state ->
            val view = state.view.get() ?: return@forEach
            val hiddenState =
                resolveEffectiveVisibilityState(
                    nativeVisibilityState = state.latestNativeVisibilityState,
                    suppressionActive = true,
                )
            if (
                applyVisibleStateLocked(
                    view = view,
                    state = hiddenState,
                )
            ) {
                hiddenTargets += 1
            } else {
                return TargetSnapshot.failure(
                    source = source,
                    reason = "visual-hide-apply-failed",
                    targets = suppressedTargets.size,
                    slots = resolvedSlots,
                )
            }
        }

        group.requestLayout()

        return TargetSnapshot.ready(
            source = source,
            targets = suppressedTargets.size,
            slots = resolvedSlots,
            wifiSuppressed = wifiSuppressionEnabled,
            mobileSuppressed = mobileSuppressionEnabled,
            hiddenTargets = hiddenTargets,
        )
    }

    private fun isModernStatusBarView(view: View): Boolean {
        var clazz: Class<*>? = view.javaClass
        while (clazz != null) {
            if (clazz.name == MODERN_STATUS_BAR_VIEW_CLASS) {
                return true
            }
            clazz = clazz.superclass
        }
        return false
    }

    private fun readVisibleStateLocked(view: View): Int? {
        val method = getVisibleStateMethod ?: return null
        return runCatching {
            (method.invoke(view) as? Number)?.toInt()
        }.getOrNull()
    }

    private fun applyVisibleStateLocked(
        view: View,
        state: Int,
    ): Boolean {
        val method = setVisibleStateMethod ?: return false
        applyingVisibilityOverride = true
        return try {
            method.invoke(
                view,
                Integer.valueOf(state),
                java.lang.Boolean.FALSE,
            )
            readVisibleStateLocked(view) == state
        } catch (_: Throwable) {
            false
        } finally {
            applyingVisibilityOverride = false
        }
    }

    private fun restoreTargetVisibilityLocked(state: SuppressedTargetState): Boolean {
        val view = state.view.get() ?: return false
        return applyVisibleStateLocked(
            view = view,
            state = state.latestNativeVisibilityState,
        )
    }

    private fun restoreSuppressedTargetsLocked(): Int {
        val states = suppressedTargets
        suppressedTargets = emptyArray()
        var restored = 0
        states.forEach { state ->
            if (restoreTargetVisibilityLocked(state)) {
                restored += 1
            }
        }
        return restored
    }

    private fun clearSessionLocked(requestLayout: Boolean) {
        val group = activeGroup?.get()
        restoreSuppressedTargetsLocked()
        activeManager = null
        activeGroup = null
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        applyingVisibilityOverride = false
        if (requestLayout) {
            group?.requestLayout()
        }
    }

    internal fun resolveEffectiveVisibilityState(
        nativeVisibilityState: Int,
        suppressionActive: Boolean,
    ): Int =
        if (suppressionActive) {
            VISIBILITY_STATE_HIDDEN
        } else {
            nativeVisibilityState
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

        data class Active(
            val targets: Int,
            val slots: List<String>,
            val wifiSuppressed: Boolean,
            val mobileSuppressed: Boolean,
            val hiddenTargets: Int,
        ) : StateResult {
            override val summary: String
                get() =
                    "active:targets=" + targets +
                        ",slots=" + slots.joinToString(",") +
                        ",wifiSuppressed=" + wifiSuppressed +
                        ",mobileSuppressed=" + mobileSuppressed +
                        ",hiddenTargets=" + hiddenTargets
        }

        data class Inactive(
            val restoredTargets: Int,
        ) : StateResult {
            override val summary: String
                get() = "inactive:restoredTargets=" + restoredTargets
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason
        }
    }

    private data class SuppressedTargetState(
        val view: WeakReference<View>,
        var latestNativeVisibilityState: Int,
    )

    private data class TargetSnapshot(
        val source: String,
        val targetCount: Int,
        val slots: List<String>,
        val wifiSuppressed: Boolean,
        val mobileSuppressed: Boolean,
        val hiddenTargetCount: Int,
        val failureReason: String?,
    ) {
        val logLine: String
            get() =
                "nativeNetworkSuppression " +
                    if (failureReason == null) {
                        "active"
                    } else {
                        "unavailable"
                    } +
                    " source=" + source +
                    " targets=" + targetCount +
                    " slots=" + slots.joinToString(",") +
                    " wifiSuppressed=" + wifiSuppressed +
                    " mobileSuppressed=" + mobileSuppressed +
                    " hiddenTargets=" + hiddenTargetCount +
                    " reason=" + (failureReason ?: "none") +
                    " contract=ModernStatusBarView.isIconVisible+setVisibleState(STATE_HIDDEN)" +
                    " nativeGeometryWrites=0"

        companion object {
            fun ready(
                source: String,
                targets: Int,
                slots: List<String>,
                wifiSuppressed: Boolean,
                mobileSuppressed: Boolean,
                hiddenTargets: Int,
            ): TargetSnapshot =
                TargetSnapshot(
                    source = source,
                    targetCount = targets,
                    slots = slots.distinct(),
                    wifiSuppressed = wifiSuppressed,
                    mobileSuppressed = mobileSuppressed,
                    hiddenTargetCount = hiddenTargets,
                    failureReason = null,
                )

            fun failure(
                source: String,
                reason: String,
                targets: Int = 0,
                slots: List<String> = emptyList(),
            ): TargetSnapshot =
                TargetSnapshot(
                    source = source,
                    targetCount = targets,
                    slots = slots.distinct(),
                    wifiSuppressed = false,
                    mobileSuppressed = false,
                    hiddenTargetCount = 0,
                    failureReason = reason,
                )
        }
    }

    private const val EXPECTED_HOOK_COUNT = 3
}
