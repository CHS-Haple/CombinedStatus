package com.chaners.combinedstatus.xposed

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList

internal object SystemUiNativeCombinedParticipantOwner {
    const val SLOT = "combined_status"

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
    private var controllerRef: WeakReference<Any>? = null
    private var handlesRef: WeakReference<NativeParticipantRuntimeAccess.Handles>? = null
    private var hostRef: WeakReference<ViewGroup>? = null
    private var eventSink: ((String) -> Unit)? = null
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
                            val context = chain.getArg(0) as? Context
                            if (
                                registry == null ||
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
                                recordFailure("registry-replacement-failed")
                                return@Hooker chain.proceed()
                            }

                            injected = true
                            failureReason = null
                            try {
                                val result = chain.proceed()
                                chain.thisObject?.let {
                                    controllerRef = WeakReference(it)
                                }
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
                                if (!registryRestored) {
                                    failureReason = "registry-restore-failed"
                                }
                                onEvent?.invoke(
                                    "nativeCombinedParticipant constructorComplete slot=" + SLOT +
                                        " registryRestored=" + registryRestored +
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
    fun attachHidden(host: Any): AttachResult {
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

        root.clipChildren = false
        root.clipToPadding = false
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
                                Gravity.CENTER,
                            ),
                        )
                    }
            }
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
        handlesRef = WeakReference(handles)
        hostRef = WeakReference(hostView)

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
            modelReady = modelUpdate?.model != null,
            tintReady = tintUpdate?.resolved != null,
        )
    }

    @Synchronized
    fun onState(
        snapshot: CombinedStatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        val update = renderController?.update(snapshot, trace)
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
                    " visible=false nativeGeometryWrites=0",
            )
        }
    }

    @Synchronized
    fun onSceneUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
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
        eventSink?.invoke(
            "nativeCombinedParticipant unlockedSlotOrder " +
                NativeStatusBarSlotOrderingProbe
                    .inspectLaidOutGroup(host)
                    .joinToString("|") +
                " nativeGeometryWrites=0",
        )
    }

    @Synchronized
    fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        renderController?.update(
            CombinedStatusStateStore.snapshot(),
            trace,
        )
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        renderController?.updateTint(update.state)
    }

    @Synchronized
    fun detach(): DetachResult {
        val handles =
            handlesRef?.get()
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
        renderViewRef?.get()?.let { render ->
            (render.parent as? ViewGroup)?.removeView(render)
        }
        rootRef = null
        renderViewRef = null
        hostRef = null
        eventSink = null
        modelReadyLogged = false
        unlockedGeometryLogged = false
        renderController = null
        controllerRef = null
        handlesRef = null
        injected = false
        registryRestored = false
        failureReason = null
        return result
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
        val binding =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(bindingClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "getShouldIconBeVisible" -> false
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

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult
        data class Failure(val reason: String) : InstallResult
    }

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
