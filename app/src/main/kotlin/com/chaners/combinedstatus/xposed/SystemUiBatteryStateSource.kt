package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiBatteryStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val REQUIRED_HOOK_COUNT = 1

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_HOOK_ID = "combinedstatus.battery.performance"
    private const val EXTREME_POWER_SAVE_HOOK_ID =
        "combinedstatus.battery.extreme-power-save"

    private const val POWER_SAVE_METHOD_NAME = "onPowerSaveChanged"
    private const val PERFORMANCE_METHOD_NAME = "onPerformanceModeChanged"
    private const val EXTREME_POWER_SAVE_METHOD_NAME = "onExtremePowerSaveChanged"

    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val CHARGING_COLOR_RESOURCE = "status_bar_battery_charging"
    private const val POWER_SAVE_COLOR_RESOURCE = "status_bar_battery_power_save"
    private const val PERFORMANCE_COLOR_RESOURCE = "status_bar_battery_performance"
    private const val EXTREME_POWER_SAVE_COLOR_RESOURCE =
        "status_bar_battery_extreme_power_save"

    @Volatile
    private var lastState: CombinedStatusStateStore.BatteryState? = null

    private var percent: Int? = null
    private var charging: Boolean? = null
    private var powerSave: Boolean? = null
    private var performanceMode: Boolean? = null
    private var extremePowerSave: Boolean? = null

    private var colorsResolved = false
    private var chargingTint: Int? = null
    private var powerSaveTint: Int? = null
    private var performanceTint: Int? = null
    private var extremePowerSaveTint: Int? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val handles = mutableListOf<HookHandle>()

        CombinedStatusStateStore.snapshot().battery?.let { seed ->
            synchronized(this) {
                percent = seed.percent
                charging = seed.charging
                powerSave = seed.powerSave
                performanceMode = seed.performanceMode
                extremePowerSave = seed.extremePowerSave
                lastState = seed
            }
        }

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

                        ensureColors(
                            view = chain.thisObject as? View,
                            onEvent = onEvent,
                        )
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
                                " mode=" + currentModeToken() +
                                " tint=" + colorToken(currentModeTint()) +
                                " eventDriven=true",
                        )
                        result
                    },
                )

        installOptionalModeHook(
            module = module,
            batteryClass = batteryClass,
            methodName = POWER_SAVE_METHOD_NAME,
            hookId = POWER_SAVE_HOOK_ID,
            update = { value -> powerSave = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        installOptionalModeHook(
            module = module,
            batteryClass = batteryClass,
            methodName = PERFORMANCE_METHOD_NAME,
            hookId = PERFORMANCE_HOOK_ID,
            update = { value -> performanceMode = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        installOptionalModeHook(
            module = module,
            batteryClass = batteryClass,
            methodName = EXTREME_POWER_SAVE_METHOD_NAME,
            hookId = EXTREME_POWER_SAVE_HOOK_ID,
            update = { value -> extremePowerSave = value },
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )?.let(handles::add)

        onEvent?.invoke(
            "batteryModeContracts source=runtime-structure " +
                "powerSave=" + handles.any { it.id == POWER_SAVE_HOOK_ID } +
                " performance=" + handles.any { it.id == PERFORMANCE_HOOK_ID } +
                " extremePowerSave=" +
                handles.any { it.id == EXTREME_POWER_SAVE_HOOK_ID } +
                " requiredHooks=" + REQUIRED_HOOK_COUNT +
                " installedHooks=" + handles.size +
                " fallback=native eventDriven=true",
        )

        return handles
    }

    private fun installOptionalModeHook(
        module: XposedModule,
        batteryClass: Class<*>,
        methodName: String,
        hookId: String,
        update: (Boolean) -> Unit,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): HookHandle? {
        val method =
            batteryClass.declaredMethods
                .firstOrNull { candidate ->
                    candidate.name == methodName &&
                        candidate.parameterTypes.contentEquals(
                            arrayOf(Boolean::class.javaPrimitiveType),
                        )
                }
                ?.apply { isAccessible = true }
                ?: run {
                    onEvent?.invoke(
                        "batteryModeContract source=runtime-structure " +
                            "method=" + methodName +
                            " available=false fallback=native",
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
                    ensureColors(
                        view = chain.thisObject as? View,
                        onEvent = onEvent,
                    )
                    synchronized(this) {
                        update(value)
                    }
                    emitCurrentState(onBatteryState)
                    onEvent?.invoke(
                        "batteryModeState source=MiuiBatteryMeterView." +
                            methodName +
                            " value=" + value +
                            " mode=" + currentModeToken() +
                            " tint=" + colorToken(currentModeTint()) +
                            " eventDriven=true",
                    )
                    result
                },
            )
    }

    @Synchronized
    private fun ensureColors(
        view: View?,
        onEvent: ((String) -> Unit)?,
    ) {
        if (colorsResolved || view == null) {
            return
        }

        fun resolveColor(name: String): Int? {
            val resourceId =
                view.resources
                    .getIdentifier(name, "color", SYSTEM_UI_PACKAGE)
                    .takeIf { it != 0 }
                    ?: return null
            return runCatching {
                view.resources.getColor(resourceId, view.context.theme)
            }.getOrNull()
        }

        chargingTint = resolveColor(CHARGING_COLOR_RESOURCE)
        powerSaveTint = resolveColor(POWER_SAVE_COLOR_RESOURCE)
        performanceTint = resolveColor(PERFORMANCE_COLOR_RESOURCE)
        extremePowerSaveTint = resolveColor(EXTREME_POWER_SAVE_COLOR_RESOURCE)
        colorsResolved = true

        onEvent?.invoke(
            "batteryColorContracts source=SystemUI.Resources " +
                "charging=" + colorToken(chargingTint) +
                " powerSave=" + colorToken(powerSaveTint) +
                " performance=" + colorToken(performanceTint) +
                " extremePowerSave=" + colorToken(extremePowerSaveTint) +
                " fallback=native",
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
                    performanceMode = performanceMode,
                    extremePowerSave = extremePowerSave,
                    resolvedModeTint = currentModeTintLocked(),
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

    private fun currentModeTint(): Int? =
        synchronized(this) {
            currentModeTintLocked()
        }

    private fun currentModeTintLocked(): Int? =
        when {
            charging == true -> chargingTint
            extremePowerSave == true ->
                extremePowerSaveTint
                    ?: powerSaveTint.takeIf { powerSave == true }
            powerSave == true -> powerSaveTint
            performanceMode == true -> performanceTint
            else -> null
        }

    private fun currentModeToken(): String =
        synchronized(this) {
            when {
                charging == true -> "charging"
                extremePowerSave == true -> "extreme-power-save"
                powerSave == true -> "power-save"
                performanceMode == true -> "performance"
                else -> "normal"
            }
        }

    private fun colorToken(color: Int?): String =
        color?.let { value ->
            "0x" + value.toUInt().toString(16).padStart(8, '0')
        } ?: "unavailable"

    fun matches(handle: HookHandle): Boolean =
        handle.id in
            setOf(
                LEVEL_HOOK_ID,
                POWER_SAVE_HOOK_ID,
                PERFORMANCE_HOOK_ID,
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
        colorsResolved = false
        chargingTint = null
        powerSaveTint = null
        performanceTint = null
        extremePowerSaveTint = null
    }
}
