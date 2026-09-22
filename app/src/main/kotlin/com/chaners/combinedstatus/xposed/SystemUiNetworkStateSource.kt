package com.chaners.combinedstatus.xposed

import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap

internal object SystemUiNetworkStateSource {
    const val WIFI_BINDER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder"
    const val WIFI_BIND_METHOD_NAME = "bind"
    const val WIFI_ICON_EMITTER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder\$bind\$1\$1\$2\$1"
    const val WIFI_ICON_EMIT_METHOD_NAME = "emit"
    const val WIFI_LOCATION_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.LocationBasedWifiViewModel"
    private const val WIFI_ICON_VISIBLE_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon\$Visible"
    private const val WIFI_ICON_HIDDEN_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon\$Hidden"
    private const val ICON_RESOURCE_CLASS_NAME =
        "com.android.systemui.common.shared.model.Icon\$Resource"
    const val HOME_WIFI_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.HomeWifiViewModel"

    const val MOBILE_BINDER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder"
    const val MOBILE_BIND_METHOD_NAME = "bind"
    const val MOBILE_LOCATION_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel"
    const val HOME_MOBILE_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.HomeMobileIconViewModel"
    const val MOBILE_ICON_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel"
    const val MOBILE_VIEW_LOGGER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger"
    const val MOBILE_SIGNAL_EMITTER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder\$bind\$1\$1\$4\$2"
    const val MOBILE_SIGNAL_EMIT_METHOD_NAME = "emit"

    const val HOOK_COUNT = 4

    private const val WIFI_BIND_HOOK_ID = "combinedstatus.network.wifi.bind"
    private const val WIFI_ICON_HOOK_ID = "combinedstatus.network.wifi.icon"
    private const val WIFI_ICON_COLLECTOR_CLASS_ID = 1
    private const val MOBILE_BIND_HOOK_ID = "combinedstatus.network.mobile.bind"
    private const val MOBILE_SIGNAL_HOOK_ID = "combinedstatus.network.mobile.signal"

    private val hookIds = setOf(
        WIFI_BIND_HOOK_ID,
        WIFI_ICON_HOOK_ID,
        MOBILE_BIND_HOOK_ID,
        MOBILE_SIGNAL_HOOK_ID,
    )

    private val wifiRoots = WeakHashMap<ViewGroup, Unit>()
    private val mobileRoots = WeakHashMap<ViewGroup, Int>()
    private val lastWifiEvents = WeakHashMap<ImageView, String>()
    private val lastMobileEvents = WeakHashMap<ImageView, String>()

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onWifiState: (CombinedStatusStateStore.WifiState) -> Unit,
        onMobileIcon: (CombinedStatusStateStore.MobileIconUpdate) -> Unit,
        onAirplaneMode: (Boolean) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val created = mutableListOf<HookHandle>()

        return try {
            val wifiBinderClass = Class.forName(WIFI_BINDER_CLASS_NAME, false, classLoader)
            val wifiLocationVmClass =
                Class.forName(WIFI_LOCATION_VIEW_MODEL_CLASS_NAME, false, classLoader)
            val continuationClass =
                Class.forName("kotlin.coroutines.Continuation", false, classLoader)
            val wifiBindMethod = wifiBinderClass.getDeclaredMethod(
                WIFI_BIND_METHOD_NAME,
                ViewGroup::class.java,
                wifiLocationVmClass,
            )
            val wifiIconEmitterClass =
                Class.forName(WIFI_ICON_EMITTER_CLASS_NAME, false, classLoader)
            val wifiIconEmitMethod = wifiIconEmitterClass.getDeclaredMethod(
                WIFI_ICON_EMIT_METHOD_NAME,
                Any::class.java,
                continuationClass,
            )
            val wifiIconImageField = wifiIconEmitterClass
                .getDeclaredField("\$iconView")
                .apply { isAccessible = true }
            val wifiIconClassIdField = wifiIconEmitterClass
                .getDeclaredField("\$r8\$classId")
                .apply { isAccessible = true }
            val wifiVisibleClass =
                Class.forName(WIFI_ICON_VISIBLE_CLASS_NAME, false, classLoader)
            val wifiVisibleIconField = wifiVisibleClass
                .getDeclaredField("icon")
                .apply { isAccessible = true }
            val iconResourceClass =
                Class.forName(ICON_RESOURCE_CLASS_NAME, false, classLoader)
            val iconResourceResIdField = iconResourceClass
                .getDeclaredField("resId")
                .apply { isAccessible = true }

            val mobileBinderClass = Class.forName(MOBILE_BINDER_CLASS_NAME, false, classLoader)
            val mobileLocationVmClass =
                Class.forName(MOBILE_LOCATION_VIEW_MODEL_CLASS_NAME, false, classLoader)
            val mobileIconVmClass =
                Class.forName(MOBILE_ICON_VIEW_MODEL_CLASS_NAME, false, classLoader)
            val mobileLoggerClass =
                Class.forName(MOBILE_VIEW_LOGGER_CLASS_NAME, false, classLoader)
            val mobileBindMethod = mobileBinderClass.getDeclaredMethod(
                MOBILE_BIND_METHOD_NAME,
                ViewGroup::class.java,
                mobileLocationVmClass,
                mobileIconVmClass,
                mobileLoggerClass,
            )

            val mobileSignalEmitterClass =
                Class.forName(MOBILE_SIGNAL_EMITTER_CLASS_NAME, false, classLoader)
            val mobileSignalEmitMethod = mobileSignalEmitterClass.getDeclaredMethod(
                MOBILE_SIGNAL_EMIT_METHOD_NAME,
                Any::class.java,
                continuationClass,
            )
            val mobileImageField = mobileSignalEmitterClass
                .getDeclaredField("\$mobile")
                .apply { isAccessible = true }
            val mobileClassIdField = mobileSignalEmitterClass
                .getDeclaredField("\$r8\$classId")
                .apply { isAccessible = true }
            val subscriptionIdMethod = mobileLocationVmClass
                .getDeclaredMethod("getSubscriptionId")
                .apply { isAccessible = true }

            created += module
                .hook(wifiBindMethod)
                .setId(WIFI_BIND_HOOK_ID)
                .intercept(wifiBindHooker(onEvent))
            created += module
                .hook(wifiIconEmitMethod)
                .setId(WIFI_ICON_HOOK_ID)
                .intercept(
                    wifiIconHooker(
                        wifiImageField = wifiIconImageField,
                        wifiClassIdField = wifiIconClassIdField,
                        wifiVisibleIconField = wifiVisibleIconField,
                        iconResourceResIdField = iconResourceResIdField,
                        onWifiState = onWifiState,
                        onEvent = onEvent,
                    ),
                )
            created += module
                .hook(mobileBindMethod)
                .setId(MOBILE_BIND_HOOK_ID)
                .intercept(mobileBindHooker(subscriptionIdMethod, onEvent))
            created += module
                .hook(mobileSignalEmitMethod)
                .setId(MOBILE_SIGNAL_HOOK_ID)
                .intercept(
                    mobileSignalHooker(
                        mobileImageField = mobileImageField,
                        mobileClassIdField = mobileClassIdField,
                        onMobileIcon = onMobileIcon,
                        onAirplaneMode = onAirplaneMode,
                        onEvent = onEvent,
                    ),
                )

            created.toList()
        } catch (error: Throwable) {
            created.forEach { handle ->
                runCatching { handle.unhook() }
            }
            throw error
        }
    }

    fun matches(handle: HookHandle): Boolean = handle.id in hookIds

    private fun wifiBindHooker(
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val root = chain.getArg(0) as? ViewGroup
        val viewModel = chain.getArg(1)

        if (
            root != null &&
            viewModel?.javaClass?.name == HOME_WIFI_VIEW_MODEL_CLASS_NAME
        ) {
            val firstBinding = synchronized(this) {
                wifiRoots.put(root, Unit) == null
            }
            if (firstBinding) {
                onEvent?.invoke(
                    "networkPipeline wifi bound " +
                        "stage=beforeProceed " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " vm=" + viewModel.javaClass.simpleName,
                )
            }
        }

        chain.proceed()
    }

    private fun wifiIconHooker(
        wifiImageField: Field,
        wifiClassIdField: Field,
        wifiVisibleIconField: Field,
        iconResourceResIdField: Field,
        onWifiState: (CombinedStatusStateStore.WifiState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val emitter = chain.thisObject
        val classId = runCatching {
            wifiClassIdField.getInt(emitter)
        }.getOrDefault(-1)

        if (classId != WIFI_ICON_COLLECTOR_CLASS_ID) {
            return@Hooker chain.proceed()
        }

        val image = runCatching {
            wifiImageField.get(emitter) as? ImageView
        }.getOrNull()
        val value = chain.getArg(0)

        if (image == null || !findWifiBinding(image)) {
            return@Hooker chain.proceed()
        }

        val semanticState =
            when (value?.javaClass?.name) {
                WIFI_ICON_VISIBLE_CLASS_NAME -> {
                    val resId =
                        runCatching {
                            val icon = wifiVisibleIconField.get(value)
                            iconResourceResIdField.getInt(icon)
                        }.getOrDefault(0)
                            .takeIf { it != 0 }
                    val resourceName = resId?.let { id -> resourceName(image, id) }
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = resId,
                        signal = SystemUiSignalParser.wifi(resourceName),
                    )
                }

                WIFI_ICON_HIDDEN_CLASS_NAME ->
                    CombinedStatusStateStore.WifiState.Hidden

                else -> null
            }

        val eventKey =
            when (semanticState) {
                null -> "unknown:" + (value?.javaClass?.name ?: "null")
                CombinedStatusStateStore.WifiState.Hidden -> "hidden"
                is CombinedStatusStateStore.WifiState.Visible ->
                    "visible:" + (semanticState.iconResId ?: 0)
                CombinedStatusStateStore.WifiState.Unknown -> "unknown"
            }
        val changed =
            semanticState != null &&
                synchronized(this) {
                    lastWifiEvents.put(image, eventKey) != eventKey
                }

        if (changed) {
            onWifiState(requireNotNull(semanticState))
        }

        val result = chain.proceed()

        if (changed) {
            val appliedTaggedResId = (image.tag as? Number)?.toInt()
            val semanticResId =
                (semanticState as? CombinedStatusStateStore.WifiState.Visible)
                    ?.iconResId
            val semanticResourceName =
                semanticResId?.let { id -> resourceName(image, id) }

            onEvent?.invoke(
                "networkPipeline wifi iconEvent " +
                    "phase=beforeProceed " +
                    "viewId=" + resourceId(image) +
                    " classId=" + classId +
                    " valueType=" + (value?.javaClass?.simpleName ?: "null") +
                    " visibility=" + visibilityName(image.visibility) +
                    " semanticResId=" + (semanticResId ?: 0) +
                    " appliedTaggedResId=" + (appliedTaggedResId ?: 0) +
                    " resource=" + (semanticResourceName ?: "n/a"),
            )
        }

        result
    }

    private fun mobileBindHooker(
        subscriptionIdMethod: Method,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val root = chain.getArg(0) as? ViewGroup
        val locationViewModel = chain.getArg(1)
        val iconViewModel = chain.getArg(2)
        var bindingLog: String? = null

        if (
            root != null &&
            locationViewModel?.javaClass?.name == HOME_MOBILE_VIEW_MODEL_CLASS_NAME
        ) {
            val subscriptionId = runCatching {
                (subscriptionIdMethod.invoke(locationViewModel) as Number).toInt()
            }.getOrDefault(-1)

            val previous = synchronized(this) {
                mobileRoots.put(root, subscriptionId)
            }
            if (previous == null || previous != subscriptionId) {
                bindingLog =
                    "networkPipeline mobile bound " +
                        "stage=beforeProceed " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " locationVm=" + locationViewModel.javaClass.simpleName +
                        " subId=" + subscriptionId +
                        " iconVm=" + (iconViewModel?.javaClass?.simpleName ?: "none")
            }
        }

        val result = chain.proceed()
        bindingLog?.let { onEvent?.invoke(it) }
        result
    }

    private fun mobileSignalHooker(
        mobileImageField: Field,
        mobileClassIdField: Field,
        onMobileIcon: (CombinedStatusStateStore.MobileIconUpdate) -> Unit,
        onAirplaneMode: (Boolean) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Hooker = Hooker { chain ->
        val emitter = chain.thisObject
        val image = runCatching {
            mobileImageField.get(emitter) as? ImageView
        }.getOrNull()
        val classId = runCatching {
            mobileClassIdField.getInt(emitter)
        }.getOrDefault(-1)
        val value = chain.getArg(0)

        var eventLog: String? = null
        if (image != null) {
            val subscriptionId = findMobileSubscription(image)
            if (subscriptionId != null) {
                val valueText = when (value) {
                    is Number -> value.toLong().toString()
                    null -> "null"
                    else -> value.javaClass.simpleName
                }
                val eventKey = classId.toString() + ":" + valueText
                val changed = synchronized(this) {
                    lastMobileEvents.put(image, eventKey) != eventKey
                }

                if (changed) {
                    val resourceId = (value as? Number)
                        ?.toInt()
                        ?.takeIf { it != 0 }
                    val resourceName = resourceId?.let { id -> resourceName(image, id) }
                    val kind = when (classId) {
                        0 -> CombinedStatusStateStore.MobileIconKind.SIGNAL
                        1 -> CombinedStatusStateStore.MobileIconKind.VOLTE
                        2 -> CombinedStatusStateStore.MobileIconKind.VOWIFI
                        else -> null
                    }
                    if (kind == CombinedStatusStateStore.MobileIconKind.SIGNAL) {
                        val airplaneMode =
                            Settings.Global.getInt(
                                image.context.contentResolver,
                                Settings.Global.AIRPLANE_MODE_ON,
                                0,
                            ) != 0
                        onAirplaneMode(airplaneMode)
                    }
                    if (kind != null) {
                        onMobileIcon(
                            CombinedStatusStateStore.MobileIconUpdate(
                                subscriptionId = subscriptionId,
                                kind = kind,
                                resourceId = resourceId,
                                signal = if (
                                    kind == CombinedStatusStateStore.MobileIconKind.SIGNAL
                                ) {
                                    SystemUiSignalParser.mobile(resourceName)
                                } else {
                                    null
                                },
                            ),
                        )
                    }

                    eventLog =
                        "networkPipeline mobile iconEvent " +
                            "phase=beforeProceed " +
                            "subId=" + subscriptionId +
                            " viewId=" + resourceId(image) +
                            " classId=" + classId +
                            " value=" + valueText +
                            " resource=" + (resourceName ?: "n/a")
                }
            }
        }

        val result = chain.proceed()
        eventLog?.let { onEvent?.invoke(it) }
        result
    }

    @Synchronized
    private fun findWifiBinding(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            val group = current as? ViewGroup
            if (group != null && wifiRoots.containsKey(group)) {
                return true
            }
            current = current.parent as? View
        }
        return false
    }

    @Synchronized
    private fun findMobileSubscription(view: View): Int? {
        var current: View? = view
        while (current != null) {
            val group = current as? ViewGroup
            if (group != null && mobileRoots.containsKey(group)) {
                return mobileRoots[group]
            }
            current = current.parent as? View
        }
        return null
    }

    private fun visibilityName(visibility: Int): String = when (visibility) {
        View.VISIBLE -> "VISIBLE"
        View.INVISIBLE -> "INVISIBLE"
        View.GONE -> "GONE"
        else -> visibility.toString()
    }

    private fun resourceId(view: View): String {
        if (view.id == View.NO_ID) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(view.id)
        }.getOrElse {
            view.id.toString()
        }
    }

    private fun resourceName(
        view: View,
        resId: Int,
    ): String {
        if (resId == 0) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(resId)
        }.getOrElse {
            resId.toString()
        }
    }
}
