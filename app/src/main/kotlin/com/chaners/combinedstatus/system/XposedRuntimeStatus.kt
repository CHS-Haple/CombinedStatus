package com.chaners.combinedstatus.system

internal sealed interface XposedRuntimeStatus {
    data object Checking : XposedRuntimeStatus

    data object FrameworkUnavailable : XposedRuntimeStatus

    data object QueryUnavailable : XposedRuntimeStatus

    data class Connected(
        val systemUiInScope: Boolean,
        val systemUiRunning: Boolean,
    ) : XposedRuntimeStatus
}
