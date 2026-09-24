package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
                iconResId = 1,
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
                iconResId = 1,
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
                iconResId = 1,
                internet = InternetState.NO_INTERNET,
            ),
            result,
        )
    }

    @Test
    fun otherTransportDoesNotRenderWifiWhenSystemUiInternetSemanticsAreUnknown() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.OTHER,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )

        assertEquals(
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            ),
            CombinedStatusConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
        assertEquals(
            false,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = connectivity,
            ),
        )
    }

    @Test
    fun unknownConnectivityDoesNotRenderWifiWhenSystemUiInternetSemanticsAreUnknown() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = 1,
                signal = SignalStrength.Level(2),
                internetValidated = null,
            )
        val connectivity = SystemUiConnectivityStateSource.State.Unknown

        assertNull(
            CombinedStatusConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = null,
            ),
        )
        assertEquals(
            false,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = connectivity,
            ),
        )
    }

    @Test
    fun systemUiWifiResourceIdentityRemainsAuthoritativeAcrossSignalLevels() {
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        val resourceIds =
            (0..3).map { level ->
                val result =
                    CombinedStatusConnectivityPolicy.resolve(
                        wifi =
                            CombinedStatusStateStore.WifiState.Visible(
                                iconResId = 100 + level,
                                signal = SignalStrength.Level(level),
                                internetValidated = true,
                            ),
                        airplaneMode = false,
                        connectivity = connectivity,
                        mobileType = null,
                    )
                (result as CenterIndicator.Wifi).iconResId
            }

        assertEquals(listOf(100, 101, 102, 103), resourceIds)
    }

    @Test
    fun missingWifiResourceNeverClaimsNativeReplacement() {
        val wifi =
            CombinedStatusStateStore.WifiState.Visible(
                iconResId = null,
                signal = SignalStrength.Level(2),
                internetValidated = true,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.WIFI,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = true,
            )

        assertNull(
            CombinedStatusConnectivityPolicy.resolve(
                wifi = wifi,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = null,
            ),
        )
        assertEquals(
            false,
            CombinedStatusConnectivityPolicy.wifiReplacementReady(
                wifi = wifi,
                connectivity = connectivity,
            ),
        )
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
