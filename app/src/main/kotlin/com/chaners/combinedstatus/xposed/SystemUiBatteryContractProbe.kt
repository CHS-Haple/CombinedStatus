package com.chaners.combinedstatus.xposed

internal object SystemUiBatteryContractProbe {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private val targetMethodNames =
        setOf(
            "onBatteryLevelChanged",
            "onChargeStateChanged",
            "updateAll",
        )

    private val targetFieldNames =
        setOf(
            "mLevel",
            "mCharging",
            "mCharged",
            "mQuickCharging",
            "mBatteryController",
        )

    fun inspect(classLoader: ClassLoader): Result {
        val batteryClass =
            Class.forName(
                BATTERY_VIEW_CLASS_NAME,
                false,
                classLoader,
            )

        val interfaces =
            batteryClass.interfaces
                .map { type -> type.name }
                .sorted()

        val methods =
            batteryClass.declaredMethods
                .filter { method -> method.name in targetMethodNames }
                .map { method ->
                    method.name +
                        "(" +
                        method.parameterTypes.joinToString(",") { type -> type.typeName } +
                        "):" +
                        method.returnType.typeName
                }
                .sorted()

        val fields =
            batteryClass.declaredFields
                .filter { field -> field.name in targetFieldNames }
                .map { field -> field.name + ":" + field.type.typeName }
                .sorted()

        return Result(
            interfaces = interfaces,
            methods = methods,
            fields = fields,
        )
    }

    internal data class Result(
        val interfaces: List<String>,
        val methods: List<String>,
        val fields: List<String>,
    ) {
        val hasBatteryCallbackInterface: Boolean
            get() =
                interfaces.any { name ->
                    name.endsWith("BatteryController$BatteryStateChangeCallback")
                }

        val hasLevelCallback: Boolean
            get() =
                methods.any { method ->
                    method.startsWith(
                        "onBatteryLevelChanged(int,boolean,boolean):",
                    )
                }

        val logLine: String
            get() =
                "batteryContract " +
                    "callbackInterface=" + hasBatteryCallbackInterface +
                    " levelCallback=" + hasLevelCallback +
                    " interfaces=" + interfaces.joinToString(",", prefix = "[", postfix = "]") +
                    " methods=" + methods.joinToString(",", prefix = "[", postfix = "]") +
                    " fields=" + fields.joinToString(",", prefix = "[", postfix = "]")
    }
}
