package com.chaners.combinedstatus.xposed

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.Collections
import java.util.WeakHashMap

internal object SystemUiNativeCombinedParticipantOwner {
    const val SLOT = "combined_status"
    private const val ZERO_SLOT_WIDTH = 0

    private const val CONTROLLER_IMPL =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl"
    private const val REGISTRY_IMPL =
        "com.android.systemui.statusbar.pipeline.icons.shared.BindableIconsRegistryImpl"
    private const val BINDABLE_ICON =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.BindableIcon"
    private const val CREATOR =
        "com.android.systemui.statusbar.pipeline.icons.shared.model.ModernStatusBarViewCreator"
    private const val MODERN_VIEW =
        "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
    private const val BINDING =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding"
    private const val BINDABLE_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder\$BindableIconHolder"
    private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val HOOK_ID =
        "combinedstatus.nativeCombinedParticipant.constructor"

    private var constructorHook: HookHandle? = null
    private var rootRef: WeakReference<FrameLayout>? = null
    private var renderViewRef: WeakReference<CombinedStatusRenderView>? = null
    private var renderController: CombinedStatusRenderController? = null
    private var hostRef: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null
    private val bindingStates =
        Collections.synchronizedMap(
            WeakHashMap<FrameLayout, BindingState>(),
        )
    private var targetBindingState: BindingState? = null
    private var batteryRef: WeakReference<View>? = null
    private var handoffSink: ((Boolean) -> Unit)? = null
    private var pendingPreDrawRoot: WeakReference<View>? = null
    private var pendingPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    private var modelReady = false
    private var tintReady = false
    private var currentSurface = SystemUiSceneStateSource.Surface.UNKNOWN
    private var handoffPending = false
    private var handoffCommitted = false
    private var modelReadyLogged = false
    private var unlockedGeometryLogged = false
    private var registryRestored = false
    private var injected = false
    private var failureReason: String? = null

    val installedHookCount: Int
        @Synchronized get() = if (constructorHook != null) 1 else 0

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onSlotOrderResult: ((NativeStatusBarSlotReservation.Result) -> Unit)? = null,
    ): InstallResult {
        if (constructorHook != null) return InstallResult.AlreadyInstalled
        eventSink = onEvent

        val controllerClass =
            classOrNull(CONTROLLER_IMPL, classLoader)
                ?: return InstallResult.Failure("controller-class-missing")
        val registryClass =
            classOrNull(REGISTRY_IMPL, classLoader)
                ?: return InstallResult.Failure("registry-class-missing")
        val bindableIconClass =
            classOrNull(BINDABLE_ICON, classLoader)
                ?: return InstallResult.Failure("bindable-icon-class-missing")
        val creatorClass =
            classOrNull(CREATOR, classLoader)
                ?: return InstallResult.Failure("creator-class-missing")
        val modernViewClass =
            classOrNull(MODERN_VIEW, classLoader)
                ?: return InstallResult.Failure("modern-view-class-missing")
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: return InstallResult.Failure("binding-class-missing")
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: return InstallResult.Failure("function0-class-missing")

        if (
            !bindableIconClass.isInterface ||
            !creatorClass.isInterface ||
            !bindingClass.isInterface ||
            !function0Class.isInterface
        ) {
            return InstallResult.Failure("proxy-contract-mismatch")
        }

        val constructor =
            controllerClass.declaredConstructors
                .firstOrNull { it.parameterTypes.lastOrNull() == registryClass }
                ?: return InstallResult.Failure("controller-registry-constructor-missing")
        val iconListParameterIndex =
            constructor.parameterTypes.indexOfFirst { type ->
                type.name == NativeStatusBarSlotReservation.STATUS_BAR_ICON_LIST
            }
        if (iconListParameterIndex < 0) {
            return InstallResult.Failure("controller-icon-list-parameter-missing")
        }
        val registryField =
            registryClass.declaredFields
                .firstOrNull { it.name == "bindableIcons" }
                ?: return InstallResult.Failure("registry-list-field-missing")
        registryField.isAccessible = true

        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull {
                    it.parameterTypes.map { type -> type.name } ==
                        listOf("android.content.Context", "android.util.AttributeSet")
                }
                ?: return InstallResult.Failure("modern-view-constructor-missing")
        viewConstructor.isAccessible = true

        val initView =
            modernViewClass.declaredMethods
                .firstOrNull {
                    it.name == "initView" &&
                        it.parameterTypes.map { type -> type.name } ==
                        listOf("java.lang.String", FUNCTION0)
                }
                ?: return InstallResult.Failure("modern-view-init-missing")
        initView.isAccessible = true
        constructor.isAccessible = true

        val handle =
            runCatching {
                module
                    .hook(constructor)
                    .setId(HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val registry =
                                chain.getArg(constructor.parameterCount - 1)
                            val iconList = chain.getArg(iconListParameterIndex)
                            val context = chain.getArg(0) as? Context
                            if (
                                registry == null ||
                                iconList == null ||
                                context == null ||
                                !registryClass.isInstance(registry)
                            ) {
                                recordFailure("constructor-args-missing")
                                return@Hooker chain.proceed()
                            }

                            @Suppress("UNCHECKED_CAST")
                            val original =
                                runCatching {
                                    registryField.get(registry) as? List<Any?>
                                }.getOrNull()
                                    ?: run {
                                        recordFailure("registry-list-unreadable")
                                        return@Hooker chain.proceed()
                                    }

                            if (original.any { slotOfBindableIcon(it) == SLOT }) {
                                recordFailure("slot-already-present")
                                return@Hooker chain.proceed()
                            }

                            val preflight =
                                runCatching {
                                    createRoot(
                                        context = context,
                                        classLoader = classLoader,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "creator-preflight-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            preflight.removeAllViews()

                            val creator =
                                runCatching {
                                    createCreatorProxy(
                                        classLoader = classLoader,
                                        creatorClass = creatorClass,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "creator-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            val bindable =
                                runCatching {
                                    createBindableIconProxy(
                                        classLoader = classLoader,
                                        bindableIconClass = bindableIconClass,
                                        creator = creator,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "bindable-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }
                            val slotPreparation =
                                when (
                                    val result =
                                        NativeStatusBarSlotReservation.reserveTail(
                                            iconList = iconList,
                                            slot = SLOT,
                                        )
                                ) {
                                    is NativeStatusBarSlotReservation.ReservationResult.Ready ->
                                        result

                                    is NativeStatusBarSlotReservation.ReservationResult.Failure -> {
                                        onSlotOrderResult?.invoke(result.result)
                                        onEvent?.invoke(result.result.logLine)
                                        recordFailure("slot-predeclare-" + result.result.reason)
                                        return@Hooker chain.proceed()
                                    }
                                }
                            val slotReservation = slotPreparation.reservation

                            val extended =
                                ArrayList<Any?>(original.size + 1).apply {
                                    addAll(original)
                                    add(bindable)
                                }

                            val replaced =
                                runCatching {
                                    registryField.set(registry, extended)
                                    registryField.get(registry) === extended
                                }.getOrDefault(false)
                            if (!replaced) {
                                val slotRolledBack = slotReservation.rollback()
                                val slotFailure =
                                    NativeStatusBarSlotReservation.Result.Failure(
                                        if (slotRolledBack) {
                                            "transaction-aborted-registry-replacement"
                                        } else {
                                            "transaction-aborted-registry-replacement-slot-rollback-failed"
                                        },
                                    )
                                onSlotOrderResult?.invoke(slotFailure)
                                onEvent?.invoke(slotFailure.logLine)
                                recordFailure("registry-replacement-failed")
                                return@Hooker chain.proceed()
                            }

                            injected = true
                            failureReason = null
                            var controllerCreated = false
                            try {
                                val result = chain.proceed()
                                controllerCreated = true
                                onSlotOrderResult?.invoke(slotPreparation.result)
                                onEvent?.invoke(slotPreparation.result.logLine)
                                onEvent?.invoke(
                                    "nativeCombinedParticipant injected slot=" + SLOT +
                                        " registryOriginal=" + original.size +
                                        " registryExtended=" + extended.size +
                                        " visible=false nativeGeometryWrites=0",
                                )
                                result
                            } finally {
                                registryRestored =
                                    runCatching {
                                        registryField.set(registry, original)
                                        registryField.get(registry) === original
                                    }.getOrDefault(false)
                                if (!controllerCreated) {
                                    val slotRolledBack = slotReservation.rollback()
                                    val slotFailure =
                                        NativeStatusBarSlotReservation.Result.Failure(
                                            if (slotRolledBack) {
                                                "transaction-aborted-controller-construction"
                                            } else {
                                                "transaction-aborted-controller-construction-slot-rollback-failed"
                                            },
                                        )
                                    onSlotOrderResult?.invoke(slotFailure)
                                    onEvent?.invoke(slotFailure.logLine)
                                    injected = false
                                    failureReason =
                                        if (slotRolledBack) {
                                            "controller-construction-failed"
                                        } else {
                                            "controller-construction-failed-slot-rollback-failed"
                                        }
                                } else if (!registryRestored) {
                                    failureReason = "registry-restore-failed"
                                }
                                onEvent?.invoke(
                                    "nativeCombinedParticipant constructorComplete slot=" + SLOT +
                                        " registryRestored=" + registryRestored +
                                        " slotReserved=" + controllerCreated +
                                        " nativeGeometryWrites=0",
                                )
                            }
                        },
                    )
            }.getOrElse {
                return InstallResult.Failure(
                    "constructor-hook-" + (it.message ?: it.javaClass.simpleName),
                )
            }

        constructorHook = handle
        return InstallResult.Installed
    }

    @Synchronized
    fun releaseGenerationForHotReload(): Boolean {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return false
        }

        removePendingPreDraw()
        rootRef = null
        renderViewRef = null
        renderController = null
        hostRef = null
        batteryRef = null
        targetBindingState = null
        handoffSink = null
        bindingStates.clear()
        modelReady = false
        tintReady = false
        currentSurface = SystemUiSceneStateSource.Surface.UNKNOWN
        handoffPending = false
        handoffCommitted = false
        modelReadyLogged = false
        unlockedGeometryLogged = false
        eventSink = null
        return true
    }

    @Synchronized
    fun adoptAfterHotReload(
        host: Any,
    ): HotReloadAdoptResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return HotReloadAdoptResult.Failure("main-thread-required")
        }

        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return HotReloadAdoptResult.Failure(resolution.reason)
            }
        val holder =
            NativeParticipantRuntimeAccess.iconHolder(
                handles = handles,
                slot = SLOT,
            ) ?: return HotReloadAdoptResult.NotPresent
        if (holder.javaClass.name != BINDABLE_HOLDER) {
            return HotReloadAdoptResult.Failure("native-holder-type-mismatch")
        }

        val initializerField =
            fieldOrNull(holder, "initializer")
                ?: return HotReloadAdoptResult.Failure("native-initializer-field-missing")
        val slotField =
            fieldOrNull(holder, "slot")
                ?: return HotReloadAdoptResult.Failure("native-slot-field-missing")
        val visibleField =
            fieldOrNull(holder, "isVisible")
                ?: return HotReloadAdoptResult.Failure("native-visible-field-missing")
        val removal =
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?: return HotReloadAdoptResult.Failure("removal-contract-missing")
        val iconList =
            readField(handles.controller, "mStatusBarIconList")
                ?: return HotReloadAdoptResult.Failure("status-bar-icon-list-missing")
        val creator =
            runCatching {
                createRuntimeCreator(handles.classLoader)
            }.getOrElse { error ->
                return HotReloadAdoptResult.Failure(
                    error.message ?: error.javaClass.simpleName,
                )
            }

        val removed =
            runCatching {
                NativeParticipantRuntimeAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
                true
            }.getOrDefault(false)
        if (!removed) {
            return HotReloadAdoptResult.Failure("native-removal-failed")
        }

        val cleared =
            NativeParticipantRuntimeAccess.clearBindableEntries(
                handles = handles,
                slot = SLOT,
                expectedHolder = holder,
            )
        if (
            NativeParticipantRuntimeAccess.findSlotView(handles.group, SLOT) != null ||
            NativeParticipantRuntimeAccess.iconHolder(handles, SLOT) != null
        ) {
            return HotReloadAdoptResult.Failure("native-removal-verification-failed")
        }

        val sanitized =
            runCatching {
                initializerField.set(holder, null)
                initializerField.get(holder) == null
            }.getOrDefault(false)
        if (!sanitized) {
            return HotReloadAdoptResult.Failure("native-holder-sanitize-failed")
        }

        val slotPreparation =
            when (
                val result =
                    NativeStatusBarSlotReservation.reserveTail(
                        iconList = iconList,
                        slot = SLOT,
                    )
            ) {
                is NativeStatusBarSlotReservation.ReservationResult.Ready ->
                    result
                is NativeStatusBarSlotReservation.ReservationResult.Failure ->
                    return HotReloadAdoptResult.Failure(
                        "slot-reservation-" + result.result.reason,
                    )
            }

        val rebound =
            runCatching {
                initializerField.set(holder, creator)
                slotField.set(holder, SLOT)
                visibleField.setBoolean(holder, true)
                NativeParticipantRuntimeAccess.invokeSetIconHolder(
                    handles = handles,
                    slot = SLOT,
                    holder = holder,
                )
                NativeParticipantRuntimeAccess.iconHolder(handles, SLOT) === holder &&
                    NativeParticipantRuntimeAccess.findSlotView(handles.group, SLOT) != null
            }.getOrDefault(false)
        if (!rebound) {
            runCatching {
                NativeParticipantRuntimeAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
            }
            NativeParticipantRuntimeAccess.clearBindableEntries(
                handles = handles,
                slot = SLOT,
                expectedHolder = holder,
            )
            runCatching { initializerField.set(holder, null) }
            slotPreparation.reservation.rollback()
            recordFailure("hot-reload-adoption-failed")
            return HotReloadAdoptResult.Failure("native-rebind-failed")
        }

        injected = true
        registryRestored = true
        failureReason = null
        eventSink?.invoke(
            "nativeCombinedParticipant hotReloadAdopt slot=" + SLOT +
                " viewReady=true managerEntriesRefreshed=true " +
                "clearedManagerEntries=" + cleared +
                " mainThread=true nativeGeometryWrites=0",
        )
        return HotReloadAdoptResult.Ready(
            clearedManagerEntries = cleared,
        )
    }

    @Synchronized
    fun attachHidden(
        host: Any,
        onHandoffStateChanged: ((Boolean) -> Unit)? = null,
    ): AttachResult {
        if (!injected) {
            return AttachResult.Failure(failureReason ?: "participant-not-injected")
        }
        val resolution = NativeParticipantRuntimeAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return AttachResult.Failure(resolution.reason)
            }

        val root =
            NativeParticipantRuntimeAccess.findSlotView(handles.group, SLOT) as? FrameLayout
                ?: return AttachResult.Failure("native-root-missing")
        val bindingState =
            bindingStates[root]
                ?: return AttachResult.Failure("native-binding-state-missing")
        val hostView =
            host as? ViewGroup
                ?: return AttachResult.Failure("host-not-view-group")
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return AttachResult.Failure("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return AttachResult.Failure("battery-view-missing")
        if (battery.width <= 0 || battery.height <= 0) {
            return AttachResult.Failure("battery-geometry-not-ready")
        }

        val rootLayoutParams =
            root.layoutParams
                ?: return AttachResult.Failure("native-root-layout-params-missing")
        val originalShellWidth = rootLayoutParams.width
        val originalShellHeight = rootLayoutParams.height
        val shellGeometryAdjusted =
            if (
                originalShellWidth != ZERO_SLOT_WIDTH ||
                originalShellHeight != battery.height
            ) {
                runCatching {
                    rootLayoutParams.width = ZERO_SLOT_WIDTH
                    rootLayoutParams.height = battery.height
                    root.layoutParams = rootLayoutParams
                    root.layoutParams?.width == ZERO_SLOT_WIDTH &&
                        root.layoutParams?.height == battery.height
                }.getOrDefault(false)
            } else {
                true
            }
        if (!shellGeometryAdjusted) {
            return AttachResult.Failure("native-root-geometry-adjustment-failed")
        }
        eventSink?.invoke(
            "nativeCombinedParticipant shellGeometry " +
                "originalWidth=" + originalShellWidth +
                " targetWidth=" + ZERO_SLOT_WIDTH +
                " originalHeight=" + originalShellHeight +
                " targetHeight=" + battery.height +
                " moduleOwnedSlotWidthWrite=" + (originalShellWidth != ZERO_SLOT_WIDTH) +
                " customShellHeightWrite=" + (originalShellHeight != battery.height) +
                " peerNativeGeometryWrites=0",
        )

        root.clipChildren = false
        root.clipToPadding = false
        bindingState.visible = false
        root.visibility = View.GONE

        val existing = renderViewRef?.get()
        val render =
            if (existing != null && existing.parent === root) {
                existing
            } else {
                val attached =
                    (0 until root.childCount)
                        .asSequence()
                        .map { index -> root.getChildAt(index) }
                        .filterIsInstance<CombinedStatusRenderView>()
                        .firstOrNull()
                attached
                    ?: CombinedStatusRenderView(root.context).also { child ->
                        root.addView(
                            child,
                            FrameLayout.LayoutParams(
                                battery.width,
                                battery.height,
                            ),
                        )
                    }
            }
        val renderLayoutParams =
            (render.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(
                    battery.width,
                    battery.height,
                )
        renderLayoutParams.width = battery.width
        renderLayoutParams.height = battery.height
        renderLayoutParams.gravity = Gravity.NO_GRAVITY
        render.layoutParams = renderLayoutParams

        renderViewRef = WeakReference(render)
        renderController =
            renderController ?: CombinedStatusRenderController(render)

        render.measure(
            View.MeasureSpec.makeMeasureSpec(battery.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(battery.height, View.MeasureSpec.EXACTLY),
        )
        val shellHeight =
            root.layoutParams
                ?.height
                ?.takeIf { height -> height > 0 }
                ?: battery.height
        val renderTop = (shellHeight - battery.height) / 2
        render.layout(
            0,
            renderTop,
            battery.width,
            renderTop + battery.height,
        )
        val modelUpdate =
            renderController?.update(CombinedStatusStateStore.snapshot())
        val tintUpdate =
            SystemUiTintStateSource.currentState(battery)?.let {
                renderController?.updateTint(it)
            }

        rootRef = WeakReference(root)
        hostRef = WeakReference(hostView)
        batteryRef = WeakReference(battery)
        targetBindingState = bindingState
        handoffSink = onHandoffStateChanged
        modelReady =
            modelUpdate?.model != null &&
                modelUpdate.candidateComplete
        tintReady = tintUpdate?.resolved != null
        currentSurface =
            SystemUiSceneStateSource.currentState(battery)?.surface
                ?: SystemUiSceneStateSource.Surface.UNKNOWN
        reconcileVisibleHandoff("attach")

        return AttachResult.Ready(
            registryRestored = registryRestored,
            rootClass = root.javaClass.name,
            rootVisibility = visibilityName(root.visibility),
            iconVisible = NativeParticipantRuntimeAccess.iconVisible(root),
            layoutWidth = root.layoutParams?.width ?: Int.MIN_VALUE,
            layoutHeight = root.layoutParams?.height ?: Int.MIN_VALUE,
            renderWidth = render.measuredWidth,
            renderHeight = render.measuredHeight,
            renderTop = render.top,
            renderBottom = render.bottom,
            managerEntry =
                (readField(handles.manager, "mBindableIcons") as? Map<*, *>)
                    ?.containsKey(SLOT) == true,
            modelReady = modelReady,
            tintReady = tintReady,
        )
    }

    @Synchronized
    fun onState(
        snapshot: CombinedStatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        val update = renderController?.update(snapshot, trace)
        if (update?.model != null && update.candidateComplete) {
            modelReady = true
        }
        if (
            update?.model != null &&
            !modelReadyLogged
        ) {
            modelReadyLogged = true
            val render = renderViewRef?.get()
            val root = rootRef?.get()
            eventSink?.invoke(
                "nativeCombinedParticipant rendererReady " +
                    "modelReady=true" +
                    " candidateComplete=" + update.candidateComplete +
                    " render=" +
                    (render?.measuredWidth ?: -1) + "x" +
                    (render?.measuredHeight ?: -1) +
                    " rootVisibility=" +
                    (root?.let { visibilityName(it.visibility) } ?: "none") +
                    " iconVisible=" +
                    (root?.let { NativeParticipantRuntimeAccess.iconVisible(it) } ?: "none") +
                    " visible=" + handoffCommitted +
                    " nativeGeometryWrites=0",
            )
        }
        reconcileVisibleHandoff("state")
    }

    @Synchronized
    fun onSceneUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
        val sourceBattery = batteryRef?.get()
        if (sourceBattery != null && update.sourceView !== sourceBattery) {
            return
        }
        currentSurface = update.surface
        reconcileVisibleHandoff("scene-" + update.surface.name)
        if (
            update.surface != SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR ||
            unlockedGeometryLogged
        ) {
            return
        }
        val host = hostRef?.get() ?: return
        val batteryContainer =
            host.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return
        val statusIcons =
            batteryContainer.directChild(STATUS_ICON_CONTAINER)
                ?: return
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return

        unlockedGeometryLogged = true
        eventSink?.invoke(
            "nativeCombinedParticipant unlockedGeometry " +
                "statusIconsWidth=" + statusIcons.width +
                " statusIconsRight=" + statusIcons.right +
                " batteryWidth=" + battery.width +
                " batteryBounds=" + battery.left + "-" + battery.right +
                " adjacentGap=" + (battery.left - statusIcons.right) +
                " rootVisibility=" +
                (rootRef?.get()?.let { visibilityName(it.visibility) } ?: "none") +
                " visible=false nativeGeometryWrites=0",
        )
    }

    @Synchronized
    fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        val update =
            renderController?.update(
                CombinedStatusStateStore.snapshot(),
                trace,
            )
        if (update?.model != null && update.candidateComplete) {
            modelReady = true
        }
        reconcileVisibleHandoff("presentation")
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        val battery = batteryRef?.get() ?: return
        if (update.sourceView !== battery) {
            return
        }

        val tintUpdate = renderController?.updateTint(update.state)
        if (tintUpdate?.resolved != null) {
            tintReady = true
        }
        reconcileVisibleHandoff("tint")
    }

    private fun reconcileVisibleHandoff(source: String) {
        val root = rootRef?.get() ?: return
        val bindingState = targetBindingState ?: return
        if (handoffCommitted) {
            if (!bindingState.visible) {
                bindingState.visible = true
                requestNativeLayout(root)
            }
            return
        }

        val handoffMode =
            resolveHandoffMode(
                surface = currentSurface,
                rootShown = root.isShown,
            )
        if (
            handoffPending ||
            !modelReady ||
            !tintReady ||
            handoffMode == HandoffMode.BLOCKED ||
            !root.isAttachedToWindow ||
            root.parent == null
        ) {
            return
        }

        bindingState.visible = true
        root.visibility = View.VISIBLE
        handoffPending = true
        requestNativeLayout(root)
        eventSink?.invoke(
            "nativeCombinedParticipant handoffPrepare source=" + source +
                " modelReady=" + modelReady +
                " tintReady=" + tintReady +
                " scene=" + currentSurface.name +
                " mode=" + handoffMode.name +
                " rootShownBefore=" + root.isShown +
                " bootstrapVisibilityRelease=true nativeGeometryWrites=0",
        )

        val listener =
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    removePendingPreDraw()
                    synchronized(this@SystemUiNativeCombinedParticipantOwner) {
                        handoffPending = false
                        if (
                            rootRef?.get() !== root ||
                            targetBindingState !== bindingState
                        ) {
                            return@synchronized
                        }

                        val iconVisible =
                            NativeParticipantRuntimeAccess.iconVisible(root) == true
                        val render = renderViewRef?.get()
                        val battery = batteryRef?.get()
                        val parent = root.parent as? ViewGroup
                        val rootLocation = IntArray(2)
                        val batteryLocation = IntArray(2)
                        if (root.isAttachedToWindow) {
                            root.getLocationOnScreen(rootLocation)
                        }
                        if (battery?.isAttachedToWindow == true) {
                            battery.getLocationOnScreen(batteryLocation)
                        }
                        val bridgeReady =
                            isZeroSlotHandoffReady(
                                rootMeasuredWidth = root.measuredWidth,
                                rootMeasuredHeight = root.measuredHeight,
                                renderMeasuredWidth = render?.measuredWidth ?: -1,
                                renderMeasuredHeight = render?.measuredHeight ?: -1,
                                expectedVisualWidth = battery?.width ?: -1,
                                expectedVisualHeight = battery?.height ?: -1,
                                parentClipsChildren = parent?.clipChildren ?: true,
                                rootScreenX = rootLocation[0],
                                batteryScreenX = batteryLocation[0],
                                renderLeft = render?.left ?: Int.MIN_VALUE,
                                renderRight = render?.right ?: Int.MIN_VALUE,
                            )
                        val resolvedMode =
                            resolveHandoffMode(
                                surface = currentSurface,
                                rootShown = root.isShown,
                            )
                        val ready =
                            modelReady &&
                                tintReady &&
                                resolvedMode != HandoffMode.BLOCKED &&
                                bindingState.visible &&
                                iconVisible &&
                                root.isAttachedToWindow &&
                                bridgeReady

                        if (ready) {
                            handoffCommitted = true
                            handoffSink?.invoke(true)
                            eventSink?.invoke(
                                "nativeCombinedParticipant handoffCommit " +
                                    "mode=" + resolvedMode.name +
                                    " rootShown=" + root.isShown +
                                    " hiddenAncestor=" + firstHiddenAncestor(root) +
                                    " measured=" + root.measuredWidth + "x" +
                                    root.measuredHeight +
                                    " renderMeasured=" +
                                    (render?.measuredWidth ?: -1) + "x" +
                                    (render?.measuredHeight ?: -1) +
                                    " rootScreenX=" + rootLocation[0] +
                                    " batteryScreenX=" + batteryLocation[0] +
                                    " renderBounds=" +
                                    (render?.left ?: Int.MIN_VALUE) + "-" +
                                    (render?.right ?: Int.MIN_VALUE) +
                                    " parentClipChildren=" +
                                    (parent?.clipChildren ?: true) +
                                    " bridge=zero-slot-to-native-battery " +
                                    "iconVisible=true overlayActive=false " +
                                    "peerNativeGeometryWrites=0",
                            )
                        } else {
                            bindingState.visible = false
                            root.visibility = View.GONE
                            requestNativeLayout(root)
                            eventSink?.invoke(
                                "nativeCombinedParticipant handoffRollback " +
                                    "modelReady=" + modelReady +
                                    " tintReady=" + tintReady +
                                    " scene=" + currentSurface.name +
                                    " mode=" + resolvedMode.name +
                                    " rootShown=" + root.isShown +
                                    " hiddenAncestor=" + firstHiddenAncestor(root) +
                                    " iconVisible=" + iconVisible +
                                    " measured=" + root.measuredWidth + "x" +
                                    root.measuredHeight +
                                    " renderMeasured=" +
                                    (render?.measuredWidth ?: -1) + "x" +
                                    (render?.measuredHeight ?: -1) +
                                    " rootScreenX=" + rootLocation[0] +
                                    " batteryScreenX=" + batteryLocation[0] +
                                    " renderBounds=" +
                                    (render?.left ?: Int.MIN_VALUE) + "-" +
                                    (render?.right ?: Int.MIN_VALUE) +
                                    " parentClipChildren=" +
                                    (parent?.clipChildren ?: true) +
                                    " bridgeReady=" + bridgeReady +
                                    " overlayActive=true nativeGeometryWrites=0",
                            )
                        }
                    }
                    return true
                }
            }
        pendingPreDrawRoot = WeakReference(root)
        pendingPreDrawListener = listener
        root.viewTreeObserver.addOnPreDrawListener(listener)
    }

    internal fun isZeroSlotHandoffReady(
        rootMeasuredWidth: Int,
        rootMeasuredHeight: Int,
        renderMeasuredWidth: Int,
        renderMeasuredHeight: Int,
        expectedVisualWidth: Int,
        expectedVisualHeight: Int,
        parentClipsChildren: Boolean,
        rootScreenX: Int,
        batteryScreenX: Int,
        renderLeft: Int,
        renderRight: Int,
    ): Boolean =
        rootMeasuredWidth == ZERO_SLOT_WIDTH &&
            rootMeasuredHeight > 0 &&
            renderMeasuredWidth == expectedVisualWidth &&
            renderMeasuredHeight == expectedVisualHeight &&
            expectedVisualWidth > 0 &&
            expectedVisualHeight > 0 &&
            !parentClipsChildren &&
            rootScreenX == batteryScreenX &&
            renderLeft == 0 &&
            renderRight == expectedVisualWidth

    internal enum class HandoffMode {
        BLOCKED,
        VISIBLE_HOME,
        PREARMED_KEYGUARD,
    }

    internal fun resolveHandoffMode(
        surface: SystemUiSceneStateSource.Surface,
        rootShown: Boolean,
    ): HandoffMode =
        when {
            surface == SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR ->
                HandoffMode.VISIBLE_HOME
            surface == SystemUiSceneStateSource.Surface.KEYGUARD && !rootShown ->
                HandoffMode.PREARMED_KEYGUARD
            else ->
                HandoffMode.BLOCKED
        }

    private fun firstHiddenAncestor(view: View): String {
        var current = view.parent as? View
        while (current != null) {
            if (current.visibility != View.VISIBLE || !current.isShown) {
                return current.javaClass.simpleName +
                    "{visibility=" + visibilityName(current.visibility) +
                    ",shown=" + current.isShown +
                    ",alpha=" + current.alpha + "}"
            }
            current = current.parent as? View
        }
        return "none"
    }

    private fun requestNativeLayout(root: View) {
        root.requestLayout()
        (root.parent as? View)?.requestLayout()
    }

    private fun resolveCurrentHandles(): NativeParticipantRuntimeAccess.Handles? {
        val host = hostRef?.get() ?: return null
        return when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
            is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                resolution.handles
            is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                null
        }
    }

    private fun removePendingPreDraw() {
        val root = pendingPreDrawRoot?.get()
        val listener = pendingPreDrawListener
        if (root != null && listener != null) {
            val observer = root.viewTreeObserver
            if (observer.isAlive) {
                observer.removeOnPreDrawListener(listener)
            }
        }
        pendingPreDrawRoot = null
        pendingPreDrawListener = null
    }

    @Synchronized
    fun detach(): DetachResult {
        val handles =
            resolveCurrentHandles()
                ?: return reset(DetachResult.NotAttached)
        val removal =
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?: return reset(DetachResult.Failure("removal-contract-missing"))

        val result =
            runCatching {
                renderViewRef?.get()?.let { render ->
                    (render.parent as? ViewGroup)?.removeView(render)
                }
                NativeParticipantRuntimeAccess.invokeRemoval(
                    handles = handles,
                    removal = removal,
                    slot = SLOT,
                )
                NativeParticipantRuntimeAccess.clearBindableEntries(
                    handles = handles,
                    slot = SLOT,
                )
                DetachResult.Ready
            }.getOrElse {
                DetachResult.Failure(
                    "remove-" + (it.message ?: it.javaClass.simpleName),
                )
            }
        return reset(result)
    }

    @Synchronized
    fun resetRuntimeState() {
        constructorHook = null
        reset(Unit)
    }

    private fun <T> reset(result: T): T {
        removePendingPreDraw()
        if (handoffCommitted) {
            handoffSink?.invoke(false)
        }
        targetBindingState?.visible = false
        renderViewRef?.get()?.let { render ->
            (render.parent as? ViewGroup)?.removeView(render)
        }
        bindingStates.clear()
        rootRef = null
        renderViewRef = null
        hostRef = null
        batteryRef = null
        handoffSink = null
        targetBindingState = null
        modelReady = false
        tintReady = false
        currentSurface = SystemUiSceneStateSource.Surface.UNKNOWN
        handoffPending = false
        handoffCommitted = false
        eventSink = null
        modelReadyLogged = false
        unlockedGeometryLogged = false
        renderController = null
        injected = false
        registryRestored = false
        failureReason = null
        return result
    }

    private fun createRuntimeCreator(classLoader: ClassLoader): Any {
        val creatorClass =
            classOrNull(CREATOR, classLoader)
                ?: error("creator-class-missing")
        val modernViewClass =
            classOrNull(MODERN_VIEW, classLoader)
                ?: error("modern-view-class-missing")
        val bindingClass =
            classOrNull(BINDING, classLoader)
                ?: error("binding-class-missing")
        val function0Class =
            classOrNull(FUNCTION0, classLoader)
                ?: error("function0-class-missing")
        check(
            creatorClass.isInterface &&
                bindingClass.isInterface &&
                function0Class.isInterface,
        ) {
            "proxy-contract-mismatch"
        }
        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull {
                    it.parameterTypes.map { type -> type.name } ==
                        listOf("android.content.Context", "android.util.AttributeSet")
                } ?: error("modern-view-constructor-missing")
        viewConstructor.isAccessible = true
        val initView =
            modernViewClass.declaredMethods
                .firstOrNull {
                    it.name == "initView" &&
                        it.parameterTypes.map { type -> type.name } ==
                        listOf("java.lang.String", FUNCTION0)
                } ?: error("modern-view-init-missing")
        initView.isAccessible = true
        return createCreatorProxy(
            classLoader = classLoader,
            creatorClass = creatorClass,
            modernViewClass = modernViewClass,
            bindingClass = bindingClass,
            function0Class = function0Class,
            viewConstructor = viewConstructor,
            initView = initView,
        )
    }

    private fun createBindableIconProxy(
        classLoader: ClassLoader,
        bindableIconClass: Class<*>,
        creator: Any,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(bindableIconClass),
        ) { proxy, method, args ->
            when (method.name) {
                "getSlot" -> SLOT
                "getShouldBindIcon" -> true
                "getInitializer" -> creator
                "toString" -> "CombinedStatusNativeParticipant(slot=$SLOT)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }

    private fun createCreatorProxy(
        classLoader: ClassLoader,
        creatorClass: Class<*>,
        modernViewClass: Class<*>,
        bindingClass: Class<*>,
        function0Class: Class<*>,
        viewConstructor: java.lang.reflect.Constructor<*>,
        initView: Method,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(creatorClass),
        ) { proxy, method, args ->
            when (method.name) {
                "createAndBind" -> {
                    val context =
                        args?.firstOrNull() as? Context
                            ?: error("creator-context-missing")
                    createRoot(
                        context = context,
                        classLoader = classLoader,
                        modernViewClass = modernViewClass,
                        bindingClass = bindingClass,
                        function0Class = function0Class,
                        viewConstructor = viewConstructor,
                        initView = initView,
                    )
                }
                "toString" -> "CombinedStatusNativeParticipantCreator"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }

    private fun createRoot(
        context: Context,
        classLoader: ClassLoader,
        modernViewClass: Class<*>,
        bindingClass: Class<*>,
        function0Class: Class<*>,
        viewConstructor: java.lang.reflect.Constructor<*>,
        initView: Method,
    ): FrameLayout {
        val bindingState = BindingState()
        val binding =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(bindingClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "getShouldIconBeVisible" -> bindingState.visible
                    "isCollecting" -> true
                    "toString" -> "CombinedStatusNativeParticipantBinding"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> defaultValue(method.returnType)
                }
            }
        val bindingFactory =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(function0Class),
            ) { proxy, method, args ->
                when (method.name) {
                    "invoke" -> binding
                    "toString" -> "CombinedStatusNativeParticipantBindingFactory"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> defaultValue(method.returnType)
                }
            }

        val root =
            viewConstructor.newInstance(
                context,
                null as AttributeSet?,
            ) as? FrameLayout
                ?: error("modern-view-not-frame-layout")
        check(modernViewClass.isInstance(root)) {
            "modern-view-instance-mismatch"
        }
        root.clipChildren = false
        root.clipToPadding = false
        initView.invoke(root, SLOT, bindingFactory)
        bindingStates[root] = bindingState
        root.visibility = View.GONE
        return root
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun slotOfBindableIcon(icon: Any?): String? {
        if (icon == null) return null
        val accessor =
            icon.javaClass.methods.firstOrNull {
                it.name == "getSlot" &&
                    it.parameterCount == 0 &&
                    it.returnType == String::class.java
            } ?: return null
        return runCatching { accessor.invoke(icon) as? String }.getOrNull()
    }

    private fun classOrNull(name: String, classLoader: ClassLoader): Class<*>? =
        runCatching { Class.forName(name, false, classLoader) }.getOrNull()

    private fun fieldOrNull(target: Any, name: String): Field? =
        generateSequence<Class<*>>(target.javaClass) { it.superclass }
            .mapNotNull { clazz ->
                clazz.declaredFields.firstOrNull { field -> field.name == name }
            }
            .firstOrNull()
            ?.apply { isAccessible = true }

    private fun readField(target: Any, name: String): Any? {
        val field =
            generateSequence<Class<*>>(target.javaClass) { it.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { it.name == name }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun recordFailure(reason: String) {
        injected = false
        failureReason = reason
    }

    private fun defaultValue(type: Class<*>): Any? =
        when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            java.lang.Void.TYPE -> null
            else -> null
        }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal sealed interface HotReloadAdoptResult {
        data class Ready(
            val clearedManagerEntries: Int,
        ) : HotReloadAdoptResult
        data object NotPresent : HotReloadAdoptResult
        data class Failure(
            val reason: String,
        ) : HotReloadAdoptResult
    }

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult
        data class Failure(val reason: String) : InstallResult
    }

    private class BindingState(
        @Volatile var visible: Boolean = false,
    )

    internal sealed interface AttachResult {
        data class Ready(
            val registryRestored: Boolean,
            val rootClass: String,
            val rootVisibility: String,
            val iconVisible: Boolean?,
            val layoutWidth: Int,
            val layoutHeight: Int,
            val renderWidth: Int,
            val renderHeight: Int,
            val renderTop: Int,
            val renderBottom: Int,
            val managerEntry: Boolean,
            val modelReady: Boolean,
            val tintReady: Boolean,
        ) : AttachResult
        data class Failure(val reason: String) : AttachResult
    }

    internal sealed interface DetachResult {
        data object Ready : DetachResult
        data object NotAttached : DetachResult
        data class Failure(val reason: String) : DetachResult
    }
}
