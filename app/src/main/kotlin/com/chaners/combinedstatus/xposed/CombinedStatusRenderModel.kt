package com.chaners.combinedstatus.xposed

internal data class CombinedStatusRenderModel(
    val batteryPercent: Int,
    val charging: Boolean,
    val batteryModeTint: Int? = null,
    val centerIndicator: CenterIndicator,
    val mobileLevel: Int?,
    val mobileUnavailableMark: Boolean = false,
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

            val airplaneMode = snapshot.airplaneMode == true
            val mobileRecoveryPending = snapshot.mobileRecoveryPending
            val selectedSignal = selectedMobile?.second?.signal

            val mobileLevel =
                if (airplaneMode || mobileRecoveryPending) {
                    null
                } else {
                    when (selectedSignal) {
                        null -> null
                        SignalStrength.Unknown -> null
                        SignalStrength.Unavailable -> null
                        is SignalStrength.Level -> selectedSignal.value.coerceIn(0, 4)
                    }
                }
            val noSimIcon =
                presentation.statusIcons.noSimIcon
                    ?.takeIf { presentation.statusIcons.noSimVisible }

            val centerIndicator =
                CombinedStatusConnectivityPolicy.resolve(
                    wifi = snapshot.wifi,
                    airplaneMode = airplaneMode,
                    connectivity = presentation.connectivity,
                    mobileType =
                        if (mobileRecoveryPending) {
                            null
                        } else {
                            presentation.mobilePresentation?.networkType
                        },
                    noSimIcon = noSimIcon,
                )
                    ?: if (mobileRecoveryPending) {
                        CenterIndicator.Empty
                    } else {
                        return null
                    }

            val mobileUnavailableMark =
                when {
                    airplaneMode -> true
                    mobileRecoveryPending -> false
                    noSimIcon != null -> true
                    selectedSignal is SignalStrength.Unavailable -> true
                    else -> false
                }

            return CombinedStatusRenderModel(
                batteryPercent = battery.percent.coerceIn(0, 100),
                charging = battery.charging,
                batteryModeTint = battery.resolvedModeTint,
                centerIndicator = centerIndicator,
                mobileLevel = mobileLevel,
                mobileUnavailableMark = mobileUnavailableMark,
                effectiveDataSubscriptionId = selectedSubscriptionId,
            )
        }
    }
}
