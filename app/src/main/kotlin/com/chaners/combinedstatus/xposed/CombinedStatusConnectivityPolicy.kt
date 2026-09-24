package com.chaners.combinedstatus.xposed

internal object CombinedStatusConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        airplaneMode: Boolean,
        connectivity: SystemUiConnectivityStateSource.State,
        mobileType: NativePresentationResolver.NetworkType?,
    ): CenterIndicator? {
        val nativeWifi =
            when (wifi) {
                CombinedStatusStateStore.WifiState.Unknown -> null
                CombinedStatusStateStore.WifiState.Hidden -> null
                is CombinedStatusStateStore.WifiState.Visible ->
                    when (val signal = wifi.signal) {
                        SignalStrength.Unknown -> null
                        SignalStrength.Unavailable -> null
                        is SignalStrength.Level ->
                            CenterIndicator.Wifi(
                                segments = wifiSegments(signal.value),
                                internet =
                                    when {
                                        wifi.internet != InternetState.UNKNOWN ->
                                            wifi.internet
                                        connectivity.known &&
                                            connectivity.transport ==
                                                SystemUiConnectivityStateSource.Transport.WIFI ->
                                            connectivity.internetState()
                                        else -> InternetState.UNKNOWN
                                    },
                            )
                    }
            }

        if (nativeWifi != null) {
            return nativeWifi
        }

        if (airplaneMode) {
            return CenterIndicator.Empty
        }

        if (!connectivity.known) {
            return mobileType?.let {
                CenterIndicator.MobileType(
                    label = it.label,
                    enhanced = it.enhanced,
                    internet = InternetState.UNKNOWN,
                )
            }
        }

        return when (connectivity.transport) {
            SystemUiConnectivityStateSource.Transport.WIFI ->
                null

            SystemUiConnectivityStateSource.Transport.CELLULAR ->
                if (connectivity.mobileDataEnabled == false) {
                    CenterIndicator.Empty
                } else {
                    mobileType?.let {
                        CenterIndicator.MobileType(
                            label = it.label,
                            enhanced = it.enhanced,
                            internet = connectivity.internetState(),
                        )
                    } ?: CenterIndicator.Empty
                }

            SystemUiConnectivityStateSource.Transport.OTHER ->
                if (connectivity.mobileDataEnabled == true && mobileType != null) {
                    CenterIndicator.MobileType(
                        label = mobileType.label,
                        enhanced = mobileType.enhanced,
                        internet = connectivity.internetState(),
                    )
                } else {
                    CenterIndicator.Empty
                }

            SystemUiConnectivityStateSource.Transport.NONE ->
                CenterIndicator.Empty
        }
    }

    private fun SystemUiConnectivityStateSource.State.internetState(): InternetState =
        if (validated && hasInternetCapability) {
            InternetState.VALIDATED
        } else {
            InternetState.NO_INTERNET
        }

    internal fun wifiSegments(level: Int): Int =
        when (level.coerceIn(0, 4)) {
            0, 1 -> 1
            2, 3 -> 2
            else -> 3
        }
}

internal enum class InternetState {
    UNKNOWN,
    VALIDATED,
    NO_INTERNET,
}

internal sealed interface CenterIndicator {
    data class Wifi(
        val segments: Int,
        val internet: InternetState,
    ) : CenterIndicator

    data class MobileType(
        val label: String,
        val enhanced: Boolean,
        val internet: InternetState,
    ) : CenterIndicator

    data object Empty : CenterIndicator
}
