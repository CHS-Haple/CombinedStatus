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
    private const val HOME_ICON_ADDED_HOOK_ID =
        "combinedstatus.nativeNetworkSuppression.homeIconAdded"

    private val wifiOnlyTargetSlots = setOf("wifi")
    private val mobileOnlyTargetSlots = setOf("mobile")
    private val wifiAndMobileTargetSlots = setOf("wifi", "mobile")
    private val observableTargetSlots = wifiAndMobileTargetSlots

    private val installedHandles = mutableListOf<HookHandle>()
    private var activeManager: Any? = null
    private var activeGroup: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null

    @Volatile
    private var suppressedBindings: Array<WeakReference<Any>> = emptyArray()

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
                resolveVisibilityMethod(
                    classLoader = classLoader,
                    className = WIFI_BINDING_CLASS,
                )
            val mobileVisibility =
                resolveVisibilityMethod(
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
                    .intercept(visibilityHooker())
            created +=
                module
                    .hook(mobileVisibility)
                    .setId(MOBILE_VISIBILITY_HOOK_ID)
                    .intercept(visibilityHooker())
            created +=
                module
                    .hook(onIconAdded)
                    .setId(HOME_ICON_ADDED_HOOK_ID)
                    .intercept(homeIconAddedHooker())

            installedHandles.clear()
            installedHandles.addAll(created)
            eventSink = onEvent
            InstallResult.Installed
        }.getOrElse { error ->
            created.forEach { handle ->
                runCatching { handle.unhook() }
            }
            installedHandles.clear()
            suppressedBindings = emptyArray()
            activeManager = null
            activeGroup = null
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
        )
    }

    @Synchronized
    fun updateWifiPolicy(
        suppressWifi: Boolean,
        source: String,
    ): StateResult? {
        if (wifiSuppressionEnabled == suppressWifi) {
            return null
        }
        wifiSuppressionEnabled = suppressWifi
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
        )
    }

    @Synchronized
    fun updateMobilePolicy(
        suppressMobile: Boolean,
        source: String,
    ): StateResult? {
        if (mobileSuppressionEnabled == suppressMobile) {
            return null
        }
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
        )
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val group = activeGroup?.get()
        val previousCount = suppressedBindings.count { reference -> reference.get() != null }
        clearSessionLocked(requestLayout = false)
        group?.requestLayout()
        if (previousCount > 0) {
            eventSink?.invoke(
                "nativeNetworkSuppression inactive source=" + source +
                    " restoredBindings=" + previousCount +
                    " nativeGeometryWrites=0",
            )
        }
        return StateResult.Inactive(previousCount)
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        installedHandles.clear()
        eventSink = null
    }

    private fun visibilityHooker(): Hooker =
        Hooker { chain ->
            val binding = chain.thisObject
            if (isSuppressed(binding)) {
                false
            } else {
                chain.proceed()
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
        suppressedBindings.any { reference ->
            reference.get() === binding
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
                    emptySet()
            }
        val targetViews = mutableListOf<Pair<String, View>>()
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val slot = NativeParticipantRuntimeAccess.slotOf(child) ?: continue
            if (slot in targetSlots) {
                targetViews += slot to child
            }
        }

        val bindings = mutableListOf<Any>()
        val resolvedSlots = mutableListOf<String>()
        targetViews.forEach { (slot, view) ->
            val binding = bindingOf(view)
            if (binding != null) {
                bindings += binding
                resolvedSlots += slot
            }
        }

        if (targetViews.isNotEmpty() && bindings.size != targetViews.size) {
            return BindingSnapshot.failure(
                source = source,
                reason = "binding-resolution-incomplete",
                targetViews = targetViews.size,
                bindings = bindings.size,
                slots = resolvedSlots,
            )
        }

        suppressedBindings =
            bindings
                .distinctBy { binding -> System.identityHashCode(binding) }
                .map(::WeakReference)
                .toTypedArray()

        group.requestLayout()

        return BindingSnapshot.ready(
            source = source,
            targetViews = targetViews.size,
            bindings = bindings.size,
            slots = resolvedSlots,
            wifiSuppressed = wifiSuppressionEnabled,
            mobileSuppressed = mobileSuppressionEnabled,
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

    private fun resolveVisibilityMethod(
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

    private fun clearSessionLocked(requestLayout: Boolean) {
        val group = activeGroup?.get()
        activeManager = null
        activeGroup = null
        suppressedBindings = emptyArray()
        wifiSuppressionEnabled = false
        mobileSuppressionEnabled = false
        if (requestLayout) {
            group?.requestLayout()
        }
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
        ) : StateResult {
            override val summary: String
                get() =
                    "active:bindings=" + bindings +
                        ",slots=" + slots.joinToString(",") +
                        ",wifiSuppressed=" + wifiSuppressed +
                        ",mobileSuppressed=" + mobileSuppressed
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

    private data class BindingSnapshot(
        val source: String,
        val targetViewCount: Int,
        val bindingCount: Int,
        val slots: List<String>,
        val wifiSuppressed: Boolean,
        val mobileSuppressed: Boolean,
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
                    " reason=" + (failureReason ?: "none") +
                    " nativeGeometryWrites=0"

        companion object {
            fun ready(
                source: String,
                targetViews: Int,
                bindings: Int,
                slots: List<String>,
                wifiSuppressed: Boolean,
                mobileSuppressed: Boolean,
            ): BindingSnapshot =
                BindingSnapshot(
                    source = source,
                    targetViewCount = targetViews,
                    bindingCount = bindings,
                    slots = slots.distinct(),
                    wifiSuppressed = wifiSuppressed,
                    mobileSuppressed = mobileSuppressed,
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
                    failureReason = reason,
                )
        }
    }

    private const val EXPECTED_HOOK_COUNT = 3
}
