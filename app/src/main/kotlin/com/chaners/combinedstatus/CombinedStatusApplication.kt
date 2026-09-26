package com.chaners.combinedstatus

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chaners.combinedstatus.settings.CENTER_FOLLOWS_BATTERY_COLOR_KEY
import com.chaners.combinedstatus.settings.COMBINED_STATUS_ENABLED_KEY
import com.chaners.combinedstatus.settings.COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY
import com.chaners.combinedstatus.settings.COMBINED_STATUS_FEATURE_PREFS_NAME
import com.chaners.combinedstatus.settings.COMBINED_STATUS_VISUAL_PREFS_NAME
import com.chaners.combinedstatus.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.combinedstatus.settings.DIAGNOSTICS_PREFS_NAME
import com.chaners.combinedstatus.settings.DiagnosticsLevel
import com.chaners.combinedstatus.settings.MOBILE_FOLLOWS_BATTERY_COLOR_KEY
import com.chaners.combinedstatus.settings.RUNTIME_REMOTE_PREFS_NAME
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

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

    private val diagnosticsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val featureListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == COMBINED_STATUS_ENABLED_KEY) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    private val visualListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == MOBILE_FOLLOWS_BATTERY_COLOR_KEY ||
                key == CENTER_FOLLOWS_BATTERY_COLOR_KEY
            ) {
                xposedService?.let(::syncRuntimeConfig)
            }
        }

    override fun onCreate() {
        super.onCreate()
        diagnosticsPreferences.registerOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.registerOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.registerOnSharedPreferenceChangeListener(visualListener)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        syncRuntimeConfig(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
        }
    }

    override fun onTerminate() {
        diagnosticsPreferences.unregisterOnSharedPreferenceChangeListener(diagnosticsListener)
        featurePreferences.unregisterOnSharedPreferenceChangeListener(featureListener)
        visualPreferences.unregisterOnSharedPreferenceChangeListener(visualListener)
        xposedService = null
        super.onTerminate()
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
                mainExecutor.execute(onComplete)
            }
            true
        }.getOrElse { throwable ->
            Log.w(TAG, "Unable to request hot reload: " + throwable.message)
            false
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
        val centerFollowsBattery =
            visualPreferences.getBoolean(
                CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                false,
            )

        runCatching {
            val remote = service.getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor
                .putString(DIAGNOSTICS_LEVEL_KEY, level)
                .putBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    combinedStatusEnabled,
                )
                .putLong(
                    COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                    featureChangeElapsedRealtimeNanos,
                )
                .putBoolean(
                    MOBILE_FOLLOWS_BATTERY_COLOR_KEY,
                    mobileFollowsBattery,
                )
                .putBoolean(
                    CENTER_FOLLOWS_BATTERY_COLOR_KEY,
                    centerFollowsBattery,
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
    }
}
