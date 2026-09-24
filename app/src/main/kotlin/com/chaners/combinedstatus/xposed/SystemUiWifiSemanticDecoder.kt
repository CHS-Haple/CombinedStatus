package com.chaners.combinedstatus.xposed

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class SystemUiWifiSemanticDecoder private constructor(
    private val visibleResMethod: Method,
    private val fullIcons: IntArray,
    private val noInternetIcons: IntArray,
    private val noNetworkIcon: Int?,
    val contractMode: String,
) {
    fun decode(
        value: Any,
        resourceName: (Int) -> String?,
    ): Decoded? {
        if (value.javaClass.name != WIFI_ICON_VISIBLE_CLASS_NAME) {
            return null
        }

        val resId =
            runCatching {
                (visibleResMethod.invoke(value) as Number).toInt()
            }.getOrNull()
                ?.takeIf { it != 0 }
                ?: return null

        return decodeResource(
            resId = resId,
            fullIcons = fullIcons,
            noInternetIcons = noInternetIcons,
            noNetworkIcon = noNetworkIcon,
        ) ?: Decoded(
            resId = resId,
            signal = SystemUiSignalParser.wifi(resourceName(resId)),
            internet = InternetState.UNKNOWN,
            source = "resource-name-fallback",
        )
    }

    internal data class Decoded(
        val resId: Int,
        val signal: SignalStrength,
        val internet: InternetState,
        val source: String,
    )

    companion object {
        const val WIFI_ICON_VISIBLE_CLASS_NAME =
            "com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon\$Visible"
        private const val WIFI_ICONS_CLASS_NAME =
            "com.android.systemui.statusbar.connectivity.WifiIcons"

        fun resolve(classLoader: ClassLoader): SystemUiWifiSemanticDecoder {
            val visibleClass =
                Class.forName(
                    WIFI_ICON_VISIBLE_CLASS_NAME,
                    false,
                    classLoader,
                )
            val resMethod =
                visibleClass.declaredMethods
                    .firstOrNull { method ->
                        method.name == "getRes" &&
                            method.parameterCount == 0 &&
                            method.returnType == Integer.TYPE
                    }
                    ?.apply { isAccessible = true }
                    ?: error("wifi-visible-res-accessor-missing")

            val wifiIconsClass =
                Class.forName(
                    WIFI_ICONS_CLASS_NAME,
                    false,
                    classLoader,
                )
            val directFull = staticIntArray(wifiIconsClass, "WIFI_FULL_ICONS")
            val directNoInternet =
                staticIntArray(wifiIconsClass, "WIFI_NO_INTERNET_ICONS")
            val matrix =
                staticIntMatrix(wifiIconsClass, "WIFI_SIGNAL_STRENGTH")

            val fullIcons: IntArray
            val noInternetIcons: IntArray
            val mode: String
            if (
                directFull != null &&
                directNoInternet != null &&
                directFull.isNotEmpty() &&
                directFull.size == directNoInternet.size
            ) {
                fullIcons = directFull
                noInternetIcons = directNoInternet
                mode = "direct-arrays"
            } else if (
                matrix != null &&
                matrix.size >= 2 &&
                matrix[0].isNotEmpty() &&
                matrix[0].size == matrix[1].size
            ) {
                noInternetIcons = matrix[0]
                fullIcons = matrix[1]
                mode = "signal-strength-matrix"
            } else {
                error("wifi-icon-semantic-arrays-missing")
            }

            return SystemUiWifiSemanticDecoder(
                visibleResMethod = resMethod,
                fullIcons = fullIcons.copyOf(),
                noInternetIcons = noInternetIcons.copyOf(),
                noNetworkIcon = staticInt(wifiIconsClass, "WIFI_NO_NETWORK"),
                contractMode = mode,
            )
        }

        internal fun decodeResource(
            resId: Int,
            fullIcons: IntArray,
            noInternetIcons: IntArray,
            noNetworkIcon: Int?,
        ): Decoded? {
            val fullLevel = fullIcons.indexOf(resId)
            if (fullLevel >= 0) {
                return Decoded(
                    resId = resId,
                    signal = SignalStrength.Level(fullLevel),
                    internet = InternetState.VALIDATED,
                    source = "system-full",
                )
            }

            val noInternetLevel = noInternetIcons.indexOf(resId)
            if (noInternetLevel >= 0) {
                return Decoded(
                    resId = resId,
                    signal = SignalStrength.Level(noInternetLevel),
                    internet = InternetState.NO_INTERNET,
                    source = "system-no-internet",
                )
            }

            if (noNetworkIcon != null && resId == noNetworkIcon) {
                return Decoded(
                    resId = resId,
                    signal = SignalStrength.Unavailable,
                    internet = InternetState.NO_INTERNET,
                    source = "system-no-network",
                )
            }
            return null
        }

        private fun staticIntArray(
            clazz: Class<*>,
            name: String,
        ): IntArray? =
            staticField(clazz, name)
                ?.let { field ->
                    runCatching { field.get(null) as? IntArray }.getOrNull()
                }

        private fun staticIntMatrix(
            clazz: Class<*>,
            name: String,
        ): Array<IntArray>? =
            staticField(clazz, name)
                ?.let { field ->
                    @Suppress("UNCHECKED_CAST")
                    runCatching { field.get(null) as? Array<IntArray> }.getOrNull()
                }

        private fun staticInt(
            clazz: Class<*>,
            name: String,
        ): Int? =
            staticField(clazz, name)
                ?.let { field ->
                    runCatching { field.getInt(null) }.getOrNull()
                }

        private fun staticField(
            clazz: Class<*>,
            name: String,
        ): Field? =
            runCatching {
                clazz
                    .getDeclaredField(name)
                    .apply {
                        check(Modifier.isStatic(modifiers))
                        isAccessible = true
                    }
            }.getOrNull()
    }
}
