package com.chaners.combinedstatus.xposed

internal object CombinedStatusConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        airplaneMode: Boolean,
        connectivity: SystemUiConnectivityStateSource.State,
        mobileType: NativePresentationResolver.NetworkType?,
    ): CenterIndicator? {
        val wifiSegments =
            when (wifi) {
                CombinedStatusStateStore.WifiState.Unknown -> null
                CombinedStatusStateStore.WifiState.Hidden -> null
                is CombinedStatusStateStore.WifiState.Visible ->
                    when (val signal = wifi.signal) {
                        SignalStrength.Unknown -> null
                        SignalStrength.Unavailable -> 0
                        is SignalStrength.Level -> wifiSegments(signal.value)
                    }
            }

        if (!connectivity.known) {
            if (wifiSegments != null && wifiSegments > 0) {
                return CenterIndicator.Wifi(
                    segments = wifiSegments,
                    internet = InternetState.UNKNOWN,
                )
            }
            if (airplaneMode) {
                return CenterIndicator.Empty
            }
            return mobileType?.let {
                CenterIndicator.MobileType(
                    label = it.label,
                    enhanced = it.enhanced,
                    internet = InternetState.UNKNOWN,
                )
            }
        }

        if (connectivity.transport == SystemUiConnectivityStateSource.Transport.WIFI) {
            if (wifiSegments == null || wifiSegments <= 0) {
                return null
            }
            return CenterIndicator.Wifi(
                segments = wifiSegments,
                internet = connectivity.internetState(),
            )
        }

        if (airplaneMode) {
            return CenterIndicator.Empty
        }

        return when (connectivity.transport) {
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
                when {
                    wifiSegments != null && wifiSegments > 0 ->
                        CenterIndicator.Wifi(
                            segments = wifiSegments,
                            internet = connectivity.internetState(),
                        )

                    connectivity.mobileDataEnabled == true && mobileType != null ->
                        CenterIndicator.MobileType(
                            label = mobileType.label,
                            enhanced = mobileType.enhanced,
                            internet = connectivity.internetState(),
                        )

                    else -> CenterIndicator.Empty
                }

            SystemUiConnectivityStateSource.Transport.NONE ->
                CenterIndicator.Empty

            SystemUiConnectivityStateSource.Transport.WIFI ->
                error("Wi-Fi transport handled above")
        }
    }

    private fun SystemUiConnectivityStateSource.State.internetState(): InternetState =
        if (validated && hasInternetCapability) {
            InternetState.VALIDATED
        } else {
            InternetState.NO_INTERNET
        }

    private fun wifiSegments(level: Int): Int =
        when {
            level <= 0 -> 1
            level == 1 -> 2
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
