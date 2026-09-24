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
