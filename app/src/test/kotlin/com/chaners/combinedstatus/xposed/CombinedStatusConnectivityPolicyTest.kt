package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusConnectivityPolicyTest {
    @Test
    fun systemUiWifiNoInternetWinsEvenWhenCellularIsDefault() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = false,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType =
                    NativePresentationResolver.NetworkType(
                        label = "5G",
                        enhanced = false,
                        source =
                            NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    ),
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 2,
                internet = InternetState.NO_INTERNET,
            ),
            result,
        )
    }

    @Test
    fun systemUiWifiLevelRemainsAuthoritativeAcrossDefaultTransportChanges() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(0),
                        internetValidated = true,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 0,
                internet = InternetState.VALIDATED,
            ),
            result,
        )
    }

    @Test
    fun connectivityOnlyFillsUnknownWifiInternetSemantics() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(1),
                        internetValidated = null,
                    ),
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
                        validated = false,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 1,
                internet = InternetState.NO_INTERNET,
            ),
            result,
        )
    }

    @Test
    fun systemUiWifiLevelsRemainFourDistinctVisualStates() {
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val segments =
            (0..3).map { level ->
                val result =
                    CombinedStatusConnectivityPolicy.resolve(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = level + 1,
                                signal = SignalStrength.Level(level),
                                internetValidated = true,
                            ),
                        airplaneMode = false,
                        connectivity = connectivity,
                        mobileType = null,
                    )
                (result as CenterIndicator.Wifi).segments
            }

        assertEquals(listOf(0, 1, 2, 3), segments)
    }

    @Test
    fun unavailableWifiSignalDoesNotMasqueradeAsLevelZero() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Unavailable,
                internetValidated = true,
            )
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
                mobileType = null,
            )

        assertEquals(CenterIndicator.Empty, result)
        assertEquals(
            false,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            ),
        )
    }

    @Test
    fun nativeWifiReplacementIsReadyWhenSystemUiProvidesInternetSemantics() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = false,
            )

        val ready =
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(true, ready)
    }

    @Test
    fun nativeWifiReplacementUsesConnectivityOnlyWhenWifiIsDefault() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(1),
                internetValidated = null,
            )
        val wifiDefault =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val cellularDefault =
            wifiDefault.copy(
                transport = SystemUiConnectivityStateSource.Transport.CELLULAR,
            )

        assertEquals(
            true,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = wifiDefault,
            ),
        )
        assertEquals(
            false,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = cellularDefault,
            ),
        )
    }

    @Test
    fun unknownWifiSignalNeverClaimsNativeReplacement() {
        val ready =
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Unknown,
                        internetValidated = true,
                    ),
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = true,
                    ),
            )

        assertEquals(false, ready)
    }
}
