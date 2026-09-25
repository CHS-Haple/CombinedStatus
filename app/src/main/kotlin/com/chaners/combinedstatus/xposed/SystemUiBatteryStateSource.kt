package com.chaners.combinedstatus.xposed

import android.view.View
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

    /** Core contract. HyperOS mode callbacks are compatibility-safe extensions. */
    const val REQUIRED_HOOK_COUNT = 1

    private const val LEVEL_HOOK_ID = "combinedstatus.battery.level"
    private const val POWER_SAVE_HOOK_ID = "combinedstatus.battery.power-save"
    private const val PERFORMANCE_MODE_HOOK_ID = "combinedstatus.battery.performance"
    private const val EXTREME_POWER_SAVE_HOOK_ID =
        "combinedstatus.battery.extreme-power-save"

    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val CHARGING_COLOR_RESOURCE = "status_bar_battery_charging"
    private const val POWER_SAVE_COLOR_RESOURCE = "status_bar_battery_power_save"
    private const val PERFORMANCE_COLOR_RESOURCE = "status_bar_battery_performance"
    private const val EXTREME_POWER_SAVE_COLOR_RESOURCE =
        "status_bar_battery_extreme_power_save"

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

    @Volatile
    private var paletteResolved = false

    private var chargingTint: Int? = null
    private var powerSaveTint: Int? = null
    private var performanceTint: Int? = null
    private var extremePowerSaveTint: Int? = null
    private var extremePowerSaveTintSource = "unresolved"

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
                        ensurePalette(
                            view = chain.thisObject as? View,
                            onEvent = onEvent,
                        )
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
                                " modeTint=" + colorToken(resolveCurrentModeTint()) +
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
                "powerSave=" + handles.any { it.id == POWER_SAVE_HOOK_ID } +
                " performanceMode=" +
                handles.any { it.id == PERFORMANCE_MODE_HOOK_ID } +
                " extremePowerSave=" +
                handles.any { it.id == EXTREME_POWER_SAVE_HOOK_ID } +
                " requiredLevel=true colorAuthority=systemui-resources eventDriven=true",
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
                    ensurePalette(
                        view = chain.thisObject as? View,
                        onEvent = onEvent,
                    )
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
                            " modeTint=" + colorToken(resolveCurrentModeTint()) +
                            " eventDriven=true",
                    )
                    result
                },
            )
    }

    @Synchronized
    private fun ensurePalette(
        view: View?,
        onEvent: ((String) -> Unit)?,
    ) {
        if (paletteResolved || view == null) {
            return
        }

        val resources = view.resources
        val packageName =
            view.context.packageName
                .takeIf { it == SYSTEM_UI_PACKAGE }
                ?: SYSTEM_UI_PACKAGE

        fun resolveColor(name: String): Int? {
            val id =
                resources
                    .getIdentifier(name, "color", packageName)
                    .takeIf { it != 0 }
                    ?: return null
            return runCatching {
                resources.getColor(id, view.context.theme)
            }.getOrNull()
        }

        chargingTint = resolveColor(CHARGING_COLOR_RESOURCE)
        powerSaveTint = resolveColor(POWER_SAVE_COLOR_RESOURCE)
        performanceTint = resolveColor(PERFORMANCE_COLOR_RESOURCE)
        val explicitExtremeTint =
            resolveColor(EXTREME_POWER_SAVE_COLOR_RESOURCE)
        extremePowerSaveTint =
            explicitExtremeTint
                ?: powerSaveTint
        extremePowerSaveTintSource =
            when {
                explicitExtremeTint != null -> EXTREME_POWER_SAVE_COLOR_RESOURCE
                powerSaveTint != null -> POWER_SAVE_COLOR_RESOURCE + ":fallback"
                else -> "none"
            }
        paletteResolved = true

        onEvent?.invoke(
            "batteryColorAuthority source=SystemUI.Resources " +
                "charging=" + colorToken(chargingTint) +
                " powerSave=" + colorToken(powerSaveTint) +
                " performance=" + colorToken(performanceTint) +
                " extremePowerSave=" + colorToken(extremePowerSaveTint) +
                " extremeSource=" + extremePowerSaveTintSource +
                " eventDriven=true",
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
                    resolvedModeTint = resolveCurrentModeTintLocked(),
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

    private fun resolveCurrentModeTint(): Int? =
        synchronized(this) {
            resolveCurrentModeTintLocked()
        }

    private fun resolveCurrentModeTintLocked(): Int? =
        when {
            charging == true -> chargingTint
            extremePowerSave == true -> extremePowerSaveTint
            powerSave == true -> powerSaveTint
            performanceMode == true -> performanceTint
            else -> null
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
        paletteResolved = false
        chargingTint = null
        powerSaveTint = null
        performanceTint = null
        extremePowerSaveTint = null
        extremePowerSaveTintSource = "unresolved"
    }
}
