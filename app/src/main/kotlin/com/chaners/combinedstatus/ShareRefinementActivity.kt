package com.chaners.combinedstatus

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.chaners.combinedstatus.system.DiagnosticsReportFiles
import com.chaners.combinedstatus.system.ShareDiagnosticsStore

internal class ShareRefinementActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receiver = intent.getParcelableExtra(
            Intent.EXTRA_RESULT_RECEIVER,
            ResultReceiver::class.java,
        )
        val selectedIntent = intent.getParcelableExtra(
            Intent.EXTRA_INTENT,
            Intent::class.java,
        )
        val sharedUri = intent.getParcelableExtra(
            EXTRA_SHARED_URI,
            Uri::class.java,
        )
        val clipLabel = intent.getStringExtra(EXTRA_CLIP_LABEL)
            ?.takeIf(String::isNotBlank)
            ?: DefaultClipLabel

        if (receiver == null) {
            logRefinement("refinement result=unavailable reason=no-receiver", error = true)
            finish()
            return
        }

        if (selectedIntent == null) {
            logRefinement("refinement result=cancel reason=no-selected-intent", error = true)
            receiver.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val validUri =
            sharedUri?.scheme == "content" &&
                sharedUri.authority == "${BuildConfig.APPLICATION_ID}.fileprovider"

        val directTarget =
            selectedIntent.component?.packageName
                ?: selectedIntent.`package`
                ?: selectedIntent.selector?.component?.packageName
                ?: selectedIntent.selector?.`package`

        val resolvedTarget =
            directTarget
                ?: resolveTargetPackage(selectedIntent)
                ?: selectedIntent.selector?.let(::resolveTargetPackage)

        var grantSucceeded = false
        var grantError: Throwable? = null

        if (validUri && !resolvedTarget.isNullOrBlank()) {
            grantError = runCatching {
                grantUriPermission(
                    resolvedTarget,
                    sharedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.exceptionOrNull()
            grantSucceeded = grantError == null
        }

        val finalIntent = Intent(selectedIntent).apply {
            if (validUri) {
                if (type.isNullOrBlank()) {
                    type = DiagnosticsReportFiles.ShareMimeType
                }
                putExtra(Intent.EXTRA_STREAM, sharedUri)
                clipData = ClipData.newUri(
                    contentResolver,
                    clipLabel,
                    sharedUri,
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        logRefinement(
            buildString {
                append("refinement result=ok ")
                append("directTarget=")
                append(directTarget.orEmpty())
                append(" resolvedTarget=")
                append(resolvedTarget.orEmpty())
                append(" component=")
                append(selectedIntent.component?.flattenToShortString().orEmpty())
                append(" package=")
                append(selectedIntent.`package`.orEmpty())
                append(" selectorComponent=")
                append(selectedIntent.selector?.component?.flattenToShortString().orEmpty())
                append(" selectorPackage=")
                append(selectedIntent.selector?.`package`.orEmpty())
                append(" uriValid=")
                append(validUri)
                append(" grantAttempted=")
                append(validUri && !resolvedTarget.isNullOrBlank())
                append(" grant=")
                append(grantSucceeded)
                if (grantError != null) {
                    append(" grantError=")
                    append(grantError.javaClass.simpleName)
                }
                append(" stream=")
                append(finalIntent.hasExtra(Intent.EXTRA_STREAM))
                append(" clipItems=")
                append(finalIntent.clipData?.itemCount ?: 0)
                append(" flags=0x")
                append(finalIntent.flags.toString(16))
            },
        )

        receiver.send(
            RESULT_OK,
            Bundle().apply {
                putParcelable(Intent.EXTRA_INTENT, finalIntent)
            },
        )
        finish()
    }

    private fun resolveTargetPackage(intent: Intent): String? =
        runCatching {
            packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }.getOrNull()

    private fun logRefinement(
        message: String,
        error: Boolean = false,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        if (error) {
            Log.e(ShareLogTag, message)
        } else {
            Log.i(ShareLogTag, message)
        }
        ShareDiagnosticsStore.append(this, message)
    }

    internal companion object {
        const val EXTRA_SHARED_URI =
            "com.chaners.combinedstatus.extra.SHARE_REFINEMENT_URI"
        const val EXTRA_CLIP_LABEL =
            "com.chaners.combinedstatus.extra.SHARE_REFINEMENT_CLIP_LABEL"

        private const val ShareLogTag = "CombinedStatusShare"
        private const val DefaultClipLabel = "diagnostic-report"
    }
}
