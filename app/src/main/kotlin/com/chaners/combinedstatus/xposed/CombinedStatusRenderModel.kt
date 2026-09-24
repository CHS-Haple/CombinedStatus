package com.chaners.combinedstatus.xposed

internal data class CombinedStatusRenderModel(
    val batteryPercent: Int,
    val charging: Boolean,
    val centerIndicator: CenterIndicator,
    val mobileLevel: Int?,
    val effectiveDataSubscriptionId: Int,
) {
    companion object {
        fun from(
            snapshot: CombinedStatusStateStore.Snapshot,
            presentation: CombinedStatusPresentationStateStore.Snapshot,
            defaultDataSubscriptionId: Int,
        ): CombinedStatusRenderModel? {
            val battery = snapshot.battery ?: return null

            val preferredDataSubscriptionId =
                presentation.mobilePresentation
                    ?.effectiveDataSubscriptionId
                    ?.takeIf { subscriptionId -> subscriptionId >= 0 }
                    ?: defaultDataSubscriptionId

            val selectedMobile =
                snapshot.mobile[preferredDataSubscriptionId]
                    ?.takeIf { it.signal !is SignalStrength.Unknown }
                    ?.let { preferredDataSubscriptionId to it }
                    ?: snapshot.mobile.entries
                        .firstOrNull { it.value.signal !is SignalStrength.Unknown }
                        ?.let { it.key to it.value }

            val selectedSubscriptionId =
                selectedMobile?.first
                    ?: preferredDataSubscriptionId.takeIf { it >= 0 }
                    ?: snapshot.mobile.keys.firstOrNull()
                    ?: -1

            val mobileLevel =
                if (snapshot.airplaneMode == true) {
                    null
                } else {
                    when (val signal = selectedMobile?.second?.signal) {
                        null -> null
                        SignalStrength.Unknown -> null
                        SignalStrength.Unavailable -> null
                        is SignalStrength.Level -> signal.value.coerceIn(0, 4)
                    }
                }

            val centerIndicator =
                CombinedStatusConnectivityPolicy.resolve(
                    wifi = snapshot.wifi,
                    airplaneMode = snapshot.airplaneMode == true,
                    connectivity = presentation.connectivity,
                    mobileType = presentation.mobilePresentation?.networkType,
                    hotspot = snapshot.hotspot,
                ) ?: return null

            return CombinedStatusRenderModel(
                batteryPercent = battery.percent.coerceIn(0, 100),
                charging = battery.charging,
                centerIndicator = centerIndicator,
                mobileLevel = mobileLevel,
                effectiveDataSubscriptionId = selectedSubscriptionId,
            )
        }
    }
}
