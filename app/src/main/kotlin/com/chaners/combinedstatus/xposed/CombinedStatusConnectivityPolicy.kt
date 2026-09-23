package com.chaners.combinedstatus.xposed

internal object CombinedStatusConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        mobileSignal: SignalStrength?,
        airplaneMode: Boolean,
        connectivity: SystemUiConnectivityStateSource.State,
        connectivityFreshForWifi: Boolean,
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

        if (wifiSegments != null && wifiSegments > 0) {
            val internet =
                when {
                    !connectivity.known -> InternetState.UNKNOWN
                    !connectivityFreshForWifi -> InternetState.UNKNOWN
                    connectivity.transport == SystemUiConnectivityStateSource.Transport.WIFI ->
                        connectivity.internetState()
                    else -> InternetState.NO_INTERNET
                }
            return CenterIndicator.Wifi(
                segments = wifiSegments,
                internet = internet,
            )
        }

        if (airplaneMode) {
            return CenterIndicator.NoNetwork
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

        if (
            connectivity.mobileDataEnabled == false &&
            mobileSignal is SignalStrength.Level
        ) {
            return CenterIndicator.MobileDataOff
        }

        if (
            mobileType != null &&
            connectivity.mobileDataEnabled != false
        ) {
            val internet =
                when {
                    !connectivityFreshForWifi &&
                        connectivity.transport == SystemUiConnectivityStateSource.Transport.WIFI ->
                        InternetState.UNKNOWN

                    connectivity.transport == SystemUiConnectivityStateSource.Transport.CELLULAR ->
                        connectivity.internetState()

                    connectivity.transport == SystemUiConnectivityStateSource.Transport.NONE ->
                        InternetState.NO_INTERNET

                    connectivity.transport == SystemUiConnectivityStateSource.Transport.OTHER ->
                        connectivity.internetState()

                    else -> InternetState.UNKNOWN
                }

            return CenterIndicator.MobileType(
                label = mobileType.label,
                enhanced = mobileType.enhanced,
                internet = internet,
            )
        }

        return when {
            mobileSignal !is SignalStrength.Unknown &&
                connectivity.mobileDataEnabled == true ->
                CenterIndicator.NoNetwork

            connectivity.transport == SystemUiConnectivityStateSource.Transport.NONE ->
                CenterIndicator.NoNetwork

            else -> null
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

    data object MobileDataOff : CenterIndicator

    data object NoNetwork : CenterIndicator
}
