package com.chaners.combinedstatus.xposed

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
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
    private var islandMotionSourceInstalled = false
    private var diagnosticsPreferences: SharedPreferences? = null
    private var runtimeSessionId = newRuntimeSessionId()
    private val diagnosticSequence = AtomicLong(0L)
    private val renderTraceSequence = AtomicLong(0L)

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
            installPresentationRuntimeSources(
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
                visual = CombinedStatusHomeRenderSession.visualHandoffView(),
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
        teardownRuntimeResources("hotReload.prepare", preserveRendererVisual = true)
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
            islandMotionSourceInstalled = false
            SystemUiNetworkStateSource.resetEventState()
            SystemUiPresentationRuntimeOwner.resetRuntimeState()
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
            logDiagnostic(
                level = Log.INFO,
                event = "compatibility.revalidated",
                component = "compatibility",
                state = "ready",
                "statusHost" to "available",
                "source" to "hotReloadHook",
            )
            logDiagnostic(
                level = Log.INFO,
                event = "hook.replace",
                component = "statusHostHook",
                state = "ready",
                "source" to "hotReload",
            )
            logCurrentDiagnosticsHealth()

            val classLoader = statusHostHandle.executable.declaringClass.classLoader
                ?: error("SystemUI class loader unavailable after hot reload")
            installNetworkStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installPresentationRuntimeSources(
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
                    logDiagnostic(
                        level = Log.INFO,
                        event = "host.restore",
                        component = "statusHost",
                        state = "ready",
                        "identity" to capture.identity,
                        "replacement" to capture.replacement,
                        "source" to "hotReloadTransfer",
                    )
                    val restoredSnapshot =
                        CombinedStatusStateStore.restoreHotReloadState(restored.state)
                    val bindings =
                        SystemUiNetworkStateSource.restoreHotReloadBindings(restored.bindings)
                    attachHostRuntime(
                        host = capture.host,
                        source = "hotReloadRestore",
                        previousVisual = restored.visual,
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
                    val trace = beginRenderTrace("wifi")
                    val previous = CombinedStatusStateStore.snapshot().wifi
                    val changed = CombinedStatusStateStore.updateWifi(state)
                    if (changed != null) {
                        var stateTrace = markStateCommitted(trace)
                        val wasVisible =
                            previous is CombinedStatusStateStore.WifiState.Visible
                        val isVisible =
                            state is CombinedStatusStateStore.WifiState.Visible
                        if (wasVisible != isVisible) {
                            CombinedStatusPresentationStateStore.markWifiSemanticChanged()
                            stateTrace = markPresentationCommitted(stateTrace)
                        }
                        onCombinedStateChanged(
                            snapshot = changed,
                            trace = stateTrace,
                        )
                    }
                },
                onMobileIcon = { update ->
                    val trace = beginRenderTrace("mobile")
                    val changed = CombinedStatusStateStore.updateMobile(update)
                    val stateTrace =
                        if (changed != null) {
                            markStateCommitted(trace)
                        } else {
                            trace
                        }
                    refreshMobilePresentation(stateTrace)
                    changed?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = stateTrace,
                        )
                    }
                },
                onAirplaneMode = { enabled ->
                    val trace = beginRenderTrace("airplaneSignal")
                    CombinedStatusStateStore.updateAirplaneMode(enabled)?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = markStateCommitted(trace),
                        )
                    }
                },
                onPresentationChanged = {
                    refreshMobilePresentation(beginRenderTrace("networkPresentation"))
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

    private fun installPresentationRuntimeSources(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiPresentationRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onTintState = CombinedStatusHomeRenderSession::onTintUpdate,
                onSceneState = CombinedStatusHomeRenderSession::onSceneUpdate,
                onMobileTypeChanged = { drawable ->
                    refreshMobilePresentation(
                        trace = beginRenderTrace("mobileType"),
                        pendingMobileTypeDrawable = drawable,
                    )
                },
                onTintEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onTintSourceEvent else null,
                onSceneEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onSceneSourceEvent else null,
            )
        }.onSuccess { result ->
            logDiagnostic(
                level =
                    if (result.tintReady && result.sceneReady && result.mobileTypeReady) {
                        Log.INFO
                    } else {
                        Log.WARN
                    },
                event = "source.install",
                component = "presentationRuntime",
                state =
                    if (result.tintReady && result.sceneReady && result.mobileTypeReady) {
                        "ready"
                    } else {
                        "partial"
                    },
                "tintHooks" to result.tintHooks,
                "sceneHooks" to result.sceneHooks,
                "mobileTypeHooks" to result.mobileTypeHooks,
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
        }.onFailure { error ->
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "presentationRuntime",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
                "nativeGeometryWrites" to 0,
            )
            log(Log.ERROR, TAG, "Presentation runtime source installation failed", error)
        }
    }

    private fun refreshMobilePresentation(
        trace: RuntimeRenderTrace? = null,
        pendingMobileTypeDrawable: Drawable? = null,
    ) {
        val presentation =
            NativePresentationResolver.resolve(
                state = CombinedStatusStateStore.snapshot(),
                pendingMobileTypeDrawable = pendingMobileTypeDrawable,
            )
        val changed =
            CombinedStatusPresentationStateStore.updateMobilePresentation(presentation)
        if (changed != null) {
            val presentationTrace = markPresentationCommitted(trace)
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
                    "networkTypeSubId" to presentation.networkTypeSubscriptionId,
                    "networkType" to presentation.networkType?.label,
                    "enhanced" to presentation.networkType?.enhanced,
                    "geometryWrites" to 0,
                )
            }
            CombinedStatusHomeRenderSession.onPresentationStateChanged(
                presentationTrace,
            )
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
        trace: RuntimeRenderTrace? = null,
    ) {
        CombinedStatusHomeRenderSession.onState(snapshot, trace)
    }

    private fun teardownRuntimeResources(
        source: String,
        preserveRendererVisual: Boolean = false,
    ) {
        val nativeShadowDetach =
            if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                NativeParticipantShadowSession.detach()
            } else {
                NativeParticipantShadowSession.DetachResult.AlreadyDetached
            }
        CombinedStatusHomeRenderSession.detach(preserveVisual = preserveRendererVisual)
        StatusBarStableSession.detach()
        SystemUiCoreRuntimeOwner.detach()
        SystemUiPresentationRuntimeOwner.resetRuntimeState()
        CombinedStatusPresentationStateStore.reset()
        SystemUiIslandMotionSource.resetRuntimeState()

        val nativeShadowDetached =
            nativeShadowDetach !is NativeParticipantShadowSession.DetachResult.Failure
        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
            logDiagnostic(
                level = if (nativeShadowDetached) Log.INFO else Log.WARN,
                event = "participant.detach",
                component = "nativeParticipantShadow",
                state = if (nativeShadowDetached) "ready" else "error",
                "source" to source,
                "cleanup" to
                    when (nativeShadowDetach) {
                        NativeParticipantShadowSession.DetachResult.Removed ->
                            "removed"
                        NativeParticipantShadowSession.DetachResult.AlreadyDetached ->
                            "already-detached"
                        is NativeParticipantShadowSession.DetachResult.Failure ->
                            "failed"
                    },
                "reason" to
                    (nativeShadowDetach as? NativeParticipantShadowSession.DetachResult.Failure)
                        ?.reason,
                "nativeGeometryWrites" to 0,
            )
        }
        logDiagnostic(
            level = if (nativeShadowDetached) Log.INFO else Log.WARN,
            event = "runtime.teardown",
            component = "runtimeSession",
            state = if (nativeShadowDetached) "ready" else "partial",
            "source" to source,
            "rendererDetached" to !preserveRendererVisual,
            "rendererVisualPreserved" to preserveRendererVisual,
            "stableStatusDetached" to true,
            "airplaneObserverDetached" to true,
            "defaultDataSubscriptionObserverDetached" to true,
            "nativeShadowDetached" to nativeShadowDetached,
            "nativeShadowReason" to
                (nativeShadowDetach as? NativeParticipantShadowSession.DetachResult.Failure)
                    ?.reason,
        )
    }

    private fun attachHostRuntime(
        host: Any,
        source: String,
        previousVisual: android.view.View? = null,
    ) {
        val hostContext = (host as? android.view.View)?.context
        val coreRuntime =
            hostContext?.let { context ->
                SystemUiCoreRuntimeOwner.attach(
                    context = context,
                    onAirplaneMode = { enabled ->
                        val trace = beginRenderTrace("airplaneObserver")
                        CombinedStatusStateStore.updateAirplaneMode(enabled)?.let { snapshot ->
                            onCombinedStateChanged(
                                snapshot = snapshot,
                                trace = markStateCommitted(trace),
                            )
                        }
                    },
                    onDefaultDataSubscriptionChanged = {
                        refreshMobilePresentation(
                            beginRenderTrace("defaultDataSubscription"),
                        )
                    },
                    onConnectivityState = { state ->
                        val trace = beginRenderTrace("connectivity")
                        val changed =
                            CombinedStatusPresentationStateStore.updateConnectivity(state)
                        val presentationTrace =
                            if (changed != null) {
                                markPresentationCommitted(trace)
                            } else {
                                trace
                            }
                        refreshMobilePresentation(presentationTrace)
                        changed?.let {
                            CombinedStatusHomeRenderSession.onPresentationStateChanged(
                                presentationTrace,
                            )
                        }
                    },
                    onEvent =
                        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                            ::onNetworkPipelineEvent
                        } else {
                            null
                        },
                )
            }

        logDiagnostic(
            level = if (coreRuntime?.airplaneReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "airplane",
            state = if (coreRuntime?.airplaneReady == true) "ready" else "unavailable",
            "source" to source,
            "observer" to "settings-global-content-observer",
        )
        logDiagnostic(
            level =
                if (coreRuntime?.defaultDataSubscriptionReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "defaultDataSubscription",
            state =
                if (coreRuntime?.defaultDataSubscriptionReady == true) "ready" else "unavailable",
            "source" to source,
            "observer" to "default-data-subscription-broadcast",
            "subscriptionId" to SystemUiDefaultDataSubscriptionSource.currentSubscriptionId(),
            "eventDriven" to true,
        )
        logDiagnostic(
            level = if (coreRuntime?.connectivityReady == true) Log.INFO else Log.WARN,
            event = "source.attach",
            component = "connectivity",
            state = if (coreRuntime?.connectivityReady == true) "ready" else "unavailable",
            "source" to source,
        )
        refreshMobilePresentation()

        when (
            val stableSession = StatusBarStableSession.attach(
                host = host,
                onBatteryState = { state ->
                    val trace = beginRenderTrace("battery")
                    CombinedStatusStateStore.updateBattery(state)?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = markStateCommitted(trace),
                        )
                    }
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

        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
            when (
                val nativeShadow =
                    NativeParticipantShadowSession.attach(
                        host = host,
                        onEvent = { event ->
                            if (detailedDiagnosticsEnabled) {
                                log(Log.INFO, TAG, event)
                            }
                        },
                    )
            ) {
                is NativeParticipantShadowSession.AttachResult.Ready -> {
                    val shadow = nativeShadow.snapshot
                    logDiagnostic(
                        level = Log.INFO,
                        event = "participant.attach",
                        component = "nativeParticipantShadow",
                        state = "ready",
                        "source" to source,
                        "slot" to shadow.slot,
                        "root" to shadow.rootClass,
                        "rootIndex" to shadow.rootIndex,
                        "visibility" to shadow.rootVisibility,
                        "iconVisible" to shadow.iconVisible,
                        "measured" to
                            shadow.measuredWidth.toString() +
                                "x" +
                                shadow.measuredHeight,
                        "layoutHidden" to shadow.layoutHidden,
                        "childrenBefore" to shadow.childrenBefore,
                        "childrenAfter" to shadow.childrenAfter,
                        "bootstrapRes" to
                            "0x" +
                                shadow.bootstrapResourceId
                                    .toUInt()
                                    .toString(16),
                        "bootstrapSlot" to shadow.bootstrapSourceSlot,
                        "bootstrapIndex" to shadow.bootstrapSourceIndex,
                        "creationMode" to shadow.creationMode,
                        "removalMode" to shadow.removalMode,
                        "visible" to false,
                        "nativeGeometryWrites" to 0,
                    )
                }

                is NativeParticipantShadowSession.AttachResult.Failure -> {
                    logDiagnostic(
                        level = Log.WARN,
                        event = "participant.attach",
                        component = "nativeParticipantShadow",
                        state = "unavailable",
                        "source" to source,
                        "reason" to nativeShadow.reason,
                        "visible" to false,
                        "nativeGeometryWrites" to 0,
                    )
                }
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
                onLatencySample = ::onRenderLatencySample,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                previousVisual = previousVisual,
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
            "setIconHolder" to nativeParticipant.setIconHolder,
            "resourceSetIconMode" to nativeParticipant.resourceSetIconMode,
            "setIconVisibility" to nativeParticipant.setIconVisibility,
            "removalReady" to nativeParticipant.removalReady,
            "removalMode" to nativeParticipant.removalMode,
            "addIconGroup" to nativeParticipant.addIconGroup,
            "removeIconGroup" to nativeParticipant.removeIconGroup,
            "addHolder" to nativeParticipant.addHolder,
            "holderFactoryReady" to nativeParticipant.holderFactoryReady,
            "statusIconDisplayable" to nativeParticipant.iconViewDisplayable,
            "slotAccessor" to nativeParticipant.iconViewSlotAccessor,
            "systemManagedCreationReady" to nativeParticipant.systemManagedCreationReady,
            "registrationReady" to nativeParticipant.registrationContractReady,
            "setIconSignatures" to
                nativeParticipant.setIconSignatures.joinToString("|"),
            "removeSignatures" to
                nativeParticipant.removeSignatures.joinToString("|"),
            "holderFactories" to
                nativeParticipant.holderFactorySignatures.joinToString("|"),
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
        renderTraceSequence.set(0L)
    }

    private fun beginRenderTrace(source: String): RuntimeRenderTrace? {
        if (!detailedDiagnosticsEnabled) {
            return null
        }

        return RuntimeRenderTrace(
            id = renderTraceSequence.incrementAndGet(),
            source = source,
            sourceNanos = SystemClock.elapsedRealtimeNanos(),
        )
    }

    private fun markStateCommitted(trace: RuntimeRenderTrace?): RuntimeRenderTrace? =
        trace?.withStateCommitted(SystemClock.elapsedRealtimeNanos())

    private fun markPresentationCommitted(trace: RuntimeRenderTrace?): RuntimeRenderTrace? =
        trace?.withPresentationCommitted(SystemClock.elapsedRealtimeNanos())

    private fun onRenderLatencySample(sample: RuntimeRenderLatencySample) {
        if (!detailedDiagnosticsEnabled) {
            return
        }

        logDiagnostic(
            level = Log.INFO,
            event = "pipeline.latency",
            component = "renderLatency",
            state = "observed",
            "traceId" to sample.traceId,
            "source" to sample.source,
            "sourceToStateUs" to sample.sourceToStateUs,
            "sourceToPresentationUs" to sample.sourceToPresentationUs,
            "stateToPresentationUs" to sample.stateToPresentationUs,
            "stateToModelUs" to sample.stateToModelUs,
            "presentationToModelUs" to sample.presentationToModelUs,
            "modelToDrawUs" to sample.modelToDrawUs,
            "sourceToDrawUs" to sample.sourceToDrawUs,
            "commitMainThread" to sample.committedOnMainThread,
            "sampling" to "latest-visible-change-only",
            "healthSnapshot" to false,
        )
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
