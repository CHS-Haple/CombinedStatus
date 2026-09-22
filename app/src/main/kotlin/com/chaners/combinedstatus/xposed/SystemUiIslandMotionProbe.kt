package com.chaners.combinedstatus.xposed

import android.os.SystemClock
import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.abs

internal object SystemUiIslandMotionProbe {
    const val HOOK_COUNT = 2

    private const val ISLAND_CONTROLLER_CLASS =
        "com.android.systemui.statusbar.StatusBarIslandControllerImpl"
    private const val STRETCH_ANIMATION_CLASS =
        "com.android.systemui.statusbar.phone.IslandStretchAnimation"
    private const val REFRESH_HOOK_ID = "combinedstatus.island.refresh"
    private const val STRETCH_HOOK_ID = "combinedstatus.island.stretch"

    private var lastControllerTranslation: Int? = null
    private var lastStretchInput: Float? = null
    private var burstStartUptimeMs = 0L
    private var burstSamples = 0

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: (String) -> Unit,
    ): List<HookHandle> {
        val controllerClass =
            Class.forName(ISLAND_CONTROLLER_CLASS, false, classLoader)
        val refreshMethod =
            controllerClass.getDeclaredMethod("refreshTranslation")
                .apply { isAccessible = true }
        val getTranslationX =
            controllerClass.getDeclaredMethod("getTranslationX")
                .apply { isAccessible = true }
        val getBatteryWidth =
            controllerClass.getDeclaredMethod("getBatteryWidth")
                .apply { isAccessible = true }

        val stretchClass =
            Class.forName(STRETCH_ANIMATION_CLASS, false, classLoader)
        val setTranslationMethod =
            stretchClass.getDeclaredMethod(
                "setTranslationX",
                Float::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val leftField =
            stretchClass.getDeclaredField("leftContainer")
                .apply { isAccessible = true }
        val rightField =
            stretchClass.getDeclaredField("rightContainer")
                .apply { isAccessible = true }
        val privacyField =
            stretchClass.getDeclaredField("privacyArea")
                .apply { isAccessible = true }

        val refresh =
            module
                .hook(refreshMethod)
                .setId(REFRESH_HOOK_ID)
                .intercept(
                    refreshHooker(
                        getTranslationX = getTranslationX,
                        getBatteryWidth = getBatteryWidth,
                        onEvent = onEvent,
                    ),
                )
        val stretch =
            module
                .hook(setTranslationMethod)
                .setId(STRETCH_HOOK_ID)
                .intercept(
                    stretchHooker(
                        leftField = leftField,
                        rightField = rightField,
                        privacyField = privacyField,
                        onEvent = onEvent,
                    ),
                )
        return listOf(refresh, stretch)
    }

    fun matches(handle: HookHandle): Boolean =
        handle.id == REFRESH_HOOK_ID || handle.id == STRETCH_HOOK_ID

    private fun refreshHooker(
        getTranslationX: Method,
        getBatteryWidth: Method,
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val result = chain.proceed()
        val controller = chain.thisObject
        val translation =
            runCatching {
                (getTranslationX.invoke(controller) as Number).toInt()
            }.getOrNull()
        val batteryWidth =
            runCatching {
                (getBatteryWidth.invoke(controller) as Number).toInt()
            }.getOrNull()

        if (translation != null) {
            val changed =
                synchronized(this) {
                    val old = lastControllerTranslation
                    lastControllerTranslation = translation
                    old != translation
                }
            if (changed) {
                onEvent(
                    "islandMotion controller translationX=" + translation +
                        " batteryWidth=" + (batteryWidth ?: -1) +
                        " geometryWrites=0",
                )
            }
        }
        result
    }

    private fun stretchHooker(
        leftField: Field,
        rightField: Field,
        privacyField: Field,
        onEvent: (String) -> Unit,
    ): Hooker = Hooker { chain ->
        val input = (chain.getArg(0) as? Number)?.toFloat()
        val result = chain.proceed()

        if (input == null) {
            return@Hooker result
        }

        val now = SystemClock.uptimeMillis()
        val shouldLog =
            synchronized(this) {
                if (now - burstStartUptimeMs > BURST_GAP_MS) {
                    burstStartUptimeMs = now
                    burstSamples = 0
                    lastStretchInput = null
                }
                val changed =
                    lastStretchInput == null ||
                        abs(input - requireNotNull(lastStretchInput)) >= MIN_DELTA
                if (!changed || burstSamples >= MAX_BURST_SAMPLES) {
                    false
                } else {
                    lastStretchInput = input
                    burstSamples += 1
                    true
                }
            }

        if (shouldLog) {
            val owner = chain.thisObject
            val left = runCatching { leftField.get(owner) as? View }.getOrNull()
            val right = runCatching { rightField.get(owner) as? View }.getOrNull()
            val privacy = runCatching { privacyField.get(owner) as? View }.getOrNull()
            onEvent(
                "islandMotion stretch input=" + input +
                    " left=" + viewMotion(left) +
                    " right=" + viewMotion(right) +
                    " privacy=" + viewMotion(privacy) +
                    " sample=" + burstSamples + "/" + MAX_BURST_SAMPLES +
                    " geometryWrites=0",
            )
        }

        result
    }

    private fun viewMotion(view: View?): String =
        if (view == null) {
            "none"
        } else {
            view.javaClass.simpleName +
                "{tx=" + view.translationX +
                ",a=" + view.alpha +
                ",v=" + view.visibility +
                ",x=" + view.x +
                ",w=" + view.width +
                "}"
        }

    private const val MAX_BURST_SAMPLES = 12
    private const val BURST_GAP_MS = 500L
    private const val MIN_DELTA = 0.5f
}
