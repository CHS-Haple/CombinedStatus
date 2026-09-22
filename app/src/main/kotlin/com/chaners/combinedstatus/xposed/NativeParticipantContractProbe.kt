package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal object NativeParticipantContractProbe {
    private const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"

    private val CONTROLLER_IMPL_CANDIDATES =
        listOf(
            "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
            "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
        )

    private val ICON_MANAGER_CANDIDATES =
        listOf(
            "com.android.systemui.statusbar.phone.ui.IconManager",
            "com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager",
        )

    private const val ICON_HOLDER =
        "com.android.systemui.statusbar.phone.StatusBarIconHolder"
    private const val STATUS_BAR_ICON_VIEW =
        "com.android.systemui.statusbar.StatusBarIconView"
    private const val STATUS_ICON_DISPLAYABLE =
        "com.android.systemui.statusbar.StatusIconDisplayable"

    fun inspect(host: Any): Snapshot {
        val hostView = host as? View
            ?: return Snapshot.unavailable("host-not-view")

        val statusBarView =
            generateSequence(hostView) { view -> view.parent as? View }
                .firstOrNull { view -> view.javaClass.name == PHONE_STATUS_BAR_VIEW }
                ?: return Snapshot.unavailable("phone-status-bar-view-missing")

        val classLoader = statusBarView.javaClass.classLoader
            ?: return Snapshot.unavailable("systemui-classloader-missing")

        val manager = statusBarView.readField("mDarkIconManager")
        val group = manager?.readField("mGroup") as? ViewGroup
        val controller = manager?.readField("mController")

        val controllerImpl = classOrNull(CONTROLLER_IMPL_CANDIDATES, classLoader)
        val managerClass = classOrNull(ICON_MANAGER_CANDIDATES, classLoader)
        val holderClass = classOrNull(ICON_HOLDER, classLoader)
        val iconViewClass = classOrNull(STATUS_BAR_ICON_VIEW, classLoader)
        val displayableClass = classOrNull(STATUS_ICON_DISPLAYABLE, classLoader)

        val controllerMatches =
            controller != null &&
                controllerImpl?.isInstance(controller) == true
        val managerMatches =
            manager != null &&
                managerClass?.isInstance(manager) == true
        val groupMatches =
            group != null &&
                resourceEntryName(group) == "statusIcons"

        val setIconMethods =
            controllerImpl
                ?.allMethods()
                ?.filter { method -> method.name == "setIcon" }
                ?.distinctBy(::methodSignature)
                ?.sortedBy(::methodSignature)
                ?.toList()
                .orEmpty()

        val setIconHolder =
            setIconMethods.any { method ->
                method.parameterTypes.map { type -> type.name } ==
                    listOf(
                        "java.lang.String",
                        ICON_HOLDER,
                    )
            }

        val resourceSetIconMode =
            setIconMethods
                .mapNotNull(::resourceSetIconMode)
                .firstOrNull()
                ?: ResourceSetIconMode.NONE

        val setIconVisibility =
            controllerImpl.hasMethod(
                "setIconVisibility",
                listOf(
                    "java.lang.String",
                    "boolean",
                ),
            )

        val removeMethods =
            controllerImpl
                ?.allMethods()
                ?.filter { method ->
                    method.name == "removeIcon" ||
                        method.name == "removeAllIconsForSlot"
                }
                ?.distinctBy(::methodSignature)
                ?.sortedBy(::methodSignature)
                ?.toList()
                .orEmpty()

        val removalReady =
            removeMethods.any { method ->
                val params = method.parameterTypes.map { type -> type.name }
                when (method.name) {
                    "removeAllIconsForSlot" ->
                        params == listOf("java.lang.String")
                    "removeIcon" ->
                        params == listOf("java.lang.String") ||
                            params == listOf("java.lang.String", "int")
                    else -> false
                }
            }

        val addIconGroup =
            controllerImpl.hasMethod(
                "addIconGroup",
                listOf(managerClass?.name ?: ""),
            )
        val removeIconGroup =
            controllerImpl.hasMethod(
                "removeIconGroup",
                listOf(managerClass?.name ?: ""),
            )
        val addHolder =
            managerClass.hasMethod(
                "addHolder",
                listOf(
                    "int",
                    "java.lang.String",
                    "boolean",
                    ICON_HOLDER,
                ),
            )

        val holderFactories =
            holderClass
                ?.declaredMethods
                ?.filter { method ->
                    Modifier.isStatic(method.modifiers) &&
                        holderClass.isAssignableFrom(method.returnType)
                }
                ?.sortedBy(::methodSignature)
                ?.toList()
                .orEmpty()

        val holderFactoryReady = holderFactories.isNotEmpty()

        val iconViewDisplayable =
            iconViewClass != null &&
                displayableClass?.isAssignableFrom(iconViewClass) == true

        val iconViewSlotAccessor =
            iconViewClass
                ?.allMethods()
                ?.any { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                } == true

        val systemManagedCreationReady =
            resourceSetIconMode != ResourceSetIconMode.NONE ||
                (setIconHolder && holderFactoryReady)

        val registrationContractReady =
            managerMatches &&
                groupMatches &&
                controllerMatches &&
                systemManagedCreationReady &&
                setIconVisibility &&
                removalReady &&
                iconViewDisplayable &&
                iconViewSlotAccessor

        return Snapshot(
            available = true,
            reason = null,
            managerClass = manager?.javaClass?.name,
            groupClass = group?.javaClass?.name,
            groupResource = group?.let(::resourceEntryName),
            controllerClass = controller?.javaClass?.name,
            controllerMatches = controllerMatches,
            managerMatches = managerMatches,
            groupMatches = groupMatches,
            setIconHolder = setIconHolder,
            resourceSetIconMode = resourceSetIconMode,
            setIconVisibility = setIconVisibility,
            removalReady = removalReady,
            addIconGroup = addIconGroup,
            removeIconGroup = removeIconGroup,
            addHolder = addHolder,
            holderFactoryReady = holderFactoryReady,
            iconViewDisplayable = iconViewDisplayable,
            iconViewSlotAccessor = iconViewSlotAccessor,
            systemManagedCreationReady = systemManagedCreationReady,
            registrationContractReady = registrationContractReady,
            setIconSignatures = setIconMethods.map(::methodSignature),
            removeSignatures = removeMethods.map(::methodSignature),
            holderFactorySignatures = holderFactories.map(::methodSignature),
        )
    }

    private fun resourceSetIconMode(method: Method): ResourceSetIconMode? {
        if (method.parameterCount != 3) {
            return null
        }
        val params = method.parameterTypes.map { type -> type.name }

        if (
            params[1] == "java.lang.String" &&
            params[2] == "int" &&
            !method.parameterTypes[0].isPrimitive
        ) {
            return ResourceSetIconMode.CONTENT_SLOT_RES
        }

        if (
            params[0] == "java.lang.String" &&
            params[1] == "int" &&
            !method.parameterTypes[2].isPrimitive
        ) {
            return ResourceSetIconMode.SLOT_RES_CONTENT
        }

        return null
    }

    private fun methodSignature(method: Method): String =
        method.name +
            "(" +
            method.parameterTypes.joinToString(",") { type -> type.name } +
            "):" +
            method.returnType.name

    private fun Class<*>?.hasMethod(
        name: String,
        parameterTypes: List<String>,
    ): Boolean =
        this
            ?.allMethods()
            ?.any { method ->
                method.name == name &&
                    method.parameterTypes.map { type -> type.name } == parameterTypes
            } == true

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
        candidates: List<String>,
        classLoader: ClassLoader,
    ): Class<*>? =
        candidates.firstNotNullOfOrNull { name ->
            runCatching {
                Class.forName(name, false, classLoader)
            }.getOrNull()
        }

    private fun classOrNull(
        name: String,
        classLoader: ClassLoader,
    ): Class<*>? =
        runCatching {
            Class.forName(name, false, classLoader)
        }.getOrNull()

    private fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    internal enum class ResourceSetIconMode {
        CONTENT_SLOT_RES,
        SLOT_RES_CONTENT,
        NONE,
    }

    internal data class Snapshot(
        val available: Boolean,
        val reason: String?,
        val managerClass: String?,
        val groupClass: String?,
        val groupResource: String?,
        val controllerClass: String?,
        val controllerMatches: Boolean,
        val managerMatches: Boolean,
        val groupMatches: Boolean,
        val setIconHolder: Boolean,
        val resourceSetIconMode: ResourceSetIconMode,
        val setIconVisibility: Boolean,
        val removalReady: Boolean,
        val addIconGroup: Boolean,
        val removeIconGroup: Boolean,
        val addHolder: Boolean,
        val holderFactoryReady: Boolean,
        val iconViewDisplayable: Boolean,
        val iconViewSlotAccessor: Boolean,
        val systemManagedCreationReady: Boolean,
        val registrationContractReady: Boolean,
        val setIconSignatures: List<String>,
        val removeSignatures: List<String>,
        val holderFactorySignatures: List<String>,
    ) {
        val logLine: String
            get() =
                "nativeParticipantContract available=" + available +
                    " reason=" + (reason ?: "none") +
                    " manager=" + (managerClass ?: "none") +
                    " group=" + (groupClass ?: "none") +
                    " groupRes=" + (groupResource ?: "none") +
                    " controller=" + (controllerClass ?: "none") +
                    " controllerMatches=" + controllerMatches +
                    " managerMatches=" + managerMatches +
                    " groupMatches=" + groupMatches +
                    " setIconHolder=" + setIconHolder +
                    " resourceSetIconMode=" + resourceSetIconMode.name +
                    " setIconVisibility=" + setIconVisibility +
                    " removalReady=" + removalReady +
                    " addIconGroup=" + addIconGroup +
                    " removeIconGroup=" + removeIconGroup +
                    " addHolder=" + addHolder +
                    " holderFactoryReady=" + holderFactoryReady +
                    " statusIconDisplayable=" + iconViewDisplayable +
                    " slotAccessor=" + iconViewSlotAccessor +
                    " systemManagedCreationReady=" + systemManagedCreationReady +
                    " registrationReady=" + registrationContractReady +
                    " setIconSignatures=" + setIconSignatures.joinToString("|") +
                    " removeSignatures=" + removeSignatures.joinToString("|") +
                    " holderFactories=" + holderFactorySignatures.joinToString("|") +
                    " geometryWrites=0"

        companion object {
            fun unavailable(reason: String): Snapshot =
                Snapshot(
                    available = false,
                    reason = reason,
                    managerClass = null,
                    groupClass = null,
                    groupResource = null,
                    controllerClass = null,
                    controllerMatches = false,
                    managerMatches = false,
                    groupMatches = false,
                    setIconHolder = false,
                    resourceSetIconMode = ResourceSetIconMode.NONE,
                    setIconVisibility = false,
                    removalReady = false,
                    addIconGroup = false,
                    removeIconGroup = false,
                    addHolder = false,
                    holderFactoryReady = false,
                    iconViewDisplayable = false,
                    iconViewSlotAccessor = false,
                    systemManagedCreationReady = false,
                    registrationContractReady = false,
                    setIconSignatures = emptyList(),
                    removeSignatures = emptyList(),
                    holderFactorySignatures = emptyList(),
                )
        }
    }
}
