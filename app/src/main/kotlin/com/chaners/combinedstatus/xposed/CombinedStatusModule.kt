package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import android.os.Process
import android.os.SystemClock
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
import java.util.concurrent.atomic.AtomicLong

class CombinedStatusModule : XposedModule() {
    private var statusHostHookInstalled = false
    private var networkSourceHookCount = 0
    private var airplaneObserverAttached = false
    private var tintSourceInstalled = false
    private var sceneSourceInstalled = false
    private var mobileTypeSourceInstalled = false
    private var islandMotionSourceInstalled = false
    private var diagnosticsPreferences: SharedPreferences? = null
    private var runtimeSessionId = newRuntimeSessionId()
    private val diagnosticSequence = AtomicLong(0L)

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
        logCurrentDiagnosticsHealth()
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
            installSceneStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
            installMobileTypeStateSource(
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

        val host = SystemUiHostRegistry.currentStatusHost()
        val snapshot = CombinedStatusStateStore.snapshot()
        val bindingCounts = SystemUiNetworkStateSource.hotReloadBindingCounts()
        val bindingStateReady =
            (snapshot.wifi is CombinedStatusStateStore.WifiState.Unknown || bindingCounts.first > 0) &&
                (snapshot.mobile.isEmpty() || bindingCounts.second > 0)
        if (host == null || !bindingStateReady) {
            logDiagnostic(
                level = Log.WARN,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "unavailable",
                "reason" to if (host == null) "status-host-not-captured" else "network-bindings-not-ready",
                "wifiRoots" to bindingCounts.first,
                "mobileRoots" to bindingCounts.second,
                "restartScope" to true,
            )
            log(
                Log.WARN,
                TAG,
                "Hot reload declined reason=" +
                    if (host == null) "status-host-not-captured" else "network-bindings-not-ready",
            )
            return false
        }

        val transfer =
            CombinedStatusHotReloadTransfer.capture(
                host = host,
                state = CombinedStatusStateStore.exportHotReloadState(),
                bindings = SystemUiNetworkStateSource.exportHotReloadBindings(),
            )
        if (transfer == null) {
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "error",
                "reason" to "state-transfer-capture-failed",
                "restartScope" to true,
            )
            return false
        }

        val saved = runCatching {
            param.setSavedInstanceState(transfer)
        }
        if (saved.isFailure) {
            val error = saved.exceptionOrNull()
            logDiagnostic(
                level = Log.ERROR,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "error",
                "reason" to (error?.message ?: error?.javaClass?.simpleName ?: "saved-state-rejected"),
                "restartScope" to true,
            )
            log(Log.ERROR, TAG, "Hot reload saved-state transfer rejected", error)
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
                if (sceneSourceInstalled) {
                    SystemUiSceneStateSource.HOOK_COUNT
                } else {
                    0
                } +
                if (mobileTypeSourceInstalled) {
                    SystemUiMobileTypeStateSource.HOOK_COUNT
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
            "transfer" to "saved-instance-state",
            "hostIdentity" to System.identityHashCode(host),
            "wifiRoots" to bindingCounts.first,
            "mobileRoots" to bindingCounts.second,
        )
        log(
            Log.INFO,
            TAG,
            "Hot reload preparing build=" + BuildConfig.BUILD_ID +
                " hooks=" + hookCount +
                " transfer=saved-instance-state",
        )
        teardownRuntimeResources("hotReload.prepare")
        unbindRuntimeDiagnostics()
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        rotateDiagnosticSession()
        val oldHandles = param.oldHookHandles
        val statusHostHandle = oldHandles.firstOrNull(StatusBarHostCapture::matches)

        if (statusHostHandle == null) {
            oldHandles.forEach { handle -> runCatching { handle.unhook() } }
            bindRuntimeDiagnostics()
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
            sceneSourceInstalled = false
            mobileTypeSourceInstalled = false
            islandMotionSourceInstalled = false
            SystemUiNetworkStateSource.resetEventState()
            SystemUiTintStateSource.resetRuntimeState()
            SystemUiSceneStateSource.resetRuntimeState()
            SystemUiIslandMotionSource.resetRuntimeState()
            bindRuntimeDiagnostics()
            logDiagnostic(
                level = Log.INFO,
                event = "module.reloaded",
                component = "module",
                state = "ready",
                "build" to BuildConfig.BUILD_ID,
                "channel" to BuildConfig.BUILD_CHANNEL,
            )
            logCurrentDiagnosticsHealth()

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
            installSceneStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installMobileTypeStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                installIslandMotionSource(
                    classLoader = classLoader,
                    source = "hotReload",
                )
            }

            val restored = CombinedStatusHotReloadTransfer.restore(param.savedInstanceState)
            val restoreReady =
                if (restored != null) {
                    val capture = SystemUiHostRegistry.restoreStatusHost(restored.host)
                    val restoredSnapshot =
                        CombinedStatusStateStore.restoreHotReloadState(restored.state)
                    val bindings =
                        SystemUiNetworkStateSource.restoreHotReloadBindings(restored.bindings)
                    attachHostRuntime(
                        host = capture.host,
                        source = "hotReloadRestore",
                    )
                    logDiagnostic(
                        level = Log.INFO,
                        event = "hotReload.restore",
                        component = "hotReload",
                        state = "ready",
                        "hostIdentity" to capture.identity,
                        "wifiRoots" to bindings.wifiRoots,
                        "mobileRoots" to bindings.mobileRoots,
                        "state" to restoredSnapshot.logLine,
                    )
                    true
                } else {
                    CombinedStatusStateStore.restoreHotReloadState(null)
                    logDiagnostic(
                        level = Log.WARN,
                        event = "hotReload.restore",
                        component = "hotReload",
                        state = "unavailable",
                        "reason" to "saved-state-missing-or-legacy-generation",
                        "restartScope" to true,
                    )
                    false
                }

            logDiagnostic(
                level = if (restoreReady) Log.INFO else Log.WARN,
                event = "hotReload.complete",
                component = "hotReload",
                state = if (restoreReady) "ready" else "partial",
                "build" to BuildConfig.BUILD_ID,
                "statusHostHook" to "replaced",
                "staleHooks" to removed,
                "restartScope" to !restoreReady,
            )
            log(
                if (restoreReady) Log.INFO else Log.WARN,
                TAG,
                "Hot reload completed build=" + BuildConfig.BUILD_ID +
                    " statusHostHook=replaced staleHooks=" + removed +
                    " restored=" + restoreReady,
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
                    val changed = CombinedStatusStateStore.updateMobile(update)
                    refreshMobilePresentation()
                    changed?.let(::onCombinedStateChanged)
                },
                onAirplaneMode = { enabled ->
                    CombinedStatusStateStore.updateAirplaneMode(enabled)
                        ?.let(::onCombinedStateChanged)
                },
                onPresentationChanged = ::refreshMobilePresentation,
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

    private fun installSceneStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiSceneStateSource.install(
                module = this,
                classLoader = classLoader,
                onSceneState = CombinedStatusHomeRenderSession::onSceneUpdate,
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onSceneSourceEvent else null,
            )
        }.onSuccess { handles ->
            sceneSourceInstalled = handles.size == SystemUiSceneStateSource.HOOK_COUNT
            logDiagnostic(
                level = if (sceneSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "scene",
                state = if (sceneSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SystemUiSceneStateSource.HOOK_COUNT,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
        }.onFailure { error ->
            sceneSourceInstalled = false
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "scene",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Scene state source installation failed", error)
        }
    }

    private fun installMobileTypeStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiMobileTypeStateSource.install(
                module = this,
                classLoader = classLoader,
                onChanged = ::refreshMobilePresentation,
            )
        }.onSuccess { handles ->
            mobileTypeSourceInstalled =
                handles.size == SystemUiMobileTypeStateSource.HOOK_COUNT
            logDiagnostic(
                level = if (mobileTypeSourceInstalled) Log.INFO else Log.WARN,
                event = "source.install",
                component = "mobileType",
                state = if (mobileTypeSourceInstalled) "ready" else "partial",
                "hooks" to handles.size,
                "expectedHooks" to SystemUiMobileTypeStateSource.HOOK_COUNT,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
        }.onFailure { error ->
            mobileTypeSourceInstalled = false
            logDiagnostic(
                level = Log.WARN,
                event = "source.install",
                component = "mobileType",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
        }
    }

    private fun refreshMobilePresentation() {
        val presentation =
            NativePresentationResolver.resolve(
                state = CombinedStatusStateStore.snapshot(),
            )
        CombinedStatusPresentationStateStore
            .updateMobilePresentation(presentation)
            ?.let {
                if (detailedDiagnosticsEnabled) {
                    log(Log.INFO, TAG, presentation.logLine)
                    logDiagnostic(
                        level = Log.INFO,
                        event = "presentation.resolve",
                        component = "mobilePresentation",
                        state = "ready",
                        "mode" to presentation.mode.name,
                        "boundRoots" to presentation.boundRoots,
                        "visibleRoots" to presentation.visibleRoots,
                        "activeSubIds" to presentation.activeSubscriptionIds.joinToString(","),
                        "presentationRootSubId" to presentation.presentationRootSubscriptionId,
                        "effectiveDataSubId" to presentation.effectiveDataSubscriptionId,
                        "networkType" to presentation.networkType?.label,
                        "enhanced" to presentation.networkType?.enhanced,
                        "geometryWrites" to 0,
                    )
                }
                CombinedStatusHomeRenderSession.onPresentationStateChanged()
            }
    }

    private fun onSceneSourceEvent(event: String) {
        if (detailedDiagnosticsEnabled) {
            log(Log.INFO, TAG, event)
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
        SystemUiConnectivityStateSource.detach()
        CombinedStatusPresentationStateStore.reset()
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

        val hostContext = (host as? android.view.View)?.context
        val connectivityReady =
            hostContext?.let { context ->
                SystemUiConnectivityStateSource.attach(
                    context = context,
                    onState = { state ->
                        val changed =
                            CombinedStatusPresentationStateStore.updateConnectivity(state)
                        refreshMobilePresentation()
                        changed?.let {
                            CombinedStatusHomeRenderSession.onPresentationStateChanged()
                        }
                    },
                    onEvent =
                        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                            { event ->
                                if (detailedDiagnosticsEnabled) {
                                    log(Log.INFO, TAG, event)
                                }
                            }
                        } else {
                            null
                        },
                )
            } == true
        logDiagnostic(
            level = if (connectivityReady) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "connectivity",
            state = if (connectivityReady) "ready" else "unavailable",
            "source" to source,
        )
        refreshMobilePresentation()

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

        scheduleNativeSlotProbe(host = host, source = source)

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.attach",
            component = "runtimeSession",
            state = "ready",
            "source" to source,
        )
    }

    private fun scheduleNativeSlotProbe(
        host: Any,
        source: String,
    ) {
        if (
            !BuildConfig.DEVELOPMENT_PROBES &&
            !(BuildConfig.RUNTIME_DIAGNOSTICS && detailedDiagnosticsEnabled)
        ) {
            return
        }

        SystemUiNetworkStateSource.bindingTopologyLines().forEach { line ->
            log(Log.INFO, TAG, line)
        }

        val nativeParticipant = NativeParticipantContractProbe.inspect(host)
        log(Log.INFO, TAG, nativeParticipant.logLine)
        logDiagnostic(
            level =
                if (nativeParticipant.registrationContractReady) {
                    Log.INFO
                } else {
                    Log.WARN
                },
            event = "contract.probe",
            component = "nativeParticipant",
            state =
                if (nativeParticipant.registrationContractReady) {
                    "ready"
                } else {
                    "observed"
                },
            "available" to nativeParticipant.available,
            "reason" to nativeParticipant.reason,
            "manager" to nativeParticipant.managerClass,
            "group" to nativeParticipant.groupClass,
            "groupRes" to nativeParticipant.groupResource,
            "controller" to nativeParticipant.controllerClass,
            "controllerMatches" to nativeParticipant.controllerMatches,
            "managerMatches" to nativeParticipant.managerMatches,
            "groupMatches" to nativeParticipant.groupMatches,
            "setIcon" to nativeParticipant.setIcon,
            "setIconVisibility" to nativeParticipant.setIconVisibility,
            "addIconGroup" to nativeParticipant.addIconGroup,
            "removeIconGroup" to nativeParticipant.removeIconGroup,
            "addHolder" to nativeParticipant.addHolder,
            "createLayoutParams" to nativeParticipant.createLayoutParams,
            "holderCtor" to nativeParticipant.holderConstructor,
            "iconViewCtor" to nativeParticipant.iconViewConstructor,
            "statusIconDisplayable" to nativeParticipant.iconViewDisplayable,
            "registrationReady" to nativeParticipant.registrationContractReady,
            "nativeGeometryWrites" to 0,
        )

        SystemUiNativeStatusInventory.schedule(host) { snapshot ->
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
                logDiagnostic(
                    level = Log.INFO,
                    event = "slot.probe",
                    component = "nativeSlot",
                    state = "observed",
                    "source" to source,
                    "root" to subtree.rootClassName,
                    "children" to subtree.rootChildCount,
                    "nodes" to subtree.entries.size,
                    "truncated" to subtree.truncated,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
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

    private fun logCurrentDiagnosticsHealth() {
        val state =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS -> "disabled"
                diagnosticsPreferences != null -> "ready"
                else -> "unavailable"
            }
        logDiagnostic(
            level = if (state == "unavailable") Log.WARN else Log.INFO,
            event = "diagnostics.snapshot",
            component = "diagnostics",
            state = state,
            "level" to if (detailedDiagnosticsEnabled) "detailed" else "general",
            "channel" to BuildConfig.BUILD_CHANNEL,
        )
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

    private fun rotateDiagnosticSession() {
        runtimeSessionId = newRuntimeSessionId()
        diagnosticSequence.set(0L)
    }

    private fun newRuntimeSessionId(): String =
        BuildConfig.BUILD_ID + "-" +
            Process.myPid() + "-" +
            SystemClock.elapsedRealtime().toString(36)

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
                put("sessionId", runtimeSessionId)
                put("uptimeMs", SystemClock.elapsedRealtime().toString())
                put("sequence", diagnosticSequence.incrementAndGet().toString())
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
