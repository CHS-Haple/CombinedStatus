package com.chaners.combinedstatus.xposed

import android.os.Bundle

internal object CombinedStatusStateStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun updateBattery(state: BatteryState): Snapshot? {
        if (current.battery == state) {
            return null
        }

        current = current.copy(battery = state)
        return current
    }

    @Synchronized
    fun updateWifi(state: WifiState): Snapshot? {
        if (current.wifi == state) {
            return null
        }

        current = current.copy(wifi = state)
        return current
    }

    @Synchronized
    fun updateAirplaneMode(enabled: Boolean): Snapshot? {
        if (current.airplaneMode == enabled) {
            return null
        }

        current = current.copy(airplaneMode = enabled)
        return current
    }

    @Synchronized
    fun updateMobile(update: MobileIconUpdate): Snapshot? {
        val previous = current.mobile[update.subscriptionId] ?: MobileState()
        val resourceId = update.resourceId?.takeIf { it != 0 }
        val next = when (update.kind) {
            MobileIconKind.SIGNAL -> previous.copy(
                signalResId = resourceId,
                signal = update.signal ?: SignalStrength.Unknown,
            )
            MobileIconKind.VOLTE -> previous.copy(volteResId = resourceId)
            MobileIconKind.VOWIFI -> previous.copy(vowifiResId = resourceId)
        }

        if (previous == next) {
            return null
        }

        val mobile = current.mobile.toMutableMap()
        mobile[update.subscriptionId] = next
        current = current.copy(mobile = mobile.toSortedMap())
        return current
    }

    @Synchronized
    fun exportHotReloadState(): Bundle =
        Bundle().apply {
            current.battery?.let { battery ->
                putBoolean(KEY_BATTERY_PRESENT, true)
                putInt(KEY_BATTERY_PERCENT, battery.percent)
                putBoolean(KEY_BATTERY_CHARGING, battery.charging)
                putInt(KEY_BATTERY_PLUGGED, battery.plugged)
            }
            when (val wifi = current.wifi) {
                WifiState.Unknown -> putInt(KEY_WIFI_KIND, WIFI_KIND_UNKNOWN)
                WifiState.Hidden -> putInt(KEY_WIFI_KIND, WIFI_KIND_HIDDEN)
                is WifiState.Visible -> {
                    putInt(KEY_WIFI_KIND, WIFI_KIND_VISIBLE)
                    putInt(KEY_WIFI_RES_ID, wifi.iconResId ?: 0)
                    putInt(KEY_WIFI_SIGNAL, encodeSignal(wifi.signal))
                    putInt(KEY_WIFI_INTERNET, encodeInternet(wifi.internet))
                }
            }
            putInt(
                KEY_AIRPLANE,
                when (current.airplaneMode) {
                    null -> AIRPLANE_UNKNOWN
                    false -> AIRPLANE_OFF
                    true -> AIRPLANE_ON
                },
            )

            val flattened = IntArray(current.mobile.size * MOBILE_STRIDE)
            current.mobile.entries.forEachIndexed { index, (subscriptionId, state) ->
                val base = index * MOBILE_STRIDE
                flattened[base] = subscriptionId
                flattened[base + 1] = state.signalResId ?: 0
                flattened[base + 2] = encodeSignal(state.signal)
                flattened[base + 3] = state.volteResId ?: 0
                flattened[base + 4] = state.vowifiResId ?: 0
            }
            putIntArray(KEY_MOBILE, flattened)
        }

    @Synchronized
    fun restoreHotReloadState(bundle: Bundle?): Snapshot {
        if (bundle == null) {
            current = Snapshot()
            return current
        }

        val battery =
            if (bundle.getBoolean(KEY_BATTERY_PRESENT, false)) {
                BatteryState(
                    percent = bundle.getInt(KEY_BATTERY_PERCENT),
                    charging = bundle.getBoolean(KEY_BATTERY_CHARGING),
                    plugged = bundle.getInt(KEY_BATTERY_PLUGGED),
                )
            } else {
                null
            }

        val wifi =
            when (bundle.getInt(KEY_WIFI_KIND, WIFI_KIND_UNKNOWN)) {
                WIFI_KIND_HIDDEN -> WifiState.Hidden
                WIFI_KIND_VISIBLE ->
                    WifiState.Visible(
                        iconResId = bundle.getInt(KEY_WIFI_RES_ID).takeIf { it != 0 },
                        signal = decodeSignal(bundle.getInt(KEY_WIFI_SIGNAL, SIGNAL_UNKNOWN)),
                        internet =
                            decodeInternet(
                                bundle.getInt(KEY_WIFI_INTERNET, INTERNET_UNKNOWN),
                            ),
                    )
                else -> WifiState.Unknown
            }

        val airplane =
            when (bundle.getInt(KEY_AIRPLANE, AIRPLANE_UNKNOWN)) {
                AIRPLANE_OFF -> false
                AIRPLANE_ON -> true
                else -> null
            }

        val mobile = sortedMapOf<Int, MobileState>()
        val flattened = bundle.getIntArray(KEY_MOBILE) ?: IntArray(0)
        var offset = 0
        while (offset + MOBILE_STRIDE <= flattened.size) {
            val subscriptionId = flattened[offset]
            mobile[subscriptionId] =
                MobileState(
                    signalResId = flattened[offset + 1].takeIf { it != 0 },
                    signal = decodeSignal(flattened[offset + 2]),
                    volteResId = flattened[offset + 3].takeIf { it != 0 },
                    vowifiResId = flattened[offset + 4].takeIf { it != 0 },
                )
            offset += MOBILE_STRIDE
        }

        current =
            Snapshot(
                battery = battery,
                wifi = wifi,
                mobile = mobile,
                airplaneMode = airplane,
            )
        return current
    }

    private fun encodeSignal(signal: SignalStrength): Int =
        when (signal) {
            SignalStrength.Unknown -> SIGNAL_UNKNOWN
            SignalStrength.Unavailable -> SIGNAL_UNAVAILABLE
            is SignalStrength.Level -> signal.value.coerceIn(0, 4)
        }

    private fun decodeSignal(value: Int): SignalStrength =
        when (value) {
            SIGNAL_UNKNOWN -> SignalStrength.Unknown
            SIGNAL_UNAVAILABLE -> SignalStrength.Unavailable
            else -> SignalStrength.Level(value.coerceIn(0, 4))
        }

    private fun encodeInternet(internet: InternetState): Int =
        when (internet) {
            InternetState.UNKNOWN -> INTERNET_UNKNOWN
            InternetState.VALIDATED -> INTERNET_VALIDATED
            InternetState.NO_INTERNET -> INTERNET_NO_INTERNET
        }

    private fun decodeInternet(value: Int): InternetState =
        when (value) {
            INTERNET_VALIDATED -> InternetState.VALIDATED
            INTERNET_NO_INTERNET -> InternetState.NO_INTERNET
            else -> InternetState.UNKNOWN
        }

    internal data class Snapshot(
        val battery: BatteryState? = null,
        val wifi: WifiState = WifiState.Unknown,
        val mobile: Map<Int, MobileState> = emptyMap(),
        val airplaneMode: Boolean? = null,
    ) {
        val logLine: String
            get() {
                val batteryText = battery?.let { state ->
                    state.percent.toString() + ":" +
                        (if (state.charging) "charging" else "discharging") +
                        ":plugged=" + state.plugged
                } ?: "unknown"

                val wifiText = when (val state = wifi) {
                    WifiState.Unknown -> "unknown"
                    WifiState.Hidden -> "hidden"
                    is WifiState.Visible ->
                        "visible:" + state.signal.logToken +
                            ":internet=" + state.internet.name +
                            ":res=" + (state.iconResId ?: 0)
                }

                val mobileText = mobile.entries.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ";",
                ) { (subscriptionId, state) ->
                    subscriptionId.toString() +
                        ":signal=" + state.signal.logToken + ":res=" + (state.signalResId ?: 0) +
                        ",volte=" + (state.volteResId ?: 0) +
                        ",vowifi=" + (state.vowifiResId ?: 0)
                }

                return "battery=$batteryText wifi=$wifiText mobile=$mobileText " +
                    "airplane=" + (airplaneMode?.toString() ?: "unknown")
            }
    }

    internal data class BatteryState(
        val percent: Int,
        val charging: Boolean,
        val plugged: Int,
    )

    internal sealed interface WifiState {
        data object Unknown : WifiState
        data object Hidden : WifiState

        data class Visible(
            val iconResId: Int?,
            val signal: SignalStrength,
            val internet: InternetState = InternetState.UNKNOWN,
        ) : WifiState
    }

    internal enum class MobileIconKind {
        SIGNAL,
        VOLTE,
        VOWIFI,
    }

    internal data class MobileIconUpdate(
        val subscriptionId: Int,
        val kind: MobileIconKind,
        val resourceId: Int?,
        val signal: SignalStrength? = null,
    )

    internal data class MobileState(
        val signalResId: Int? = null,
        val signal: SignalStrength = SignalStrength.Unknown,
        val volteResId: Int? = null,
        val vowifiResId: Int? = null,
    )

    private const val KEY_BATTERY_PRESENT = "batteryPresent"
    private const val KEY_BATTERY_PERCENT = "batteryPercent"
    private const val KEY_BATTERY_CHARGING = "batteryCharging"
    private const val KEY_BATTERY_PLUGGED = "batteryPlugged"
    private const val KEY_WIFI_KIND = "wifiKind"
    private const val KEY_WIFI_RES_ID = "wifiResId"
    private const val KEY_WIFI_SIGNAL = "wifiSignal"
    private const val KEY_WIFI_INTERNET = "wifiInternet"
    private const val KEY_AIRPLANE = "airplane"
    private const val KEY_MOBILE = "mobile"

    private const val WIFI_KIND_UNKNOWN = 0
    private const val WIFI_KIND_HIDDEN = 1
    private const val WIFI_KIND_VISIBLE = 2

    private const val AIRPLANE_UNKNOWN = -1
    private const val AIRPLANE_OFF = 0
    private const val AIRPLANE_ON = 1

    private const val SIGNAL_UNKNOWN = -2
    private const val SIGNAL_UNAVAILABLE = -1
    private const val INTERNET_UNKNOWN = 0
    private const val INTERNET_VALIDATED = 1
    private const val INTERNET_NO_INTERNET = 2
    private const val MOBILE_STRIDE = 5
}
