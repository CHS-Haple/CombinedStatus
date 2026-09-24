package com.chaners.combinedstatus.xposed

internal object CombinedStatusPresentationStateStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun updateConnectivity(
        state: SystemUiConnectivityStateSource.State,
    ): Snapshot? {
        if (current.connectivity == state) {
            return null
        }
        current = current.copy(connectivity = state)
        return current
    }

    @Synchronized
    fun updateMobilePresentation(
        state: NativePresentationResolver.Snapshot,
    ): Snapshot? {
        if (current.mobilePresentation == state) {
            return null
        }
        current = current.copy(mobilePresentation = state)
        return current
    }

    @Synchronized
    fun reset() {
        current = Snapshot()
    }

    internal data class Snapshot(
        val connectivity: SystemUiConnectivityStateSource.State =
            SystemUiConnectivityStateSource.State.Unknown,
        val mobilePresentation: NativePresentationResolver.Snapshot? = null,
    )
}
