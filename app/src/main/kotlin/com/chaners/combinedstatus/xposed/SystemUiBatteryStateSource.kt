package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object SystemUiBatteryStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val CHARGE_STATE_METHOD_NAME = "onChargeStateChanged"
    const val POWER_SAVE_METHOD_NAME = "onPowerSaveChanged"
    const val PERFORMANCE_METHOD_NAME = "onPerformanceModeChanged"
    const val HOOK_COUNT = 4

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val CHARGE_HOOK_ID = "combinedstatus.battery.charge"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_HOOK_ID = "combinedstatus.battery.performance"

    @Volatile
    private var lastState: CombinedStatusStateStore.BatteryState? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass = Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val levelField = batteryClass.requiredField("mLevel")
        val chargingField = batteryClass.requiredField("mCharging")
        val powerSaveField = batteryClass.requiredField("mPowerSave")
        val performanceField = batteryClass.requiredField("mPerformanceMode")
        val batteryIconField = batteryClass.requiredField("mBatteryIconView")
        val iconClass = batteryIconField.type
        val progressStatusMethod =
            iconClass.getDeclaredMethod("getProgressStatus").apply { isAccessible = true }
        val chargingColorField = iconClass.requiredField("mBatteryChargingColor")
        val powerSaveColorField = iconClass.requiredField("mBatteryPowerSaveColor")
        val performanceColorField = iconClass.requiredField("mBatteryPerformanceModeColor")
        val lowColorField = iconClass.requiredField("mBatteryLowColor")

        fun readState(sourceView: View): CombinedStatusStateStore.BatteryState? {
            val level =
                runCatching { levelField.getInt(sourceView) }.getOrNull()
                    ?.coerceIn(0, 100)
                    ?: return null
            val charging =
                runCatching { chargingField.getBoolean(sourceView) }.getOrNull()
                    ?: return null
            val powerSave = runCatching { powerSaveField.getBoolean(sourceView) }.getOrDefault(false)
            val performanceMode =
                runCatching { performanceField.getBoolean(sourceView) }.getOrDefault(false)
            val iconView = runCatching { batteryIconField.get(sourceView) }.getOrNull()
            val nativeStatusName =
                iconView?.let { icon ->
                    runCatching {
                        (progressStatusMethod.invoke(icon) as? Enum<*>)?.name
                    }.getOrNull()
                }
            val semanticState =
                SystemUiBatterySemanticPolicy.fromNativeProgressStatus(nativeStatusName)
                    ?: SystemUiBatterySemanticPolicy.fallback(
                        level = level,
                        charging = charging,
                        powerSave = powerSave,
                        performanceMode = performanceMode,
                    )
            val systemSemanticColor =
                iconView?.let { icon ->
                    semanticColor(
                        icon = icon,
                        state = semanticState,
                        chargingColorField = chargingColorField,
                        powerSaveColorField = powerSaveColorField,
                        performanceColorField = performanceColorField,
                        lowColorField = lowColorField,
                    )
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
                    if (lastState == state) false
                    else {
                        lastState = state
                        true
                    }
                }
            if (!changed) return
            onBatteryState(state)
            onEvent?.invoke(
                "batteryState source=MiuiBatteryMeterView." + sourceMethod +
                    " percent=" + state.percent +
                    " charging=" + state.charging +
                    " semantic=" + state.semanticState?.name +
                    " systemColor=" +
                    (state.systemSemanticColor?.let(::colorHex) ?: "default") +
                    " semanticAuthority=MiuiBatteryMeterIconView.getProgressStatus()" +
                    " eventDriven=true",
            )
        }

        fun hook(method: Method, hookId: String): HookHandle =
            module.hook(method).setId(hookId).intercept(
                Hooker { chain ->
                    val result = chain.proceed()
                    val sourceView = chain.thisObject as? View ?: return@Hooker result
                    publish(sourceView, method.name)
                    result
                },
            )

        val levelMethod =
            batteryClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val chargeMethod =
            batteryClass.getDeclaredMethod(
                CHARGE_STATE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val powerSaveMethod =
            batteryClass.getDeclaredMethod(
                POWER_SAVE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val performanceMethod =
            batteryClass.getDeclaredMethod(
                PERFORMANCE_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        return listOf(
            hook(levelMethod, LEVEL_HOOK_ID),
            hook(chargeMethod, CHARGE_HOOK_ID),
            hook(powerSaveMethod, POWER_SAVE_HOOK_ID),
            hook(performanceMethod, PERFORMANCE_HOOK_ID),
        )
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
        return runCatching { field.getInt(icon) }.getOrNull()
            ?.takeIf { color -> (color ushr 24) != 0 }
    }

    private fun Class<*>.requiredField(name: String): Field =
        getDeclaredField(name).apply { isAccessible = true }

    private fun colorHex(color: Int): String =
        "#" + color.toUInt().toString(16).padStart(8, '0')

    fun matches(handle: HookHandle): Boolean =
        handle.id == LEVEL_HOOK_ID ||
            handle.id == CHARGE_HOOK_ID ||
            handle.id == POWER_SAVE_HOOK_ID ||
            handle.id == PERFORMANCE_HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        lastState = null
    }
}
