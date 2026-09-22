package com.chaners.combinedstatus

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chaners.combinedstatus.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.combinedstatus.settings.DIAGNOSTICS_PREFS_NAME
import com.chaners.combinedstatus.settings.DIAGNOSTICS_REMOTE_PREFS_NAME
import com.chaners.combinedstatus.settings.DiagnosticsLevel
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class CombinedStatusApplication :
    Application(),
    XposedServiceHelper.OnServiceListener {

    private val diagnosticsPreferences: SharedPreferences by lazy {
        getSharedPreferences(DIAGNOSTICS_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var xposedService: XposedService? = null

    private val diagnosticsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                xposedService?.let(::syncDiagnosticsLevel)
            }
        }

    override fun onCreate() {
        super.onCreate()
        diagnosticsPreferences.registerOnSharedPreferenceChangeListener(diagnosticsListener)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        syncDiagnosticsLevel(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
        }
    }

    override fun onTerminate() {
        diagnosticsPreferences.unregisterOnSharedPreferenceChangeListener(diagnosticsListener)
        xposedService = null
        super.onTerminate()
    }

    private fun syncDiagnosticsLevel(service: XposedService) {
        val level =
            diagnosticsPreferences.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagnosticsLevel.General.name,
            ) ?: DiagnosticsLevel.General.name

        runCatching {
            val remote = service.getRemotePreferences(DIAGNOSTICS_REMOTE_PREFS_NAME)
            val editor = remote.edit() ?: error("remote preference editor unavailable")
            editor.putString(DIAGNOSTICS_LEVEL_KEY, level)
            check(editor.commit()) { "remote preference commit failed" }
        }.onFailure { throwable ->
            Log.w(
                TAG,
                "Unable to mirror diagnostics preference: " + throwable.message,
            )
        }
    }

    private companion object {
        const val TAG = "CombinedStatus[App]"
    }
}
