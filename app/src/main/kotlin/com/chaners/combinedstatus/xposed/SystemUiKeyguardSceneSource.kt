package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field

internal object SystemUiKeyguardSceneSource {
    const val HOOK_COUNT = 5

    private const val KEYGUARD_CLASS =
        "com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView"
    private const val KEYGUARD_BASE_CLASS =
        "com.android.systemui.statusbar.phone.KeyguardStatusBarView"
    private const val AOD_CONTROLLER_CLASS =
        "com.android.systemui.statusbar.phone.KeyguardStatusBarViewControllerInject"
    private const val CC_FAKE_CONTROLLER_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterFakeViewController"
    private const val BATTERY_VIEW_CLASS =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private const val ATTACH_HOOK_ID = "combinedstatus.keyguard.scene.attach"
    private const val DETACH_HOOK_ID = "combinedstatus.keyguard.scene.detach"
    private const val VISIBILITY_HOOK_ID = "combinedstatus.keyguard.scene.visibility"
    private const val TINT_HOOK_ID = "combinedstatus.keyguard.scene.tint"
    private const val AOD_HOOK_ID = "combinedstatus.keyguard.scene.full-aod"

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { true },
    ): List<HookHandle> {
        val keyguardClass = Class.forName(KEYGUARD_CLASS, false, classLoader)
        val keyguardBaseClass = Class.forName(KEYGUARD_BASE_CLASS, false, classLoader)
        val aodControllerClass = Class.forName(AOD_CONTROLLER_CLASS, false, classLoader)
        val ccFakeControllerClass = Class.forName(CC_FAKE_CONTROLLER_CLASS, false, classLoader)
        val batteryViewClass = Class.forName(BATTERY_VIEW_CLASS, false, classLoader)

        val contract =
            Contract(
                systemIconsField = requireField(keyguardClass, "mSystemIconsContainer"),
                statusIconsField = requireField(keyguardClass, "mStatusIconContainer"),
                batteryField = requireField(keyguardClass, "mBatteryView"),
                lightWallpaperField = requireField(keyguardClass, "mLightLockScreenWallpaper"),
                toLockScreenField = requireField(keyguardClass, "mToLockScreen"),
                dependencyField = requireField(keyguardClass, "mDep"),
                ccFakeField =
                    requireField(
                        requireField(keyguardClass, "mDep").type,
                        "ccFake",
                    ),
                realSystemIconsField = requireField(ccFakeControllerClass, "realSystemIcons"),
                aodKeyguardViewField =
                    requireField(aodControllerClass, "keyguardStatusBarView"),
                batteryAodAnimateField = optionalField(batteryViewClass, "mIsAodAnimate"),
                batteryToAodField = optionalField(batteryViewClass, "mToAod"),
            )

        val attachMethod =
            keyguardClass
                .getDeclaredMethod("onAttachedToWindow")
                .apply { isAccessible = true }
        val detachMethod =
            keyguardClass
                .getDeclaredMethod("onDetachedFromWindow")
                .apply { isAccessible = true }
        val tintMethod =
            keyguardClass
                .getDeclaredMethod("updateIconsAndTextColors")
                .apply { isAccessible = true }
        val visibilityMethod =
            keyguardBaseClass
                .getDeclaredMethod(
                    "setVisibility",
                    Int::class.javaPrimitiveType,
                ).apply { isAccessible = true }
        val fullAodMethod =
            aodControllerClass
                .getDeclaredMethod(
                    "animateFullAod",
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                ).apply { isAccessible = true }

        fun emit(
            source: String,
            candidate: Any?,
            extra: String = "",
        ) {
            if (onEvent == null || !isProbeEnabled()) return
            val host = candidate as? ViewGroup ?: return
            if (!isKeyguardHostClassName(host.javaClass.name)) return
            onEvent(
                "keyguardScene source=" + source +
                    if (extra.isEmpty()) " " else " " + extra + " " +
                    contract.snapshot(host) +
                    " readOnly=true nativeGeometryWrites=0",
            )
        }

        val handles = ArrayList<HookHandle>(HOOK_COUNT)
        handles +=
            module
                .hook(attachMethod)
                .setId(ATTACH_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        emit("attached", chain.thisObject)
                        result
                    },
                )
        handles +=
            module
                .hook(detachMethod)
                .setId(DETACH_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        emit("detached", chain.thisObject)
                        result
                    },
                )
        handles +=
            module
                .hook(visibilityMethod)
                .setId(VISIBILITY_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val requested = chain.getArg(0) as? Int
                        val result = chain.proceed()
                        emit(
                            source = "visibility",
                            candidate = chain.thisObject,
                            extra = "requested=" + (requested ?: "unknown"),
                        )
                        result
                    },
                )
        handles +=
            module
                .hook(tintMethod)
                .setId(TINT_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        emit("tint", chain.thisObject)
                        result
                    },
                )
        handles +=
            module
                .hook(fullAodMethod)
                .setId(AOD_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val toLock = chain.getArg(0) as? Boolean
                        val animate = chain.getArg(1) as? Boolean
                        val result = chain.proceed()
                        val host =
                            runCatching {
                                contract.aodKeyguardViewField.get(chain.thisObject)
                            }.getOrNull()
                        emit(
                            source = "full-aod",
                            candidate = host,
                            extra =
                                "toLock=" + (toLock ?: "unknown") +
                                    " animate=" + (animate ?: "unknown"),
                        )
                        result
                    },
                )
        return handles
    }

    internal fun isKeyguardHostClassName(className: String): Boolean =
        className == KEYGUARD_CLASS

    private data class Contract(
        val systemIconsField: Field,
        val statusIconsField: Field,
        val batteryField: Field,
        val lightWallpaperField: Field,
        val toLockScreenField: Field,
        val dependencyField: Field,
        val ccFakeField: Field,
        val realSystemIconsField: Field,
        val aodKeyguardViewField: Field,
        val batteryAodAnimateField: Field?,
        val batteryToAodField: Field?,
    ) {
        fun snapshot(host: ViewGroup): String {
            val systemIcons =
                runCatching { systemIconsField.get(host) as? View }.getOrNull()
            val statusIcons =
                runCatching { statusIconsField.get(host) as? View }.getOrNull()
            val battery =
                runCatching { batteryField.get(host) as? View }.getOrNull()
            val lightWallpaper =
                runCatching { lightWallpaperField.getBoolean(host) }.getOrNull()
            val toLockScreen =
                runCatching { toLockScreenField.getBoolean(host) }.getOrNull()
            val dependency =
                runCatching { dependencyField.get(host) }.getOrNull()
            val ccFake =
                dependency?.let { dep ->
                    runCatching { ccFakeField.get(dep) }.getOrNull()
                }
            val realSystemIcons =
                ccFake?.let { controller ->
                    runCatching { realSystemIconsField.get(controller) }.getOrNull()
                }
            val batteryAodAnimate =
                if (battery != null && batteryAodAnimateField != null) {
                    runCatching { batteryAodAnimateField.getBoolean(battery) }.getOrNull()
                } else {
                    null
                }
            val batteryToAod =
                if (battery != null && batteryToAodField != null) {
                    runCatching { batteryToAodField.getBoolean(battery) }.getOrNull()
                } else {
                    null
                }

            return "root=" + viewSummary(host) +
                " systemIcons=" + viewSummary(systemIcons) +
                " statusIcons=" + viewSummary(statusIcons) +
                " battery=" + viewSummary(battery) +
                " toLock=" + (toLockScreen ?: "unknown") +
                " lightWallpaper=" + (lightWallpaper ?: "unknown") +
                " aodAnimate=" + (batteryAodAnimate ?: "unknown") +
                " toAod=" + (batteryToAod ?: "unknown") +
                " selectedAsRealSystemIcons=" + (systemIcons != null && realSystemIcons === systemIcons)
        }
    }

    private fun requireField(
        type: Class<*>,
        name: String,
    ): Field {
        var current: Class<*>? = type
        while (current != null) {
            runCatching {
                return current
                    .getDeclaredField(name)
                    .apply { isAccessible = true }
            }
            current = current.superclass
        }
        error("Missing field " + type.name + "." + name)
    }

    private fun optionalField(
        type: Class<*>,
        name: String,
    ): Field? =
        runCatching {
            requireField(type, name)
        }.getOrNull()

    private fun viewSummary(view: View?): String {
        if (view == null) return "missing"
        val location = IntArray(2)
        val locationReady =
            runCatching {
                view.getLocationOnScreen(location)
                true
            }.getOrDefault(false)
        return view.javaClass.simpleName +
            "{" +
            "attached=" + view.isAttachedToWindow +
            ",v=" + view.visibility +
            ",a=" + view.alpha +
            ",x=" + if (locationReady) location[0] else "unknown" +
            ",y=" + if (locationReady) location[1] else "unknown" +
            ",w=" + view.width +
            ",h=" + view.height +
            ",mw=" + view.measuredWidth +
            ",mh=" + view.measuredHeight +
            ",tx=" + view.translationX +
            "}"
    }
}
