package com.chaners.combinedstatus.system

import android.os.Build
import com.chaners.combinedstatus.BuildConfig
import java.time.OffsetDateTime

internal object DiagnosticsReportBuilder {
    private const val LogTimeoutSeconds = 10L
    private const val DebugLogLineLimit = 600
    private const val ReleaseLogLineLimit = 120

    private const val LsposedModuleLogCommand =
        "for f in \$(ls -1t /data/adb/lspd/log/modules_*.log " +
            "/data/adb/lspd/log.old/modules_*.log 2>/dev/null | head -n 8); do " +
            "if grep -Fq 'com.chaners.combinedstatus' \"\$f\"; then " +
            "grep -F 'com.chaners.combinedstatus' \"\$f\" || true; break; fi; done"

    private const val LogcatCommand =
        "logcat -d -b all -v threadtime -t 3000"

    suspend fun build(): String {
        val lsposedResult = RootShell.execute(
            command = LsposedModuleLogCommand,
            timeoutSeconds = LogTimeoutSeconds,
        )
        val lsposedLines = filterModuleLines(lsposedResult.output)

        val selected = if (lsposedLines.isNotEmpty()) {
            CollectedLog(
                source = "lsposed-modules",
                result = lsposedResult,
                lines = lsposedLines,
            )
        } else {
            val logcatResult = RootShell.execute(
                command = LogcatCommand,
                timeoutSeconds = LogTimeoutSeconds,
            )
            CollectedLog(
                source = "logcat-fallback",
                result = logcatResult,
                lines = filterModuleLines(logcatResult.output),
            )
        }

        val lineLimit =
            if (BuildConfig.DEBUG) {
                DebugLogLineLimit
            } else {
                ReleaseLogLineLimit
            }
        val moduleLines = selectLatestSession(selected.lines).takeLast(lineLimit)

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
            appendLine("source=" + selected.source)
            appendLine("collection=" + collectionState(selected.result))
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

    private fun filterModuleLines(output: String): List<String> =
        output
            .lineSequence()
            .filter { line ->
                line.contains("com.chaners.combinedstatus") &&
                    line.contains("CombinedStatus")
            }
            .toList()

    private fun selectLatestSession(lines: List<String>): List<String> {
        if (lines.isEmpty()) {
            return lines
        }

        val currentBuild = "build=" + BuildConfig.BUILD_ID
        val currentAnchor = lines.indexOfLast { line ->
            line.contains(currentBuild) &&
                (
                    line.contains("Module loaded in com.android.systemui") ||
                        line.contains("Hot reload completed")
                )
        }
        val anchor = if (currentAnchor >= 0) {
            currentAnchor
        } else {
            lines.indexOfLast { line ->
                line.contains("Module loaded in com.android.systemui")
            }
        }

        if (anchor < 0) {
            return lines
        }

        val processId = processId(lines[anchor]) ?: return lines.drop(anchor)
        val start = (anchor downTo 0).firstOrNull { index ->
            processId(lines[index]) == processId &&
                lines[index].contains("Module loaded in com.android.systemui")
        } ?: anchor

        return lines
            .subList(start, lines.size)
            .filter { line -> processId(line) == processId }
    }

    private fun processId(line: String): String? =
        ProcessIdRegex.find(line)?.groupValues?.getOrNull(1)

    private val ProcessIdRegex =
        Regex(""":\s*(\d+):\s*\d+\s+[A-Z]/LSPosedFramework""")

    private fun collectionState(result: RootShell.Result): String =
        when {
            result.isSuccess -> "ok"
            result.timedOut -> "timeout"
            result.error != null -> "error:" + result.error
            else -> "exit:" + result.exitCode
        }

    private data class CollectedLog(
        val source: String,
        val result: RootShell.Result,
        val lines: List<String>,
    )
}
