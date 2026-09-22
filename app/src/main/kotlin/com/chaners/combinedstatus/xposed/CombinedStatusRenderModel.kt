package com.chaners.combinedstatus.xposed

internal data class CombinedStatusRenderModel(
    val batteryPercent: Int,
    val charging: Boolean,
    val wifiSegments: Int?,
    val mobileLevel: Int?,
    val mobileSubscriptionId: Int,
) {
    companion object {
        fun from(
            snapshot: CombinedStatusStateStore.Snapshot,
            defaultDataSubscriptionId: Int,
        ): CombinedStatusRenderModel? {
            val battery = snapshot.battery ?: return null
            val wifiSegments = when (val wifi = snapshot.wifi) {
                CombinedStatusStateStore.WifiState.Unknown -> return null
                CombinedStatusStateStore.WifiState.Hidden -> null
                is CombinedStatusStateStore.WifiState.Visible -> {
                    when (val signal = wifi.signal) {
                        SignalStrength.Unknown -> return null
                        SignalStrength.Unavailable -> null
                        is SignalStrength.Level -> wifiSegments(signal.value)
                    }
                }
            }

            if (snapshot.airplaneMode == true) {
                val selectedSubscriptionId =
                    defaultDataSubscriptionId
                        .takeIf { it >= 0 }
                        ?: snapshot.mobile.keys.firstOrNull()
                        ?: -1
                return CombinedStatusRenderModel(
                    batteryPercent = battery.percent.coerceIn(0, 100),
                    charging = battery.charging,
                    wifiSegments = wifiSegments,
                    mobileLevel = null,
                    mobileSubscriptionId = selectedSubscriptionId,
                )
            }

            val selectedMobile =
                snapshot.mobile[defaultDataSubscriptionId]
                    ?.takeIf { it.signal !is SignalStrength.Unknown }
                    ?.let { defaultDataSubscriptionId to it }
                    ?: snapshot.mobile.entries
                        .firstOrNull { it.value.signal !is SignalStrength.Unknown }
                        ?.let { it.key to it.value }
                    ?: return null

            val mobileLevel = when (val signal = selectedMobile.second.signal) {
                SignalStrength.Unknown -> return null
                SignalStrength.Unavailable -> null
                is SignalStrength.Level -> signal.value.coerceIn(0, 4)
            }

            return CombinedStatusRenderModel(
                batteryPercent = battery.percent.coerceIn(0, 100),
                charging = battery.charging,
                wifiSegments = wifiSegments,
                mobileLevel = mobileLevel,
                mobileSubscriptionId = selectedMobile.first,
            )
        }

        private fun wifiSegments(level: Int): Int =
            when {
                level <= 0 -> 1
                level == 1 -> 2
                else -> 3
            }
    }
}
