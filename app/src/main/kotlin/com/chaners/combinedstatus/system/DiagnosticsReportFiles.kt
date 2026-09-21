package com.chaners.combinedstatus.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
    private const val ShareLogTag = "CombinedStatusShare"
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

            if (BuildConfig.DEBUG) {
                val probe = runCatching {
                    val mimeType = context.contentResolver.getType(uri)
                    val descriptorSize = context.contentResolver
                        .openFileDescriptor(uri, "r")
                        ?.use { descriptor -> descriptor.statSize }
                        ?: -1L
                    val selfReadable = context.contentResolver
                        .openInputStream(uri)
                        ?.use { input ->
                            input.read()
                            true
                        }
                        ?: false

                    "mime=$mimeType descriptorSize=$descriptorSize selfReadable=$selfReadable"
                }.getOrElse { error ->
                    "probeError=${error.javaClass.simpleName}"
                }

                val message =
                    "prepare file=${file.name} exists=${file.exists()} readable=${file.canRead()} " +
                        "bytes=${file.length()} scheme=${uri.scheme} authority=${uri.authority} $probe"
                Log.i(ShareLogTag, message)
                ShareDiagnosticsStore.append(context, message)
            }

            PreparedShare(uri = uri, file = file)
        }.onFailure { error ->
            if (BuildConfig.DEBUG) {
                val message =
                    "prepare failed error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                Log.e(ShareLogTag, message)
                ShareDiagnosticsStore.append(context, message)
            }
        }.getOrNull()
    }

    fun logMimeCompatibilityProbe(
        context: Context,
        uri: Uri,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        listOf(
            "text/plain",
            "application/octet-stream",
            "*/*",
        ).forEach { mimeType ->
            val probeIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("diagnostic-report", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val activities = runCatching {
                context.packageManager
                    .queryIntentActivities(probeIntent, 0)
                    .mapNotNull { info ->
                        val activity = info.activityInfo ?: return@mapNotNull null
                        "${activity.packageName}/${activity.name}"
                    }
                    .distinct()
                    .sorted()
            }.getOrDefault(emptyList())

            val qqActivities =
                activities.filter { component ->
                    component.startsWith("com.tencent.mobileqq/")
                }
            val wechatActivities =
                activities.filter { component ->
                    component.startsWith("com.tencent.mm/")
                }

            val message =
                "mimeProbe type=$mimeType targets=${activities.size} " +
                    "qq=${qqActivities.joinToString(prefix = "[", postfix = "]")} " +
                    "wechat=${wechatActivities.joinToString(prefix = "[", postfix = "]")}"
            Log.i(ShareLogTag, message)
            ShareDiagnosticsStore.append(context, message)
        }
    }

    fun logShareIntent(
        context: Context,
        intent: Intent,
        uri: Uri,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val packages = runCatching {
            context.packageManager
                .queryIntentActivities(intent, 0)
                .mapNotNull { info -> info.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())

        val message =
            "intent action=${intent.action} type=${intent.type} flags=0x${intent.flags.toString(16)} " +
                "clipItems=${intent.clipData?.itemCount ?: 0} uriAuthority=${uri.authority} " +
                "targets=${packages.size} qq=${"com.tencent.mobileqq" in packages} " +
                "wechat=${"com.tencent.mm" in packages}"
        Log.i(ShareLogTag, message)
        ShareDiagnosticsStore.append(context, message)
    }

    fun logChooserLaunch(
        context: Context,
        error: Throwable? = null,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val message =
            if (error == null) {
                "chooser launch=ok"
            } else {
                "chooser launch=failed error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
            }

        if (error == null) {
            Log.i(ShareLogTag, message)
        } else {
            Log.e(ShareLogTag, message)
        }
        ShareDiagnosticsStore.append(context, message)
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
