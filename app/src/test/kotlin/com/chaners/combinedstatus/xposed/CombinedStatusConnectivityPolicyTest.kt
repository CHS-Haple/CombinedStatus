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
                connectivityFreshForWifi = true,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 3,
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
                connectivityFreshForWifi = true,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 1,
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
                connectivityFreshForWifi = true,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 2,
                internet = InternetState.NO_INTERNET,
            ),
            result,
        )
    }
}
