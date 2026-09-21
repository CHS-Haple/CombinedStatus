package com.chaners.combinedstatus.system

internal object SystemUiScopeController {
    private const val CommandTimeoutSeconds = 20L
    private const val RestartCommand =
        "if [ -x /system/bin/killall ]; then exec /system/bin/killall com.android.systemui; " +
            "else exec killall com.android.systemui; fi"

    suspend fun restart(): Boolean =
        RootShell.execute(
            command = RestartCommand,
            timeoutSeconds = CommandTimeoutSeconds,
        ).isSuccess
}
