package com.chaners.combinedstatus.xposed

internal object SystemUiBatteryContractProbe {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private const val BATTERY_CALLBACK_SUFFIX =
        "BatteryController\$BatteryStateChangeCallback"

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

        val callbackInterface =
            batteryClass.interfaces
                .firstOrNull { type ->
                    type.name.endsWith(BATTERY_CALLBACK_SUFFIX)
                }

        val callbackMethods =
            callbackInterface
                ?.methods
                ?.map(::methodSignature)
                ?.sorted()
                .orEmpty()

        val methods =
            batteryClass.declaredMethods
                .filter { method -> method.name in targetMethodNames }
                .map(::methodSignature)
                .sorted()

        val fields =
            batteryClass.declaredFields
                .filter { field -> field.name in targetFieldNames }
                .map { field -> field.name + ":" + field.type.typeName }
                .sorted()

        return Result(
            interfaces = interfaces,
            callbackInterfaceName = callbackInterface?.name,
            callbackMethods = callbackMethods,
            methods = methods,
            fields = fields,
        )
    }

    private fun methodSignature(method: java.lang.reflect.Method): String =
        method.name +
            "(" +
            method.parameterTypes.joinToString(",") { type -> type.typeName } +
            "):" +
            method.returnType.typeName

    internal data class Result(
        val interfaces: List<String>,
        val callbackInterfaceName: String?,
        val callbackMethods: List<String>,
        val methods: List<String>,
        val fields: List<String>,
    ) {
        val hasBatteryCallbackInterface: Boolean
            get() = callbackInterfaceName != null

        val hasLevelCallback: Boolean
            get() =
                callbackMethods.any { method ->
                    method.startsWith(
                        "onBatteryLevelChanged(int,boolean,boolean):",
                    )
                } ||
                    methods.any { method ->
                        method.startsWith(
                            "onBatteryLevelChanged(int,boolean,boolean):",
                        )
                    }

        val hasChargeStateCallback: Boolean
            get() =
                methods.any { method ->
                    method.startsWith(
                        "onChargeStateChanged(boolean,boolean):",
                    )
                }

        val logLine: String
            get() =
                "batteryContract " +
                    "callbackInterface=" + (callbackInterfaceName ?: "none") +
                    " levelCallback=" + hasLevelCallback +
                    " chargeStateCallback=" + hasChargeStateCallback +
                    " callbackMethods=" +
                    callbackMethods.joinToString(",", prefix = "[", postfix = "]") +
                    " methods=" +
                    methods.joinToString(",", prefix = "[", postfix = "]") +
                    " fields=" +
                    fields.joinToString(",", prefix = "[", postfix = "]")
    }
}
