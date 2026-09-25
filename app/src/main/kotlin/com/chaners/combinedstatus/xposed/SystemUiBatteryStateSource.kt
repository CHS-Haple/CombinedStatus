package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiBatteryStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val POWER_SAVE_METHOD_NAME = "onPowerSaveChanged"
    const val PERFORMANCE_MODE_METHOD_NAME = "onPerformanceModeChanged"
    const val EXTREME_POWER_SAVE_METHOD_NAME = "onExtremePowerSaveChanged"

    /** Required core contract. Mode callbacks are optional, fingerprint-scoped extensions. */
    const val REQUIRED_HOOK_COUNT = 1

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_MODE_HOOK_ID = "combinedstatus.battery.performance"
    private const val EXTREME_POWER_SAVE_HOOK_ID = "combinedstatus.battery.extreme-power-save"

    @Volatile
    private var lastState: CombinedStatusStateStore.BatteryState? = null

    @Volatile
    private var percent: Int? = null

    @Volatile
    private var charging: Boolean? = null

    @Volatile
    private var powerSave: Boolean? = null

    @Volatile
    private var performanceMode: Boolean? = null

    @Volatile
    private var extremePowerSave: Boolean? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val handles = mutableListOf<HookHandle>()

        val levelMethod =
            batteryClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        handles +=
            module
                .hook(levelMethod)
                .setId(LEVEL_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val level =
                            (chain.getArg(0) as? Number)
                                ?.toInt()
                                ?.coerceIn(0, 100)
                                ?: return@Hooker result
                        val pluggedIn =
                            chain.getArg(1) as? Boolean
                                ?: return@Hooker result
                        val isCharging =
                            chain.getArg(2) as? Boolean
                                ?: return@Hooker result

                        synchronized(this) {
                            percent = level
                            charging = isCharging
                        }
                        emitCurrentState(onBatteryState)

                        onEvent?.invoke(
                            "batteryState source=MiuiBatteryMeterView." +
                                BATTERY_LEVEL_METHOD_NAME +
                                " percent=" + level +
                                " pluggedIn=" + pluggedIn +
                                " charging=" + isCharging +
                                " powerSave=" + (powerSave?.toString() ?: "unknown") +
                                " extremePowerSave=" +
                                (extremePowerSave?.toString() ?: "unknown") +
                                " performanceMode=" +
                                (performanceMode?.toString() ?: "unknown") +
                                " eventDriven=true",
                        )
                        result
                    },
                )

        installOptionalBooleanCallback(
            module = module,
            batteryClass = batteryClass,
            methodName = POWER_SAVE_METHOD_NAME,
            hookId = POWER_SAVE_HOOK_ID,
            onValue = { value -> powerSave = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        installOptionalBooleanCallback(
            module = module,
            batteryClass = batteryClass,
            methodName = PERFORMANCE_MODE_METHOD_NAME,
            hookId = PERFORMANCE_MODE_HOOK_ID,
            onValue = { value -> performanceMode = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        installOptionalBooleanCallback(
            module = module,
            batteryClass = batteryClass,
            methodName = EXTREME_POWER_SAVE_METHOD_NAME,
            hookId = EXTREME_POWER_SAVE_HOOK_ID,
            onValue = { value -> extremePowerSave = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        onEvent?.invoke(
            "batteryModeContracts source=MiuiBatteryMeterView " +
                "powerSave=" + (handles.any { it.id == POWER_SAVE_HOOK_ID }) +
                " performanceMode=" +
                (handles.any { it.id == PERFORMANCE_MODE_HOOK_ID }) +
                " extremePowerSave=" +
                (handles.any { it.id == EXTREME_POWER_SAVE_HOOK_ID }) +
                " requiredLevel=true eventDriven=true",
        )

        return handles
    }

    private fun installOptionalBooleanCallback(
        module: XposedModule,
        batteryClass: Class<*>,
        methodName: String,
        hookId: String,
        onValue: (Boolean) -> Unit,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): HookHandle? {
        val method =
            runCatching {
                batteryClass
                    .getDeclaredMethod(
                        methodName,
                        Boolean::class.javaPrimitiveType,
                    )
                    .apply { isAccessible = true }
            }.getOrNull()
                ?: run {
                    onEvent?.invoke(
                        "batteryModeContract source=MiuiBatteryMeterView." +
                            methodName +
                            " available=false fallback=unknown",
                    )
                    return null
                }

        return module
            .hook(method)
            .setId(hookId)
            .intercept(
                Hooker { chain ->
                    val result = chain.proceed()
                    val value =
                        chain.getArg(0) as? Boolean
                            ?: return@Hooker result
                    synchronized(this) {
                        onValue(value)
                    }
                    emitCurrentState(onBatteryState)
                    onEvent?.invoke(
                        "batteryModeState source=MiuiBatteryMeterView." +
                            methodName +
                            " value=" + value +
                            " eventDriven=true",
                    )
                    result
                },
            )
    }

    private fun emitCurrentState(
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
    ) {
        val state =
            synchronized(this) {
                val currentPercent = percent ?: return
                val currentCharging = charging ?: return
                CombinedStatusStateStore.BatteryState(
                    percent = currentPercent,
                    charging = currentCharging,
                    powerSave = powerSave,
                    extremePowerSave = extremePowerSave,
                    performanceMode = performanceMode,
                )
            }

        val changed =
            synchronized(this) {
                if (lastState == state) {
                    false
                } else {
                    lastState = state
                    true
                }
            }
        if (changed) {
            onBatteryState(state)
        }
    }

    fun matches(handle: HookHandle): Boolean =
        handle.id in
            setOf(
                LEVEL_HOOK_ID,
                POWER_SAVE_HOOK_ID,
                PERFORMANCE_MODE_HOOK_ID,
                EXTREME_POWER_SAVE_HOOK_ID,
            )

    @Synchronized
    fun resetRuntimeState() {
        lastState = null
        percent = null
        charging = null
        powerSave = null
        performanceMode = null
        extremePowerSave = null
    }
}
