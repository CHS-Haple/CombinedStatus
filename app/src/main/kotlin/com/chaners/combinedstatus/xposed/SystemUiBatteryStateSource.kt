package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field

internal object SystemUiBatteryStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val ICON_DARK_CHANGE_METHOD_NAME = "onDarkChangeInternal"
    const val HOOK_COUNT = 2

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val SEMANTIC_HOOK_ID = "combinedstatus.battery.semantic"

    @Volatile
    private var lastState: CombinedStatusStateStore.BatteryState? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val levelField = batteryClass.requiredField("mLevel")
        val chargingField = batteryClass.requiredField("mCharging")
        val batteryIconField = batteryClass.requiredField("mBatteryIconView")

        val iconClass = batteryIconField.type
        val progressStatusMethod =
            iconClass.getDeclaredMethod("getProgressStatus")
                .apply { isAccessible = true }
        val iconDarkChangeMethod =
            iconClass.getDeclaredMethod(ICON_DARK_CHANGE_METHOD_NAME)
                .apply { isAccessible = true }
        val optimizationField = iconClass.requiredField("mMiuiOptimizationEnabled")
        val chargingColorField = iconClass.requiredField("mBatteryChargingColor")
        val powerSaveColorField = iconClass.requiredField("mBatteryPowerSaveColor")
        val performanceColorField = iconClass.requiredField("mBatteryPerformanceModeColor")
        val lowColorField = iconClass.requiredField("mBatteryLowColor")

        fun readState(sourceView: View): CombinedStatusStateStore.BatteryState? {
            val level =
                runCatching { levelField.getInt(sourceView) }
                    .getOrNull()
                    ?.coerceIn(0, 100)
                    ?: return null
            val charging =
                runCatching { chargingField.getBoolean(sourceView) }
                    .getOrNull()
                    ?: return null
            val icon =
                runCatching { batteryIconField.get(sourceView) }
                    .getOrNull()
            val nativeStatusName =
                icon?.let { iconView ->
                    runCatching {
                        (progressStatusMethod.invoke(iconView) as? Enum<*>)?.name
                    }.getOrNull()
                }
            val semanticState =
                SystemUiBatterySemanticPolicy.fromNativeProgressStatus(nativeStatusName)
            val optimizationEnabled =
                icon?.let { iconView ->
                    runCatching { optimizationField.getBoolean(iconView) }
                        .getOrNull()
                } ?: false
            val systemSemanticColor =
                if (icon != null && semanticState != null && optimizationEnabled) {
                    semanticColor(
                        icon = icon,
                        state = semanticState,
                        chargingColorField = chargingColorField,
                        powerSaveColorField = powerSaveColorField,
                        performanceColorField = performanceColorField,
                        lowColorField = lowColorField,
                    )
                } else {
                    null
                }

            return CombinedStatusStateStore.BatteryState(
                percent = level,
                charging = charging,
                semanticState = semanticState,
                systemSemanticColor = systemSemanticColor,
            )
        }

        fun publish(sourceView: View, sourceMethod: String) {
            val state = readState(sourceView) ?: return
            val changed =
                synchronized(this) {
                    if (lastState == state) {
                        false
                    } else {
                        lastState = state
                        true
                    }
                }
            if (!changed) {
                return
            }
            onBatteryState(state)
            onEvent?.invoke(
                "batteryState source=" + sourceMethod +
                    " percent=" + state.percent +
                    " charging=" + state.charging +
                    " semantic=" + (state.semanticState?.name ?: "unavailable") +
                    " systemColor=" +
                    (state.systemSemanticColor?.let(::colorHex) ?: "status-icon") +
                    " semanticAuthority=MiuiBatteryMeterIconView.getProgressStatus()" +
                    " eventDriven=true",
            )
        }

        val levelMethod =
            batteryClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val levelHandle =
            module
                .hook(levelMethod)
                .setId(LEVEL_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = chain.thisObject as? View
                            ?: return@Hooker result
                        publish(
                            sourceView = sourceView,
                            sourceMethod = "MiuiBatteryMeterView.$BATTERY_LEVEL_METHOD_NAME",
                        )
                        result
                    },
                )

        val semanticHandle =
            module
                .hook(iconDarkChangeMethod)
                .setId(SEMANTIC_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val iconView = chain.thisObject as? View
                            ?: return@Hooker result
                        val sourceView =
                            findBatteryView(iconView)
                                ?: return@Hooker result
                        publish(
                            sourceView = sourceView,
                            sourceMethod =
                                "MiuiBatteryMeterIconView.$ICON_DARK_CHANGE_METHOD_NAME",
                        )
                        result
                    },
                )

        return listOf(levelHandle, semanticHandle)
    }

    private fun semanticColor(
        icon: Any,
        state: CombinedStatusBatterySemanticState,
        chargingColorField: Field,
        powerSaveColorField: Field,
        performanceColorField: Field,
        lowColorField: Field,
    ): Int? {
        val field =
            when (state) {
                CombinedStatusBatterySemanticState.NORMAL -> return null
                CombinedStatusBatterySemanticState.CHARGING -> chargingColorField
                CombinedStatusBatterySemanticState.POWER_SAVE -> powerSaveColorField
                CombinedStatusBatterySemanticState.PERFORMANCE -> performanceColorField
                CombinedStatusBatterySemanticState.LOW -> lowColorField
            }
        return runCatching { field.getInt(icon) }
            .getOrNull()
            ?.takeIf { color -> color ushr 24 != 0 }
    }

    private fun findBatteryView(iconView: View): View? {
        var current: View? = iconView
        while (current != null) {
            if (current.javaClass.name == BATTERY_VIEW_CLASS_NAME) {
                return current
            }
            current = current.parent as? View
        }
        return null
    }

    private fun Class<*>.requiredField(name: String): Field =
        getDeclaredField(name).apply { isAccessible = true }

    private fun colorHex(color: Int): String =
        "#" + color.toUInt().toString(16).padStart(8, '0')

    fun matches(handle: HookHandle): Boolean =
        handle.id == LEVEL_HOOK_ID || handle.id == SEMANTIC_HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        lastState = null
    }
}
