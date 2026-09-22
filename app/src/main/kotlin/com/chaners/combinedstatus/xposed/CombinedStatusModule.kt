package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import android.util.Log
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.combinedstatus.settings.DIAGNOSTICS_REMOTE_PREFS_NAME
import com.chaners.combinedstatus.settings.DiagnosticsLevel
import com.chaners.combinedstatus.system.RuntimeDiagnosticsProtocol
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class CombinedStatusModule : XposedModule() {
    private var statusHostHookInstalled = false
    private var networkSourceHookCount = 0
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
        logDiagnostic(
            level = Log.INFO,
            event = "module.loaded",
            component = "module",
            state = "ready",
            "process" to param.processName,
            "build" to BuildConfig.BUILD_ID,
            "channel" to BuildConfig.BUILD_CHANNEL,
            "api" to apiVersion,
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != SYSTEM_UI_PACKAGE) {
            return
        }

        val compatibility = SystemUiCompatibilityProbe.inspect(param.classLoader)
        log(Log.INFO, TAG, compatibility.summary)
        val statusHostAvailable = compatibility.isAvailable("statusHost")
        logDiagnostic(
            level = if (statusHostAvailable) Log.INFO else Log.WARN,
            event = "compatibility.probe",
            component = "compatibility",
            state = if (statusHostAvailable) "ready" else "unavailable",
            "statusHost" to if (statusHostAvailable) "available" else "missing",
        )

        if (!statusHostAvailable) {
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
            logDiagnostic(
                level = Log.INFO,
                event = "hook.install",
                component = "statusHostHook",
                state = "ready",
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "hook.install",
                component = "statusHostHook",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
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
            logDiagnostic(
                level = Log.WARN,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "unavailable",
                "reason" to "status-host-hook-not-ready",
            )
            log(Log.WARN, TAG, "Hot reload declined reason=status-host-hook-not-ready")
            return false
        }

        val hookCount =
            1 +
                networkSourceHookCount +
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
        logDiagnostic(
            level = Log.INFO,
            event = "hotReload.prepare",
            component = "hotReload",
            state = "preparing",
            "hooks" to hookCount,
            "build" to BuildConfig.BUILD_ID,
        )
        log(
            Log.INFO,
            TAG,
            "Hot reload preparing build=" + BuildConfig.BUILD_ID + " hooks=" + hookCount,
        )
        teardownRuntimeResources("hotReload.prepare")
        unbindRuntimeDiagnostics()
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        val oldHandles = param.oldHookHandles
        val statusHostHandle = oldHandles.firstOrNull(StatusBarHostCapture::matches)

        if (statusHostHandle == null) {
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to "status-host-hook-missing",
                "restartScope" to true,
            )
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
            networkSourceHookCount = 0
            tintSourceInstalled = false
            islandMotionSourceInstalled = false
            SystemUiNetworkStateSource.resetRuntimeState()
            SystemUiTintStateSource.resetRuntimeState()
            SystemUiIslandMotionSource.resetRuntimeState()

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
                attachHostRuntime(
                    host = host,
                    source = "hotReload",
                )
            }

            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.complete",
                component = "hotReload",
                state = "ready",
                "build" to BuildConfig.BUILD_ID,
                "statusHostHook" to "replaced",
                "staleHooks" to removed,
            )
            log(
                Log.INFO,
                TAG,
                "Hot reload completed build=" + BuildConfig.BUILD_ID +
                    " statusHostHook=replaced staleHooks=" + removed,
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.complete",
                component = "hotReload",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "restartScope" to true,
            )
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
        }.onSuccess { result ->
            networkSourceHookCount = result.handles.size
            val fullyReady =
                result.wifiReady &&
                    result.mobileReady &&
                    networkSourceHookCount == SystemUiNetworkStateSource.HOOK_COUNT
            val state =
                when {
                    fullyReady -> "ready"
                    networkSourceHookCount > 0 -> "partial"
                    else -> "error"
                }
            logDiagnostic(
                level =
                    when (state) {
                        "ready" -> Log.INFO
                        "partial" -> Log.WARN
                        else -> Log.ERROR
                    },
                event = "source.install",
                component = "network",
                state = state,
                "hooks" to networkSourceHookCount,
                "expectedHooks" to SystemUiNetworkStateSource.HOOK_COUNT,
                "wifi" to if (result.wifiReady) "ready" else "error",
                "mobile" to if (result.mobileReady) "ready" else "error",
                "source" to source,
            )
            result.failures.forEach { failure ->
                logDiagnostic(
                    level = Log.ERROR,
                    event = "source.install.branch",
                    component = "network." + failure.component,
                    state = "error",
                    "stage" to failure.stage,
                    "errorType" to failure.errorType,
                    "reason" to failure.reason,
                    "source" to source,
                )
                log(
                    Log.ERROR,
                    TAG,
                    "Network branch installation failed component=" + failure.component +
                        " stage=" + failure.stage +
                        " errorType=" + failure.errorType +
                        " reason=" + failure.reason +
                        " source=" + source,
                )
            }
            log(
                if (fullyReady) Log.INFO else Log.WARN,
                TAG,
                "networkSource state=" + state +
                    " hooks=" + networkSourceHookCount +
                    "/" + SystemUiNetworkStateSource.HOOK_COUNT +
                    " wifi=" + result.wifiReady +
                    " mobile=" + result.mobileReady +
                    " source=" + source +
                    " rebindRequired=" + (source == "hotReload"),
            )
        }.onFailure { error ->
            networkSourceHookCount = 0
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "network",
                state = "error",
                "errorType" to error.javaClass.name,
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
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
            logDiagnostic(
                level = Log.WARN,
                event = "source.attach",
                component = "airplane",
                state = "unavailable",
                "source" to source,
                "reason" to "host-not-view",
            )
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
            logDiagnostic(
                level = if (attached) Log.INFO else Log.WARN,
                event = "source.attach",
                component = "airplane",
                state = if (attached) "ready" else "unavailable",
                "source" to source,
                "observer" to "settings-global-content-observer",
            )
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
            logDiagnostic(
                level = Log.ERROR,
                event = "source.attach",
                component = "airplane",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
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
            logDiagnostic(
                level = if (islandMotionSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "islandMotion",
                state = if (islandMotionSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SystemUiIslandMotionSource.HOOK_COUNT,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
            log(
                Log.INFO,
                TAG,
                "islandMotionSource hooks=ready count=" + handles.size +
                    " source=" + source +
                    " motion=ownerProbe nativeGeometryWrites=0",
            )
        }.onFailure { error ->
            islandMotionSourceInstalled = false
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "islandMotion",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
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
            logDiagnostic(
                level = if (tintSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "tint",
                state = if (tintSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SystemUiTintStateSource.HOOK_COUNT,
                "source" to source,
            )
            log(
                Log.INFO,
                TAG,
                "tintSource hooks=ready count=" + handles.size +
                    " source=" + source,
            )
        }.onFailure { error ->
            tintSourceInstalled = false
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "tint",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
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

    private fun teardownRuntimeResources(source: String) {
        CombinedStatusHomeRenderSession.detach()
        StatusBarStableSession.detach()
        SystemUiAirplaneStateSource.detach()
        SystemUiIslandMotionSource.resetRuntimeState()
        airplaneObserverAttached = false
        logDiagnostic(
            level = Log.INFO,
            event = "runtime.teardown",
            component = "runtimeSession",
            state = "ready",
            "source" to source,
            "rendererDetached" to true,
            "stableStatusDetached" to true,
            "airplaneObserverDetached" to true,
        )
    }

    private fun attachHostRuntime(
        host: Any,
        source: String,
    ) {
        attachAirplaneStateSource(host = host, source = source)

        when (
            val stableSession = StatusBarStableSession.attach(
                host = host,
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
            StatusBarStableSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "session.attach",
                    component = "stableStatus",
                    state = "ready",
                    "source" to source,
                )
            }

            is StatusBarStableSession.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "session.attach",
                    component = "stableStatus",
                    state = "unavailable",
                    "reason" to stableSession.reason,
                    "source" to source,
                )
            }
        }

        when (
            val renderSession = CombinedStatusHomeRenderSession.attach(
                host = host,
                onEvent = { event ->
                    if (detailedDiagnosticsEnabled) {
                        log(Log.INFO, TAG, event)
                    }
                },
            )
        ) {
            CombinedStatusHomeRenderSession.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "renderer.attach",
                    component = "renderer",
                    state = "ready",
                    "source" to source,
                )
            }

            is CombinedStatusHomeRenderSession.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "renderer.attach",
                    component = "renderer",
                    state = "unavailable",
                    "reason" to renderSession.reason,
                    "source" to source,
                )
            }
        }

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.attach",
            component = "runtimeSession",
            state = "ready",
            "source" to source,
        )
    }

    private fun onStatusHostCaptured(capture: SystemUiHostRegistry.Capture) {
        logDiagnostic(
            level = Log.INFO,
            event = "host.capture",
            component = "statusHost",
            state = "ready",
            "identity" to capture.identity,
            "replacement" to capture.replacement,
        )
        log(
            Log.INFO,
            TAG,
            "statusHost captured id=" + capture.identity +
                " replacement=" + capture.replacement,
        )
        attachHostRuntime(
            host = capture.host,
            source = if (capture.replacement) "hostReplacement" else "hostCapture",
        )

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
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "disabled",
                "channel" to BuildConfig.BUILD_CHANNEL,
            )
            return
        }

        runCatching {
            getRemotePreferences(DIAGNOSTICS_REMOTE_PREFS_NAME)
        }.onSuccess { preferences ->
            diagnosticsPreferences = preferences
            updateDetailedDiagnostics(preferences)
            preferences.registerOnSharedPreferenceChangeListener(diagnosticsPreferenceListener)
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            diagnosticsPreferences = null
            detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES
            logDiagnostic(
                level = Log.WARN,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
            log(Log.WARN, TAG, "Runtime diagnostics preference unavailable", error)
        }
    }

    private fun unbindRuntimeDiagnostics() {
        diagnosticsPreferences
            ?.unregisterOnSharedPreferenceChangeListener(diagnosticsPreferenceListener)
        diagnosticsPreferences = null
    }

    private fun updateDetailedDiagnostics(preferences: SharedPreferences) {
        val previous = detailedDiagnosticsEnabled
        detailedDiagnosticsEnabled =
            BuildConfig.DEVELOPMENT_PROBES ||
                preferences.getString(
                    DIAGNOSTICS_LEVEL_KEY,
                    DiagnosticsLevel.General.name,
                ) == DiagnosticsLevel.Detailed.name
        if (previous != detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.level",
                component = "diagnostics",
                state = "ready",
                "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
            )
        }
    }

    private fun logDiagnostic(
        level: Int,
        event: String,
        component: String,
        state: String,
        vararg fields: Pair<String, Any?>,
    ) {
        val values =
            buildMap {
                fields.forEach { (key, value) ->
                    if (value != null) {
                        put(key, value.toString())
                    }
                }
            }
        log(
            level,
            TAG,
            RuntimeDiagnosticsProtocol.format(
                event = event,
                component = component,
                state = state,
                fields = values,
            ),
        )
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
