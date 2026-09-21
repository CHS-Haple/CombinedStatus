package com.chaners.combinedstatus

import android.app.Activity
import android.content.ClipData
import android.content.Intent
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

        if (
            receiver == null ||
            selectedIntent == null ||
            sharedUri?.scheme != "content" ||
            sharedUri.authority != "${BuildConfig.APPLICATION_ID}.fileprovider"
        ) {
            logRefinement(
                "refinement result=cancel reason=invalid-input " +
                    "receiver=${receiver != null} selectedIntent=${selectedIntent != null} " +
                    "uriAuthority=${sharedUri?.authority.orEmpty()}",
                error = true,
            )
            receiver?.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val targetPackage =
            selectedIntent.component?.packageName
                ?: selectedIntent.`package`
                ?: selectedIntent.selector?.component?.packageName
                ?: selectedIntent.selector?.`package`

        if (targetPackage.isNullOrBlank()) {
            logRefinement(
                "refinement result=cancel reason=no-target " +
                    "component=${selectedIntent.component?.flattenToShortString().orEmpty()}",
                error = true,
            )
            receiver.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val grantError = runCatching {
            grantUriPermission(
                targetPackage,
                sharedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.exceptionOrNull()

        if (grantError != null) {
            logRefinement(
                "refinement result=cancel target=$targetPackage grant=false " +
                    "error=${grantError.javaClass.simpleName}",
                error = true,
            )
            receiver.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val finalIntent = Intent(selectedIntent).apply {
            type = DiagnosticsReportFiles.ShareMimeType
            putExtra(Intent.EXTRA_STREAM, sharedUri)
            clipData = ClipData.newUri(
                contentResolver,
                clipLabel,
                sharedUri,
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        logRefinement(
            "refinement result=ok target=$targetPackage " +
                "component=${finalIntent.component?.flattenToShortString().orEmpty()} " +
                "type=${finalIntent.type.orEmpty()} grant=true " +
                "stream=true clipItems=${finalIntent.clipData?.itemCount ?: 0} " +
                "flags=0x${finalIntent.flags.toString(16)}",
        )

        receiver.send(
            RESULT_OK,
            Bundle().apply {
                putParcelable(Intent.EXTRA_INTENT, finalIntent)
            },
        )
        finish()
    }

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
