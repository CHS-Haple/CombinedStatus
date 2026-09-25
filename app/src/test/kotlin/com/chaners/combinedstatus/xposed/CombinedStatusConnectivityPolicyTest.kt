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
                segments = 2,
                internet = InternetState.NO_INTERNET,
                nativeResourceId = 1,
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
                nativeResourceId = 1,
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
                nativeResourceId = 1,
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
    fun airplaneModeUsesNativeAirplaneCenterWhenWifiIsAbsent() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun airplaneModeUsesAirplaneCenterBeforeConnectivityIsKnown() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity = SystemUiConnectivityStateSource.State.Unknown,
                mobileType = null,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun visibleWifiRemainsCenterPriorityDuringAirplaneMode() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Level(2),
                        internetValidated = true,
                    ),
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.WIFI,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
            )

        assertEquals(
            CenterIndicator.Wifi(
                segments = 2,
                internet = InternetState.VALIDATED,
                nativeResourceId = 1,
            ),
            result,
        )
    }

    @Test
    fun noSimUsesNativeCenterWhenWifiIsAbsent() {
        val nativeNoSim =
            CombinedStatusPresentationStateStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
                noSimIcon = nativeNoSim,
            )

        assertEquals(CenterIndicator.NoSim(nativeNoSim), result)
    }

    @Test
    fun airplaneRemainsHigherPriorityThanNoSim() {
        val nativeNoSim =
            CombinedStatusPresentationStateStore.NativeIconResource(
                packageName = "com.android.systemui",
                resourceId = 42,
            )

        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = true,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.NONE,
                        validated = false,
                        hasInternetCapability = false,
                        mobileDataEnabled = false,
                    ),
                mobileType = null,
                noSimIcon = nativeNoSim,
            )

        assertEquals(CenterIndicator.Airplane, result)
    }

    @Test
    fun vpnDefaultNetworkDoesNotSuppressAuthoritativeMobileTypeAtBootstrap() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.VPN,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
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
            CenterIndicator.MobileType(
                label = "5G",
                enhanced = false,
                internet = InternetState.VALIDATED,
            ),
            result,
        )
    }

    @Test
    fun vpnOnlyTransportWaitsForAuthoritativeWifiAbsence() {
        val mobileType =
            NativePresentationResolver.NetworkType(
                label = "5G",
                enhanced = false,
                source =
                    NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        val connectivity =
            SystemUiConnectivityStateSource.State(
                known = true,
                transport = SystemUiConnectivityStateSource.Transport.VPN,
                validated = true,
                hasInternetCapability = true,
                mobileDataEnabled = false,
            )

        assertNull(
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Unknown,
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
        assertNull(
            CombinedStatusConnectivityPolicy.resolve(
                wifi =
                    CombinedStatusStateStore.WifiState.Visible(
                        iconResId = 1,
                        signal = SignalStrength.Unknown,
                        internetValidated = null,
                    ),
                airplaneMode = false,
                connectivity = connectivity,
                mobileType = mobileType,
            ),
        )
    }

    @Test
    fun genericOtherTransportKeepsExistingMobileDataGate() {
        val result =
            CombinedStatusConnectivityPolicy.resolve(
                wifi = CombinedStatusStateStore.WifiState.Hidden,
                airplaneMode = false,
                connectivity =
                    SystemUiConnectivityStateSource.State(
                        known = true,
                        transport = SystemUiConnectivityStateSource.Transport.OTHER,
                        validated = true,
                        hasInternetCapability = true,
                        mobileDataEnabled = false,
                    ),
                mobileType =
                    NativePresentationResolver.NetworkType(
                        label = "5G",
                        enhanced = false,
                        source =
                            NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    ),
            )

        assertEquals(CenterIndicator.Empty, result)
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
