package com.chaners.combinedstatus.xposed

internal object CombinedStatusConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        airplaneMode: Boolean,
        connectivity: SystemUiConnectivityStateSource.State,
        mobileType: NativePresentationResolver.NetworkType?,
    ): CenterIndicator? {
        val wifiVisible =
            wifi as? CombinedStatusStateStore.WifiState.Visible
        val wifiSegments =
            when (val signal = wifiVisible?.signal) {
                null -> null
                SignalStrength.Unknown -> null
                SignalStrength.Unavailable -> 0
                is SignalStrength.Level -> wifiSegments(signal.value)
            }

        if (wifiVisible != null && wifiSegments != null && wifiSegments > 0) {
            return CenterIndicator.Wifi(
                segments = wifiSegments,
                internet =
                    when (wifiVisible.internetValidated) {
                        true -> InternetState.VALIDATED
                        false -> InternetState.NO_INTERNET
                        null -> InternetState.UNKNOWN
                    },
            )
        }

        if (!connectivity.known) {
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

            SystemUiConnectivityStateSource.Transport.WIFI ->
                null
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
