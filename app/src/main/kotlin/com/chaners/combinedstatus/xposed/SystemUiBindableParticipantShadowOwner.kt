package com.chaners.combinedstatus.xposed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

internal object SystemUiBindableParticipantShadowOwner {
    const val SLOT = "combined_status_bindable_shadow"

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
    private const val HOOK_ID =
        "combinedstatus.nativeBindableParticipant.shadowConstructor"

    private var constructorHook: HookHandle? = null
    private var state = State()

    val installedHookCount: Int
        @Synchronized get() = if (constructorHook != null) 1 else 0

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
    ): InstallResult {
        if (constructorHook != null) {
            return InstallResult.AlreadyInstalled
        }

        val controllerClass =
            classOrNull(CONTROLLER_IMPL, classLoader)
                ?: return InstallResult.Failure("controller-class-missing")
        val registryClass =
            classOrNull(REGISTRY_IMPL, classLoader)
                ?: return InstallResult.Failure("bindable-registry-class-missing")
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
            return InstallResult.Failure("bindable-proxy-contract-mismatch")
        }

        val constructor =
            controllerClass.declaredConstructors
                .firstOrNull { candidate ->
                    candidate.parameterTypes.lastOrNull() == registryClass
                }
                ?: return InstallResult.Failure("controller-registry-constructor-missing")

        val registryField =
            registryClass.declaredFields
                .firstOrNull { field -> field.name == "bindableIcons" }
                ?: return InstallResult.Failure("registry-list-field-missing")
        registryField.isAccessible = true

        val viewConstructor =
            modernViewClass.declaredConstructors
                .firstOrNull { candidate ->
                    candidate.parameterTypes.map { type -> type.name } ==
                        listOf(
                            "android.content.Context",
                            "android.util.AttributeSet",
                        )
                }
                ?: return InstallResult.Failure("modern-view-constructor-missing")
        viewConstructor.isAccessible = true

        val initView =
            modernViewClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "initView" &&
                        method.parameterTypes.map { type -> type.name } ==
                            listOf(
                                "java.lang.String",
                                FUNCTION0,
                            )
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
                            val context =
                                chain.getArg(0) as? Context

                            if (
                                registry == null ||
                                context == null ||
                                !registryClass.isInstance(registry)
                            ) {
                                recordFailure("controller-constructor-args-missing")
                                return@Hooker chain.proceed()
                            }

                            val originalList =
                                runCatching {
                                    @Suppress("UNCHECKED_CAST")
                                    registryField.get(registry) as? List<Any?>
                                }.getOrNull()
                            if (originalList == null) {
                                recordFailure("registry-list-unreadable")
                                return@Hooker chain.proceed()
                            }

                            if (
                                originalList.any { icon ->
                                    slotOfBindableIcon(icon) == SLOT
                                }
                            ) {
                                recordFailure("shadow-slot-already-present")
                                return@Hooker chain.proceed()
                            }

                            val creatorCalls = AtomicInteger(0)
                            val creatorProxy =
                                runCatching {
                                    createCreatorProxy(
                                        classLoader = classLoader,
                                        creatorClass = creatorClass,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                        creatorCalls = creatorCalls,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "creator-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }

                            val bindableProxy =
                                runCatching {
                                    createBindableIconProxy(
                                        classLoader = classLoader,
                                        bindableIconClass = bindableIconClass,
                                        creatorProxy = creatorProxy,
                                    )
                                }.getOrElse { error ->
                                    recordFailure(
                                        "bindable-proxy-" +
                                            (error.message ?: error.javaClass.simpleName),
                                    )
                                    return@Hooker chain.proceed()
                                }

                            val preflight =
                                runCatching {
                                    createShadowView(
                                        context = context,
                                        modernViewClass = modernViewClass,
                                        bindingClass = bindingClass,
                                        function0Class = function0Class,
                                        viewConstructor = viewConstructor,
                                        initView = initView,
                                        classLoader = classLoader,
                                    )
                                }.getOrNull()
                            if (preflight !is View) {
                                recordFailure("creator-preflight-failed")
                                return@Hooker chain.proceed()
                            }

                            val extended =
                                ArrayList<Any?>(originalList.size + 1).apply {
                                    addAll(originalList)
                                    add(bindableProxy)
                                }

                            val replaced =
                                runCatching {
                                    registryField.set(registry, extended)
                                    registryField.get(registry) === extended
                                }.getOrDefault(false)
                            if (!replaced) {
                                recordFailure("registry-list-replacement-failed")
                                return@Hooker chain.proceed()
                            }

                            markInjectedAttempt()
                            var registryRestored = false
                            try {
                                val result = chain.proceed()
                                val controller = chain.thisObject
                                if (controller != null) {
                                    rememberController(controller)
                                }
                                onEvent?.invoke(
                                    "nativeBindableShadow injected " +
                                        "slot=" + SLOT +
                                        " registryOriginal=" + originalList.size +
                                        " registryExtended=" + extended.size +
                                        " preflight=ready nativeGeometryWrites=0",
                                )
                                result
                            } finally {
                                registryRestored =
                                    runCatching {
                                        registryField.set(registry, originalList)
                                        registryField.get(registry) === originalList
                                    }.getOrDefault(false)
                                finishConstructor(
                                    registryRestored = registryRestored,
                                    creatorCalls = creatorCalls.get(),
                                    failureReason =
                                        if (registryRestored) {
                                            null
                                        } else {
                                            "registry-restore-failed"
                                        },
                                )
                                onEvent?.invoke(
                                    "nativeBindableShadow constructorComplete " +
                                        "slot=" + SLOT +
                                        " registryRestored=" + registryRestored +
                                        " creatorCalls=" + creatorCalls.get() +
                                        " nativeGeometryWrites=0",
                                )
                            }
                        },
                    )
            }.getOrElse { error ->
                return InstallResult.Failure(
                    "constructor-hook-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        constructorHook = handle
        return InstallResult.Installed
    }

    private fun createBindableIconProxy(
        classLoader: ClassLoader,
        bindableIconClass: Class<*>,
        creatorProxy: Any,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(bindableIconClass),
        ) { proxy, method, args ->
            when (method.name) {
                "getSlot" -> SLOT
                "getShouldBindIcon" -> true
                "getInitializer" -> creatorProxy
                "toString" -> "CombinedStatusBindableShadow(slot=$SLOT)"
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
        creatorCalls: AtomicInteger,
    ): Any =
        Proxy.newProxyInstance(
            classLoader,
            arrayOf(creatorClass),
        ) { proxy, method, args ->
            when (method.name) {
                "createAndBind" -> {
                    creatorCalls.incrementAndGet()
                    val context =
                        args?.firstOrNull() as? Context
                            ?: error("creator-context-missing")
                    createShadowView(
                        context = context,
                        modernViewClass = modernViewClass,
                        bindingClass = bindingClass,
                        function0Class = function0Class,
                        viewConstructor = viewConstructor,
                        initView = initView,
                        classLoader = classLoader,
                    )
                }

                "toString" -> "CombinedStatusBindableShadowCreator"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> defaultValue(method.returnType)
            }
        }

    private fun createShadowView(
        context: Context,
        modernViewClass: Class<*>,
        bindingClass: Class<*>,
        function0Class: Class<*>,
        viewConstructor: java.lang.reflect.Constructor<*>,
        initView: Method,
        classLoader: ClassLoader,
    ): Any {
        val binding =
            Proxy.newProxyInstance(
                classLoader,
                arrayOf(bindingClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "getShouldIconBeVisible" -> false
                    "isCollecting" -> true
                    "toString" -> "CombinedStatusBindableShadowBinding"
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
                    "toString" -> "CombinedStatusBindableShadowBindingFactory"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> defaultValue(method.returnType)
                }
            }

        val view =
            viewConstructor.newInstance(
                context,
                null as AttributeSet?,
            )
        check(modernViewClass.isInstance(view)) {
            "modern-view-instance-mismatch"
        }
        initView.invoke(
            view,
            SLOT,
            bindingFactory,
        )
        (view as View).visibility = View.GONE
        return view
    }

    private fun slotOfBindableIcon(icon: Any?): String? {
        if (icon == null) {
            return null
        }
        val accessor =
            icon.javaClass.methods
                .firstOrNull { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                }
                ?: return null
        return runCatching {
            accessor.invoke(icon) as? String
        }.getOrNull()
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

    @Synchronized
    private fun markInjectedAttempt() {
        state =
            state.copy(
                injectionAttempted = true,
                injected = true,
                failureReason = null,
            )
    }

    @Synchronized
    private fun rememberController(controller: Any) {
        state =
            state.copy(
                controller = WeakReference(controller),
            )
    }

    @Synchronized
    private fun finishConstructor(
        registryRestored: Boolean,
        creatorCalls: Int,
        failureReason: String?,
    ) {
        state =
            state.copy(
                registryRestored = registryRestored,
                creatorCalls = creatorCalls,
                failureReason = failureReason,
            )
    }

    @Synchronized
    private fun recordFailure(reason: String) {
        state =
            State(
                injectionAttempted = true,
                injected = false,
                registryRestored = false,
                creatorCalls = 0,
                failureReason = reason,
            )
    }

    @Synchronized
    fun validateAndCleanup(host: Any): ValidationResult {
        val snapshot = state
        if (!snapshot.injectionAttempted) {
            return ValidationResult.NotAttempted
        }
        if (!snapshot.injected) {
            return ValidationResult.Failure(
                reason = snapshot.failureReason ?: "shadow-not-injected",
                registryRestored = snapshot.registryRestored,
                creatorCalls = snapshot.creatorCalls,
            )
        }

        val resolution = NativeParticipantRuntimeAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles

                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return ValidationResult.Failure(
                        reason = resolution.reason,
                        registryRestored = snapshot.registryRestored,
                        creatorCalls = snapshot.creatorCalls,
                    )
            }

        val viewBefore = NativeParticipantRuntimeAccess.findSlotView(handles.group, SLOT)
            ?: return cleanupAfterFailure(
                handles = handles,
                reason = "shadow-view-missing",
                snapshot = snapshot,
            )
        val layout = viewBefore.layoutParams
        val mapBefore = bindableMap(handles.manager)
        val managerEntryBefore = mapBefore?.containsKey(SLOT) == true
        val iconVisible = NativeParticipantRuntimeAccess.iconVisible(viewBefore)

        val removal =
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?: return cleanupAfterFailure(
                    handles = handles,
                    reason = "shadow-removal-contract-missing",
                    snapshot = snapshot,
                )

        return runCatching {
            NativeParticipantRuntimeAccess.invokeRemoval(
                handles = handles,
                removal = removal,
                slot = SLOT,
            )
            cleanupExperimentMetadata(handles.controller)
            cleanupBindableMaps(handles.controller)

            val viewAfter =
                NativeParticipantRuntimeAccess.findSlotView(handles.group, SLOT)
            val managerEntryAfter =
                bindableMap(handles.manager)?.containsKey(SLOT) == true
            val residualSlot = hasResidualSlot(handles.controller)

            if (viewAfter != null || managerEntryAfter || residualSlot) {
                ValidationResult.Failure(
                    reason =
                        "shadow-cleanup-residual" +
                            " view=" + (viewAfter != null) +
                            " map=" + managerEntryAfter +
                            " slot=" + residualSlot,
                    registryRestored = snapshot.registryRestored,
                    creatorCalls = snapshot.creatorCalls,
                )
            } else {
                ValidationResult.Ready(
                    registryRestored = snapshot.registryRestored,
                    creatorCalls = snapshot.creatorCalls,
                    viewClass = viewBefore.javaClass.name,
                    visibility = visibilityName(viewBefore.visibility),
                    measuredWidth = viewBefore.measuredWidth,
                    measuredHeight = viewBefore.measuredHeight,
                    layoutWidth = layout?.width ?: Int.MIN_VALUE,
                    layoutHeight = layout?.height ?: Int.MIN_VALUE,
                    iconVisible = iconVisible,
                    managerEntryBefore = managerEntryBefore,
                    removalMode = removal.mode.name,
                )
            }
        }.getOrElse { error ->
            ValidationResult.Failure(
                reason =
                    "shadow-cleanup-" +
                        (error.message ?: error.javaClass.simpleName),
                registryRestored = snapshot.registryRestored,
                creatorCalls = snapshot.creatorCalls,
            )
        }
    }

    private fun cleanupAfterFailure(
        handles: NativeParticipantRuntimeAccess.Handles,
        reason: String,
        snapshot: State,
    ): ValidationResult {
        runCatching {
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?.let { removal ->
                    NativeParticipantRuntimeAccess.invokeRemoval(
                        handles = handles,
                        removal = removal,
                        slot = SLOT,
                    )
                }
            cleanupExperimentMetadata(handles.controller)
            cleanupBindableMaps(handles.controller)
        }
        return ValidationResult.Failure(
            reason = reason,
            registryRestored = snapshot.registryRestored,
            creatorCalls = snapshot.creatorCalls,
        )
    }

    @Synchronized
    fun resetRuntimeState() {
        constructorHook = null
        state = State()
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    private fun bindableMap(manager: Any): Map<*, *>? =
        readField(manager, "mBindableIcons") as? Map<*, *>

    private fun cleanupBindableMaps(controller: Any) {
        val groups = readField(controller, "mIconGroups") as? Collection<*> ?: return
        groups.forEach { manager ->
            if (manager == null) return@forEach
            val map = readField(manager, "mBindableIcons") ?: return@forEach
            runCatching {
                @Suppress("UNCHECKED_CAST")
                (map as? MutableMap<Any?, Any?>)?.remove(SLOT)
            }
        }
    }

    private fun cleanupExperimentMetadata(controller: Any) {
        val iconList = readField(controller, "mStatusBarIconList") ?: return
        removeNamedSlot(readField(iconList, "mSlots"))
        removeNamedSlot(readField(iconList, "mViewOnlySlots"))
    }

    private fun removeNamedSlot(candidate: Any?) {
        @Suppress("UNCHECKED_CAST")
        val list = candidate as? MutableList<Any?> ?: return
        val iterator = list.listIterator()
        while (iterator.hasNext()) {
            val slot = iterator.next() ?: continue
            if (readField(slot, "mName") == SLOT) {
                iterator.remove()
            }
        }
    }

    private fun hasResidualSlot(controller: Any): Boolean {
        val iconList = readField(controller, "mStatusBarIconList") ?: return false
        return listContainsSlot(readField(iconList, "mSlots")) ||
            listContainsSlot(readField(iconList, "mViewOnlySlots"))
    }

    private fun listContainsSlot(candidate: Any?): Boolean =
        ((candidate as? Collection<*>)
            ?.any { slot -> slot != null && readField(slot, "mName") == SLOT }) == true

    private fun readField(
        target: Any,
        name: String,
    ): Any? {
        val field =
            generateSequence<Class<*>>(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate -> candidate.name == name }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    private data class State(
        val injectionAttempted: Boolean = false,
        val injected: Boolean = false,
        val registryRestored: Boolean = false,
        val creatorCalls: Int = 0,
        val failureReason: String? = null,
        val controller: WeakReference<Any>? = null,
    )

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult

        data class Failure(
            val reason: String,
        ) : InstallResult
    }

    internal sealed interface ValidationResult {
        data object NotAttempted : ValidationResult

        data class Ready(
            val registryRestored: Boolean,
            val creatorCalls: Int,
            val viewClass: String,
            val visibility: String,
            val measuredWidth: Int,
            val measuredHeight: Int,
            val layoutWidth: Int,
            val layoutHeight: Int,
            val iconVisible: Boolean?,
            val managerEntryBefore: Boolean,
            val removalMode: String,
        ) : ValidationResult

        data class Failure(
            val reason: String,
            val registryRestored: Boolean,
            val creatorCalls: Int,
        ) : ValidationResult
    }
}
