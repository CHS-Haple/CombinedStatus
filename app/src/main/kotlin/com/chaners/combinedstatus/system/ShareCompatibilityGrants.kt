package com.chaners.combinedstatus.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.chaners.combinedstatus.BuildConfig

internal object ShareCompatibilityGrants {
    private const val ShareLogTag = "CombinedStatusShare"

    private val targetPackages = listOf(
        "com.tencent.mobileqq",
        "com.tencent.mm",
    )

    fun grantKnownReceivers(
        context: Context,
        uri: Uri,
    ) {
        targetPackages.forEach { packageName ->
            val error = runCatching {
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.exceptionOrNull()

            log(
                context = context,
                message =
                    if (error == null) {
                        "compatibilityGrant package=$packageName result=ok"
                    } else {
                        "compatibilityGrant package=$packageName result=failed " +
                            "error=${error.javaClass.simpleName}"
                    },
                error = error != null,
            )
        }
    }

    fun revokeKnownReceivers(
        context: Context,
        uri: Uri,
    ) {
        targetPackages.forEach { packageName ->
            val error = runCatching {
                context.revokeUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.exceptionOrNull()

            log(
                context = context,
                message =
                    if (error == null) {
                        "compatibilityRevoke package=$packageName result=ok"
                    } else {
                        "compatibilityRevoke package=$packageName result=failed " +
                            "error=${error.javaClass.simpleName}"
                    },
                error = error != null,
            )
        }
    }

    private fun log(
        context: Context,
        message: String,
        error: Boolean,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        if (error) {
            Log.e(ShareLogTag, message)
        } else {
            Log.i(ShareLogTag, message)
        }
        ShareDiagnosticsStore.append(context, message)
    }
}
