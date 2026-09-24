package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiSignalParserTest {
    @Test
    fun mobileSignalLevelsCoverZeroThroughFour() {
        for (level in 0..4) {
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.mobile(
                    "com.android.systemui:drawable/stat_sys_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun mobileNullSignalIsUnavailable() {
        assertEquals(
            SignalStrength.Unavailable,
            SystemUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_null",
            ),
        )
    }

    @Test
    fun wifiFallbackCoversNativeAndModernLevelsZeroThroughFour() {
        for (level in 0..4) {
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.wifi(
                    "com.android.systemui:drawable/stat_sys_wifi_signal_" + level,
                ),
            )
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.wifi(
                    "com.android.systemui:drawable/stat_sys_wifi_signal_" +
                        level + "_fully",
                ),
            )
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.wifi(
                    "com.android.systemui:drawable/ic_wifi_" + level,
                ),
            )
            assertEquals(
                SignalStrength.Level(level),
                SystemUiSignalParser.wifi(
                    "com.android.systemui:drawable/ic_no_internet_wifi_signal_" + level,
                ),
            )
        }
    }

    @Test
    fun nativeWifiIconArraysDecodeLevelAndInternetState() {
        val full = intArrayOf(10, 11, 12, 13, 14)
        val noInternet = intArrayOf(20, 21, 22, 23, 24)

        for (level in 0..4) {
            val validated =
                SystemUiWifiSemanticDecoder.decodeResource(
                    resId = full[level],
                    fullIcons = full,
                    noInternetIcons = noInternet,
                    noNetworkIcon = 30,
                )
            assertEquals(SignalStrength.Level(level), validated?.signal)
            assertEquals(InternetState.VALIDATED, validated?.internet)

            val offline =
                SystemUiWifiSemanticDecoder.decodeResource(
                    resId = noInternet[level],
                    fullIcons = full,
                    noInternetIcons = noInternet,
                    noNetworkIcon = 30,
                )
            assertEquals(SignalStrength.Level(level), offline?.signal)
            assertEquals(InternetState.NO_INTERNET, offline?.internet)
        }
    }

    @Test
    fun nativeWifiNoNetworkAndUnknownResourcesRemainExplicit() {
        val noNetwork =
            SystemUiWifiSemanticDecoder.decodeResource(
                resId = 30,
                fullIcons = intArrayOf(10, 11, 12, 13, 14),
                noInternetIcons = intArrayOf(20, 21, 22, 23, 24),
                noNetworkIcon = 30,
            )
        assertEquals(SignalStrength.Unavailable, noNetwork?.signal)
        assertEquals(InternetState.NO_INTERNET, noNetwork?.internet)

        val unknown =
            SystemUiWifiSemanticDecoder.decodeResource(
                resId = 99,
                fullIcons = intArrayOf(10, 11, 12, 13, 14),
                noInternetIcons = intArrayOf(20, 21, 22, 23, 24),
                noNetworkIcon = 30,
            )
        assertEquals(null, unknown)
    }

    @Test
    fun unknownResourcesStayUnknown() {
        assertEquals(
            SignalStrength.Unknown,
            SystemUiSignalParser.mobile(
                "com.android.systemui:drawable/stat_sys_signal_roaming",
            ),
        )
        assertEquals(
            SignalStrength.Unknown,
            SystemUiSignalParser.wifi(
                "com.android.systemui:drawable/stat_sys_wifi_unavailable",
            ),
        )
        assertEquals(SignalStrength.Unknown, SystemUiSignalParser.wifi(null))
    }
}
