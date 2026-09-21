package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap

internal object SystemUiNetworkPipelineProbe {
    const val WIFI_BINDER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.binder.MiuiWifiViewBinder"
    const val WIFI_BIND_METHOD_NAME = "bind"
    const val WIFI_ICON_METHOD_NAME = "setImageViewResId"
    const val WIFI_LOCATION_VIEW_MODEL_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.LocationBasedWifiViewModel"
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
    private val lastWifiResources = WeakHashMap<ImageView, Int>()
    private val lastMobileEvents = WeakHashMap<ImageView, String>()

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: (String) -> Unit,
    ): List<HookHandle> {
        val created = mutableListOf<HookHandle>()

        return try {
            val wifiBinderClass = Class.forName(WIFI_BINDER_CLASS_NAME, false, classLoader)
            val wifiLocationVmClass =
                Class.forName(WIFI_LOCATION_VIEW_MODEL_CLASS_NAME, false, classLoader)
            val tripleClass = Class.forName("kotlin.Triple", false, classLoader)
            val wifiBindMethod = wifiBinderClass.getDeclaredMethod(
                WIFI_BIND_METHOD_NAME,
                ViewGroup::class.java,
                wifiLocationVmClass,
            )
            val wifiIconMethod = wifiBinderClass.getDeclaredMethod(
                WIFI_ICON_METHOD_NAME,
                ImageView::class.java,
                Int::class.javaPrimitiveType,
                tripleClass,
            )

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

            val continuationClass =
                Class.forName("kotlin.coroutines.Continuation", false, classLoader)
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
                .hook(wifiIconMethod)
                .setId(WIFI_ICON_HOOK_ID)
                .intercept(wifiIconHooker(onEvent))
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
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()
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
                onEvent(
                    "networkPipeline wifi bound " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " vm=" + viewModel.javaClass.simpleName,
                )
            }
        }

        result
    }

    private fun wifiIconHooker(
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()
        val image = chain.getArg(0) as? ImageView
        val resId = (chain.getArg(1) as? Number)?.toInt()

        if (image != null && resId != null && findWifiBinding(image)) {
            val changed = synchronized(this) {
                lastWifiResources.put(image, resId) != resId
            }
            if (changed) {
                onEvent(
                    "networkPipeline wifi icon " +
                        "viewId=" + resourceId(image) +
                        " resId=" + resId +
                        " resource=" + resourceName(image, resId),
                )
            }
        }

        result
    }

    private fun mobileBindHooker(
        subscriptionIdMethod: Method,
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()
        val root = chain.getArg(0) as? ViewGroup
        val locationViewModel = chain.getArg(1)
        val iconViewModel = chain.getArg(2)

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
                onEvent(
                    "networkPipeline mobile bound " +
                        "root=" + root.javaClass.simpleName +
                        " rootId=" + resourceId(root) +
                        " locationVm=" + locationViewModel.javaClass.simpleName +
                        " subId=" + subscriptionId +
                        " iconVm=" + (iconViewModel?.javaClass?.simpleName ?: "none"),
                )
            }
        }

        result
    }

    private fun mobileSignalHooker(
        mobileImageField: Field,
        mobileClassIdField: Field,
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()
        val emitter = chain.thisObject
        val image = runCatching {
            mobileImageField.get(emitter) as? ImageView
        }.getOrNull()
        val classId = runCatching {
            mobileClassIdField.getInt(emitter)
        }.getOrDefault(-1)
        val value = chain.getArg(0)

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
                    val valueResource = (value as? Number)
                        ?.toInt()
                        ?.let { id -> resourceName(image, id) }
                        ?: "n/a"
                    onEvent(
                        "networkPipeline mobile iconEvent " +
                            "subId=" + subscriptionId +
                            " viewId=" + resourceId(image) +
                            " classId=" + classId +
                            " value=" + valueText +
                            " resource=" + valueResource,
                    )
                }
            }
        }

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
