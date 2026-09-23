package com.chaners.combinedstatus.xposed

import android.content.Context

internal object SystemUiCoreRuntimeOwner {
    internal data class AttachResult(
        val airplaneReady: Boolean,
        val defaultDataSubscriptionReady: Boolean,
        val connectivityReady: Boolean,
    )

    @Synchronized
    fun attach(
        context: Context,
        onAirplaneMode: (Boolean) -> Unit,
        onDefaultDataSubscriptionChanged: (Int) -> Unit,
        onConnectivityState: (SystemUiConnectivityStateSource.State) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): AttachResult {
        val airplaneReady =
            SystemUiAirplaneStateSource.attach(
                context = context,
                onAirplaneMode = onAirplaneMode,
                onEvent = onEvent,
            )
        val defaultDataSubscriptionReady =
            SystemUiDefaultDataSubscriptionSource.attach(
                context = context,
                onChanged = onDefaultDataSubscriptionChanged,
                onEvent = onEvent,
            )
        val connectivityReady =
            SystemUiConnectivityStateSource.attach(
                context = context,
                onState = onConnectivityState,
                onEvent = onEvent,
            )
        return AttachResult(
            airplaneReady = airplaneReady,
            defaultDataSubscriptionReady = defaultDataSubscriptionReady,
            connectivityReady = connectivityReady,
        )
    }

    @Synchronized
    fun detach() {
        SystemUiConnectivityStateSource.detach()
        SystemUiDefaultDataSubscriptionSource.detach()
        SystemUiAirplaneStateSource.detach()
    }
}
