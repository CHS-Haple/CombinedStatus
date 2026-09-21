package com.chaners.combinedstatus.system

import android.net.Uri
import androidx.core.content.FileProvider

internal class DiagnosticShareFileProvider : FileProvider() {
    override fun getType(uri: Uri): String = DiagnosticsReportFiles.ShareMimeType
}
