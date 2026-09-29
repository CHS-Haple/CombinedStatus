package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeWifiOpticalReferencePolicyTest {
    @Test
    fun connectedResourceKeepsItsOwnLevelReference() {
        assertEquals(
            "stat_sys_wifi_signal_3",
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun unavailableResourceMapsToConnectedPeerAtSameLevel() {
        assertEquals(
            "stat_sys_wifi_signal_2",
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_unavailable_2",
            ),
        )
    }

    @Test
    fun hotspotResourceMapsToConnectedPeerAtSameLevel() {
        assertEquals(
            "stat_sys_wifi_signal_1",
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "stat_sys_hotspot_signal_1",
            ),
        )
    }

    @Test
    fun qualifiedUnavailableResourceMapsToConnectedPeer() {
        assertEquals(
            "stat_sys_wifi_signal_3",
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "com.android.systemui:drawable/stat_sys_wifi_signal_unavailable_3",
            ),
        )
    }

    @Test
    fun tintLikeConnectedVariantMapsBackToBaseReference() {
        assertEquals(
            "stat_sys_wifi_signal_2",
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "stat_sys_wifi_signal_2_tint",
            ),
        )
    }

    @Test
    fun nonWifiResourceHasNoReference() {
        assertNull(
            NativeWifiOpticalReferencePolicy.connectedReferenceEntry(
                "stat_sys_signal_4",
            ),
        )
    }

    @Test
    fun referenceViewportMustMatchExactly() {
        assertTrue(
            NativeWifiOpticalReferencePolicy.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 24,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
        assertFalse(
            NativeWifiOpticalReferencePolicy.canShareReferenceViewport(
                currentWidth = 24,
                currentHeight = 20,
                referenceWidth = 24,
                referenceHeight = 24,
            ),
        )
    }
}
