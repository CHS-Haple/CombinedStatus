package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal object SystemUiNativeNetworkSuppressionOwner {
    private const val WIFI_BINDING_CLASS =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder\$bind\$2"
    private const val MOBILE_BINDING_CLASS =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder\$bind\$2"
    private const val HOME_MANAGER_CLASS =
        "com.android.systemui.statusbar.phone.ui.DarkIconManager"
    private const val MODERN_BINDING_INTERFACE =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"

    private const val WIFI_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.wifiVisibility"
    private const val MOBILE_VISIBILITY_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.mobileVisibility"
    private const val WIFI_VISIBILITY_STATE_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.wifiVisibilityState"
    private const val MOBILE_VISIBILITY_STATE_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.mobileVisibilityState"
    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"

    private const val VISIBILITY_STATE_HIDDEN = 2

    private val noTargetSlots = emptySet<String>()
    private val wifiOnlyTargetSlots = setOf("wifi")
    private val mobileOnlyTargetSlots = setOf("mobile")
    private val wifiAndMobileTargetSlots = setOf("wifi", "mobile")
    private val observableTargetSlots = wifiAndMobileTargetSlots

    private val installedHandles = mutableListOf<HookHandle>()
    private var wifiVisibilityStateMethod: Method? = null
    private var mobileVisibilityStateMethod: Method? = null
    private var activeManager: Any? = null
    private var activeGroup: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null
    private var applyingVisibilityOverride = false

    @Volatile
    private var suppressedBindings: Array<SuppressedBindingState> = emptyArray()

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
            val wifiVisibility =
                resolveShouldVisibilityMethod(
                    classLoader = classLoader,
                    className = WIFI_BINDING_CLASS,
                )
            val mobileVisibility =
                resolveShouldVisibilityMethod(
                    classLoader = classLoader,
                    className = MOBILE_BINDING_CLASS,
                )
            val wifiVisibilityState =
                resolveVisibilityStateMethod(
                    classLoader = classLoader,
                    className = WIFI_BINDING_CLASS,
                )
            val mobileVisibilityState =
                resolveVisibilityStateMethod(
                    classLoader = classLoader,
                    className = MOBILE_BINDING_CLASS,
                )
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
                    .hook(wifiVisibility)
                    .setId(WIFI_VISIBILITY_HOOK_ID)
                    .intercept(shouldVisibilityHooker())
            created +=
                module
                    .hook(mobileVisibility)
                    .setId(MOBILE_VISIBILITY_HOOK_ID)
                    .intercept(shouldVisibilityHooker())
            created +=
                module
                    .hook(wifiVisibilityState)
                    .setId(WIFI_VISIBILITY_STATE_HOOK_ID)
                    .intercept(visibilityStateHooker())
            created +=
                module
                    .hook(mobileVisibilityState)
                    .setId(MOBILE_VISIBILITY_STATE_HOOK_ID)
                    .intercept(visibilityStateHooker())
            created +=
                module
                    .hook(onIconAdded)
                    .setId(HOME_ICON_ADDED_HOOK_ID)
                    .intercept(homeIconAddedHooker())

            wifiVisibilityStateMethod = wifiVisibilityState
            mobileVisibilityStateMethod = mobileVisibilityState
            installedHandles.clear()
            installedHandles.addAll(created)
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            created.forEach { handle ->
                runCatching { handle.unhook() }
            }
            installedHandles.clear()
            wifiVisibilityStateMethod = null
            mobileVisibilityStateMethod = null
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

        val snapshot = refreshBindingsLocked("handoff")
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return StateResult.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return StateResult.Active(
            bindings = snapshot.bindingCount,
            slots = snapshot.slots,
            wifiSuppressed = snapshot.wifiSuppressed,
            mobileSuppressed = snapshot.mobileSuppressed,
            hiddenBindings = snapshot.hiddenBindingCount,
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

        val snapshot = refreshBindingsLocked(source)
        if (snapshot.failureReason != null) {
            clearSessionLocked(requestLayout = true)
            return StateResult.Failure(snapshot.failureReason)
        }

        eventSink?.invoke(snapshot.logLine)
        return StateResult.Active(
            bindings = snapshot.bindingCount,
            slots = snapshot.slots,
            wifiSuppressed = snapshot.wifiSuppressed,
            mobileSuppressed = snapshot.mobileSuppressed,
            hiddenBindings = snapshot.hiddenBindingCount,
        )
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val group = activeGroup?.get()
        val previousCount = suppressedBindings.count { state -> state.binding.get() != null }
        val restoredCount = restoreSuppressedBindingsLocked()

        activeManager = null
        activeGroup = null
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        applyingVisibilityOverride = false

        group?.requestLayout()
        if (previousCount > 0) {
            eventSink?.invoke(
                "nativeNetworkSuppression inactive source=" + source +
                    " restoredBindings=" + restoredCount +
                    " nativeGeometryWrites=0",
            )
        }
        return StateResult.Inactive(restoredCount)
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        installedHandles.clear()
        wifiVisibilityStateMethod = null
        mobileVisibilityStateMethod = null
        eventSink = null
    }

    private fun shouldVisibilityHooker(): Hooker =
        Hooker { chain ->
            val binding = chain.thisObject
            if (isSuppressed(binding)) {
                false
            } else {
                chain.proceed()
            }
        }

    private fun visibilityStateHooker(): Hooker =
        Hooker { chain ->
            val binding = chain.thisObject
            val requestedState = (chain.getArg(0) as? Number)?.toInt()
            val effectiveState =
                synchronized(this) {
                    val state =
                        suppressedBindings.firstOrNull { candidate ->
                            candidate.binding.get() === binding
                        }
                    if (
                        applyingVisibilityOverride ||
                        state == null ||
                        requestedState == null
                    ) {
                        null
                    } else {
                        state.latestNativeVisibilityState = requestedState
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
                        val snapshot = refreshBindingsLocked("iconAdded:" + slot)
                        eventSink?.invoke(snapshot.logLine)
                    }
                }
            }
            result
        }

    private fun isSuppressed(binding: Any): Boolean =
        suppressedBindings.any { state ->
            state.binding.get() === binding
        }

    private fun refreshBindingsLocked(source: String): BindingSnapshot {
        val group =
            activeGroup?.get()
                ?: return BindingSnapshot.failure(
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

        val oldStates = suppressedBindings
        val newStates = mutableListOf<SuppressedBindingState>()
        val resolvedSlots = mutableListOf<String>()

        targetViews.forEach { (slot, view) ->
            val binding =
                bindingOf(view)
                    ?: return BindingSnapshot.failure(
                        source = source,
                        reason = "binding-resolution-incomplete",
                        targetViews = targetViews.size,
                        bindings = newStates.size,
                        slots = resolvedSlots,
                    )
            val visibilityMethod =
                visibilityStateMethodOf(binding)
                    ?: return BindingSnapshot.failure(
                        source = source,
                        reason = "visibility-state-method-missing",
                        targetViews = targetViews.size,
                        bindings = newStates.size,
                        slots = resolvedSlots,
                    )
            val previous =
                oldStates.firstOrNull { state ->
                    state.binding.get() === binding
                }
            val nativeVisibilityState =
                previous?.latestNativeVisibilityState
                    ?: currentVisibilityStateOf(view)
                    ?: return BindingSnapshot.failure(
                        source = source,
                        reason = "native-visibility-state-unavailable",
                        targetViews = targetViews.size,
                        bindings = newStates.size,
                        slots = resolvedSlots,
                    )

            newStates +=
                SuppressedBindingState(
                    binding = WeakReference(binding),
                    visibilityStateMethod = visibilityMethod,
                    latestNativeVisibilityState = nativeVisibilityState,
                )
            resolvedSlots += slot
        }

        suppressedBindings =
            newStates
                .distinctBy { state ->
                    state.binding.get()?.let(System::identityHashCode)
                }
                .toTypedArray()

        val currentBindings =
            suppressedBindings
                .mapNotNull { state -> state.binding.get() }
                .toSet()
        oldStates
            .filter { state ->
                val binding = state.binding.get()
                binding != null && binding !in currentBindings
            }
            .forEach { state ->
                restoreBindingVisibilityLocked(state)
            }

        var hiddenBindings = 0
        suppressedBindings.forEach { state ->
            val binding = state.binding.get() ?: return@forEach
            val hiddenState =
                resolveEffectiveVisibilityState(
                    nativeVisibilityState = state.latestNativeVisibilityState,
                    suppressionActive = true,
                )
            if (
                applyVisibilityStateLocked(
                    binding = binding,
                    method = state.visibilityStateMethod,
                    state = hiddenState,
                )
            ) {
                hiddenBindings += 1
            } else {
                return BindingSnapshot.failure(
                    source = source,
                    reason = "visual-hide-apply-failed",
                    targetViews = targetViews.size,
                    bindings = suppressedBindings.size,
                    slots = resolvedSlots,
                )
            }
        }

        group.requestLayout()

        return BindingSnapshot.ready(
            source = source,
            targetViews = targetViews.size,
            bindings = suppressedBindings.size,
            slots = resolvedSlots,
            wifiSuppressed = wifiSuppressionEnabled,
            mobileSuppressed = mobileSuppressionEnabled,
            hiddenBindings = hiddenBindings,
        )
    }

    private fun bindingOf(view: View): Any? {
        val getter =
            generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.name.startsWith("getBinding\$") &&
                        method.returnType.name == MODERN_BINDING_INTERFACE
                }
                ?: return null
        return runCatching {
            getter.isAccessible = true
            getter.invoke(view)
        }.getOrNull()
    }

    private fun currentVisibilityStateOf(view: View): Int? {
        val getter =
            generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.name == "getVisibleState" &&
                        method.returnType == Integer.TYPE
                }
                ?: return null
        return runCatching {
            getter.isAccessible = true
            (getter.invoke(view) as? Number)?.toInt()
        }.getOrNull()
    }

    private fun visibilityStateMethodOf(binding: Any): Method? =
        when (binding.javaClass.name) {
            WIFI_BINDING_CLASS -> wifiVisibilityStateMethod
            MOBILE_BINDING_CLASS -> mobileVisibilityStateMethod
            else -> null
        }

    private fun resolveShouldVisibilityMethod(
        classLoader: ClassLoader,
        className: String,
    ): Method {
        val clazz =
            Class.forName(
                className,
                false,
                classLoader,
            )
        return clazz
            .getDeclaredMethod("getShouldIconBeVisible")
            .apply {
                check(returnType == java.lang.Boolean.TYPE) {
                    "visibility-return-type-mismatch:" + className
                }
                isAccessible = true
            }
    }

    private fun resolveVisibilityStateMethod(
        classLoader: ClassLoader,
        className: String,
    ): Method {
        val clazz =
            Class.forName(
                className,
                false,
                classLoader,
            )
        return clazz
            .getDeclaredMethod(
                "onVisibilityStateChanged",
                Integer.TYPE,
            ).apply {
                check(returnType == Void.TYPE) {
                    "visibility-state-return-type-mismatch:" + className
                }
                isAccessible = true
            }
    }

    private fun applyVisibilityStateLocked(
        binding: Any,
        method: Method,
        state: Int,
    ): Boolean {
        applyingVisibilityOverride = true
        return try {
            method.invoke(
                binding,
                Integer.valueOf(state),
            )
            true
        } catch (_: Throwable) {
            false
        } finally {
            applyingVisibilityOverride = false
        }
    }

    private fun restoreBindingVisibilityLocked(state: SuppressedBindingState): Boolean {
        val binding = state.binding.get() ?: return false
        return applyVisibilityStateLocked(
            binding = binding,
            method = state.visibilityStateMethod,
            state = state.latestNativeVisibilityState,
        )
    }

    private fun restoreSuppressedBindingsLocked(): Int {
        val states = suppressedBindings
        suppressedBindings = emptyArray()
        var restored = 0
        states.forEach { state ->
            if (restoreBindingVisibilityLocked(state)) {
                restored += 1
            }
        }
        return restored
    }

    private fun clearSessionLocked(requestLayout: Boolean) {
        val group = activeGroup?.get()
        restoreSuppressedBindingsLocked()
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
            val bindings: Int,
            val slots: List<String>,
            val wifiSuppressed: Boolean,
            val mobileSuppressed: Boolean,
            val hiddenBindings: Int,
        ) : StateResult {
            override val summary: String
                get() =
                    "active:bindings=" + bindings +
                        ",slots=" + slots.joinToString(",") +
                        ",wifiSuppressed=" + wifiSuppressed +
                        ",mobileSuppressed=" + mobileSuppressed +
                        ",hiddenBindings=" + hiddenBindings
        }

        data class Inactive(
            val restoredBindings: Int,
        ) : StateResult {
            override val summary: String
                get() = "inactive:restoredBindings=" + restoredBindings
        }

        data class Failure(
            val reason: String,
        ) : StateResult {
            override val summary: String
                get() = "failed:" + reason
        }
    }

    private data class SuppressedBindingState(
        val binding: WeakReference<Any>,
        val visibilityStateMethod: Method,
        var latestNativeVisibilityState: Int,
    )

    private data class BindingSnapshot(
        val source: String,
        val targetViewCount: Int,
        val bindingCount: Int,
        val slots: List<String>,
        val wifiSuppressed: Boolean,
        val mobileSuppressed: Boolean,
        val hiddenBindingCount: Int,
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
                    " targetViews=" + targetViewCount +
                    " bindings=" + bindingCount +
                    " slots=" + slots.joinToString(",") +
                    " wifiSuppressed=" + wifiSuppressed +
                    " mobileSuppressed=" + mobileSuppressed +
                    " hiddenBindings=" + hiddenBindingCount +
                    " reason=" + (failureReason ?: "none") +
                    " visualContract=ModernStatusBarViewBinding.onVisibilityStateChanged(STATE_HIDDEN)" +
                    " nativeGeometryWrites=0"

        companion object {
            fun ready(
                source: String,
                targetViews: Int,
                bindings: Int,
                slots: List<String>,
                wifiSuppressed: Boolean,
                mobileSuppressed: Boolean,
                hiddenBindings: Int,
            ): BindingSnapshot =
                BindingSnapshot(
                    source = source,
                    targetViewCount = targetViews,
                    bindingCount = bindings,
                    slots = slots.distinct(),
                    wifiSuppressed = wifiSuppressed,
                    mobileSuppressed = mobileSuppressed,
                    hiddenBindingCount = hiddenBindings,
                    failureReason = null,
                )

            fun failure(
                source: String,
                reason: String,
                targetViews: Int = 0,
                bindings: Int = 0,
                slots: List<String> = emptyList(),
            ): BindingSnapshot =
                BindingSnapshot(
                    source = source,
                    targetViewCount = targetViews,
                    bindingCount = bindings,
                    slots = slots.distinct(),
                    wifiSuppressed = false,
                    mobileSuppressed = false,
                    hiddenBindingCount = 0,
                    failureReason = reason,
                )
        }
    }

    private const val EXPECTED_HOOK_COUNT = 5
}
