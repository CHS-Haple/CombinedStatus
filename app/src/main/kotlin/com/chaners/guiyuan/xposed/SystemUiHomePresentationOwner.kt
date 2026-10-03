package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

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
    private const val BATTERY_HIDE_HOOK_ID =
        "combinedstatus.homePresentation.batteryHideState"
    private const val FAKE_ISLAND_MONITOR_CLASS =
        "com.android.systemui.statusbar.IslandMonitor\$FakeContainerIslandMonitor"
    private const val FAKE_ISLAND_WIDTH_HOOK_ID =
        "combinedstatus.homePresentation.fakeIslandWidth2D"
    private const val CONTROL_CENTER_FAKE_SURFACE = "control-center-fake"

    private val representedSlots =
        linkedSetOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")

    private var measureHook: HookHandle? = null
    private var layoutHook: HookHandle? = null
    private var batteryHideHook: HookHandle? = null
    private var fakeIslandWidthHook: HookHandle? = null
    private var ignoredSlotsField: Field? = null
    private var addIgnoredSlotsMethod: java.lang.reflect.Method? = null
    private var setIgnoredSlotsMethod: java.lang.reflect.Method? = null
    private var batteryHideField: Field? = null
    private var current: Session? = null
    private var keyguardCurrent: Session? = null
    private var controlCenterCurrent: Session? = null
    private var controlCenterEventSink: ((String) -> Unit)? = null
    private var controlCenterFailNativeSink: ((String) -> Unit)? = null
    private var controlCenterReadySink: ((ControlCenterStateResult.Active) -> Unit)? = null
    private var eventSink: ((String) -> Unit)? = null
    private var failNativeSink: ((String) -> Unit)? = null
    private var keyguardEventSink: ((String) -> Unit)? = null
    private var keyguardFailNativeSink: ((String) -> Unit)? = null
    private var keyguardReadySink: ((StateResult.Active) -> Unit)? = null
    private var contractProbeEnabled: () -> Boolean = { false }
    private var controlCenterIslandContractProbeSummary: String? = null

    val installedHookCount: Int
        @Synchronized get() = listOfNotNull(measureHook, layoutHook, batteryHideHook).size

    @Synchronized
    internal fun currentHomeRepresentedSlotOwnership(): Set<String> =
        current?.ownedRepresentedSlots() ?: emptySet()

    @Synchronized
    internal fun currentKeyguardRepresentedSlotOwnership(): Set<String> =
        keyguardCurrent?.ownedRepresentedSlots() ?: emptySet()

    @Synchronized
    internal fun currentControlCenterNativeLayoutAuthority(): Boolean =
        controlCenterCurrent?.usesNativeLayoutAuthority() == true

    @Synchronized
    internal fun currentControlCenterIslandContractProbeSummary(): String =
        controlCenterIslandContractProbeSummary ?: "unavailable"

    @Synchronized
    private fun publishControlCenterIslandContractProbeSummary(summary: String) {
        controlCenterIslandContractProbeSummary = summary
    }

    @Synchronized
    fun onVisualSettingsChanged() {
        current?.syncEndReservation()
        keyguardCurrent?.syncEndReservation()
        controlCenterCurrent?.syncEndReservation()
    }

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onFailNative: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
    ): InstallResult {
        if (installedHookCount == 3) {
            eventSink = onEvent
            failNativeSink = onFailNative
            contractProbeEnabled = isProbeEnabled
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
        val addIgnoredSlots =
            containerClass.declaredMethods
                .filter { method ->
                    method.name == "addIgnoredSlots" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.util.List::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                .singleOrNull()
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("add-ignored-slots-contract-missing")
        val setIgnoredSlots =
            containerClass.declaredMethods
                .filter { method ->
                    method.name == "setIgnoredSlots" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.util.List::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                .singleOrNull()
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("set-ignored-slots-contract-missing")
        val batteryContainerClass =
            runCatching {
                Class.forName(BATTERY_CONTAINER, false, classLoader)
            }.getOrElse {
                return InstallResult.Failure("battery-container-class-missing")
            }
        val hideField =
            generateSequence(batteryContainerClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == "mIsHideBattery" &&
                            candidate.type == java.lang.Boolean.TYPE
                    }
                }
                .firstOrNull()
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("battery-hide-field-missing")
        val batterySetIsHide =
            batteryContainerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "setIsHideBattery" &&
                        method.parameterTypes.contentEquals(
                            arrayOf(java.lang.Boolean::class.java),
                        ) &&
                        method.returnType == Void.TYPE
                }
                ?.apply { isAccessible = true }
                ?: return InstallResult.Failure("battery-hide-method-contract-missing")

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
        contractProbeEnabled = isProbeEnabled
        ignoredSlotsField = field
        addIgnoredSlotsMethod = addIgnoredSlots
        setIgnoredSlotsMethod = setIgnoredSlots
        batteryHideField = hideField

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
        val third =
            runCatching {
                module
                    .hook(batterySetIsHide)
                    .setId(BATTERY_HIDE_HOOK_ID)
                    .intercept(batteryHideStateHooker())
            }.getOrElse { error ->
                runCatching { first.unhook() }
                runCatching { second.unhook() }
                clearInstallState()
                return InstallResult.Failure(
                    "battery-hide-hook-" + (error.message ?: error.javaClass.simpleName),
                )
            }

        measureHook = first
        layoutHook = second
        batteryHideHook = third
        fakeIslandWidthHook =
            runCatching {
                val monitorClass =
                    Class.forName(FAKE_ISLAND_MONITOR_CLASS, false, classLoader)
                val getIslandWidth =
                    monitorClass.declaredMethods
                        .filter { method ->
                            method.name == "getIslandWidth" &&
                                method.parameterCount == 0 &&
                                method.returnType == Int::class.javaPrimitiveType
                        }
                        .singleOrNull()
                        ?.apply { isAccessible = true }
                        ?: error("get-island-width-contract-missing")
                module
                    .hook(getIslandWidth)
                    .setId(FAKE_ISLAND_WIDTH_HOOK_ID)
                    .intercept(fakeIslandWidthHooker())
            }.onFailure { error ->
                eventSink?.invoke(
                    "controlCenterIslandWidthGate unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=native",
                )
            }.getOrNull()
        return InstallResult.Installed
    }

    @Synchronized
    fun activate(host: Any): StateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return StateResult.Failure("main-thread-required")
        }
        if (installedHookCount != 3) {
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
        val batteryCarrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return StateResult.Failure("battery-core-carrier-missing")
        val field =
            ignoredSlotsField
                ?: return StateResult.Failure("ignored-slots-field-unavailable")
        val hideField =
            batteryHideField
                ?: return StateResult.Failure("battery-hide-field-unavailable")
        val baseSlotWidthPx =
            SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(batteryCarrier)
                ?: return StateResult.Failure("battery-core-width-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(statusIcons) as? MutableList<String> }.getOrNull()
                ?: return StateResult.Failure("ignored-slots-list-unavailable")
        list.size

        val existing = current
        if (existing?.matches(hostView, statusIcons, batteryContainer, battery, batteryCarrier) == true) {
            existing.syncEndReservation()
            val masked = existing.refreshClipMasks()
            batteryContainer.requestLayout()
            return StateResult.Active(representedSlots.size, masked, true)
        }

        existing?.stop("host-replaced")
        val session =
            Session(
                host = hostView,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = null,
                setIgnoredSlotsMethod = null,
                ignoredSlotLifetime = IgnoredSlotLifetime.NATIVE_CALL,
                batteryHideField = hideField,
                surfaceName = "home",
                eventPrefix = "homePresentation",
                retainReservationOnTransientLiveWidthLoss = false,
                onEvent = { event -> eventSink?.invoke(event) },
                onFailNative = ::onSessionFailure,
            )
        current = session
        val masked = session.start()
        batteryContainer.requestLayout()
        eventSink?.invoke(
            "homePresentation active carrier=MiuiStatusBatteryContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + masked +
                " slotExclusion=scoped-native-measure-layout " +
                    "carrierReservation=status-icons-end-padding " +
                    "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "nativeLayoutReservationWrites=1 nativeTranslationWrites=0 " +
                    "nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Active(representedSlots.size, masked, false)
    }

    @Synchronized
    fun activateKeyguard(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        onEvent: (String) -> Unit,
        onFailNative: (String) -> Unit,
        onReady: (StateResult.Active) -> Unit,
    ): StateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return StateResult.Failure("main-thread-required")
        }
        if (installedHookCount != 3) {
            return StateResult.Failure("hooks-not-ready")
        }

        val field =
            ignoredSlotsField
                ?: return StateResult.Failure("ignored-slots-field-unavailable")
        val addMethod =
            addIgnoredSlotsMethod
                ?: return StateResult.Failure("add-ignored-slots-method-unavailable")
        val setMethod =
            setIgnoredSlotsMethod
                ?: return StateResult.Failure("set-ignored-slots-method-unavailable")
        val hideField =
            batteryHideField
                ?: return StateResult.Failure("battery-hide-field-unavailable")
        SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(resolved.batteryCarrier)
            ?: return StateResult.Failure("keyguard-battery-core-width-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(resolved.statusIcons) as? MutableList<String> }.getOrNull()
                ?: return StateResult.Failure("keyguard-ignored-slots-list-unavailable")
        list.size

        keyguardEventSink = onEvent
        keyguardFailNativeSink = onFailNative
        keyguardReadySink = onReady

        val existing = keyguardCurrent
        if (
            existing?.matches(
                host = resolved.host,
                statusIcons = resolved.statusIcons,
                batteryContainer = resolved.systemIcons,
                battery = resolved.battery,
                batteryCarrier = resolved.batteryCarrier,
            ) == true
        ) {
            val masked =
                existing.start(
                    deferVisualMaskUntilLayout = true,
                    onLayoutReady = { maskedViews ->
                        onKeyguardSessionLayoutReady(
                            session = existing,
                            maskedViews = maskedViews,
                            reused = true,
                        )
                    },
                )
            return if (existing.isLayoutCutoverReady()) {
                StateResult.Active(representedSlots.size, masked, true)
            } else {
                StateResult.Prepared(representedSlots.size, true)
            }
        }

        existing?.stop("keyguard-host-replaced")
        val session =
            Session(
                host = resolved.host,
                statusIcons = resolved.statusIcons,
                batteryContainer = resolved.systemIcons,
                battery = resolved.battery,
                batteryCarrier = resolved.batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = addMethod,
                setIgnoredSlotsMethod = setMethod,
                ignoredSlotLifetime = IgnoredSlotLifetime.PRESENTATION_SESSION,
                batteryHideField = hideField,
                surfaceName = "keyguard",
                eventPrefix = "keyguardPresentation",
                retainReservationOnTransientLiveWidthLoss = false,
                onEvent = { event -> keyguardEventSink?.invoke(event) },
                onFailNative = ::onKeyguardSessionFailure,
            )
        keyguardCurrent = session
        val masked =
            session.start(
                deferVisualMaskUntilLayout = true,
                onLayoutReady = { maskedViews ->
                    onKeyguardSessionLayoutReady(
                        session = session,
                        maskedViews = maskedViews,
                        reused = false,
                    )
                },
            )
        return if (session.isLayoutCutoverReady()) {
            StateResult.Active(representedSlots.size, masked, false)
        } else {
            StateResult.Prepared(representedSlots.size, false)
        }
    }

    @Synchronized
    fun deactivateKeyguard(source: String): StateResult {
        val session = keyguardCurrent ?: return StateResult.Inactive(0)
        keyguardCurrent = null
        val restored = session.stop(source)
        keyguardEventSink?.invoke(
            "keyguardPresentation inactive source=" + source +
                " restoredViews=" + restored +
                " nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        keyguardEventSink = null
        keyguardFailNativeSink = null
        keyguardReadySink = null
        return StateResult.Inactive(restored)
    }

    @Synchronized
    fun ownsBatteryContainer(candidate: ViewGroup): Boolean =
        current?.ownsBatteryContainer(candidate) == true

    @Synchronized
    fun ownsKeyguardBatteryContainer(candidate: ViewGroup): Boolean =
        keyguardCurrent?.ownsBatteryContainer(candidate) == true

    @Synchronized
    fun activateControlCenter(
        host: ViewGroup,
        statusIcons: ViewGroup,
        batteryContainer: ViewGroup,
        battery: View,
        batteryCarrier: View,
        onEvent: (String) -> Unit,
        onFailNative: (String) -> Unit,
        onReady: (ControlCenterStateResult.Active) -> Unit,
        nativeLayoutAuthority: Boolean = false,
    ): ControlCenterStateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return ControlCenterStateResult.Failure("main-thread-required")
        }
        if (installedHookCount != 3) {
            return ControlCenterStateResult.Failure("hooks-not-ready")
        }
        if (host !== batteryContainer || host.javaClass.name != BATTERY_CONTAINER) {
            return ControlCenterStateResult.Failure("fake-status-bar-area-mismatch")
        }
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER) {
            return ControlCenterStateResult.Failure("status-icon-group-type-mismatch")
        }
        if (battery.javaClass.name != BATTERY_VIEW) {
            return ControlCenterStateResult.Failure("battery-view-type-mismatch")
        }

        val field =
            ignoredSlotsField
                ?: return ControlCenterStateResult.Failure("ignored-slots-field-unavailable")
        val addMethod =
            addIgnoredSlotsMethod
                ?: return ControlCenterStateResult.Failure("add-ignored-slots-method-unavailable")
        val setMethod =
            setIgnoredSlotsMethod
                ?: return ControlCenterStateResult.Failure("set-ignored-slots-method-unavailable")
        val hideField =
            batteryHideField
                ?: return ControlCenterStateResult.Failure("battery-hide-field-unavailable")
        SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(batteryCarrier)
            ?: return ControlCenterStateResult.Failure("battery-core-width-unavailable")

        @Suppress("UNCHECKED_CAST")
        val list =
            runCatching { field.get(statusIcons) as? MutableList<String> }.getOrNull()
                ?: return ControlCenterStateResult.Failure("ignored-slots-list-unavailable")
        list.size

        controlCenterEventSink = onEvent
        controlCenterFailNativeSink = onFailNative
        controlCenterReadySink = onReady

        val existing = controlCenterCurrent
        if (
            existing?.matches(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
            ) == true &&
            existing.usesNativeLayoutAuthority() == nativeLayoutAuthority
        ) {
            val masked =
                existing.start(
                    deferVisualMaskUntilLayout = true,
                    onLayoutReady = { maskedViews ->
                        onControlCenterSessionLayoutReady(
                            session = existing,
                            maskedViews = maskedViews,
                            reused = true,
                        )
                    },
                )
            return if (existing.isLayoutCutoverReady()) {
                ControlCenterStateResult.Active(
                    representedSlots = representedSlots.size,
                    maskedViews = masked,
                    reused = true,
                )
            } else {
                ControlCenterStateResult.Prepared(
                    representedSlots = representedSlots.size,
                    reused = true,
                )
            }
        }

        existing?.stop("host-replaced")
        val session =
            Session(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
                ignoredSlotsField = field,
                addIgnoredSlotsMethod = addMethod,
                setIgnoredSlotsMethod = setMethod,
                ignoredSlotLifetime = IgnoredSlotLifetime.PRESENTATION_SESSION,
                batteryHideField = hideField,
                surfaceName = "control-center-fake",
                eventPrefix = "controlCenterPresentation",
                retainReservationOnTransientLiveWidthLoss = true,
                nativeLayoutAuthority = nativeLayoutAuthority,
                isProbeEnabled = { contractProbeEnabled() },
                onEvent = { event -> controlCenterEventSink?.invoke(event) },
                onFailNative = ::onControlCenterSessionFailure,
            )
        controlCenterCurrent = session
        val masked =
            session.start(
                deferVisualMaskUntilLayout = true,
                onLayoutReady = { maskedViews ->
                    onControlCenterSessionLayoutReady(
                        session = session,
                        maskedViews = maskedViews,
                        reused = false,
                    )
                },
            )
        return if (session.isLayoutCutoverReady()) {
            ControlCenterStateResult.Active(
                representedSlots = representedSlots.size,
                maskedViews = masked,
                reused = false,
            )
        } else {
            ControlCenterStateResult.Prepared(
                representedSlots = representedSlots.size,
                reused = false,
            )
        }
    }

    @Synchronized
    fun adoptControlCenterLayoutCutoverFromHotReload(): ControlCenterStateResult {
        val session =
            controlCenterCurrent
                ?: return ControlCenterStateResult.Inactive(0)
        val masked =
            session.adoptTransferredCompactLayout()
                ?: return ControlCenterStateResult.Failure(
                    "transferred-compact-layout-adoption-failed",
                )
        return ControlCenterStateResult.Active(
            representedSlots = representedSlots.size,
            maskedViews = masked,
            reused = true,
        )
    }

    @Synchronized
    fun deactivateControlCenter(source: String): ControlCenterStateResult {
        val session =
            controlCenterCurrent
                ?: return ControlCenterStateResult.Inactive(0)
        controlCenterCurrent = null
        val restored = session.stop(source)
        controlCenterEventSink?.invoke(
            "controlCenterPresentation inactive source=" + source +
                " restoredViews=" + restored +
                " nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        return ControlCenterStateResult.Inactive(restored)
    }

    @Synchronized
    fun updateControlCenterTransitionReservation(
        requestedSlotWidthPx: Int,
    ): Boolean =
        controlCenterCurrent
            ?.updateTransitionReservation(requestedSlotWidthPx)
            ?: false

    @Synchronized
    fun clearControlCenterTransitionReservation(source: String): Boolean =
        controlCenterCurrent
            ?.clearTransitionReservation(source)
            ?: true

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
    fun releaseGenerationForHotReload(
        requestLayout: Boolean = true,
    ): Int {
        val controlCenterRestored =
            controlCenterCurrent?.let { session ->
                controlCenterCurrent = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
        val homeRestored =
            current?.let { session ->
                current = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
        val keyguardRestored =
            keyguardCurrent?.let { session ->
                keyguardCurrent = null
                session.stop(
                    source = "hotReload-oldGeneration",
                    requestLayout = requestLayout,
                )
            } ?: 0
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        eventSink = null
        failNativeSink = null
        keyguardEventSink = null
        keyguardFailNativeSink = null
        keyguardReadySink = null
        return homeRestored + keyguardRestored + controlCenterRestored
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivateControlCenter(source)
        deactivateKeyguard(source)
        deactivate(source)
        runCatching { measureHook?.unhook() }
        runCatching { layoutHook?.unhook() }
        runCatching { batteryHideHook?.unhook() }
        runCatching { fakeIslandWidthHook?.unhook() }
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
                    controlCenterCurrent?.takeIf { candidate -> candidate.owns(target) }
                        ?: current?.takeIf { candidate -> candidate.owns(target) }
                        ?: keyguardCurrent?.takeIf { candidate -> candidate.owns(target) }
                } ?: return@Hooker chain.proceed()

            val result = session.withRepresentedSlotsIgnored { chain.proceed() }
            if (refreshMasksAfter) {
                session.captureNativePeerVerticalBand()
                session.accumulateIslandAvoidedSlotsAfterNativeLayout()
            }
            if (
                refreshMasksAfter &&
                session.validateNativeLayoutBeforeVisualMask()
            ) {
                val masked = session.refreshClipMasks()
                session.onNativeLayoutCompleted(masked)
            }
            if (refreshMasksAfter) {
                session.reconcileIslandGestureAfterNativeLayout()
            }
            result
        }

    private fun fakeIslandWidthHooker(): Hooker =
        Hooker { chain ->
            val nativeResult = chain.proceed()
            val nativeWidth =
                nativeResult as? Int
                    ?: return@Hooker nativeResult
            if (nativeWidth <= 0) {
                return@Hooker nativeResult
            }
            val monitor = chain.thisObject
            val session =
                synchronized(this) {
                    controlCenterCurrent
                        ?.takeIf { candidate -> candidate.ownsIslandMonitor(monitor) }
                } ?: return@Hooker nativeResult
            session.resolve2DIslandWidth(nativeWidth)
        }

    private fun batteryHideStateHooker(): Hooker =
        Hooker { chain ->
            val target = chain.thisObject as? ViewGroup
                ?: return@Hooker chain.proceed()
            val result = chain.proceed()
            val session =
                synchronized(this) {
                    controlCenterCurrent
                        ?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                        ?: current?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                        ?: keyguardCurrent
                            ?.takeIf { candidate -> candidate.ownsBatteryContainer(target) }
                }
            session?.syncEndReservation()
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

    @Synchronized
    private fun onKeyguardSessionFailure(reason: String) {
        val session = keyguardCurrent ?: return
        keyguardCurrent = null
        session.stop("fail-native:" + reason)
        keyguardEventSink?.invoke(
            "keyguardPresentation failNative reason=" + reason +
                " restoredNative=true",
        )
        keyguardFailNativeSink?.invoke(reason)
        keyguardEventSink = null
        keyguardFailNativeSink = null
        keyguardReadySink = null
    }

    @Synchronized
    private fun onKeyguardSessionLayoutReady(
        session: Session,
        maskedViews: Int,
        reused: Boolean,
    ) {
        if (keyguardCurrent !== session) {
            return
        }
        val active =
            StateResult.Active(
                representedSlots = representedSlots.size,
                maskedViews = maskedViews,
                reused = reused,
            )
        keyguardEventSink?.invoke(
            "keyguardPresentation active carrier=MiuiStatusBatteryContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + maskedViews +
                " slotExclusion=session-native-ignored-slots " +
                "carrierReservation=status-icons-end-padding " +
                "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "motion=keyguard-system-icons-inherited cutover=compact-layout-ready " +
                "nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        keyguardReadySink?.invoke(active)
    }

    @Synchronized
    private fun onControlCenterSessionLayoutReady(
        session: Session,
        maskedViews: Int,
        reused: Boolean,
    ) {
        if (controlCenterCurrent !== session) {
            return
        }
        val active =
            ControlCenterStateResult.Active(
                representedSlots = representedSlots.size,
                maskedViews = maskedViews,
                reused = reused,
            )
        val nativeLayoutAuthority = session.usesNativeLayoutAuthority()
        val slotExclusion = "session-native-ignored-slots"
        val carrierReservation =
            if (nativeLayoutAuthority) {
                "qs-fake-island-progress-padding"
            } else {
                "qs-fake-capacity-lease+status-icons-end-padding"
            }
        controlCenterEventSink?.invoke(
            "controlCenterPresentation active carrier=QS_FAKE.system_icon_area " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + maskedViews +
                " slotExclusion=" + slotExclusion +
                " carrierReservation=" + carrierReservation +
                " carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "cutover=layout-ready nativeLayoutReservationOwner=single " +
                "nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        controlCenterReadySink?.invoke(active)
    }

    @Synchronized
    private fun onControlCenterSessionFailure(reason: String) {
        val session = controlCenterCurrent ?: return
        controlCenterCurrent = null
        session.stop("fail-native:" + reason)
        controlCenterEventSink?.invoke(
            "controlCenterPresentation failNative reason=" + reason +
                " restoredNative=true",
        )
        controlCenterFailNativeSink?.invoke(reason)
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
    }

    private fun clearInstallState() {
        measureHook = null
        layoutHook = null
        batteryHideHook = null
        fakeIslandWidthHook = null
        ignoredSlotsField = null
        addIgnoredSlotsMethod = null
        setIgnoredSlotsMethod = null
        batteryHideField = null
        controlCenterCurrent = null
        keyguardCurrent = null
        controlCenterEventSink = null
        controlCenterFailNativeSink = null
        controlCenterReadySink = null
        eventSink = null
        failNativeSink = null
        keyguardEventSink = null
        keyguardFailNativeSink = null
        keyguardReadySink = null
        contractProbeEnabled = { false }
        controlCenterIslandContractProbeSummary = null
    }

    private class Session(
        host: ViewGroup,
        statusIcons: ViewGroup,
        batteryContainer: ViewGroup,
        battery: View,
        batteryCarrier: View,
        private val ignoredSlotsField: Field,
        private val addIgnoredSlotsMethod: java.lang.reflect.Method?,
        private val setIgnoredSlotsMethod: java.lang.reflect.Method?,
        private val ignoredSlotLifetime: IgnoredSlotLifetime,
        private val batteryHideField: Field,
        private val surfaceName: String,
        private val eventPrefix: String,
        private val retainReservationOnTransientLiveWidthLoss: Boolean,
        private val nativeLayoutAuthority: Boolean = false,
        private val isProbeEnabled: () -> Boolean = { false },
        private val onEvent: (String) -> Unit,
        private val onFailNative: (String) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusIcons = WeakReference(statusIcons)
        private val batteryContainer = WeakReference(batteryContainer)
        private val battery = WeakReference(battery)
        private val batteryCarrier = WeakReference(batteryCarrier)
        private var active = true
        private var started = false
        private var deferVisualMaskUntilLayout = false
        private var compactLayoutReady = false
        private var layoutReadyCallback: ((Int) -> Unit)? = null
        private var lastReservationDelta: Int? = null
        private var nativePadding: PaddingState? = null
        private var appliedPadding: PaddingState? = null
        private var nativeFakeCarrierLayoutWidthPx: Int? = null
        private var nativeFakeCarrierParentContentWidthPx: Int? = null
        private var appliedFakeCarrierWidthPx: Int? = null
        private var fakeCarrierCapacityDeltaPx: Int? = null
        private var fakeCarrierCapacityBaselineReservationPx: Int? = null
        private var fakeCarrierCapacityLeaseAwaitingLayout = false
        private var transientLiveBatteryWidthUnavailable = false
        private var persistentIgnoredSlotsApplied = false
        private var ownedPersistentIgnoredSlots: List<String> = emptyList()
        private var transitionRequestedSlotWidthPx: Int? = null
        private var islandContractProbeReported = false
        private var islandMonitorRef = WeakReference<Any>(null)
        private var peerBandTopInsetPx: Int? = null
        private var peerBandBottomInsetPx: Int? = null
        private val islandRectBuffer = Rect()
        private val screenLocationBuffer = IntArray(2)
        private var lastIsland2DOverlap: Boolean? = null
        private var island2DGeometryUnavailableReported = false
        private var islandPeerLatchRequested = false
        private var islandReturnOverlapRequested = false
        private var islandReturnBaselineLayoutPending = false
        private var islandReturnExpectedCarrierWidthPx: Int? = null
        private val accumulatedIslandAvoidedSlots = linkedSetOf<String>()
        private val latchedIslandAvoidedSlots = linkedSetOf<String>()
        private val islandPeerClipStates = mutableListOf<ClipState>()
        private val clipStates = mutableListOf<ClipState>()
        private val batteryLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    left,
                    _,
                    right,
                    _,
                    oldLeft,
                    _,
                    oldRight,
                    _,
                ->
                if (right - left != oldRight - oldLeft) {
                    syncEndReservation()
                }
            }

        private val carrierLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    left,
                    _,
                    right,
                    _,
                    oldLeft,
                    _,
                    oldRight,
                    _,
                ->
                if (right - left != oldRight - oldLeft) {
                    syncEndReservation()
                }
            }

        fun matches(
            host: ViewGroup,
            statusIcons: ViewGroup,
            batteryContainer: ViewGroup,
            battery: View,
            batteryCarrier: View,
        ): Boolean =
            active &&
                this.host.get() === host &&
                this.statusIcons.get() === statusIcons &&
                this.batteryContainer.get() === batteryContainer &&
                this.battery.get() === battery &&
                this.batteryCarrier.get() === batteryCarrier

        fun owns(candidate: ViewGroup): Boolean =
            active && statusIcons.get() === candidate

        fun ownsBatteryContainer(candidate: ViewGroup): Boolean =
            active && batteryContainer.get() === candidate

        fun usesNativeLayoutAuthority(): Boolean = nativeLayoutAuthority

        fun ownsIslandMonitor(candidate: Any): Boolean =
            active && islandMonitorRef.get() === candidate

        private fun captureControlCenterIslandMonitor(group: ViewGroup) {
            if (
                !active ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                islandMonitorRef.get() != null
            ) {
                return
            }
            val field =
                generateSequence<Class<*>>(group.javaClass) { owner -> owner.superclass }
                    .takeWhile { owner -> owner != Any::class.java }
                    .mapNotNull { owner ->
                        runCatching {
                            owner.getDeclaredField("_islandMonitor").apply {
                                isAccessible = true
                            }
                        }.getOrNull()
                    }
                    .firstOrNull()
                    ?: run {
                        reportIsland2DGeometryUnavailableOnce("fake-island-monitor-field-missing")
                        return
                    }
            val monitor =
                runCatching { field.get(group) }.getOrNull()
                    ?.takeIf { value -> value.javaClass.name == FAKE_ISLAND_MONITOR_CLASS }
                    ?: run {
                        reportIsland2DGeometryUnavailableOnce("fake-island-monitor-unavailable")
                        return
                    }
            islandMonitorRef = WeakReference(monitor)
            onEvent(
                eventPrefix +
                    " islandWidth2DGate monitorBound=true" +
                    " monitor=" + monitor.javaClass.simpleName +
                    " fallback=native-on-geometry-loss nativeGeometryWrites=0",
            )
        }

        fun captureNativePeerVerticalBand() {
            if (
                !active ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE
            ) {
                return
            }
            val group = statusIcons.get() ?: return
            var top = Int.MAX_VALUE
            var bottom = Int.MIN_VALUE
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (
                    child.visibility != View.VISIBLE ||
                    child.width <= 0 ||
                    child.height <= 0
                ) {
                    continue
                }
                top = minOf(top, child.top)
                bottom = maxOf(bottom, child.bottom)
            }
            if (top == Int.MAX_VALUE || bottom <= top) {
                return
            }
            peerBandTopInsetPx = top
            peerBandBottomInsetPx = bottom
        }

        fun accumulateIslandAvoidedSlotsAfterNativeLayout() {
            if (
                !active ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                !nativeLayoutAuthority ||
                lastIsland2DOverlap != true
            ) {
                return
            }
            val group = statusIcons.get() ?: return
            val added = mutableListOf<String>()
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                val slot = NativeParticipantRuntimeAccess.slotOf(child) ?: continue
                if (
                    slot in representedSlots ||
                    child.visibility != View.VISIBLE ||
                    child.width <= 0 ||
                    child.height <= 0
                ) {
                    continue
                }
                val state =
                    SystemUiNativeNetworkSuppressionOwner
                        .readTransitionIconState(group, child)
                        ?: continue
                if (
                    !ControlCenterIslandGesturePolicy.shouldLatchPeer(
                        slot = slot,
                        representedSlots = representedSlots,
                        visible = true,
                        width = child.width,
                        height = child.height,
                        inIslandState = state.inIslandState,
                        beforeInIslandState = state.beforeInIslandState,
                    )
                ) {
                    continue
                }
                if (accumulatedIslandAvoidedSlots.add(slot)) {
                    added += slot
                }
            }
            if (added.isNotEmpty()) {
                onEvent(
                    eventPrefix +
                        " islandPeerLatch accumulate=" + added.joinToString(",") +
                        " total=" + accumulatedIslandAvoidedSlots.joinToString(",") +
                        " source=native-overlap-layouts peerStateWrites=0",
                )
            }
        }

        fun resolve2DIslandWidth(nativeWidth: Int): Int {
            if (
                nativeWidth <= 0 ||
                !active ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                !nativeLayoutAuthority
            ) {
                return nativeWidth
            }
            val group =
                statusIcons.get()
                    ?: run {
                        reportIsland2DGeometryUnavailableOnce("fake-status-icons-released")
                        return nativeWidth
                    }
            if (
                !group.isAttachedToWindow ||
                group.width <= 0 ||
                group.height <= 0
            ) {
                reportIsland2DGeometryUnavailableOnce("fake-not-laid-out")
                return nativeWidth
            }
            if (!SystemUiIslandMotionSource.copyCurrentIslandRect(islandRectBuffer)) {
                reportIsland2DGeometryUnavailableOnce("live-island-rect-unavailable")
                return nativeWidth
            }

            group.getLocationOnScreen(screenLocationBuffer)
            val fakeLeft = screenLocationBuffer[0]
            val fakeTopBase = screenLocationBuffer[1]
            val fakeRight = fakeLeft + group.width
            val topInset = peerBandTopInsetPx ?: 0
            val bottomInset = peerBandBottomInsetPx ?: group.height
            val fakeTop = fakeTopBase + topInset
            val fakeBottom = fakeTopBase + bottomInset
            val overlap =
                ControlCenterIslandOverlapPolicy.intersects(
                    left = fakeLeft,
                    top = fakeTop,
                    right = fakeRight,
                    bottom = fakeBottom,
                    islandLeft = islandRectBuffer.left,
                    islandTop = islandRectBuffer.top,
                    islandRight = islandRectBuffer.right,
                    islandBottom = islandRectBuffer.bottom,
                )
            val exposedWidth = if (overlap) nativeWidth else 0
            val previousOverlap = lastIsland2DOverlap
            if (previousOverlap != overlap) {
                if (previousOverlap == true && !overlap) {
                    latchedIslandAvoidedSlots.clear()
                    latchedIslandAvoidedSlots.addAll(accumulatedIslandAvoidedSlots)
                    islandPeerLatchRequested = true
                    islandReturnOverlapRequested = false
                    islandReturnBaselineLayoutPending = false
                    islandReturnExpectedCarrierWidthPx = null
                    onEvent(
                        eventPrefix +
                            " islandPeerLatch snapshot=" +
                            latchedIslandAvoidedSlots.joinToString(",").ifEmpty { "none" } +
                            " source=accumulated-native-overlap-layouts peerStateWrites=0",
                    )
                } else if (previousOverlap == false && overlap) {
                    accumulatedIslandAvoidedSlots.clear()
                    islandReturnOverlapRequested = true
                    islandPeerLatchRequested = false
                }
                lastIsland2DOverlap = overlap
                onEvent(
                    eventPrefix +
                        " islandWidth2DGate nativeWidth=" + nativeWidth +
                        " overlap=" + overlap +
                        " fake=(" + fakeLeft + "," + fakeTop + "," +
                        fakeRight + "," + fakeBottom + ")" +
                        " island=(" + islandRectBuffer.left + "," +
                        islandRectBuffer.top + "," +
                        islandRectBuffer.right + "," +
                        islandRectBuffer.bottom + ")" +
                        " exposedWidth=" + exposedWidth +
                        " authority=fake-container-monitor+live-island-rect" +
                        " nativeGeometryWrites=0",
                )
            }
            return exposedWidth
        }

        private fun reportIsland2DGeometryUnavailableOnce(reason: String) {
            if (island2DGeometryUnavailableReported) return
            island2DGeometryUnavailableReported = true
            onEvent(
                eventPrefix +
                    " islandWidth2DGate fallback=native reason=" + reason +
                    " nativeGeometryWrites=0",
            )
        }

        private fun reportIslandContractOnce(group: ViewGroup) {
            if (
                islandContractProbeReported ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                !isProbeEnabled()
            ) {
                return
            }
            islandContractProbeReported = true

            fun relevantToken(value: String): Boolean {
                val token = value.lowercase()
                return token.contains("island") ||
                    token.contains("monitor") ||
                    token.contains("space") ||
                    token.contains("delegate") ||
                    token.contains("controller")
            }

            fun simpleValue(value: Any?): String =
                when (value) {
                    null -> "null"
                    is Number,
                    is Boolean,
                    is CharSequence,
                    is Enum<*>,
                    -> value.toString()
                    else -> value.javaClass.name
                }

            fun traversable(value: Any): Boolean =
                value !is Number &&
                    value !is Boolean &&
                    value !is CharSequence &&
                    value !is Enum<*> &&
                    value !is Class<*>

            val queue = ArrayDeque<Triple<String, Any, Int>>()
            val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
            var view: View? = group
            var viewIndex = 0
            while (view != null && viewIndex < 5) {
                queue.add(Triple("view[" + viewIndex + "]", view, 0))
                view = view.parent as? View
                viewIndex += 1
            }

            var emitted = 0
            val persistedTokens = mutableListOf<String>()
            while (queue.isNotEmpty() && emitted < 12 && seen.size < 16) {
                val (path, target, depth) = queue.removeFirst()
                if (!seen.add(target)) continue

                val fieldTokens = mutableListOf<String>()
                val methodTokens = mutableListOf<String>()
                generateSequence<Class<*>>(target.javaClass) { owner -> owner.superclass }
                    .takeWhile { owner -> owner != Any::class.java }
                    .forEach { owner ->
                        owner.declaredFields.forEach { field ->
                            if (Modifier.isStatic(field.modifiers)) return@forEach
                            val fieldRelevant =
                                relevantToken(field.name) || relevantToken(field.type.name)
                            if (!fieldRelevant) return@forEach
                            val value =
                                runCatching {
                                    field.isAccessible = true
                                    field.get(target)
                                }.getOrNull()
                            if (fieldTokens.size < 24) {
                                fieldTokens +=
                                    owner.simpleName + "." + field.name + ":" +
                                        field.type.simpleName + "=" + simpleValue(value)
                            }
                            if (
                                depth < 2 &&
                                value != null &&
                                traversable(value) &&
                                seen.size + queue.size < 16
                            ) {
                                queue.add(
                                    Triple(
                                        path + "." + field.name,
                                        value,
                                        depth + 1,
                                    ),
                                )
                            }
                        }
                        owner.declaredMethods.forEach { method ->
                            val methodRelevant =
                                relevantToken(method.name) ||
                                    method.name == "updateContainerSize"
                            if (!methodRelevant || methodTokens.size >= 24) return@forEach
                            methodTokens +=
                                owner.simpleName + "." + method.name + "(" +
                                    method.parameterTypes.joinToString(",") { type ->
                                        type.simpleName
                                    } + "):" + method.returnType.simpleName
                        }
                    }

                val contractToken =
                    path + ":" + target.javaClass.name +
                        "{f=" + fieldTokens.joinToString(";") +
                        ",m=" + methodTokens.joinToString(";") + "}"
                if (
                    persistedTokens.size < 8 &&
                    (fieldTokens.isNotEmpty() || methodTokens.isNotEmpty())
                ) {
                    persistedTokens += contractToken
                }
                onEvent(
                    eventPrefix +
                        " islandContractProbe path=" + path +
                        " class=" + target.javaClass.name +
                        " fields=[" + fieldTokens.joinToString(";") + "]" +
                        " methods=[" + methodTokens.joinToString(";") + "]" +
                        " readOnly=true nativeGeometryWrites=0",
                )
                emitted += 1
            }
            SystemUiHomePresentationOwner
                .publishControlCenterIslandContractProbeSummary(
                    persistedTokens
                        .joinToString("|")
                        .ifEmpty { "empty" },
                )
        }

        fun ownedRepresentedSlots(): Set<String> {
            if (!active || !compactLayoutReady) return emptySet()
            return clipStates
                .mapNotNull { state ->
                    state.view.get()
                        ?.let(NativeParticipantRuntimeAccess::slotOf)
                        ?.takeIf(representedSlots::contains)
                }
                .toSet()
        }

        fun start(
            deferVisualMaskUntilLayout: Boolean = false,
            onLayoutReady: ((Int) -> Unit)? = null,
        ): Int {
            this.deferVisualMaskUntilLayout = deferVisualMaskUntilLayout
            this.layoutReadyCallback = onLayoutReady
            if (started) {
                syncEndReservation()
                return if (isLayoutCutoverReady()) refreshClipMasks() else 0
            }

            started = true
            host.get()?.addOnAttachStateChangeListener(this)
            val group =
                statusIcons.get()
                    ?: run {
                        onFailNative("status-icon-group-released")
                        return 0
                    }
            nativePadding = PaddingState.from(group)
            reportIslandContractOnce(group)
            captureControlCenterIslandMonitor(group)
            captureNativePeerVerticalBand()
            battery.get()?.addOnLayoutChangeListener(batteryLayoutListener)
            batteryCarrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
            if (!applyPersistentIgnoredSlotsIfNeeded(group)) return 0
            if (!syncEndReservation()) return 0

            if (
                VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                    deferVisualMaskUntilLayout = deferVisualMaskUntilLayout,
                    laidOut = group.isLaidOut,
                    layoutRequested = group.isLayoutRequested,
                    capacityLeaseAwaitingLayout =
                        fakeCarrierCapacityLeaseAwaitingLayout,
                    width = group.width,
                    height = group.height,
                )
            ) {
                val masked = refreshClipMasks()
                compactLayoutReady = true
                layoutReadyCallback = null
                onEvent(
                    eventPrefix + " layoutReady source=existing-native-status-icons-layout" +
                        " maskedViews=" + masked +
                        " compactLayoutReady=true",
                )
                return masked
            }

            if (
                VisualMaskPolicy.shouldPreserveNativeBeforeCompactCutover(
                    deferVisualMaskUntilLayout,
                )
            ) {
                compactLayoutReady = false
                onEvent(
                    eventPrefix + " preLayoutVisualMask active=false" +
                        " maskedViews=0" +
                        " compactLayoutReady=false" +
                        " fallbackVisual=native-until-native-layout",
                )
                return 0
            }

            compactLayoutReady = true
            return refreshClipMasks()
        }

        fun stop(
            source: String,
            requestLayout: Boolean = true,
        ): Int {
            if (
                !active &&
                clipStates.isEmpty() &&
                islandPeerClipStates.isEmpty() &&
                appliedPadding == null &&
                appliedFakeCarrierWidthPx == null
            ) {
                return 0
            }
            active = false
            layoutReadyCallback = null
            compactLayoutReady = false
            deferVisualMaskUntilLayout = false
            host.get()?.removeOnAttachStateChangeListener(this)
            battery.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            val reservationRestored = restoreEndReservation()
            val ignoredSlotsRestored =
                restorePersistentIgnoredSlots(
                    requestLayout = requestLayout,
                )
            val restored = restoreClipMasks()
            val restoredIslandPeerLatches = restoreIslandPeerLatchMasks()
            accumulatedIslandAvoidedSlots.clear()
            latchedIslandAvoidedSlots.clear()
            islandPeerLatchRequested = false
            islandReturnOverlapRequested = false
            islandReturnBaselineLayoutPending = false
            islandReturnExpectedCarrierWidthPx = null
            val explicitLayoutRequest =
                requestLayout && ignoredSlotLifetime == IgnoredSlotLifetime.NATIVE_CALL
            if (explicitLayoutRequest) {
                batteryContainer.get()?.requestLayout()
            }
            onEvent(
                eventPrefix + " cleanup source=" + source +
                    " restoredClipBounds=" + restored +
                    " restoredIslandPeerClips=" + restoredIslandPeerLatches +
                    " restoredEndReservation=" + reservationRestored +
                    " restoredIgnoredSlots=" + ignoredSlotsRestored +
                    " requestLayout=" + explicitLayoutRequest,
            )
            return restored + restoredIslandPeerLatches
        }

        fun <T> withRepresentedSlotsIgnored(block: () -> T): T {
            if (!active || ignoredSlotLifetime == IgnoredSlotLifetime.PRESENTATION_SESSION) {
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

        private fun applyPersistentIgnoredSlotsIfNeeded(group: ViewGroup): Boolean {
            if (
                ignoredSlotLifetime != IgnoredSlotLifetime.PRESENTATION_SESSION ||
                persistentIgnoredSlotsApplied
            ) {
                return true
            }
            val addMethod =
                addIgnoredSlotsMethod
                    ?: run {
                        onFailNative("add-ignored-slots-method-unavailable")
                        return false
                    }
            @Suppress("UNCHECKED_CAST")
            val live =
                runCatching {
                    ignoredSlotsField.get(group) as? MutableList<String>
                }.getOrNull()
                    ?: run {
                        onFailNative("ignored-slots-list-unavailable")
                        return false
                    }
            val before = live.toList()
            val owned =
                PersistentIgnoredSlotPolicy.ownedDelta(
                    existing = before,
                    requested = representedSlots,
                )
            val applied =
                runCatching {
                    if (owned.isNotEmpty()) {
                        addMethod.invoke(group, ArrayList(owned))
                    }
                    @Suppress("UNCHECKED_CAST")
                    val after =
                        ignoredSlotsField.get(group) as? MutableList<String>
                            ?: error("ignored-slots-list-unavailable-after-add")
                    check(representedSlots.all(after::contains)) {
                        "ignored-slots-native-api-did-not-retain-represented-slots"
                    }
                }
            if (applied.isFailure) {
                val rollback =
                    setIgnoredSlotsMethod?.let { setMethod ->
                        runCatching {
                            setMethod.invoke(group, ArrayList(before))
                        }.isSuccess
                    } ?: false
                onFailNative(
                    "ignored-slots-session-add-" +
                        (
                            applied.exceptionOrNull()?.message
                                ?: applied.exceptionOrNull()?.javaClass?.simpleName
                                ?: "unknown"
                        ) +
                        "-rollback=" + rollback,
                )
                return false
            }
            ownedPersistentIgnoredSlots = owned
            persistentIgnoredSlotsApplied = true
            onEvent(
                eventPrefix + " ignoredSlots active lifetime=presentation-session" +
                    " owned=" + owned.joinToString(",") +
                    " represented=" + representedSlots.joinToString(",") +
                    " nativeApi=addIgnoredSlots",
            )
            return true
        }

        private fun restorePersistentIgnoredSlots(
            requestLayout: Boolean,
        ): Boolean {
            if (
                ignoredSlotLifetime != IgnoredSlotLifetime.PRESENTATION_SESSION ||
                !persistentIgnoredSlotsApplied
            ) {
                return true
            }
            val owned = ownedPersistentIgnoredSlots
            if (owned.isEmpty()) {
                persistentIgnoredSlotsApplied = false
                ownedPersistentIgnoredSlots = emptyList()
                return true
            }
            val group = statusIcons.get() ?: return false
            @Suppress("UNCHECKED_CAST")
            val live =
                runCatching {
                    ignoredSlotsField.get(group) as? MutableList<String>
                }.getOrNull() ?: return false
            val target =
                PersistentIgnoredSlotPolicy.restoreTarget(
                    live = live,
                    ownedEntries = owned,
                )
            val useNativeSetter =
                PersistentIgnoredSlotPolicy.shouldUseNativeSetterOnRestore(
                    requestLayout = requestLayout,
                )
            val setMethod =
                if (useNativeSetter) {
                    setIgnoredSlotsMethod ?: return false
                } else {
                    null
                }
            val restored =
                runCatching {
                    if (target != live) {
                        if (useNativeSetter) {
                            setMethod!!.invoke(group, ArrayList(target))
                        } else {
                            live.clear()
                            live.addAll(target)
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    val after =
                        ignoredSlotsField.get(group) as? MutableList<String>
                            ?: return@runCatching false
                    after == target
                }.getOrDefault(false)
            if (restored) {
                persistentIgnoredSlotsApplied = false
                ownedPersistentIgnoredSlots = emptyList()
                if (!useNativeSetter) {
                    onEvent(
                        eventPrefix +
                            " ignoredSlots restore=handoff-no-layout owned=" +
                            owned.joinToString(",") +
                            " nativeApi=owned-list-delta",
                    )
                }
            } else {
                onEvent(
                    eventPrefix +
                        " ignoredSlots restore=failed owned=" + owned.joinToString(",") +
                        " requestLayout=" + requestLayout,
                )
            }
            return restored
        }

        fun updateTransitionReservation(
            requestedSlotWidthPx: Int,
        ): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return false
            val normalized = requestedSlotWidthPx.coerceAtLeast(0)
            if (transitionRequestedSlotWidthPx == normalized) return true
            transitionRequestedSlotWidthPx = normalized
            return syncEndReservation()
        }

        fun clearTransitionReservation(source: String): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true
            if (transitionRequestedSlotWidthPx == null) return true
            transitionRequestedSlotWidthPx = null
            val restored = syncEndReservation()
            onEvent(
                eventPrefix +
                    " transitionReservation cleared source=" + source +
                    " restoredCompact=" + restored,
            )
            return restored
        }

        fun syncEndReservation(): Boolean {
            if (!active) return true
            val group = statusIcons.get() ?: run { onFailNative("status-icon-group-released"); return false }
            val container = batteryContainer.get() ?: run { onFailNative("battery-container-released"); return false }
            val batteryView = battery.get() ?: run { onFailNative("battery-view-released"); return false }
            val hostView = host.get() ?: run { onFailNative(surfaceName + "-host-released"); return false }
            val baseline = nativePadding ?: PaddingState.from(group).also { nativePadding = it }
            val live = PaddingState.from(group)
            val previousApplied = appliedPadding
            if (live != baseline && live != previousApplied) {
                onFailNative("status-icon-padding-writer-conflict")
                return false
            }
            val nativeHide =
                runCatching { batteryHideField.getBoolean(container) }.getOrNull()
                    ?: run { onFailNative("battery-hide-state-unavailable"); return false }
            val actualBatteryWidthPx =
                (if (batteryView.measuredWidth > 0) batteryView.measuredWidth else batteryView.width)
                    .takeIf { width -> width > 0 }
                    ?: run {
                        if (
                            EndReservationPolicy.shouldDeferLiveBatteryWidthUnavailable(
                                retainOnTransientLoss = retainReservationOnTransientLiveWidthLoss,
                                compactLayoutReady = compactLayoutReady,
                            )
                        ) {
                            if (!transientLiveBatteryWidthUnavailable) {
                                transientLiveBatteryWidthUnavailable = true
                                onEvent(
                                    eventPrefix +
                                        " endReservation deferred reason=battery-live-width-unavailable" +
                                        " compactLayoutReady=true",
                                )
                            }
                            return true
                        }
                        onFailNative("battery-live-width-unavailable")
                        return false
                    }
            if (transientLiveBatteryWidthUnavailable) {
                transientLiveBatteryWidthUnavailable = false
                onEvent(
                    eventPrefix +
                        " endReservation resumed reason=battery-live-width-restored",
                )
            }
            val carrier =
                batteryCarrier.get()
                    ?: run { onFailNative("battery-core-carrier-released"); return false }
            val stableCarrierWidthPx =
                SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(carrier)
                    ?: run { onFailNative("battery-core-width-unavailable"); return false }
            if (actualBatteryWidthPx < stableCarrierWidthPx) {
                onFailNative("battery-presentation-narrower-than-core")
                return false
            }
            val resolved =
                CombinedStatusHomeLayoutResolver.resolve(
                    hostWidthPx = hostView.width,
                    hostHeightPx = hostView.height,
                    baseCarrierWidthPx = stableCarrierWidthPx,
                    isRtl = hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: run { onFailNative(surfaceName + "-layout-unavailable"); return false }
            val compactSlotWidthPx =
                CombinedStatusCompactReservationPolicy.resolveCenteredVisualWidth(
                    baseSlotWidthPx = resolved.requestedSlotWidthPx.toInt(),
                    userScale = RuntimeVisualPreferencesOwner.currentSettings().combinedScale,
                )
            val requestedSlotWidthPx =
                EndReservationPolicy.resolveRequestedSlotWidth(
                    compactSlotWidthPx = compactSlotWidthPx,
                    transitionRequestedSlotWidthPx = transitionRequestedSlotWidthPx,
                )
            val reservationDelta =
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide,
                    actualBatteryWidthPx = actualBatteryWidthPx,
                    requestedSlotWidthPx = requestedSlotWidthPx,
                )
            val capacityLeaseEnabled =
                ControlCenterLayoutPolicy.shouldApplyFakeCarrierCapacityLease(
                    surfaceName = surfaceName,
                    nativeLayoutAuthority = nativeLayoutAuthority,
                    island2DSeparated = lastIsland2DOverlap == false,
                )
            val capacityActivationBaselineReservationPx =
                if (
                    capacityLeaseEnabled &&
                    nativeLayoutAuthority &&
                    lastIsland2DOverlap == false
                ) {
                    reservationDelta.coerceAtLeast(0)
                } else {
                    0
                }
            val capacityDeltaPx =
                if (capacityLeaseEnabled) {
                    ensureFakeCarrierCapacityLease(
                        hostView = hostView,
                        reservationBaselinePx = capacityActivationBaselineReservationPx,
                    ) ?: return false
                } else {
                    0
                }
            val capacityBaselineReservationPx =
                if (capacityLeaseEnabled) {
                    fakeCarrierCapacityBaselineReservationPx ?: 0
                } else {
                    0
                }
            val capacityReservationGrowthPx =
                if (capacityLeaseEnabled) {
                    EndReservationPolicy.resolveCapacityReservationGrowth(
                        currentReservationPx = reservationDelta,
                        baselineReservationPx = capacityBaselineReservationPx,
                    )
                } else {
                    0
                }
            if (
                surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                capacityLeaseEnabled &&
                capacityReservationGrowthPx > capacityDeltaPx
            ) {
                onFailNative("fake-carrier-capacity-insufficient")
                return false
            }
            val target =
                PaddingState(
                    baseline.start,
                    baseline.top,
                    baseline.end + reservationDelta,
                    baseline.bottom,
                )
            if (live != target) {
                group.setPaddingRelative(target.start, target.top, target.end, target.bottom)
            }
            if (PaddingState.from(group) != target) {
                onFailNative("status-icon-end-reservation-apply-failed")
                return false
            }
            appliedPadding = if (target == baseline) null else target
            if (lastReservationDelta != reservationDelta) {
                lastReservationDelta = reservationDelta
                onEvent(
                    eventPrefix + " endReservation nativeHide=" + nativeHide +
                        " stableCarrierWidth=" + stableCarrierWidthPx +
                        " actualBatteryWidth=" + actualBatteryWidthPx +
                        " compactSlotWidth=" + compactSlotWidthPx +
                        " visualScale=" + RuntimeVisualPreferencesOwner.currentSettings().combinedScale +
                        " requestedSlotWidth=" + requestedSlotWidthPx +
                        " transitionRequestedSlotWidth=" +
                        (transitionRequestedSlotWidthPx ?: -1) +
                        " paddingEndDelta=" + reservationDelta +
                        " basePaddingEnd=" + baseline.end +
                        " appliedPaddingEnd=" + target.end +
                        " fakeCarrierWidth=" + (appliedFakeCarrierWidthPx ?: -1) +
                        " fakeCarrierCapacityDelta=" +
                        (if (capacityLeaseEnabled) capacityDeltaPx else -1) +
                        " fakeCarrierCapacityBaselineReservation=" +
                        (if (capacityLeaseEnabled) capacityBaselineReservationPx else -1) +
                        " fakeCarrierCapacityGrowth=" +
                        (if (capacityLeaseEnabled) capacityReservationGrowthPx else -1) +
                        " carrierAuthority=battery_icon_container " +
                        "owner=" +
                        (
                            if (capacityLeaseEnabled) {
                                "qs-fake-capacity-lease+statusIcons-paddingEnd"
                            } else {
                                "qs-fake-island-progress-padding"
                            }
                        ),
                )
            }
            return true
        }

        private fun restoreEndReservation(): Boolean {
            var paddingRestored = appliedPadding == null
            val applied = appliedPadding
            if (applied != null) {
                val group = statusIcons.get()
                val baseline = nativePadding
                if (group == null || baseline == null) {
                    paddingRestored = false
                    appliedPadding = null
                } else {
                    val live = PaddingState.from(group)
                    if (live != applied) {
                        onEvent(
                            eventPrefix + " endReservation restore=skipped reason=writer-changed " +
                                "livePaddingEnd=" + live.end +
                                " appliedPaddingEnd=" + applied.end,
                        )
                        paddingRestored = false
                    } else {
                        group.setPaddingRelative(
                            baseline.start,
                            baseline.top,
                            baseline.end,
                            baseline.bottom,
                        )
                        paddingRestored = PaddingState.from(group) == baseline
                    }
                    appliedPadding = null
                }
            }
            val carrierRestored = restoreFakeCarrierCapacityLease()
            return paddingRestored && carrierRestored
        }

        private fun ensureFakeCarrierCapacityLease(
            hostView: ViewGroup,
            reservationBaselinePx: Int,
        ): Int? {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return 0

            val parent =
                hostView.parent as? ViewGroup
                    ?: run {
                        onFailNative("fake-carrier-parent-unavailable")
                        return null
                    }
            if (parent.childCount != 1 || parent.getChildAt(0) !== hostView) {
                onFailNative("fake-carrier-exclusive-parent-contract-unavailable")
                return null
            }
            val params =
                hostView.layoutParams
                    ?: run {
                        onFailNative("fake-carrier-layout-params-unavailable")
                        return null
                    }
            val margins = params as? ViewGroup.MarginLayoutParams
            if (
                margins != null &&
                (
                    margins.marginStart != 0 ||
                        margins.marginEnd != 0 ||
                        margins.leftMargin != 0 ||
                        margins.rightMargin != 0
                )
            ) {
                onFailNative("fake-carrier-horizontal-margin-contract-unsupported")
                return null
            }

            val parentContentWidthPx =
                (
                    parent.width -
                        parent.paddingLeft -
                        parent.paddingRight
                ).takeIf { width -> width > 0 }
                    ?: run {
                        onFailNative("fake-carrier-parent-width-unavailable")
                        return null
                    }
            if (!isFakeCarrierEndAnchored(hostView, parent)) {
                onFailNative("fake-carrier-end-anchor-unverified")
                return null
            }

            val existingAppliedWidthPx = appliedFakeCarrierWidthPx
            if (existingAppliedWidthPx != null) {
                if (
                    nativeFakeCarrierParentContentWidthPx != parentContentWidthPx ||
                    params.width != existingAppliedWidthPx
                ) {
                    onFailNative("fake-carrier-width-writer-conflict")
                    return null
                }
                return fakeCarrierCapacityDeltaPx
            }

            val baselineWidthPx =
                hostView.width.takeIf { width -> width > 0 }
                    ?: run {
                        onFailNative("fake-carrier-width-unavailable")
                        return null
                    }
            if (params.width <= 0 || params.width != baselineWidthPx) {
                onFailNative("fake-carrier-width-contract-unavailable")
                return null
            }
            val capacityDeltaPx =
                EndReservationPolicy.resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = baselineWidthPx,
                    parentContentWidthPx = parentContentWidthPx,
                ) ?: run {
                    onFailNative("fake-carrier-capacity-unavailable")
                    return null
                }

            nativeFakeCarrierLayoutWidthPx = params.width
            nativeFakeCarrierParentContentWidthPx = parentContentWidthPx
            fakeCarrierCapacityDeltaPx = capacityDeltaPx
            fakeCarrierCapacityBaselineReservationPx =
                reservationBaselinePx.coerceAtLeast(0)

            if (params.width != parentContentWidthPx) {
                params.width = parentContentWidthPx
                hostView.layoutParams = params
                fakeCarrierCapacityLeaseAwaitingLayout = true
            }
            if (hostView.layoutParams?.width != parentContentWidthPx) {
                clearFakeCarrierCapacityLeaseSnapshot()
                onFailNative("fake-carrier-capacity-lease-apply-failed")
                return null
            }
            appliedFakeCarrierWidthPx = parentContentWidthPx
            onEvent(
                eventPrefix +
                    " fakeCarrierCapacity lease=active" +
                    " nativeWidth=" + baselineWidthPx +
                    " leasedWidth=" + parentContentWidthPx +
                    " capacityDelta=" + capacityDeltaPx +
                    " baselineReservation=" +
                    (fakeCarrierCapacityBaselineReservationPx ?: 0) +
                    " owner=control-center-fake-session",
            )
            return capacityDeltaPx
        }

        private fun isFakeCarrierEndAnchored(
            hostView: View,
            parent: ViewGroup,
        ): Boolean {
            val contentLeft = parent.paddingLeft
            val contentRight = parent.width - parent.paddingRight
            if (contentRight <= contentLeft) return false
            return if (hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                hostView.left == contentLeft
            } else {
                hostView.right == contentRight
            }
        }

        private fun restoreFakeCarrierCapacityLease(): Boolean {
            if (surfaceName != CONTROL_CENTER_FAKE_SURFACE) return true

            val appliedWidthPx = appliedFakeCarrierWidthPx
            if (appliedWidthPx == null) {
                clearFakeCarrierCapacityLeaseSnapshot()
                return true
            }
            val hostView = host.get()
            val baselineLayoutWidthPx = nativeFakeCarrierLayoutWidthPx
            if (hostView == null || baselineLayoutWidthPx == null) {
                clearFakeCarrierCapacityLeaseSnapshot()
                return false
            }
            val params = hostView.layoutParams
            if (params == null) {
                clearFakeCarrierCapacityLeaseSnapshot()
                return false
            }
            if (params.width != appliedWidthPx) {
                onEvent(
                    eventPrefix +
                        " fakeCarrierCapacity restore=skipped reason=writer-changed" +
                        " liveWidth=" + params.width +
                        " appliedWidth=" + appliedWidthPx,
                )
                clearFakeCarrierCapacityLeaseSnapshot()
                return false
            }

            params.width = baselineLayoutWidthPx
            hostView.layoutParams = params
            val restored = hostView.layoutParams?.width == baselineLayoutWidthPx
            clearFakeCarrierCapacityLeaseSnapshot()
            return restored
        }

        private fun clearFakeCarrierCapacityLeaseSnapshot() {
            nativeFakeCarrierLayoutWidthPx = null
            nativeFakeCarrierParentContentWidthPx = null
            appliedFakeCarrierWidthPx = null
            fakeCarrierCapacityDeltaPx = null
            fakeCarrierCapacityBaselineReservationPx = null
            fakeCarrierCapacityLeaseAwaitingLayout = false
        }

        fun isLayoutCutoverReady(): Boolean =
            active &&
                started &&
                (!deferVisualMaskUntilLayout || compactLayoutReady)

        fun adoptTransferredCompactLayout(): Int? {
            if (
                !active ||
                !started ||
                !deferVisualMaskUntilLayout
            ) {
                return null
            }
            if (!syncEndReservation()) {
                return null
            }
            if (fakeCarrierCapacityLeaseAwaitingLayout) {
                return null
            }
            val masked = refreshClipMasks()
            if (compactLayoutReady) {
                return masked
            }
            compactLayoutReady = true
            val callback = layoutReadyCallback
            layoutReadyCallback = null
            onEvent(
                eventPrefix + " layoutReady source=hot-reload-transfer" +
                    " maskedViews=" + masked,
            )
            callback?.invoke(masked)
            return masked
        }

        fun validateNativeLayoutBeforeVisualMask(): Boolean {
            if (!active || !started) return false
            if (!fakeCarrierCapacityLeaseAwaitingLayout) return true

            val hostView =
                host.get()
                    ?: run {
                        onFailNative("fake-carrier-post-lease-host-released")
                        return false
                    }
            val parent =
                hostView.parent as? ViewGroup
                    ?: run {
                        onFailNative("fake-carrier-post-lease-parent-unavailable")
                        return false
                    }
            val expectedWidthPx =
                appliedFakeCarrierWidthPx
                    ?: run {
                        onFailNative("fake-carrier-post-lease-state-invalid")
                        return false
                    }
            if (
                hostView.layoutParams?.width != expectedWidthPx ||
                !isFakeCarrierEndAnchored(hostView, parent)
            ) {
                onFailNative("fake-carrier-post-lease-layout-invalid")
                return false
            }
            return true
        }

        fun onNativeLayoutCompleted(maskedViews: Int) {
            if (!active || !started) {
                return
            }
            if (fakeCarrierCapacityLeaseAwaitingLayout) {
                fakeCarrierCapacityLeaseAwaitingLayout = false
            }
            if (
                !deferVisualMaskUntilLayout ||
                compactLayoutReady
            ) {
                return
            }
            compactLayoutReady = true
            val callback = layoutReadyCallback
            layoutReadyCallback = null
            onEvent(
                eventPrefix + " layoutReady source=native-status-icons-onLayout" +
                    " maskedViews=" + maskedViews,
            )
            callback?.invoke(maskedViews)
        }

        fun reconcileIslandGestureAfterNativeLayout() {
            if (
                !active ||
                surfaceName != CONTROL_CENTER_FAKE_SURFACE ||
                !nativeLayoutAuthority
            ) {
                return
            }

            if (
                latchedIslandAvoidedSlots.isNotEmpty() &&
                (
                    lastIsland2DOverlap == false ||
                        islandReturnOverlapRequested ||
                        islandReturnBaselineLayoutPending
                )
            ) {
                refreshIslandPeerLatchMasks()
            }

            if (islandReturnBaselineLayoutPending) {
                val hostView =
                    host.get()
                        ?: run {
                            onFailNative("island-return-carrier-released")
                            return
                        }
                val expectedWidthPx =
                    islandReturnExpectedCarrierWidthPx
                        ?: run {
                            onFailNative("island-return-baseline-width-missing")
                            return
                        }
                val parent =
                    hostView.parent as? ViewGroup
                        ?: run {
                            onFailNative("island-return-parent-released")
                            return
                        }
                if (
                    hostView.width != expectedWidthPx ||
                    !isFakeCarrierEndAnchored(hostView, parent)
                ) {
                    return
                }
                val restored = restoreIslandPeerLatchMasks()
                latchedIslandAvoidedSlots.clear()
                islandReturnBaselineLayoutPending = false
                islandReturnExpectedCarrierWidthPx = null
                onEvent(
                    eventPrefix +
                        " islandPeerLatch release=true restored=" + restored +
                        " source=reverse-real-overlap-baseline-layout",
                )
                return
            }

            if (islandReturnOverlapRequested) {
                islandReturnOverlapRequested = false
                val baselineWidthPx = nativeFakeCarrierLayoutWidthPx
                if (appliedFakeCarrierWidthPx != null) {
                    if (
                        baselineWidthPx == null ||
                        !restoreFakeCarrierCapacityLease()
                    ) {
                        onFailNative("island-return-capacity-restore-failed")
                        return
                    }
                    islandReturnExpectedCarrierWidthPx = baselineWidthPx
                    islandReturnBaselineLayoutPending = true
                    onEvent(
                        eventPrefix +
                            " islandPeerLatch reverseOverlap=true" +
                            " capacityLease=restoring" +
                            " clipsHeld=" + islandPeerClipStates.size,
                    )
                } else {
                    val restored = restoreIslandPeerLatchMasks()
                    latchedIslandAvoidedSlots.clear()
                    onEvent(
                        eventPrefix +
                            " islandPeerLatch reverseOverlap=true" +
                            " capacityLease=inactive restored=" + restored,
                    )
                }
                return
            }

            if (!islandPeerLatchRequested) {
                return
            }
            islandPeerLatchRequested = false
            refreshIslandPeerLatchMasks()

            val hostView =
                host.get()
                    ?: run {
                        onFailNative("island-separated-carrier-released")
                        return
                    }
            val baseline = nativePadding
            val group = statusIcons.get()
            if (baseline == null || group == null) {
                onFailNative("island-separated-capacity-state-unavailable")
                return
            }
            val livePaddingDelta =
                (PaddingState.from(group).end - baseline.end).coerceAtLeast(0)
            val capacityDeltaPx =
                ensureFakeCarrierCapacityLease(
                    hostView = hostView,
                    reservationBaselinePx = livePaddingDelta,
                ) ?: return
            val capacityBaselineReservationPx =
                fakeCarrierCapacityBaselineReservationPx ?: livePaddingDelta
            val capacityReservationGrowthPx =
                EndReservationPolicy.resolveCapacityReservationGrowth(
                    currentReservationPx = livePaddingDelta,
                    baselineReservationPx = capacityBaselineReservationPx,
                )
            if (capacityReservationGrowthPx > capacityDeltaPx) {
                onFailNative("island-separated-capacity-insufficient")
                return
            }
            onEvent(
                eventPrefix +
                    " islandPeerLatch active=true" +
                    " slots=" +
                    latchedIslandAvoidedSlots.joinToString(",").ifEmpty { "none" } +
                    " capacityLease=active" +
                    " capacityDelta=" + capacityDeltaPx +
                    " baselineReservation=" + capacityBaselineReservationPx +
                    " reservationGrowth=" + capacityReservationGrowthPx +
                    " peerStateWrites=0 nativeAlphaWrites=0" +
                    " nativeVisibilityWrites=0 nativeTranslationWrites=0",
            )
        }

        private fun refreshIslandPeerLatchMasks(): Int {
            val group =
                statusIcons.get()
                    ?: run {
                        onFailNative("island-peer-latch-status-icons-released")
                        return 0
                    }
            val targets = linkedSetOf<View>()
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (NativeParticipantRuntimeAccess.slotOf(child) in latchedIslandAvoidedSlots) {
                    targets += child
                }
            }

            val iterator = islandPeerClipStates.iterator()
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
                val existing =
                    islandPeerClipStates
                        .firstOrNull { state -> state.view.get() === view }
                if (existing == null) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    islandPeerClipStates +=
                        ClipState(
                            view = WeakReference(view),
                            nativeClip = nativeClip,
                            appliedClip = applied,
                        )
                } else if (view.clipBounds == existing.nativeClip) {
                    view.clipBounds = existing.appliedClip
                } else if (view.clipBounds != existing.appliedClip) {
                    onFailNative("island-peer-latch-clip-writer-conflict")
                    return 0
                }
            }
            return islandPeerClipStates.count { state -> state.view.get() != null }
        }

        private fun restoreIslandPeerLatchMasks(): Int {
            val states = islandPeerClipStates.toList()
            islandPeerClipStates.clear()
            var restored = 0
            states.forEach { state ->
                if (restoreClipState(state)) {
                    restored += 1
                }
            }
            return restored
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
                onFailNative(surfaceName + "-host-detached")
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

    private data class PaddingState(
        val start: Int,
        val top: Int,
        val end: Int,
        val bottom: Int,
    ) {
        companion object {
            fun from(view: View): PaddingState =
                PaddingState(view.paddingStart, view.paddingTop, view.paddingEnd, view.paddingBottom)
        }
    }

    private enum class IgnoredSlotLifetime {
        NATIVE_CALL,
        PRESENTATION_SESSION,
    }

    internal object ControlCenterIslandOverlapPolicy {
        fun intersects(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            islandLeft: Int,
            islandTop: Int,
            islandRight: Int,
            islandBottom: Int,
        ): Boolean =
            left < islandRight &&
                islandLeft < right &&
                top < islandBottom &&
                islandTop < bottom
    }

    internal object ControlCenterLayoutPolicy {
        fun shouldApplyFakeCarrierCapacityLease(
            surfaceName: String,
            nativeLayoutAuthority: Boolean,
            island2DSeparated: Boolean = false,
        ): Boolean =
            surfaceName == CONTROL_CENTER_FAKE_SURFACE &&
                (!nativeLayoutAuthority || island2DSeparated)
    }

    internal object ControlCenterIslandGesturePolicy {
        private const val NATIVE_ISLAND_HIDDEN_STATE = 10

        fun shouldLatchPeer(
            slot: String,
            representedSlots: Set<String>,
            visible: Boolean,
            width: Int,
            height: Int,
            inIslandState: Int?,
            beforeInIslandState: Int? = null,
        ): Boolean =
            slot !in representedSlots &&
                visible &&
                width > 0 &&
                height > 0 &&
                (
                    inIslandState == NATIVE_ISLAND_HIDDEN_STATE ||
                        beforeInIslandState == NATIVE_ISLAND_HIDDEN_STATE
                )
    }

    internal object PersistentIgnoredSlotPolicy {
        fun <T> ownedDelta(
            existing: Collection<T>,
            requested: Collection<T>,
        ): List<T> =
            requested.filterNot(existing::contains)

        fun <T> restoreTarget(
            live: List<T>,
            ownedEntries: Collection<T>,
        ): List<T> {
            val owned = ownedEntries.toHashSet()
            return live.filterNot(owned::contains)
        }

        fun shouldUseNativeSetterOnRestore(
            requestLayout: Boolean,
        ): Boolean = requestLayout
    }

    internal object HotReloadHandoffPolicy {
        fun shouldRequestLayoutOnRelease(
            continuousHandoff: Boolean,
        ): Boolean = !continuousHandoff
    }

    internal object VisualMaskPolicy {
        fun shouldPreserveNativeBeforeCompactCutover(
            deferVisualMaskUntilLayout: Boolean,
        ): Boolean = deferVisualMaskUntilLayout

        fun shouldAdoptExistingNativeLayout(
            deferVisualMaskUntilLayout: Boolean,
            laidOut: Boolean,
            layoutRequested: Boolean,
            capacityLeaseAwaitingLayout: Boolean = false,
            width: Int,
            height: Int,
        ): Boolean =
            deferVisualMaskUntilLayout &&
                laidOut &&
                !layoutRequested &&
                !capacityLeaseAwaitingLayout &&
                width > 0 &&
                height > 0
    }

    internal object EndReservationPolicy {
        fun shouldDeferLiveBatteryWidthUnavailable(
            retainOnTransientLoss: Boolean,
            compactLayoutReady: Boolean,
        ): Boolean =
            retainOnTransientLoss && compactLayoutReady

        fun resolveRequestedSlotWidth(
            compactSlotWidthPx: Int,
            transitionRequestedSlotWidthPx: Int?,
        ): Int {
            val compact = compactSlotWidthPx.coerceAtLeast(0)
            return transitionRequestedSlotWidthPx
                ?.coerceAtLeast(compact)
                ?: compact
        }

        fun resolvePaddingEndDelta(
            nativeHide: Boolean,
            actualBatteryWidthPx: Int,
            requestedSlotWidthPx: Int,
        ): Int {
            val requested = requestedSlotWidthPx.coerceAtLeast(0)
            val actual = actualBatteryWidthPx.coerceAtLeast(0)
            return if (nativeHide) {
                requested
            } else {
                requested - actual
            }
        }

        fun resolveCapacityReservationGrowth(
            currentReservationPx: Int,
            baselineReservationPx: Int,
        ): Int =
            (
                currentReservationPx.coerceAtLeast(0) -
                    baselineReservationPx.coerceAtLeast(0)
            ).coerceAtLeast(0)

        fun resolveFakeCarrierCapacityDelta(
            nativeCarrierWidthPx: Int,
            parentContentWidthPx: Int,
        ): Int? {
            if (nativeCarrierWidthPx <= 0 || parentContentWidthPx <= 0) return null
            if (nativeCarrierWidthPx > parentContentWidthPx) return null
            return parentContentWidthPx - nativeCarrierWidthPx
        }
    }

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
        data class Prepared(
            val representedSlots: Int,
            val reused: Boolean,
        ) : StateResult
        data class Inactive(val restoredViews: Int) : StateResult
        data class Failure(val reason: String) : StateResult
    }

    internal sealed interface ControlCenterStateResult {
        data class Active(
            val representedSlots: Int,
            val maskedViews: Int,
            val reused: Boolean,
        ) : ControlCenterStateResult

        data class Prepared(
            val representedSlots: Int,
            val reused: Boolean,
        ) : ControlCenterStateResult

        data class Inactive(
            val restoredViews: Int,
        ) : ControlCenterStateResult

        data class Failure(
            val reason: String,
        ) : ControlCenterStateResult
    }

    internal sealed interface LegacyCleanupResult {
        data object NotPresent : LegacyCleanupResult
        data object Removed : LegacyCleanupResult
        data class Failure(val reason: String) : LegacyCleanupResult
    }
}
