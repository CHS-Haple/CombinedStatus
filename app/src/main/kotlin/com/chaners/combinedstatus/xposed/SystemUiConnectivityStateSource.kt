package com.chaners.combinedstatus.xposed

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

internal object SystemUiConnectivityStateSource {
    private var manager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    @Synchronized
    fun attach(
        context: Context,
        onState: (State) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Boolean {
        detach()
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java)
                ?: return false
        val handler = Handler(Looper.getMainLooper())

        fun publish(source: String) {
            val network = connectivityManager.activeNetwork
            val capabilities =
                network?.let(connectivityManager::getNetworkCapabilities)
            val state =
                State(
                    known = true,
                    transport = transport(capabilities),
                    validated =
                        capabilities?.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        ) == true,
                    hasInternetCapability =
                        capabilities?.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET,
                        ) == true,
                    mobileDataEnabled = mobileDataEnabled(context),
                )
            onState(state)
            onEvent?.invoke(
                "connectivity source=" + source +
                    " transport=" + state.transport.name +
                    " validated=" + state.validated +
                    " internetCapability=" + state.hasInternetCapability +
                    " mobileDataEnabled=" + (state.mobileDataEnabled?.toString() ?: "unknown"),
            )
        }

        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    handler.post { publish("available") }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    handler.post { publish("capabilities") }
                }

                override fun onLost(network: Network) {
                    handler.post { publish("lost") }
                }
            }

        return runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback, handler)
            manager = connectivityManager
            callback = networkCallback
            publish("seed")
            true
        }.getOrElse {
            false
        }
    }

    @Synchronized
    fun detach() {
        val currentManager = manager
        val currentCallback = callback
        if (currentManager != null && currentCallback != null) {
            runCatching { currentManager.unregisterNetworkCallback(currentCallback) }
        }
        manager = null
        callback = null
    }

    private fun mobileDataEnabled(context: Context): Boolean? {
        val subscriptionId =
            runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
                .getOrDefault(-1)
        if (subscriptionId < 0) {
            return null
        }
        val telephony =
            context.getSystemService(TelephonyManager::class.java)
                ?: return null
        return runCatching {
            telephony
                .createForSubscriptionId(subscriptionId)
                .isDataEnabled
        }.getOrNull()
    }

    private fun transport(capabilities: NetworkCapabilities?): Transport =
        when {
            capabilities == null -> Transport.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            else -> Transport.OTHER
        }

    internal enum class Transport {
        WIFI,
        CELLULAR,
        OTHER,
        NONE,
    }

    internal data class State(
        val known: Boolean,
        val transport: Transport,
        val validated: Boolean,
        val hasInternetCapability: Boolean,
        val mobileDataEnabled: Boolean?,
    ) {
        companion object {
            val Unknown =
                State(
                    known = false,
                    transport = Transport.NONE,
                    validated = false,
                    hasInternetCapability = false,
                    mobileDataEnabled = null,
                )
        }
    }
}
