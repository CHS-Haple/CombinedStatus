package com.chaners.guiyuan

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT
import com.chaners.guiyuan.settings.BATTERY_TOP_CHARGING_ICON_SCALE_KEY
import com.chaners.guiyuan.settings.BATTERY_TOP_READOUT_ENABLED_KEY
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_SCALE_DEFAULT
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_SCALE_KEY
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_KEY
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_KEY
import com.chaners.guiyuan.settings.CENTER_FOLLOWS_BATTERY_COLOR_KEY
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import com.chaners.guiyuan.settings.COMBINED_STATUS_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_FEATURE_PREFS_NAME
import com.chaners.guiyuan.settings.COMBINED_STATUS_KEYGUARD_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_VISUAL_PREFS_NAME
import com.chaners.guiyuan.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.guiyuan.settings.DIAGNOSTICS_PREFS_NAME
import com.chaners.guiyuan.settings.DiagnosticsLevel
import com.chaners.guiyuan.settings.MOBILE_FOLLOWS_BATTERY_COLOR_KEY
import com.chaners.guiyuan.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.guiyuan.settings.isCombinedStatusVisualPreferenceKey
import com.chaners.guiyuan.settings.normalized
import com.chaners.guiyuan.system.XposedRuntimeStatus
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CombinedStatusApplication :
    Application(),
    XposedServiceHelper.OnServiceListener {

    private val diagnosticsPreferences: SharedPreferences by lazy {
        getSharedPreferences(DIAGNOSTICS_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val featurePreferences: SharedPreferences by lazy {
        getSharedPreferences(COMBINED_STATUS_FEATURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val visualPreferences: SharedPreferences by lazy {
        getSharedPreferences(COMBINED_STATUS_VISUAL_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _xposedRuntimeStatus =
        MutableStateFlow<XposedRuntimeStatus>(XposedRuntimeStatus.Checking)
    internal val xposedRuntimeStatus: StateFlow<XposedRuntimeStatus> =
        _xposedRuntimeStatus.asStateFlow()

    private val xposedServiceBindTimeout =
        Runnable {
            if (
                xposedService == null &&
                _xposedRuntimeStatus.value == XposedRuntimeStatus.Checking
            ) {
                _xposedRuntimeStatus.value = XposedRuntimeStatus.FrameworkUnavailable
            }
        }

    private val diagnosticsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val featureListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == COMBINED_STATUS_ENABLED_KEY ||
                key == COMBINED_STATUS_KEYGUARD_ENABLED_KEY
            ) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val visualListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isCombinedStatusVisualPreferenceKey(key)) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    override fun onCreate() {
        super.onCreate()
        diagnosticsPreferences.registerOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.registerOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.registerOnSharedPreferenceChangeListener(visualListener)
        XposedServiceHelper.registerListener(this)
        mainHandler.postDelayed(
            xposedServiceBindTimeout,
            XPOSED_SERVICE_BIND_TIMEOUT_MS,
        )
    }

    override fun onServiceBind(service: XposedService) {
        mainHandler.removeCallbacks(xposedServiceBindTimeout)
        xposedService = service
        syncRuntimeConfig(service)
        refreshXposedRuntimeStatus(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
            _xposedRuntimeStatus.value = XposedRuntimeStatus.FrameworkUnavailable
        }
    }

    override fun onTerminate() {
        diagnosticsPreferences.unregisterOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.unregisterOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.unregisterOnSharedPreferenceChangeListener(visualListener)
        mainHandler.removeCallbacks(xposedServiceBindTimeout)
        xposedService = null
        super.onTerminate()
    }

    internal fun refreshXposedRuntimeStatus() {
        xposedService?.let(::refreshXposedRuntimeStatus)
    }

    fun hotReloadSystemUi(onComplete: () -> Unit = {}): Boolean {
        val service = xposedService ?: return false
        if (service.apiVersion < 102) return false

        val target =
            runCatching {
                service.runningTargets.firstOrNull { it.processName == SYSTEM_UI_PROCESS }
            }.getOrElse { throwable ->
                Log.w(TAG, "Unable to query running targets: " + throwable.message)
                return false
            } ?: return false

        return runCatching {
            service.hotReloadModule(target, null) { process, result ->
                Log.i(
                    TAG,
                    "Hot reload completed process=" + process.processName + " result=" + result,
                )
                mainExecutor.execute {
                    refreshXposedRuntimeStatus()
                    onComplete()
                }
            }
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Unable to request hot reload: " + throwable.message)
            false
        }
    }

    private fun refreshXposedRuntimeStatus(service: XposedService) {
        _xposedRuntimeStatus.value =
            runCatching {
                val running =
                    service.runningTargets.any { target ->
                        target.processName == SYSTEM_UI_PROCESS
                    }
                val inScope =
                    service.scope.any { packageName ->
                        packageName == SYSTEM_UI_PROCESS
                    }

                XposedRuntimeStatus.Connected(
                    systemUiInScope = inScope,
                    systemUiRunning = running,
                )
            }.getOrElse { throwable ->
                Log.w(
                    TAG,
                    "Unable to query Xposed runtime status: " + throwable.message,
                )
                XposedRuntimeStatus.QueryUnavailable
            }
    }

    private fun syncRuntimeConfig(service: XposedService) {
        val level =
            diagnosticsPreferences.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagnosticsLevel.General.name,
            ) ?: DiagnosticsLevel.General.name
        val combinedStatusEnabled =
            featurePreferences.getBoolean(
                COMBINED_STATUS_ENABLED_KEY,
                true,
            )
        val keyguardEnabled =
            featurePreferences.getBoolean(
                COMBINED_STATUS_KEYGUARD_ENABLED_KEY,
                false,
            )
        val featureChangeElapsedRealtimeNanos =
            featurePreferences.getLong(
                COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                0L,
            )
        val mobileFollowsBattery =
            visualPreferences.getBoolean(
                MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                false,
            )
        val visualSettings =
            CombinedStatusVisualSettings(
                mobileFollowsBatteryColor = mobileFollowsBattery,
                centerFollowsBatteryColor =
                    visualPreferences.getBoolean(
                        CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                        false,
                    ),
                batteryTopReadoutEnabled =
                    visualPreferences.getBoolean(
                        BATTERY_TOP_READOUT_ENABLED_KEY,
                        false,
                    ),
                batteryTopTextScale =
                    visualPreferences.getFloat(
                        BATTERY_TOP_TEXT_SCALE_KEY,
                        BATTERY_TOP_TEXT_SCALE_DEFAULT,
                    ),
                batteryTopTextWeight =
                    visualPreferences.getInt(
                        BATTERY_TOP_TEXT_WEIGHT_KEY,
                        BATTERY_TOP_TEXT_WEIGHT_DEFAULT,
                    ),
                batteryTopVerticalOffset =
                    visualPreferences.getFloat(
                        BATTERY_TOP_VERTICAL_OFFSET_KEY,
                        BATTERY_TOP_VERTICAL_OFFSET_DEFAULT,
                    ),
                batteryTopChargingIconScale =
                    visualPreferences.getFloat(
                        BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                        BATTERY_TOP_CHARGING_ICON_SCALE_DEFAULT,
                    ),
            ).normalized()

        runCatching {
            val remote = service.getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor
                .putString(DIAGNOSTICS_LEVEL_KEY, level)
                .putBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    combinedStatusEnabled,
                )
                .putBoolean(
                    COMBINED_STATUS_KEYGUARD_ENABLED_KEY,
                    keyguardEnabled,
                )
                .putLong(
                    COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                    featureChangeElapsedRealtimeNanos,
                )
                .putBoolean(
                    MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                    visualSettings.mobileFollowsBatteryColor,
                )
                .putBoolean(
                    CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                    visualSettings.centerFollowsBatteryColor,
                )
                .putBoolean(
                    BATTERY_TOP_READOUT_ENABLED_KEY,
                    visualSettings.batteryTopReadoutEnabled,
                )
                .putFloat(
                    BATTERY_TOP_TEXT_SCALE_KEY,
                    visualSettings.batteryTopTextScale,
                )
                .putInt(
                    BATTERY_TOP_TEXT_WEIGHT_KEY,
                    visualSettings.batteryTopTextWeight,
                )
                .putFloat(
                    BATTERY_TOP_VERTICAL_OFFSET_KEY,
                    visualSettings.batteryTopVerticalOffset,
                )
                .putFloat(
                    BATTERY_TOP_CHARGING_ICON_SCALE_KEY,
                    visualSettings.batteryTopChargingIconScale,
                )
            check(editor.commit()) { "remote preference commit failed" }
        }.onFailure { throwable ->
            Log.w(
                TAG,
                "Unable to mirror runtime preferences: " + throwable.message,
            )
        }
    }

    private companion object {
        const val TAG = "CombinedStatus[App]"
        const val SYSTEM_UI_PROCESS = "com.android.systemui"
        const val XPOSED_SERVICE_BIND_TIMEOUT_MS = 1_000L
    }
}
