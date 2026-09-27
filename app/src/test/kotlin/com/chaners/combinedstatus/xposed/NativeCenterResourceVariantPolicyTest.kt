package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeCenterResourceVariantPolicyTest {
    @Test
    fun baseResourceMapsToTintVariant() {
        assertEquals(
            "stat_sys_wifi_signal_3_tint",
            NativeCenterResourceVariantPolicy.tintEntryName(
                "stat_sys_wifi_signal_3",
            ),
        )
    }

    @Test
    fun darkResourceNormalizesBeforeTintSelection() {
        assertEquals(
            "stat_sys_wifi_signal_3_tint",
            NativeCenterResourceVariantPolicy.tintEntryName(
                "stat_sys_wifi_signal_3_darkmode",
            ),
        )
    }

    @Test
    fun existingTintResourceRemainsStable() {
        assertEquals(
            "stat_sys_wifi_signal_unavailable_2_tint",
            NativeCenterResourceVariantPolicy.tintEntryName(
                "stat_sys_wifi_signal_unavailable_2_tint",
            ),
        )
    }

    @Test
    fun hotspotFamilyUsesSamePresentationSuffixContract() {
        assertEquals(
            "stat_sys_hotspot_signal_3_tint",
            NativeCenterResourceVariantPolicy.tintEntryName(
                "stat_sys_hotspot_signal_3",
            ),
        )
    }
}
