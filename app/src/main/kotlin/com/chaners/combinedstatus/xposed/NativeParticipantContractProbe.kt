package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Field
import java.lang.reflect.Method

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

        val setIcon =
            controllerImpl.hasMethod(
                "setIcon",
                listOf(
                    "java.lang.String",
                    ICON_HOLDER,
                ),
            )
        val setIconVisibility =
            controllerImpl.hasMethod(
                "setIconVisibility",
                listOf(
                    "java.lang.String",
                    "boolean",
                ),
            )
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
        val createLayoutParams =
            managerClass.hasMethod(
                "onCreateLayoutParams",
                emptyList(),
            )
        val holderConstructor =
            holderClass?.declaredConstructors?.any { constructor ->
                constructor.parameterCount == 0
            } == true
        val iconViewConstructor =
            iconViewClass?.declaredConstructors?.any { constructor ->
                constructor.parameterTypes.map { type -> type.name } ==
                    listOf(
                        "android.content.Context",
                        "java.lang.String",
                        "com.android.systemui.statusbar.notification.ExpandedNotification",
                        "boolean",
                    )
            } == true
        val iconViewDisplayable =
            iconViewClass != null &&
                displayableClass?.isAssignableFrom(iconViewClass) == true

        val registrationContractReady =
            managerMatches &&
                groupMatches &&
                controllerMatches &&
                setIcon &&
                setIconVisibility &&
                addIconGroup &&
                removeIconGroup &&
                addHolder &&
                createLayoutParams &&
                holderConstructor &&
                iconViewConstructor &&
                iconViewDisplayable

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
            setIcon = setIcon,
            setIconVisibility = setIconVisibility,
            addIconGroup = addIconGroup,
            removeIconGroup = removeIconGroup,
            addHolder = addHolder,
            createLayoutParams = createLayoutParams,
            holderConstructor = holderConstructor,
            iconViewConstructor = iconViewConstructor,
            iconViewDisplayable = iconViewDisplayable,
            registrationContractReady = registrationContractReady,
        )
    }

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
        val setIcon: Boolean,
        val setIconVisibility: Boolean,
        val addIconGroup: Boolean,
        val removeIconGroup: Boolean,
        val addHolder: Boolean,
        val createLayoutParams: Boolean,
        val holderConstructor: Boolean,
        val iconViewConstructor: Boolean,
        val iconViewDisplayable: Boolean,
        val registrationContractReady: Boolean,
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
                    " setIcon=" + setIcon +
                    " setIconVisibility=" + setIconVisibility +
                    " addIconGroup=" + addIconGroup +
                    " removeIconGroup=" + removeIconGroup +
                    " addHolder=" + addHolder +
                    " createLayoutParams=" + createLayoutParams +
                    " holderCtor=" + holderConstructor +
                    " iconViewCtor=" + iconViewConstructor +
                    " statusIconDisplayable=" + iconViewDisplayable +
                    " registrationReady=" + registrationContractReady +
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
                    setIcon = false,
                    setIconVisibility = false,
                    addIconGroup = false,
                    removeIconGroup = false,
                    addHolder = false,
                    createLayoutParams = false,
                    holderConstructor = false,
                    iconViewConstructor = false,
                    iconViewDisplayable = false,
                    registrationContractReady = false,
                )
        }
    }
}
