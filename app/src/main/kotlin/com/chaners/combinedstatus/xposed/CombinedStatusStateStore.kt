package com.chaners.combinedstatus.xposed

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
    fun updateMobile(update: MobileIconUpdate): Snapshot? {
        val previous = current.mobile[update.subscriptionId] ?: MobileState()
        val resourceId = update.resourceId?.takeIf { it != 0 }
        val next = when (update.kind) {
            MobileIconKind.SIGNAL -> previous.copy(signalResId = resourceId)
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

    internal data class Snapshot(
        val battery: BatteryState? = null,
        val wifi: WifiState = WifiState.Unknown,
        val mobile: Map<Int, MobileState> = emptyMap(),
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
                    is WifiState.Visible -> "visible:" + (state.iconResId ?: 0)
                }

                val mobileText = mobile.entries.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ";",
                ) { (subscriptionId, state) ->
                    subscriptionId.toString() +
                        ":signal=" + (state.signalResId ?: 0) +
                        ",volte=" + (state.volteResId ?: 0) +
                        ",vowifi=" + (state.vowifiResId ?: 0)
                }

                return "battery=$batteryText wifi=$wifiText mobile=$mobileText"
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
    )

    internal data class MobileState(
        val signalResId: Int? = null,
        val volteResId: Int? = null,
        val vowifiResId: Int? = null,
    )
}
