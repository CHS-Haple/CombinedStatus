package com.chaners.combinedstatus.system

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object SystemUiScopeController {
    private const val CommandTimeoutSeconds = 20L
    private const val RestartCommand =
        "if [ -x /system/bin/killall ]; then exec /system/bin/killall com.android.systemui; " +
            "else exec killall com.android.systemui; fi"

    suspend fun restart(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                "su",
                "-c",
                RestartCommand,
            )
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(CommandTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroy()
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
                return@runCatching false
            }

            process.inputStream.bufferedReader().use { it.readText() }
            process.exitValue() == 0
        }.getOrDefault(false)
    }
}
