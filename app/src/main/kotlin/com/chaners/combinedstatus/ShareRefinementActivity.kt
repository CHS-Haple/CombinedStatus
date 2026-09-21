package com.chaners.combinedstatus

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver

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

        if (receiver == null || selectedIntent == null) {
            receiver?.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val targetPackage =
            selectedIntent.component?.packageName
                ?: selectedIntent.`package`
        val sharedUri =
            selectedIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                ?: selectedIntent.data

        val validShare =
            targetPackage != null &&
                sharedUri?.scheme == "content" &&
                sharedUri.authority == "${BuildConfig.APPLICATION_ID}.fileprovider"

        if (!validShare) {
            receiver.send(RESULT_CANCELED, Bundle.EMPTY)
            finish()
            return
        }

        val granted = runCatching {
            grantUriPermission(
                targetPackage,
                sharedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess

        if (granted) {
            val result = Bundle().apply {
                putParcelable(Intent.EXTRA_INTENT, selectedIntent)
            }
            receiver.send(RESULT_OK, result)
        } else {
            receiver.send(RESULT_CANCELED, Bundle.EMPTY)
        }

        finish()
    }
}
