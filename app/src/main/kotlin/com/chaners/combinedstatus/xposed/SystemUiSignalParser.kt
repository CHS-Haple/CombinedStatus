package com.chaners.combinedstatus.xposed

internal sealed interface SignalStrength {
    data object Unknown : SignalStrength
    data object Unavailable : SignalStrength

    data class Level(
        val value: Int,
    ) : SignalStrength

    val logToken: String
        get() = when (this) {
            Unknown -> "unknown"
            Unavailable -> "unavailable"
            is Level -> "level=" + value
        }
}

internal object SystemUiSignalParser {
    private val mobileLevelPattern = Regex("^stat_sys_signal_([0-4])$")
    private val wifiLevelPattern = Regex("^stat_sys_wifi_signal_([0-3])$")

    fun mobile(resourceName: String?): SignalStrength {
        val entry = resourceEntry(resourceName) ?: return SignalStrength.Unknown
        if (entry == "stat_sys_signal_null") {
            return SignalStrength.Unavailable
        }

        val level = mobileLevelPattern.matchEntire(entry)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return SignalStrength.Unknown
        return SignalStrength.Level(level)
    }

    fun wifi(resourceName: String?): SignalStrength {
        val entry = resourceEntry(resourceName) ?: return SignalStrength.Unknown
        val level = wifiLevelPattern.matchEntire(entry)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return SignalStrength.Unknown
        return SignalStrength.Level(level)
    }

    private fun resourceEntry(resourceName: String?): String? {
        if (resourceName.isNullOrBlank()) {
            return null
        }
        return resourceName.substringAfterLast('/').takeIf { it.isNotBlank() }
    }
}
