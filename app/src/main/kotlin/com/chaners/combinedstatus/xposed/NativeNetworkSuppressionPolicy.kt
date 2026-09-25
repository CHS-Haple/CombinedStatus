package com.chaners.combinedstatus.xposed

internal object NativeNetworkSuppressionPolicy {
    fun suppressMobile(
        airplaneMode: Boolean?,
        presentation: NativePresentationResolver.Snapshot?,
        wasSuppressed: Boolean,
    ): Boolean {
        if (airplaneMode == true) {
            return true
        }
        if (presentation?.nativeMobileReplacementReady == true) {
            return true
        }
        return airplaneMode == false &&
            wasSuppressed &&
            presentation?.mode == NativePresentationResolver.Mode.UNKNOWN
    }
}
