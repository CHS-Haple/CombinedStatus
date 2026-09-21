package com.chaners.combinedstatus.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.chaners.combinedstatus.BuildConfig
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagnosticsReportFiles {
    private const val ShareDirectoryName = "diagnostics-share"
    private const val MaxSharedReports = 3
    private val MaxSharedReportAgeMillis = TimeUnit.HOURS.toMillis(24)
    private val FileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun suggestedFileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "CombinedStatus-Diagnostic-${BuildConfig.BUILD_ID}-${now.format(FileTimestampFormatter)}.txt"

    suspend fun writeExport(
        context: Context,
        uri: Uri,
        report: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Unable to open export destination")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(report)
            }
        }.isSuccess
    }

    suspend fun prepareShare(
        context: Context,
        report: String,
    ): PreparedShare? = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, ShareDirectoryName).apply {
                if (!exists() && !mkdirs()) {
                    error("Unable to create diagnostic share directory")
                }
            }

            pruneBeforeNewShare(context, directory)

            val file = uniqueShareFile(directory).apply {
                writeText(report, Charsets.UTF_8)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            )

            PreparedShare(uri = uri, file = file)
        }.getOrNull()
    }

    fun discardShare(
        context: Context,
        preparedShare: PreparedShare,
    ) {
        deleteSharedReport(context, preparedShare.file)
    }

    private fun uniqueShareFile(directory: File): File {
        val baseName = suggestedFileName().removeSuffix(".txt")
        var candidate = File(directory, "$baseName.txt")
        var suffix = 2

        while (candidate.exists()) {
            candidate = File(directory, "$baseName-$suffix.txt")
            suffix += 1
        }

        return candidate
    }

    private fun pruneBeforeNewShare(
        context: Context,
        directory: File,
    ) {
        val now = System.currentTimeMillis()
        val reports = directory
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.extension.equals("txt", ignoreCase = true) }

        reports
            .filter { file -> now - file.lastModified() > MaxSharedReportAgeMillis }
            .forEach { file -> deleteSharedReport(context, file) }

        directory
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.extension.equals("txt", ignoreCase = true) }
            .sortedByDescending(File::lastModified)
            .drop(MaxSharedReports - 1)
            .forEach { file -> deleteSharedReport(context, file) }
    }

    private fun deleteSharedReport(
        context: Context,
        file: File,
    ) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file,
            )
            context.revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        runCatching { file.delete() }
    }

    internal data class PreparedShare(
        val uri: Uri,
        val file: File,
    )
}
