package com.chaners.combinedstatus.system

import android.os.Build
import com.chaners.combinedstatus.BuildConfig
import java.time.OffsetDateTime

internal object DiagnosticsReportBuilder {
    private const val LogTimeoutSeconds = 10L
    private const val DebugLogLineLimit = 600
    private const val ReleaseLogLineLimit = 120
    private const val LogcatCommand =
        "logcat -d -b all -v threadtime -t 3000"

    suspend fun build(): String {
        val logResult = RootShell.execute(
            command = LogcatCommand,
            timeoutSeconds = LogTimeoutSeconds,
        )
        val moduleLines = logResult.output
            .lineSequence()
            .filter { line ->
                line.contains("LSPosedFramework") &&
                    line.contains("com.chaners.combinedstatus") &&
                    line.contains("CombinedStatus")
            }
            .toList()
            .takeLast(
                if (BuildConfig.DEBUG) {
                    DebugLogLineLimit
                } else {
                    ReleaseLogLineLimit
                },
            )

        return buildString {
            appendLine("CombinedStatus Diagnostic Report")
            appendLine()
            appendLine("[App]")
            appendLine("version=" + BuildConfig.VERSION_NAME)
            appendLine("build=" + BuildConfig.BUILD_ID)
            appendLine("package=" + BuildConfig.APPLICATION_ID)
            appendLine("buildType=" + if (BuildConfig.DEBUG) "debug" else "release")
            appendLine("diagnostics=" + if (BuildConfig.DEBUG) "detailed" else "basic")
            appendLine()
            appendLine("[Device]")
            appendLine("manufacturer=" + Build.MANUFACTURER)
            appendLine("model=" + Build.MODEL)
            appendLine("device=" + Build.DEVICE)
            appendLine("android=" + Build.VERSION.RELEASE)
            appendLine("sdk=" + Build.VERSION.SDK_INT)
            appendLine()
            appendLine("[Runtime log]")
            appendLine(
                "collection=" +
                    when {
                        logResult.isSuccess -> "ok"
                        logResult.timedOut -> "timeout"
                        logResult.error != null -> "error:" + logResult.error
                        else -> "exit:" + logResult.exitCode
                    },
            )
            appendLine("lines=" + moduleLines.size)
            if (moduleLines.isEmpty()) {
                appendLine("No CombinedStatus runtime log entries were available.")
            } else {
                moduleLines.forEach(::appendLine)
            }
            appendLine()
            appendLine("[Report]")
            appendLine("generatedAt=" + OffsetDateTime.now())
        }
    }
}
