package com.chaners.combinedstatus.xposed

internal object CombinedStatusPresentationStateStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun markWifiSemanticChanged(): Snapshot {
        current =
            current.copy(
                wifiSemanticChangedAtNanos = monotonicNow(),
            )
        return current
    }

    @Synchronized
    fun updateConnectivity(
        state: SystemUiConnectivityStateSource.State,
    ): Snapshot? {
        val wasStaleForWifi =
            current.connectivityObservedAtNanos <
                current.wifiSemanticChangedAtNanos
        val stateChanged = current.connectivity != state
        current =
            current.copy(
                connectivity = state,
                connectivityObservedAtNanos = monotonicNow(),
            )
        return if (stateChanged || wasStaleForWifi) {
            current
        } else {
            null
        }
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
        val wifiSemanticChangedAtNanos: Long = 0L,
        val connectivityObservedAtNanos: Long = 0L,
    ) {
        val connectivityFreshForWifi: Boolean
            get() =
                connectivityObservedAtNanos >=
                    wifiSemanticChangedAtNanos
    }

    private fun monotonicNow(): Long = System.nanoTime()
}
