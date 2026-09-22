package com.chaners.combinedstatus.system

import android.content.Context
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.settings.DiagnosticsSettingsRepository
import java.time.OffsetDateTime

internal object DiagnosticsReportBuilder {
    private const val LogTimeoutSeconds = 10L
    private const val DetailedLogLineLimit = 600
    private const val ReleaseLogLineLimit = 120
    private const val ShareLogLineLimit = 80

    private const val LsposedModuleLogCommand =
        "for f in \$(ls -1t /data/adb/lspd/log/modules_*.log " +
            "/data/adb/lspd/log.old/modules_*.log 2>/dev/null | head -n 8); do " +
            "if grep -Fq 'com.chaners.combinedstatus' \"\$f\"; then " +
            "grep -F 'com.chaners.combinedstatus' \"\$f\" || true; break; fi; done"

    private const val LogcatCommand =
        "logcat -d -b all -v threadtime -t 3000"

    private const val ShareLogcatCommand =
        "logcat -d -b all -v threadtime -t 3000 | grep -F 'CombinedStatusShare' || true"

    private const val ShareSystemLogCommand =
        "logcat -d -b all -v threadtime -t 5000 | " +
            "grep -Ei 'com\\.tencent\\.mobileqq|com\\.tencent\\.mm|Permission Denial|" +
            "SecurityException|FileProvider|combinedstatus\\.fileprovider|No such file|ENOENT' || true"

    suspend fun build(context: Context): String {
        val environment = RuntimeEnvironmentInfo.resolve(context)
        val diagnosticsLevel = DiagnosticsSettingsRepository(context).currentLevel()
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

        val shareLogResult = RootShell.execute(
            command = ShareLogcatCommand,
            timeoutSeconds = LogTimeoutSeconds,
        )
        val shareLines = ShareDiagnosticsStore.read(context)
            .takeLast(ShareLogLineLimit)
        val shareSystemLogResult = RootShell.execute(
            command = ShareSystemLogCommand,
            timeoutSeconds = LogTimeoutSeconds,
        )
        val shareSystemLines = shareSystemLogResult.output
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
            .takeLast(ShareLogLineLimit)

        val lineLimit =
            if (
                diagnosticsLevel.name == "Detailed" &&
                (BuildConfig.RUNTIME_DIAGNOSTICS || BuildConfig.DEVELOPMENT_PROBES)
            ) {
                DetailedLogLineLimit
            } else {
                ReleaseLogLineLimit
            }
        val sessionLines = selectLatestSession(selected.lines)
        val runtimeHealth = RuntimeHealthSnapshot.fromLines(sessionLines)
        val moduleLines = sessionLines.takeLast(lineLimit)
        val requestedDiagnosticsLevel = diagnosticsLevel.name.lowercase()
        val runtimeDiagnostics = runtimeHealth.component("diagnostics")
        val effectiveDiagnosticsLevel = runtimeDiagnostics?.fields?.get("level")
        val diagnosticsSyncState =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS && !BuildConfig.DEVELOPMENT_PROBES ->
                    "not-applicable"
                runtimeDiagnostics == null ||
                    runtimeDiagnostics.state == "unknown" ||
                    runtimeDiagnostics.state == "unavailable" ->
                    "unavailable"
                BuildConfig.DEVELOPMENT_PROBES ->
                    if (effectiveDiagnosticsLevel == "detailed") {
                        "development-forced"
                    } else {
                        "mismatch"
                    }
                effectiveDiagnosticsLevel == requestedDiagnosticsLevel ->
                    "matched"
                else ->
                    "mismatch"
            }

        return buildString {
            appendLine("CombinedStatus Diagnostic Report")
            appendLine()
            appendLine("[App]")
            appendLine("version=" + BuildConfig.VERSION_NAME)
            appendLine("build=" + BuildConfig.BUILD_ID)
            appendLine("package=" + BuildConfig.APPLICATION_ID)
            appendLine("buildType=" + BuildConfig.BUILD_TYPE)
            appendLine("channel=" + BuildConfig.BUILD_CHANNEL)
            appendLine("diagnosticsPreference=" + diagnosticsLevel.name.lowercase())
            appendLine(
                "diagnosticsCapability=" +
                    when {
                        BuildConfig.DEVELOPMENT_PROBES -> "development"
                        BuildConfig.RUNTIME_DIAGNOSTICS -> "runtime"
                        else -> "release"
                    },
            )
            appendLine()
            appendLine("[Diagnostics state]")
            appendLine("requestedLevel=" + requestedDiagnosticsLevel)
            appendLine("effectiveRuntimeLevel=" + (effectiveDiagnosticsLevel ?: "unavailable"))
            appendLine("syncState=" + diagnosticsSyncState)
            appendLine("schemaVersion=" + runtimeHealth.schemaVersion)
            appendLine("sessionId=" + (runtimeHealth.sessionId ?: "legacy-or-unavailable"))
            appendLine()
            appendLine("[Device]")
            appendLine("manufacturer=" + environment.manufacturer)
            appendLine("name=" + environment.deviceName)
            appendLine("model=" + environment.model)
            appendLine("device=" + environment.codename)
            appendLine("android=" + environment.androidVersion)
            appendLine("sdk=" + environment.sdk)
            appendLine("os=" + environment.osVersion)
            appendLine("systemUiVersion=" + environment.systemUiVersionName)
            appendLine(
                "systemUiVersionCode=" +
                    (environment.systemUiVersionCode?.toString() ?: "unknown"),
            )
            appendLine()
            appendLine("[Runtime health]")
            appendLine("source=structured-runtime-events")
            runtimeHealth.reportLines().forEach(::appendLine)
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
            appendLine("[Share diagnostics]")
            appendLine("source=persistent-store")
            appendLine("logcatCollection=" + collectionState(shareLogResult))
            appendLine("lines=" + shareLines.size)
            if (shareLines.isEmpty()) {
                appendLine("No share diagnostic entries were available.")
            } else {
                shareLines.forEach(::appendLine)
            }
            appendLine()
            appendLine("[Share system log]")
            appendLine("source=logcat")
            appendLine("collection=" + collectionState(shareSystemLogResult))
            appendLine("lines=" + shareSystemLines.size)
            if (shareSystemLines.isEmpty()) {
                appendLine("No relevant share-system entries were available.")
            } else {
                shareSystemLines.forEach(::appendLine)
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

        val structuredEvents =
            lines.mapIndexedNotNull { index, line ->
                RuntimeDiagnosticsProtocol.parse(line)
                    ?.fields
                    ?.get("sessionId")
                    ?.let { sessionId -> index to sessionId }
            }
        val latestSessionId = structuredEvents.lastOrNull()?.second
        if (latestSessionId != null) {
            val start =
                structuredEvents.firstOrNull { (_, sessionId) ->
                    sessionId == latestSessionId
                }?.first
                    ?: return lines
            val processId = processId(lines[start])
            if (processId == null) {
                return lines.drop(start)
            }
            return lines
                .subList(start, lines.size)
                .filter { line -> processId(line) == processId }
        }

        val currentBuild = "build=" + BuildConfig.BUILD_ID
        val currentAnchor = lines.indexOfLast { line ->
            line.contains(currentBuild) &&
                (
                    line.contains("Module loaded in com.android.systemui") ||
                        line.contains("Hot reload completed")
                )
        }
        val anchor =
            if (currentAnchor >= 0) {
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
        val start =
            (anchor downTo 0).firstOrNull { index ->
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
