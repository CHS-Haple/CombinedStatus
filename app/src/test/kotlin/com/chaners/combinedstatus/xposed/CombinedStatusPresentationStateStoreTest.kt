package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CombinedStatusPresentationStateStoreTest {
    @Test
    fun repeatedConnectivityObservationIsDeduplicatedByValue() {
        CombinedStatusPresentationStateStore.reset()
        val state =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val first =
            CombinedStatusPresentationStateStore.updateConnectivity(state)
        val repeated =
            CombinedStatusPresentationStateStore.updateConnectivity(state)

        assertSame(state, first?.connectivity)
        assertNull(repeated)
    }
}
