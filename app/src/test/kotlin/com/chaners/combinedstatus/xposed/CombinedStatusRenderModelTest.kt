package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CombinedStatusRenderModelTest {
    @Test
    fun defaultDataSubscriptionWins() {
        val snapshot = snapshot(
            wifi = CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(3),
            ),
            mobile = mapOf(
                1 to CombinedStatusStateStore.MobileState(signal = SignalStrength.Level(1)),
                4 to CombinedStatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
        )

        val model = CombinedStatusRenderModel.from(snapshot, defaultDataSubscriptionId = 1)

        assertEquals(1, model?.mobileSubscriptionId)
        assertEquals(1, model?.mobileLevel)
        assertEquals(3, model?.wifiSegments)
    }

    @Test
    fun knownFallbackIsUsedWhenDefaultDataIsNotReady() {
        val snapshot = snapshot(
            wifi = CombinedStatusStateStore.WifiState.Hidden,
            mobile = mapOf(
                1 to CombinedStatusStateStore.MobileState(signal = SignalStrength.Unknown),
                4 to CombinedStatusStateStore.MobileState(signal = SignalStrength.Level(4)),
            ),
        )

        val model = CombinedStatusRenderModel.from(snapshot, defaultDataSubscriptionId = 1)

        assertEquals(4, model?.mobileSubscriptionId)
        assertEquals(4, model?.mobileLevel)
        assertNull(model?.wifiSegments)
    }

    @Test
    fun wifiLevelsMatchLegacyThreeSegmentPolicy() {
        val expected = mapOf(0 to 1, 1 to 2, 2 to 3, 3 to 3)
        expected.forEach { (level, segments) ->
            val model = CombinedStatusRenderModel.from(
                snapshot(
                    wifi = CombinedStatusStateStore.WifiState.Visible(
                        iconResId = level,
                        signal = SignalStrength.Level(level),
                    ),
                    mobile = mapOf(
                        1 to CombinedStatusStateStore.MobileState(
                            signal = SignalStrength.Level(4),
                        ),
                    ),
                ),
                defaultDataSubscriptionId = 1,
            )
            assertEquals(segments, model?.wifiSegments)
        }
    }

    @Test
    fun unavailableMobileUsesLegacyUnavailableGlyphState() {
        val model = CombinedStatusRenderModel.from(
            snapshot(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                mobile = mapOf(
                    1 to CombinedStatusStateStore.MobileState(
                        signal = SignalStrength.Unavailable,
                    ),
                ),
            ),
            defaultDataSubscriptionId = 1,
        )

        assertNull(model?.mobileLevel)
    }

    @Test
    fun unknownNetworkStateDoesNotRender() {
        assertNull(
            CombinedStatusRenderModel.from(
                snapshot(
                    wifi = CombinedStatusStateStore.WifiState.Unknown,
                    mobile = emptyMap(),
                ),
                defaultDataSubscriptionId = 1,
            ),
        )
    }

    private fun snapshot(
        wifi: CombinedStatusStateStore.WifiState,
        mobile: Map<Int, CombinedStatusStateStore.MobileState>,
    ) =
        CombinedStatusStateStore.Snapshot(
            battery = CombinedStatusStateStore.BatteryState(
                percent = 80,
                charging = false,
                plugged = 0,
            ),
            wifi = wifi,
            mobile = mobile,
        )
}
