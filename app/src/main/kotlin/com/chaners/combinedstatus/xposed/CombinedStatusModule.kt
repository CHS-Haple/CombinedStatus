package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import android.util.Log
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.combinedstatus.settings.DIAGNOSTICS_REMOTE_PREFS_NAME
import com.chaners.combinedstatus.settings.DiagnosticsLevel
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class CombinedStatusModule : XposedModule() {
    private var statusHostHookInstalled = false
    private var networkSourceInstalled = false
    private var airplaneObserverAttached = false
    private var tintSourceInstalled = false
    private var islandMotionSourceInstalled = false
    private var diagnosticsPreferences: SharedPreferences? = null

    @Volatile
    private var detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES

    private val diagnosticsPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            if (key == DIAGNOSTICS_LEVEL_KEY) {
                updateDetailedDiagnostics(preferences)
            }
        }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        bindRuntimeDiagnostics()
        log(
            Log.INFO,
            TAG,
            "Module loaded in " + param.processName +
                " build=" + BuildConfig.BUILD_ID +
                " channel=" + BuildConfig.BUILD_CHANNEL +
                " diagnostics=" + if (detailedDiagnosticsEnabled) "detailed" else "general" +
                " with Xposed API " + apiVersion,
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != SYSTEM_UI_PACKAGE) {
            return
        }

        val compatibility = SystemUiCompatibilityProbe.inspect(param.classLoader)
        log(Log.INFO, TAG, compatibility.summary)

        if (!compatibility.isAvailable("statusHost")) {
            return
        }

        runCatching {
            StatusBarHostCapture.install(
                module = this,
                classLoader = param.classLoader,
                onCaptured = ::onStatusHostCaptured,
            )
        }.onSuccess {
            statusHostHookInstalled = true
        }.onFailure { error ->
            log(Log.ERROR, TAG, "Status host hook installation failed", error)
        }

        if (statusHostHookInstalled) {
            installNetworkStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installTintStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                installIslandMotionSource(
                    classLoader = param.classLoader,
                    source = "coldStart",
                )
            }
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        if (!statusHostHookInstalled) {
            log(Log.WARN, TAG, "Hot reload declined reason=status-host-hook-not-ready")
            return false
        }

        val hookCount =
            1 +
                if (networkSourceInstalled) {
                    SystemUiNetworkStateSource.HOOK_COUNT
                } else {
                    0
                } +
                if (tintSourceInstalled) {
                    SystemUiTintStateSource.HOOK_COUNT
                } else {
                    0
                } +
                if (islandMotionSourceInstalled) {
                    SystemUiIslandMotionSource.HOOK_COUNT
                } else {
                    0
                }
        log(
            Log.INFO,
            TAG,
            "Hot reload preparing build=" + BuildConfig.BUILD_ID + " hooks=" + hookCount,
        )
        unbindRuntimeDiagnostics()
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        val oldHandles = param.oldHookHandles
        val statusHostHandle = oldHandles.firstOrNull(StatusBarHostCapture::matches)

        if (statusHostHandle == null) {
            log(
                Log.ERROR,
                TAG,
                "Hot reload incomplete reason=status-host-hook-missing restartScope=true",
            )
            return
        }

        runCatching {
            StatusBarHostCapture.replace(
                handle = statusHostHandle,
                onCaptured = ::onStatusHostCaptured,
            )

            var removed = 0
            oldHandles.forEach { handle ->
                if (handle !== statusHostHandle) {
                    handle.unhook()
                    removed += 1
                }
            }

            statusHostHookInstalled = true
            networkSourceInstalled = false
            tintSourceInstalled = false
            islandMotionSourceInstalled = false

            val classLoader = statusHostHandle.executable.declaringClass.classLoader
                ?: error("SystemUI class loader unavailable after hot reload")
            installNetworkStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installTintStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                installIslandMotionSource(
                    classLoader = classLoader,
                    source = "hotReload",
                )
            }
            SystemUiHostRegistry.currentStatusHost()?.let { host ->
                attachAirplaneStateSource(
                    host = host,
                    source = "hotReload",
                )
            }

            log(
                Log.INFO,
                TAG,
                "Hot reload completed build=" + BuildConfig.BUILD_ID +
                    " statusHostHook=replaced staleHooks=" + removed,
            )
        }.onFailure { error ->
            log(Log.ERROR, TAG, "Hot reload failed restartScope=true", error)
        }
    }

    private fun installNetworkStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiNetworkStateSource.install(
                module = this,
                classLoader = classLoader,
                onWifiState = { state ->
                    CombinedStatusStateStore.updateWifi(state)?.let(::onCombinedStateChanged)
                },
                onMobileIcon = { update ->
                    CombinedStatusStateStore.updateMobile(update)?.let(::onCombinedStateChanged)
                },
                onAirplaneMode = { enabled ->
                    CombinedStatusStateStore.updateAirplaneMode(enabled)
                        ?.let(::onCombinedStateChanged)
                },
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onNetworkPipelineEvent else null,
            )
        }.onSuccess { handles ->
            networkSourceInstalled = handles.size == SystemUiNetworkStateSource.HOOK_COUNT
            log(
                Log.INFO,
                TAG,
                "networkSource hooks=ready count=" + handles.size +
                    " source=" + source +
                    " rebindRequired=" + (source == "hotReload"),
            )
        }.onFailure { error ->
            networkSourceInstalled = false
            log(Log.ERROR, TAG, "Network state source installation failed", error)
        }
    }

    private fun attachAirplaneStateSource(
        host: Any,
        source: String,
    ) {
        val view = host as? android.view.View
        if (view == null) {
            airplaneObserverAttached = false
            log(
                Log.WARN,
                TAG,
                "airplaneSource observer=unavailable source=" + source +
                    " reason=host-not-view",
            )
            return
        }

        runCatching {
            SystemUiAirplaneStateSource.attach(
                context = view.context,
                onAirplaneMode = { enabled ->
                    CombinedStatusStateStore.updateAirplaneMode(enabled)
                        ?.let(::onCombinedStateChanged)
                },
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onNetworkPipelineEvent else null,
            )
        }.onSuccess { attached ->
            airplaneObserverAttached = attached
            log(
                Log.INFO,
                TAG,
                "airplaneSource observer=" +
                    if (attached) "ready" else "unavailable" +
                    " source=" + source +
                    " event=settings-global-content-observer",
            )
        }.onFailure { error ->
            airplaneObserverAttached = false
            log(Log.ERROR, TAG, "Airplane state observer failed", error)
        }
    }

    private fun installIslandMotionSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiIslandMotionSource.install(
                module = this,
                classLoader = classLoader,
                onEvent = ::onIslandMotionEvent,
            )
        }.onSuccess { handles ->
            islandMotionSourceInstalled =
                handles.size == SystemUiIslandMotionSource.HOOK_COUNT
            log(
                Log.INFO,
                TAG,
                "islandMotionSource hooks=ready count=" + handles.size +
                    " source=" + source +
                    " motion=ownerProbe nativeGeometryWrites=0",
            )
        }.onFailure { error ->
            islandMotionSourceInstalled = false
            log(Log.ERROR, TAG, "Island motion source installation failed", error)
        }
    }

    private fun onIslandMotionEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun installTintStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiTintStateSource.install(
                module = this,
                classLoader = classLoader,
                onTintState = CombinedStatusHomeRenderSession::onTintUpdate,
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onTintSourceEvent else null,
            )
        }.onSuccess { handles ->
            tintSourceInstalled = handles.size == SystemUiTintStateSource.HOOK_COUNT
            log(
                Log.INFO,
                TAG,
                "tintSource hooks=ready count=" + handles.size +
                    " source=" + source,
            )
        }.onFailure { error ->
            tintSourceInstalled = false
            log(Log.ERROR, TAG, "Tint state source installation failed", error)
        }
    }

    private fun onTintSourceEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onNetworkPipelineEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
        }
    }

    private fun onCombinedStateChanged(
        snapshot: CombinedStatusStateStore.Snapshot,
    ) {
        CombinedStatusHomeRenderSession.onState(snapshot)
    }

    private fun onStatusHostCaptured(capture: SystemUiHostRegistry.Capture) {
        log(
            Log.INFO,
            TAG,
            "statusHost captured id=" + capture.identity +
                " replacement=" + capture.replacement,
        )
        attachAirplaneStateSource(
            host = capture.host,
            source = if (capture.replacement) "hostReplacement" else "hostCapture",
        )

        when (
            val stableSession = StatusBarStableSession.attach(
                host = capture.host,
                onBatteryState = { state ->
                    CombinedStatusStateStore.updateBattery(state)?.let(::onCombinedStateChanged)
                },
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        ) {
            StatusBarStableSession.AttachResult.Ready -> Unit

            is StatusBarStableSession.AttachResult.Failure -> {
                log(
                    Log.WARN,
                    TAG,
                    "stableStatus unavailable reason=" + stableSession.reason,
                )
            }
        }

        when (
            val renderSession = CombinedStatusHomeRenderSession.attach(
                host = capture.host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        ) {
            CombinedStatusHomeRenderSession.AttachResult.Ready -> Unit

            is CombinedStatusHomeRenderSession.AttachResult.Failure -> {
                log(
                    Log.WARN,
                    TAG,
                    "homeRender unavailable reason=" + renderSession.reason,
                )
            }
        }

        if (BuildConfig.DEVELOPMENT_PROBES) {
            SystemUiNativeStatusInventory.schedule(capture.host) { snapshot ->
                log(Log.INFO, TAG, snapshot.summary)
                log(Log.INFO, TAG, snapshot.hostLine)
                snapshot.entries.forEach { entry ->
                    log(Log.INFO, TAG, entry.logLine)
                }
                snapshot.statusIconSubtree?.let { subtree ->
                    log(Log.INFO, TAG, subtree.summary)
                    subtree.entries.forEach { entry ->
                        log(Log.INFO, TAG, entry.logLine)
                    }
                }
            }
        }
    }

    private fun bindRuntimeDiagnostics() {
        if (!BuildConfig.RUNTIME_DIAGNOSTICS) {
            detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES
            return
        }

        runCatching {
            getRemotePreferences(DIAGNOSTICS_REMOTE_PREFS_NAME)
        }.onSuccess { preferences ->
            diagnosticsPreferences = preferences
            updateDetailedDiagnostics(preferences)
            preferences.registerOnSharedPreferenceChangeListener(diagnosticsPreferenceListener)
        }.onFailure { error ->
            diagnosticsPreferences = null
            detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES
            log(Log.WARN, TAG, "Runtime diagnostics preference unavailable", error)
        }
    }

    private fun unbindRuntimeDiagnostics() {
        diagnosticsPreferences
            ?.unregisterOnSharedPreferenceChangeListener(diagnosticsPreferenceListener)
        diagnosticsPreferences = null
    }

    private fun updateDetailedDiagnostics(preferences: SharedPreferences) {
        detailedDiagnosticsEnabled =
            BuildConfig.DEVELOPMENT_PROBES ||
                preferences.getString(
                    DIAGNOSTICS_LEVEL_KEY,
                    DiagnosticsLevel.General.name,
                ) == DiagnosticsLevel.Detailed.name
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
