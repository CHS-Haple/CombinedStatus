package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule

internal object SystemUiBatteryStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val BATTERY_LEVEL_METHOD_NAME = "onBatteryLevelChanged"
    const val HOOK_COUNT = 1

    private const val HOOK_ID = "combinedstatus.battery.level"

    @Volatile
    private var lastState: CombinedStatusStateStore.BatteryState? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        onBatteryPresentationApplied: (() -> Unit)?,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val levelMethod =
            batteryClass.getDeclaredMethod(
                BATTERY_LEVEL_METHOD_NAME,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val handle =
            module
                .hook(levelMethod)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        onBatteryPresentationApplied?.invoke()
                        val level =
                            (chain.getArg(0) as? Number)
                                ?.toInt()
                                ?.coerceIn(0, 100)
                                ?: return@Hooker result
                        val pluggedIn =
                            chain.getArg(1) as? Boolean
                                ?: return@Hooker result
                        val charging =
                            chain.getArg(2) as? Boolean
                                ?: return@Hooker result
                        val state =
                            CombinedStatusStateStore.BatteryState(
                                percent = level,
                                charging = charging,
                            )

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
                            onEvent?.invoke(
                                "batteryState source=MiuiBatteryMeterView." +
                                    BATTERY_LEVEL_METHOD_NAME +
                                    " percent=" + level +
                                    " pluggedIn=" + pluggedIn +
                                    " charging=" + charging +
                                    " eventDriven=true",
                            )
                        }

                        result
                    },
                )

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        lastState = null
    }
}
