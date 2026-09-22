package com.chaners.combinedstatus.xposed

import android.graphics.drawable.Icon
import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method

internal object NativeParticipantRuntimeAccess {
    const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    const val ICON_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder"
    const val STATUS_BAR_ICON_VIEW =
        "com.android.systemui.statusbar.StatusBarIconView"
    const val STATUS_ICON_DISPLAYABLE =
        "com.android.systemui.statusbar.StatusIconDisplayable"

    fun resolve(host: Any): ResolveResult {
        val hostView = host as? View
            ?: return ResolveResult.Failure("host-not-view")

        val statusBarView =
            generateSequence(hostView) { view -> view.parent as? View }
                .firstOrNull { view -> view.javaClass.name == PHONE_STATUS_BAR_VIEW }
                ?: return ResolveResult.Failure("phone-status-bar-view-missing")

        val classLoader = statusBarView.javaClass.classLoader
            ?: return ResolveResult.Failure("systemui-classloader-missing")
        val manager = statusBarView.readField("mDarkIconManager")
            ?: return ResolveResult.Failure("dark-icon-manager-missing")
        val group = manager.readField("mGroup") as? ViewGroup
            ?: return ResolveResult.Failure("status-icon-group-missing")
        val controller = manager.readField("mController")
            ?: return ResolveResult.Failure("status-icon-controller-missing")

        return ResolveResult.Ready(
            Handles(
                statusBarView = statusBarView,
                manager = manager,
                group = group,
                controller = controller,
                classLoader = classLoader,
                holderClass = classOrNull(ICON_HOLDER, classLoader),
                iconViewClass = classOrNull(STATUS_BAR_ICON_VIEW, classLoader),
                displayableClass = classOrNull(STATUS_ICON_DISPLAYABLE, classLoader),
            ),
        )
    }

    fun resourceSetter(controllerClass: Class<*>): ResourceSetter? =
        controllerClass
            .allMethods()
            .filter { method -> method.name == "setIcon" }
            .mapNotNull { method ->
                classifyResourceSetIcon(method)?.let { mode ->
                    ResourceSetter(method = method, mode = mode)
                }
            }
            .firstOrNull()

    fun setIconHolderAvailable(controllerClass: Class<*>): Boolean =
        controllerClass.hasMethod(
            name = "setIcon",
            parameterTypes =
                listOf(
                    "java.lang.String",
                    ICON_HOLDER,
                ),
        )

    fun visibilityMethod(controllerClass: Class<*>): Method? =
        controllerClass.findMethod(
            name = "setIconVisibility",
            parameterTypes =
                listOf(
                    "java.lang.String",
                    "boolean",
                ),
        )

    fun removal(controllerClass: Class<*>): Removal? {
        val candidates =
            controllerClass
                .allMethods()
                .mapNotNull { method ->
                    classifyRemoval(method)?.let { mode ->
                        Removal(method = method, mode = mode)
                    }
                }
                .toList()

        return candidates.minByOrNull { candidate ->
            when (candidate.mode) {
                RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG -> 0
                RemovalMode.REMOVE_ALL_SLOT -> 1
                RemovalMode.REMOVE_TAGGED -> 2
                RemovalMode.REMOVE_SLOT -> 3
            }
        }
    }

    fun classifyResourceSetIcon(method: Method): ResourceSetIconMode? {
        if (method.name != "setIcon" || method.parameterCount != 3) {
            return null
        }
        val params = method.parameterTypes
        val intType = Int::class.javaPrimitiveType
        val firstIsText = CharSequence::class.java.isAssignableFrom(params[0])
        val thirdIsText = CharSequence::class.java.isAssignableFrom(params[2])

        return when {
            firstIsText &&
                params[1] == String::class.java &&
                params[2] == intType ->
                ResourceSetIconMode.CONTENT_SLOT_RES

            params[0] == String::class.java &&
                params[1] == intType &&
                thirdIsText ->
                ResourceSetIconMode.SLOT_RES_CONTENT

            else -> null
        }
    }

    fun classifyRemoval(method: Method): RemovalMode? {
        val params = method.parameterTypes.map { type -> type.name }
        return when {
            method.name == "removeAllIconsForSlot" &&
                params == listOf("java.lang.String", "boolean") ->
                RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG

            method.name == "removeAllIconsForSlot" &&
                params == listOf("java.lang.String") ->
                RemovalMode.REMOVE_ALL_SLOT

            method.name == "removeIcon" &&
                params == listOf("java.lang.String", "int") ->
                RemovalMode.REMOVE_TAGGED

            method.name == "removeIcon" &&
                params == listOf("java.lang.String") ->
                RemovalMode.REMOVE_SLOT

            else -> null
        }
    }

    fun invokeCreate(
        handles: Handles,
        setter: ResourceSetter,
        slot: String,
        resourceId: Int,
        contentDescription: CharSequence,
    ) {
        setter.method.isAccessible = true
        when (setter.mode) {
            ResourceSetIconMode.CONTENT_SLOT_RES ->
                setter.method.invoke(
                    handles.controller,
                    contentDescription,
                    slot,
                    resourceId,
                )

            ResourceSetIconMode.SLOT_RES_CONTENT ->
                setter.method.invoke(
                    handles.controller,
                    slot,
                    resourceId,
                    contentDescription,
                )
        }
    }

    fun invokeVisibility(
        handles: Handles,
        method: Method,
        slot: String,
        visible: Boolean,
    ) {
        method.isAccessible = true
        method.invoke(handles.controller, slot, visible)
    }

    fun invokeRemoval(
        handles: Handles,
        removal: Removal,
        slot: String,
    ) {
        removal.method.isAccessible = true
        when (removal.mode) {
            RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                    false,
                )

            RemovalMode.REMOVE_ALL_SLOT ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                )

            RemovalMode.REMOVE_TAGGED ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                    0,
                )

            RemovalMode.REMOVE_SLOT ->
                removal.method.invoke(
                    handles.controller,
                    slot,
                )
        }
    }

    fun findSlotView(
        group: ViewGroup,
        slot: String,
    ): View? {
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (slotOf(child) == slot) {
                return child
            }
        }
        return null
    }

    fun slotOf(view: View): String? {
        val getSlot =
            view.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                }
                ?: return null

        return runCatching {
            getSlot.isAccessible = true
            getSlot.invoke(view) as? String
        }.getOrNull()
    }

    fun findBootstrapResource(group: ViewGroup): BootstrapResource? {
        val expectedPackage = group.context.packageName
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val statusBarIcon = child.readField("mIcon") ?: continue
            val icon = statusBarIcon.readField("icon") as? Icon ?: continue
            if (icon.type != Icon.TYPE_RESOURCE) {
                continue
            }
            val resourceId = runCatching { icon.resId }.getOrDefault(0)
            if (resourceId == 0) {
                continue
            }
            val resourcePackage =
                runCatching {
                    group.resources.getResourcePackageName(resourceId)
                }.getOrNull()
            if (resourcePackage != expectedPackage) {
                continue
            }
            return BootstrapResource(
                resourceId = resourceId,
                resourcePackage = resourcePackage,
                sourceSlot = slotOf(child),
                sourceIndex = index,
            )
        }
        return null
    }

    fun methodSignatures(
        clazz: Class<*>,
        names: Set<String>,
    ): List<String> =
        clazz
            .allMethods()
            .filter { method -> method.name in names }
            .distinctBy(::methodSignature)
            .map(::methodSignature)
            .sorted()
            .toList()

    fun holderFactories(holderClass: Class<*>?): List<Method> {
        val clazz = holderClass ?: return emptyList()
        return clazz
            .declaredMethods
            .filter { method ->
                java.lang.reflect.Modifier.isStatic(method.modifiers) &&
                    clazz.isAssignableFrom(method.returnType)
            }
            .sortedBy(::methodSignature)
            .toList()
    }

    fun hasMethodSignature(
        clazz: Class<*>?,
        name: String,
        parameterTypes: List<String>,
    ): Boolean =
        clazz?.hasMethod(name, parameterTypes) == true

    fun methodSignature(method: Method): String =
        method.name +
            "(" +
            method.parameterTypes.joinToString(",") { type -> type.name } +
            "):" +
            method.returnType.name

    fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Class<*>.findMethod(
        name: String,
        parameterTypes: List<String>,
    ): Method? =
        allMethods().firstOrNull { method ->
            method.name == name &&
                method.parameterTypes.map { type -> type.name } == parameterTypes
        }

    private fun Class<*>.hasMethod(
        name: String,
        parameterTypes: List<String>,
    ): Boolean = findMethod(name, parameterTypes) != null

    private fun Class<*>.allMethods(): Sequence<Method> =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }

    private fun Any.readField(name: String): Any? {
        val field =
            generateSequence(javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz -> clazz.declaredFields.firstOrNull { it.name == name } }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(this)
        }.getOrNull()
    }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    internal data class Handles(
        val statusBarView: View,
        val manager: Any,
        val group: ViewGroup,
        val controller: Any,
        val classLoader: ClassLoader,
        val holderClass: Class<*>?,
        val iconViewClass: Class<*>?,
        val displayableClass: Class<*>?,
    )

    internal sealed interface ResolveResult {
        data class Ready(
            val handles: Handles,
        ) : ResolveResult

        data class Failure(
            val reason: String,
        ) : ResolveResult
    }

    internal data class ResourceSetter(
        val method: Method,
        val mode: ResourceSetIconMode,
    )

    internal data class Removal(
        val method: Method,
        val mode: RemovalMode,
    )

    internal data class BootstrapResource(
        val resourceId: Int,
        val resourcePackage: String,
        val sourceSlot: String?,
        val sourceIndex: Int,
    )

    internal enum class ResourceSetIconMode {
        CONTENT_SLOT_RES,
        SLOT_RES_CONTENT,
    }

    internal enum class RemovalMode {
        REMOVE_ALL_SLOT_PIPELINE_FLAG,
        REMOVE_ALL_SLOT,
        REMOVE_TAGGED,
        REMOVE_SLOT,
    }
}
