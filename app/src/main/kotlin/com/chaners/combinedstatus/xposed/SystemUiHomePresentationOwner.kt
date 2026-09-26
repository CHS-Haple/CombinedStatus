package com.chaners.combinedstatus.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field

internal object SystemUiHomePresentationOwner {
    private const val HOME_HOST =
        "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val LEGACY_SLOT = "combined_status"
    private const val MEASURE_HOOK_ID =
        "combinedstatus.homePresentation.statusIconsMeasure"
    private const val LAYOUT_HOOK_ID =
        "combinedstatus.homePresentation.statusIconsLayout"

    private val representedSlots =
        linkedSetOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")

    private var measureHook: HookHandle? = null
    private var layoutHook: HookHandle? = null
    private var ignoredSlotsField: Field? = null
    private var current: Session? = null
    private var eventSink: ((String) -> Unit)? = null
    private var failNativeSink: ((String) -> Unit)? = null

    val installedHookCount: Int
        @Synchronized get() = listOfNotNull(measureHook, layoutHook).size

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onFailNative: ((String) -> Unit)? = null,
    ): InstallResult {
        if (installedHookCount == 2) {
            eventSink = onEvent
            failNativeSink = onFailNative
            return InstallResult.AlreadyInstalled
        }
        if (installedHookCount != 0) {
            return InstallResult.Failure("partial-hook-state")
        }

        val containerClass =
            runCatching {
                Class.forName(STATUS_ICON_CONTAINER, false, classLoader)
            }.getOrElse {
                return InstallResult.Failure("status-icon-container-class-missing")
            }
        val field =
            generateSequence(containerClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == "ignoredSlots" &&
                            java.util.List::class.java.isAssignableFrom(candidate.type)
                    }
                }
                .firstOrNull()
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("ignored-slots-field-missing")
        val onMeasure =
            containerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "onMeasure" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                            ),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("on-measure-contract-missing")
        val onLayout =
            containerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "onLayout" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(
                                Boolean::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                            ),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("on-layout-contract-missing")

        eventSink = onEvent
        failNativeSink = onFailNative
        ignoredSlotsField = field

        val first =
            runCatching {
                module
                    .hook(onMeasure)
                    .setId(MEASURE_HOOK_ID)
                    .intercept(layoutHooker(refreshMasksAfter = false))
            }.getOrElse { error ->
                clearInstallState()
                return InstallResult.Failure(
                    "measure-hook-" + (error.message ?: error.javaClass.simpleName),
                )
            }
        val second =
            runCatching {
                module
                    .hook(onLayout)
                    .setId(LAYOUT_HOOK_ID)
                    .intercept(layoutHooker(refreshMasksAfter = true))
            }.getOrElse { error ->
                runCatching { first.unhook() }
                clearInstallState()
                return InstallResult.Failure(
                    "layout-hook-" + (error.message ?: error.javaClass.simpleName),
                )
            }

        measureHook = first
        layoutHook = second
        return InstallResult.Installed
    }

    @Synchronized
    fun activate(host: Any): StateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return StateResult.Failure("main-thread-required")
        }
        if (installedHookCount != 2) {
            return StateResult.Failure("hooks-not-ready")
        }
        val hostView =
            host as? ViewGroup
                ?: return StateResult.Failure("host-not-view-group")
        if (hostView.javaClass.name != HOME_HOST) {
            return StateResult.Failure("home-host-mismatch")
        }
        val statusIcons =
            NativeParticipantRuntimeAccess.groupFor(host)
                ?: return StateResult.Failure("status-icon-group-missing")
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER) {
            return StateResult.Failure("status-icon-group-type-mismatch")
        }
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return StateResult.Failure("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return StateResult.Failure("battery-view-missing")
        val field =
            ignoredSlotsField
                ?: return StateResult.Failure("ignored-slots-field-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(statusIcons) as? MutableList<String> }.getOrNull()
                ?: return StateResult.Failure("ignored-slots-list-unavailable")
        list.size

        val existing = current
        if (existing?.matches(hostView, statusIcons, battery) == true) {
            val masked = existing.refreshClipMasks()
            statusIcons.requestLayout()
            return StateResult.Active(representedSlots.size, masked, true)
        }

        existing?.stop("host-replaced")
        val session =
            Session(
                host = hostView,
                statusIcons = statusIcons,
                battery = battery,
                ignoredSlotsField = field,
                onEvent = { event -> eventSink?.invoke(event) },
                onFailNative = ::onSessionFailure,
            )
        current = session
        val masked = session.start()
        statusIcons.requestLayout()
        eventSink?.invoke(
            "homePresentation active carrier=MiuiNotificationStatusContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + masked +
                " slotExclusion=scoped-native-measure-layout visualMask=clipBounds " +
                "nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Active(representedSlots.size, masked, false)
    }

    @Synchronized
    fun deactivate(source: String): StateResult {
        val session = current ?: return StateResult.Inactive(0)
        current = null
        val restored = session.stop(source)
        eventSink?.invoke(
            "homePresentation inactive source=" + source +
                " restoredViews=" + restored +
                " nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Inactive(restored)
    }

    @Synchronized
    fun releaseGenerationForHotReload(): Int {
        val session = current ?: return 0
        current = null
        val restored = session.stop("hotReload-oldGeneration")
        eventSink = null
        failNativeSink = null
        return restored
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        runCatching { measureHook?.unhook() }
        runCatching { layoutHook?.unhook() }
        clearInstallState()
    }

    @Synchronized
    fun cleanupLegacyParticipant(host: Any): LegacyCleanupResult {
        val group =
            NativeParticipantRuntimeAccess.groupFor(host)
                ?: return LegacyCleanupResult.Failure("status-icon-group-missing")
        val legacyView = NativeParticipantRuntimeAccess.findSlotView(group, LEGACY_SLOT)
        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready -> resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure -> {
                    return if (legacyView == null) {
                        LegacyCleanupResult.NotPresent
                    } else {
                        LegacyCleanupResult.Failure(resolution.reason)
                    }
                }
            }
        val holder = NativeParticipantRuntimeAccess.iconHolder(handles, LEGACY_SLOT)
        if (legacyView == null && holder == null) {
            return LegacyCleanupResult.NotPresent
        }
        val removal =
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?: return LegacyCleanupResult.Failure("legacy-removal-contract-missing")
        val removed =
            runCatching {
                NativeParticipantRuntimeAccess.invokeRemoval(handles, removal, LEGACY_SLOT)
                NativeParticipantRuntimeAccess.clearBindableEntries(handles, LEGACY_SLOT)
                NativeParticipantRuntimeAccess.findSlotView(group, LEGACY_SLOT) == null &&
                    NativeParticipantRuntimeAccess.iconHolder(handles, LEGACY_SLOT) == null
            }.getOrDefault(false)
        return if (removed) {
            LegacyCleanupResult.Removed
        } else {
            LegacyCleanupResult.Failure("legacy-removal-verification-failed")
        }
    }

    private fun layoutHooker(refreshMasksAfter: Boolean): Hooker =
        Hooker { chain ->
            val target = chain.thisObject as? ViewGroup
                ?: return@Hooker chain.proceed()
            val session =
                synchronized(this) {
                    current?.takeIf { candidate -> candidate.owns(target) }
                } ?: return@Hooker chain.proceed()

            val result = session.withRepresentedSlotsIgnored { chain.proceed() }
            if (refreshMasksAfter) {
                session.refreshClipMasks()
            }
            result
        }

    @Synchronized
    private fun onSessionFailure(reason: String) {
        val session = current ?: return
        current = null
        session.stop("fail-native:" + reason)
        eventSink?.invoke(
            "homePresentation failNative reason=" + reason + " restoredNative=true",
        )
        failNativeSink?.invoke(reason)
    }

    private fun clearInstallState() {
        measureHook = null
        layoutHook = null
        ignoredSlotsField = null
        eventSink = null
        failNativeSink = null
    }

    private class Session(
        host: ViewGroup,
        statusIcons: ViewGroup,
        battery: View,
        private val ignoredSlotsField: Field,
        private val onEvent: (String) -> Unit,
        private val onFailNative: (String) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusIcons = WeakReference(statusIcons)
        private val battery = WeakReference(battery)
        private var active = true
        private val clipStates = mutableListOf<ClipState>()

        fun matches(host: ViewGroup, statusIcons: ViewGroup, battery: View): Boolean =
            active &&
                this.host.get() === host &&
                this.statusIcons.get() === statusIcons &&
                this.battery.get() === battery

        fun owns(candidate: ViewGroup): Boolean =
            active && statusIcons.get() === candidate

        fun start(): Int {
            host.get()?.addOnAttachStateChangeListener(this)
            return refreshClipMasks()
        }

        fun stop(source: String): Int {
            if (!active && clipStates.isEmpty()) {
                return 0
            }
            active = false
            host.get()?.removeOnAttachStateChangeListener(this)
            val restored = restoreClipMasks()
            statusIcons.get()?.requestLayout()
            onEvent(
                "homePresentation cleanup source=" + source +
                    " restoredClipBounds=" + restored,
            )
            return restored
        }

        fun <T> withRepresentedSlotsIgnored(block: () -> T): T {
            if (!active) {
                return block()
            }
            val container = statusIcons.get()
            if (container == null) {
                onFailNative("status-icon-group-released")
                return block()
            }
            @Suppress("UNCHECKED_CAST")
            val list =
                runCatching {
                    ignoredSlotsField.get(container) as? MutableList<String>
                }.getOrNull()
            if (list == null) {
                onFailNative("ignored-slots-list-unavailable")
                return block()
            }

            val owned =
                try {
                    OwnedListEntries.addOwnedEntries(list, representedSlots)
                } catch (error: Throwable) {
                    onFailNative(
                        "ignored-slots-add-" +
                            (error.message ?: error.javaClass.simpleName),
                    )
                    return block()
                }

            return try {
                block()
            } finally {
                OwnedListEntries.restoreOwnedEntries(list, owned)
            }
        }

        fun refreshClipMasks(): Int {
            if (!active) {
                return 0
            }
            val group = statusIcons.get()
                ?: run {
                    onFailNative("status-icon-group-released")
                    return 0
                }
            val batteryView = battery.get()
                ?: run {
                    onFailNative("battery-view-released")
                    return 0
                }

            val targets = linkedSetOf<View>()
            targets += batteryView
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (NativeParticipantRuntimeAccess.slotOf(child) in representedSlots) {
                    targets += child
                }
            }

            val iterator = clipStates.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null) {
                        restoreClipState(state)
                    }
                    iterator.remove()
                }
            }

            targets.forEach { view ->
                if (clipStates.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    clipStates += ClipState(WeakReference(view), nativeClip, applied)
                }
            }
            return clipStates.count { state -> state.view.get() != null }
        }

        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            if (active) {
                onFailNative("home-host-detached")
            }
        }

        private fun restoreClipMasks(): Int {
            val states = clipStates.toList()
            clipStates.clear()
            var restored = 0
            states.forEach { state ->
                if (restoreClipState(state)) {
                    restored += 1
                }
            }
            return restored
        }

        private fun restoreClipState(state: ClipState): Boolean {
            val view = state.view.get() ?: return false
            if (view.clipBounds != state.appliedClip) {
                return false
            }
            view.clipBounds = state.nativeClip?.let(::Rect)
            return view.clipBounds == state.nativeClip
        }
    }

    private data class ClipState(
        val view: WeakReference<View>,
        val nativeClip: Rect?,
        val appliedClip: Rect,
    )

    internal object OwnedListEntries {
        fun <T> addOwnedEntries(
            target: MutableList<T>,
            entries: Collection<T>,
        ): List<T> {
            val added = entries.filterNot(target::contains)
            val applied = mutableListOf<T>()
            return try {
                added.forEach { entry ->
                    target.add(entry)
                    applied += entry
                }
                applied
            } catch (error: Throwable) {
                applied.asReversed().forEach(target::remove)
                throw error
            }
        }

        fun <T> restoreOwnedEntries(
            target: MutableList<T>,
            ownedEntries: List<T>,
        ) {
            ownedEntries.asReversed().forEach(target::remove)
        }

        fun <T, R> withTemporaryEntries(
            target: MutableList<T>,
            entries: Collection<T>,
            block: () -> R,
        ): R {
            val owned = addOwnedEntries(target, entries)
            return try {
                block()
            } finally {
                restoreOwnedEntries(target, owned)
            }
        }
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

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult
        data class Failure(val reason: String) : InstallResult
    }

    internal sealed interface StateResult {
        data class Active(
            val representedSlots: Int,
            val maskedViews: Int,
            val reused: Boolean,
        ) : StateResult
        data class Inactive(val restoredViews: Int) : StateResult
        data class Failure(val reason: String) : StateResult
    }

    internal sealed interface LegacyCleanupResult {
        data object NotPresent : LegacyCleanupResult
        data object Removed : LegacyCleanupResult
        data class Failure(val reason: String) : LegacyCleanupResult
    }
}
