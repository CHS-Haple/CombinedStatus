package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusPresentationStateStoreTest {
    @Test
    fun repeatedConnectivityObservationAfterWifiSemanticChangeRefreshesFreshness() {
        CombinedStatusPresentationStateStore.reset()
        val state =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        CombinedStatusPresentationStateStore.updateConnectivity(state)
        CombinedStatusPresentationStateStore.markWifiSemanticChanged()

        val refreshed =
            CombinedStatusPresentationStateStore.updateConnectivity(state)

        assertNotNull(refreshed)
        assertTrue(CombinedStatusPresentationStateStore.snapshot().connectivityFreshForWifi)
    }
}
