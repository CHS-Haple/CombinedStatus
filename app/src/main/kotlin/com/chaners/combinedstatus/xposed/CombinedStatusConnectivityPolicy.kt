package com.chaners.combinedstatus.xposed

internal object CombinedStatusConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        mobileSignal: SignalStrength?,
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
            return when {
                wifiSegments != null && wifiSegments > 0 ->
                    CenterIndicator.Wifi(
                        segments = wifiSegments,
                        internet = InternetState.UNKNOWN,
                    )

                !airplaneMode && mobileType != null ->
                    CenterIndicator.MobileType(
                        label = mobileType.label,
                        enhanced = mobileType.enhanced,
                        internet = InternetState.UNKNOWN,
                    )

                else -> null
            }
        }

        if (airplaneMode) {
            return if (
                connectivity.transport == SystemUiConnectivityStateSource.Transport.WIFI &&
                wifiSegments != null &&
                wifiSegments > 0
            ) {
                CenterIndicator.Wifi(
                    segments = wifiSegments,
                    internet = connectivity.internetState(),
                )
            } else {
                CenterIndicator.NoNetwork
            }
        }

        return when (connectivity.transport) {
            SystemUiConnectivityStateSource.Transport.WIFI -> {
                if (wifiSegments == null || wifiSegments <= 0) {
                    null
                } else {
                    CenterIndicator.Wifi(
                        segments = wifiSegments,
                        internet = connectivity.internetState(),
                    )
                }
            }

            SystemUiConnectivityStateSource.Transport.CELLULAR -> {
                when {
                    mobileType != null ->
                        CenterIndicator.MobileType(
                            label = mobileType.label,
                            enhanced = mobileType.enhanced,
                            internet = connectivity.internetState(),
                        )
                    connectivity.validated -> null
                    else -> CenterIndicator.NoNetwork
                }
            }

            SystemUiConnectivityStateSource.Transport.NONE -> {
                when {
                    wifiSegments != null && wifiSegments > 0 ->
                        CenterIndicator.Wifi(
                            segments = wifiSegments,
                            internet = InternetState.NO_INTERNET,
                        )

                    connectivity.mobileDataEnabled == true &&
                        mobileSignal !is SignalStrength.Unknown &&
                        mobileType != null ->
                        CenterIndicator.MobileType(
                            label = mobileType.label,
                            enhanced = mobileType.enhanced,
                            internet = InternetState.NO_INTERNET,
                        )

                    else -> CenterIndicator.NoNetwork
                }
            }

            SystemUiConnectivityStateSource.Transport.OTHER -> {
                when {
                    wifiSegments != null && wifiSegments > 0 ->
                        CenterIndicator.Wifi(
                            segments = wifiSegments,
                            internet = connectivity.internetState(),
                        )

                    mobileType != null && connectivity.mobileDataEnabled != false ->
                        CenterIndicator.MobileType(
                            label = mobileType.label,
                            enhanced = mobileType.enhanced,
                            internet = connectivity.internetState(),
                        )

                    else -> CenterIndicator.NoNetwork
                }
            }
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

    data object NoNetwork : CenterIndicator
}
