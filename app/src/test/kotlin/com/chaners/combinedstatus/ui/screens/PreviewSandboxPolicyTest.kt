package com.chaners.combinedstatus.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSandboxPolicyTest {
    @Test
    fun noInternetWifiUsesExactHyperOsUnavailableFamily() {
        val names = previewWifiResourceNames(
            state = PreviewWifiState.NO_INTERNET,
            level = 3,
        )

        assertEquals(
            listOf("stat_sys_wifi_signal_unavailable_3"),
            names,
        )
        assertFalse(names.contains("stat_sys_wifi_signal_3"))
    }

    @Test
    fun noSimRemainsValidWithWifiSelected() {
        val state =
            PreviewSandboxUiState(
                simPresent = false,
                networkMode = PreviewNetworkMode.WIFI,
            )

        assertEquals(PreviewNetworkMode.WIFI, state.networkMode)
        assertFalse(state.mobileOptionsVisible)
    }

    @Test
    fun mobileSubordinateOptionsFoldWhenUnavailable() {
        val ready =
            PreviewSandboxUiState(
                simPresent = true,
                airplaneMode = false,
                networkMode = PreviewNetworkMode.MOBILE,
            )

        assertTrue(ready.mobileOptionsVisible)
        assertFalse(ready.copy(simPresent = false).mobileOptionsVisible)
        assertFalse(ready.copy(airplaneMode = true).mobileOptionsVisible)
    }
}
