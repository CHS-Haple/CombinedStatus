package com.chaners.combinedstatus.system

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal object RootShell {
    suspend fun execute(
        command: String,
        timeoutSeconds: Long,
    ): Result = withContext(Dispatchers.IO) {
        coroutineScope {
            try {
                val process = ProcessBuilder(
                    "su",
                    "-c",
                    command,
                )
                    .redirectErrorStream(true)
                    .start()

                val output = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().use { it.readText() }
                }

                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroy()
                    if (!process.waitFor(1, TimeUnit.SECONDS)) {
                        process.destroyForcibly()
                    }
                    val captured = runCatching { output.await() }.getOrDefault("")
                    return@coroutineScope Result(
                        exitCode = null,
                        output = captured,
                        timedOut = true,
                        error = null,
                    )
                }

                Result(
                    exitCode = process.exitValue(),
                    output = output.await(),
                    timedOut = false,
                    error = null,
                )
            } catch (error: Throwable) {
                Result(
                    exitCode = null,
                    output = "",
                    timedOut = false,
                    error = error.javaClass.simpleName,
                )
            }
        }
    }

    internal data class Result(
        val exitCode: Int?,
        val output: String,
        val timedOut: Boolean,
        val error: String?,
    ) {
        val isSuccess: Boolean
            get() = exitCode == 0 && !timedOut && error == null
    }
}
