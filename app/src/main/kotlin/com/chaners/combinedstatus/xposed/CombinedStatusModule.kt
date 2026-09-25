package com.chaners.combinedstatus.xposed

import android.graphics.drawable.Drawable
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettings
import com.chaners.combinedstatus.settings.RUNTIME_REMOTE_PREFS_NAME
import com.chaners.combinedstatus.system.RuntimeDiagnosticsProtocol
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicLong

class CombinedStatusModule : XposedModule() {
    private var islandMotionSourceInstalled = false
    private var runtimeSessionId = newRuntimeSessionId()
    private val diagnosticSequence = AtomicLong(0L)
    private val renderTraceSequence = AtomicLong(0L)

    @Volatile
    private var detailedDiagnosticsEnabled = BuildConfig.DEVELOPMENT_PROBES

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        bindRuntimeDiagnostics()
        bindRuntimeFeatureSettings()
        bindRuntimeVisualSettings()
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

        installNativeCombinedParticipant(
            classLoader = param.classLoader,
            source = "coldStart",
        )
        installNativeNetworkSuppression(
            classLoader = param.classLoader,
            source = "coldStart",
        )
        installNativeBatterySuppression(
            classLoader = param.classLoader,
            source = "coldStart",
        )
        installNativeParticipantControllerObserver(
            classLoader = param.classLoader,
            source = "coldStart",
        )

        runCatching {
            SystemUiHostRuntimeOwner.install(
                module = this,
                classLoader = param.classLoader,
                onCaptured = ::onStatusHostCaptured,
            )
        }.onSuccess {
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

        if (SystemUiHostRuntimeOwner.isReady) {
            installBatteryStateSource(
                classLoader = param.classLoader,
                source = "coldStart",
            )
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
        val prepared =
            SystemUiHotReloadRuntimeOwner.prepare(
                param = param,
            )
        if (prepared is SystemUiHotReloadRuntimeOwner.PrepareResult.Unavailable) {
            logDiagnostic(
                level = Log.WARN,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "unavailable",
                "reason" to prepared.reason,
                "wifiRoots" to prepared.wifiRoots,
                "mobileRoots" to prepared.mobileRoots,
                "restartScope" to true,
            )
            log(Log.WARN, TAG, "Hot reload declined reason=" + prepared.reason)
            return false
        }

        prepared as SystemUiHotReloadRuntimeOwner.PrepareResult.Ready
        val hookCount =
            1 +
                SystemUiBatteryRuntimeOwner.installedHookCount +
                SystemUiNetworkRuntimeOwner.installedHookCount +
                SystemUiPresentationRuntimeOwner.installedHookCount +
                SystemUiNativeParticipantRuntimeOwner.installedHookCount +
                SystemUiNativeCombinedParticipantOwner.installedHookCount +
                SystemUiNativeNetworkSuppressionOwner.installedHookCount +
                SystemUiNativeBatterySuppressionOwner.installedHookCount +
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
            "transfer" to "classloader-neutral",
            "hostIdentity" to System.identityHashCode(prepared.host),
            "wifiRoots" to prepared.wifiRoots,
            "mobileRoots" to prepared.mobileRoots,
        )
        log(
            Log.INFO,
            TAG,
            "Hot reload preparing build=" + BuildConfig.BUILD_ID +
                " hooks=" + hookCount +
                " transfer=classloader-neutral",
        )

        val hostView =
            prepared.host as? android.view.View
                ?: return false
        val cleanupScheduled =
            hostView.post {
                runCatching {
                    teardownOldGenerationForHotReload()
                }.onFailure { error ->
                    log(
                        Log.ERROR,
                        TAG,
                        "Old-generation Hot Reload cleanup failed",
                        error,
                    )
                }
            }
        if (!cleanupScheduled) {
            logDiagnostic(
                level = Log.WARN,
                event = "hotReload.prepare",
                component = "hotReload",
                state = "unavailable",
                "reason" to "main-thread-cleanup-scheduling-failed",
                "restartScope" to true,
            )
            return false
        }

        logDiagnostic(
            level = Log.INFO,
            event = "hotReload.cleanup",
            component = "hotReload",
            state = "scheduled",
            "uiMutation" to "main-thread-only",
            "nativeParticipant" to "preserved-for-adoption",
        )
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        rotateDiagnosticSession()
        val takeover =
            SystemUiHotReloadRuntimeOwner.takeOverHooks(
                param = param,
                onCaptured = ::onStatusHostCaptured,
            )

        if (takeover == null) {
            bindRuntimeDiagnostics()
            bindRuntimeFeatureSettings()
            bindRuntimeVisualSettings()
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
            val removed = takeover.removedHooks

            SystemUiBatteryRuntimeOwner.resetRuntimeState()
            SystemUiNetworkRuntimeOwner.resetRuntimeState()
            islandMotionSourceInstalled = false
            SystemUiPresentationRuntimeOwner.resetRuntimeState()
            SystemUiNativeParticipantRuntimeOwner.resetControllerRuntimeState()
            SystemUiNativeNetworkSuppressionOwner.resetRuntimeState("hotReload")
            SystemUiNativeBatterySuppressionOwner.resetRuntimeState("hotReload")
            bindRuntimeDiagnostics()
            bindRuntimeFeatureSettings()
            bindRuntimeVisualSettings()
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

            val classLoader = takeover.classLoader
            installBatteryStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNetworkStateSource(
                classLoader = classLoader,
                source = "hotReload",
            )
            installPresentationRuntimeSources(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNativeCombinedParticipant(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNativeNetworkSuppression(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNativeBatterySuppression(
                classLoader = classLoader,
                source = "hotReload",
            )
            installNativeParticipantControllerObserver(
                classLoader = classLoader,
                source = "hotReload",
            )
            if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                installIslandMotionSource(
                    classLoader = classLoader,
                    source = "hotReload",
                )
            }

            val restored = SystemUiHotReloadRuntimeOwner.restoreTransfer(param)
            if (restored == null) {
                CombinedStatusStateStore.restoreHotReloadState(null)
                logDiagnostic(
                    level = Log.WARN,
                    event = "hotReload.restore",
                    component = "hotReload",
                    state = "unavailable",
                    "reason" to "saved-state-missing-or-unsupported-generation",
                    "restartScope" to true,
                )
                logDiagnostic(
                    level = Log.WARN,
                    event = "hotReload.complete",
                    component = "hotReload",
                    state = "partial",
                    "build" to BuildConfig.BUILD_ID,
                    "statusHostHook" to "replaced",
                    "staleHooks" to removed,
                    "restartScope" to true,
                )
                return@runCatching
            }

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

            val hostView = capture.host as? android.view.View
                ?: error("restored-host-not-view")
            val restoreScheduled =
                hostView.post {
                    restoreHotReloadRuntimeOnMain(
                        capture = capture,
                        restored = restored,
                        removedHooks = removed,
                    )
                }
            if (!restoreScheduled) {
                logDiagnostic(
                    level = Log.ERROR,
                    event = "hotReload.complete",
                    component = "hotReload",
                    state = "error",
                    "reason" to "main-thread-restore-scheduling-failed",
                    "restartScope" to true,
                )
                return@runCatching
            }

            logDiagnostic(
                level = Log.INFO,
                event = "hotReload.restore",
                component = "hotReload",
                state = "scheduled",
                "hostIdentity" to capture.identity,
                "uiMutation" to "main-thread-only",
                "restartScope" to false,
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

    private fun restoreHotReloadRuntimeOnMain(
        capture: SystemUiHostRegistry.Capture,
        restored: CombinedStatusHotReloadTransfer.Restored,
        removedHooks: Int,
    ) {
        runCatching {
            var restoredSnapshot =
                CombinedStatusStateStore.restoreHotReloadState(restored.state)
            val bindings =
                SystemUiNetworkStateSource.restoreHotReloadBindings(restored.bindings)
            SystemUiNetworkStateSource.seedRestoredWifiState(
                onEvent =
                    if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                        ::onNetworkPipelineEvent
                    } else {
                        null
                    },
            )?.let { wifi ->
                CombinedStatusStateStore.updateWifi(wifi)?.let { snapshot ->
                    restoredSnapshot = snapshot
                }
            }
            val controllerRestored =
                SystemUiNativeParticipantRuntimeOwner.restoreExistingController(
                    capture.host,
                )
            val nativeAdoption =
                SystemUiNativeCombinedParticipantOwner.adoptAfterHotReload(
                    host = capture.host,
                )
            val nativeFailure =
                nativeAdoption as?
                    SystemUiNativeCombinedParticipantOwner.HotReloadAdoptResult.Failure
            val nativeReady =
                nativeAdoption is
                    SystemUiNativeCombinedParticipantOwner.HotReloadAdoptResult.Ready &&
                    controllerRestored
            val nativeState =
                if (nativeReady) {
                    "ready"
                } else {
                    "fallback"
                }
            val nativeReason =
                when {
                    nativeFailure != null ->
                        nativeFailure.reason
                    nativeAdoption is
                        SystemUiNativeCombinedParticipantOwner.HotReloadAdoptResult.NotPresent ->
                        "native-participant-not-present"
                    !controllerRestored ->
                        "controller-registration-restore-failed"
                    else ->
                        null
                }

            logDiagnostic(
                level = if (nativeReady) Log.INFO else Log.WARN,
                event = "hotReload.rebind",
                component = "nativeCombinedParticipant",
                state = nativeState,
                "result" to nativeAdoption.javaClass.simpleName,
                "reason" to nativeReason,
                "controllerRestored" to controllerRestored,
                "mainThread" to true,
                "nativeGeometryWrites" to 0,
            )

            attachHostRuntime(
                host = capture.host,
                source = "hotReloadRestore",
                initialNativeHandoffActive = nativeReady,
            )

            logDiagnostic(
                level = if (nativeReady) Log.INFO else Log.WARN,
                event = "hotReload.restore",
                component = "hotReload",
                state = if (nativeReady) "ready" else "partial",
                "hostIdentity" to capture.identity,
                "wifiRoots" to bindings.wifiRoots,
                "mobileRoots" to bindings.mobileRoots,
                "state" to restoredSnapshot.logLine,
                "nativeAdoption" to nativeAdoption.javaClass.simpleName,
                "mainThread" to true,
            )
            logDiagnostic(
                level = if (nativeReady) Log.INFO else Log.WARN,
                event = "hotReload.complete",
                component = "hotReload",
                state = if (nativeReady) "ready" else "partial",
                "build" to BuildConfig.BUILD_ID,
                "statusHostHook" to "replaced",
                "staleHooks" to removedHooks,
                "restartScope" to !nativeReady,
            )
            log(
                if (nativeReady) Log.INFO else Log.WARN,
                TAG,
                "Hot reload completed build=" + BuildConfig.BUILD_ID +
                    " statusHostHook=replaced staleHooks=" + removedHooks +
                    " restored=" + nativeReady,
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
            log(Log.ERROR, TAG, "Hot reload main-thread restore failed", error)
        }
    }

    private fun installNativeCombinedParticipant(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeCombinedParticipantOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onSlotOrderResult = { slotOrder ->
                        when (slotOrder) {
                            is NativeStatusBarSlotReservation.Result.Ready -> {
                                logDiagnostic(
                                    level = Log.INFO,
                                    event = "slot.reserve",
                                    component = "nativeSlotOrder",
                                    state = "ready",
                                    "source" to source,
                                    "mode" to "controller-pre-init",
                                    "created" to slotOrder.created,
                                    "nativeIndex" to slotOrder.nativeIndex,
                                    "fromIndex" to slotOrder.fromIndex,
                                    "toIndex" to slotOrder.toIndex,
                                    "slotCount" to slotOrder.slotCount,
                                    "viewOnlySynced" to slotOrder.viewOnlySynced,
                                    "originalOrderPreserved" to
                                        slotOrder.originalOrderPreserved,
                                    "visible" to false,
                                    "nativeGeometryWrites" to 0,
                                )
                            }

                            is NativeStatusBarSlotReservation.Result.Failure -> {
                                logDiagnostic(
                                    level = Log.WARN,
                                    event = "slot.reserve",
                                    component = "nativeSlotOrder",
                                    state = "unavailable",
                                    "source" to source,
                                    "mode" to "controller-pre-init",
                                    "reason" to slotOrder.reason,
                                    "visible" to false,
                                    "nativeGeometryWrites" to 0,
                                )
                            }
                        }
                    },
                )
        ) {
            SystemUiNativeCombinedParticipantOwner.InstallResult.Installed,
            SystemUiNativeCombinedParticipantOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeCombinedParticipantOwner.installedHookCount,
                    "visible" to false,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeCombinedParticipantOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeCombinedParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeNetworkSuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeNetworkSuppressionOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onStatusPresentationChanged = ::onStatusIconPresentationChanged,
                )
        ) {
            SystemUiNativeNetworkSuppressionOwner.InstallResult.Installed,
            SystemUiNativeNetworkSuppressionOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeNetworkSuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeNetworkSuppressionOwner.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeNetworkSuppressionOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeNetworkSuppression",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeBatterySuppression(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeBatterySuppressionOwner.install(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                    onNativeHideChanged =
                        SystemUiNativeCombinedParticipantOwner::onNativeBatteryHideChanged,
                )
        ) {
            SystemUiNativeBatterySuppressionOwner.InstallResult.Installed,
            SystemUiNativeBatterySuppressionOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeBatterySuppression",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeBatterySuppressionOwner.installedHookCount,
                    "contract" to
                        "MiuiStatusBatteryContainer.setIsHideBattery(Boolean)",
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeBatterySuppressionOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeBatterySuppression",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNativeParticipantControllerObserver(
        classLoader: ClassLoader,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeParticipantRuntimeOwner.installControllerObserver(
                    module = this,
                    classLoader = classLoader,
                    onEvent = { event ->
                        if (detailedDiagnosticsEnabled) {
                            log(Log.INFO, TAG, event)
                        }
                    },
                )
        ) {
            SystemUiNativeParticipantRuntimeOwner.InstallResult.Installed,
            SystemUiNativeParticipantRuntimeOwner.InstallResult.AlreadyInstalled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "hook.install",
                    component = "nativeParticipantControllerObserver",
                    state = "ready",
                    "source" to source,
                    "hooks" to SystemUiNativeParticipantRuntimeOwner.installedHookCount,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeParticipantRuntimeOwner.InstallResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "hook.install",
                    component = "nativeParticipantControllerObserver",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun installNetworkStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiNetworkRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onWifiState = { state ->
                    val trace = beginRenderTrace("wifi")
                    val changed = CombinedStatusStateStore.updateWifi(state)
                    if (changed != null) {
                        val stateTrace = markStateCommitted(trace)
                        updateNativeNetworkSuppressionPolicy("wifi-semantic")
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
                    if (changed != null) {
                        onCombinedStateChanged(
                            snapshot = CombinedStatusStateStore.snapshot(),
                            trace = stateTrace,
                        )
                    }
                },
                onMobileSignalWillApply = { image ->
                    SystemUiNativeNetworkSuppressionOwner.preMaskMobileSignal(image)
                },
                onPresentationChanged = {
                    refreshMobilePresentation(beginRenderTrace("networkPresentation"))
                },
                onEvent = if (BuildConfig.RUNTIME_DIAGNOSTICS) ::onNetworkPipelineEvent else null,
            )
        }.onSuccess { result ->
            updateNativeNetworkSuppressionPolicy("network-source:" + source)
            val fullyReady =
                result.wifiReady &&
                    result.mobileReady &&
                    SystemUiNetworkRuntimeOwner.installedHookCount == SystemUiNetworkStateSource.HOOK_COUNT
            val state =
                when {
                    fullyReady -> "ready"
                    SystemUiNetworkRuntimeOwner.installedHookCount > 0 -> "partial"
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
                "hooks" to SystemUiNetworkRuntimeOwner.installedHookCount,
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
                    " hooks=" + SystemUiNetworkRuntimeOwner.installedHookCount +
                    "/" + SystemUiNetworkStateSource.HOOK_COUNT +
                    " wifi=" + result.wifiReady +
                    " mobile=" + result.mobileReady +
                    " source=" + source +
                    " rebindRequired=" + (source == "hotReload"),
            )
        }.onFailure { error ->
            SystemUiNetworkRuntimeOwner.resetRuntimeState()
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
                isProbeEnabled = {
                    BuildConfig.DEVELOPMENT_PROBES || detailedDiagnosticsEnabled
                },
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

    private fun installBatteryStateSource(
        classLoader: ClassLoader,
        source: String,
    ) {
        runCatching {
            SystemUiBatteryRuntimeOwner.attach(
                module = this,
                classLoader = classLoader,
                onBatteryState = { state ->
                    val trace = beginRenderTrace("battery")
                    CombinedStatusStateStore.updateBattery(state)?.let { snapshot ->
                        onCombinedStateChanged(
                            snapshot = snapshot,
                            trace = markStateCommitted(trace),
                        )
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
        }.onSuccess { result ->
            logDiagnostic(
                level = if (result.ready) Log.INFO else Log.WARN,
                event = "source.install",
                component = "batteryState",
                state = if (result.ready) "ready" else "partial",
                "hooks" to result.hooks,
                "expectedHooks" to SystemUiBatteryStateSource.HOOK_COUNT,
                "source" to source,
                "authority" to
                    "MiuiBatteryMeterView.onBatteryLevelChanged(int,boolean,boolean)",
                "eventDriven" to true,
            )
        }.onFailure { error ->
            SystemUiBatteryRuntimeOwner.resetRuntimeState()
            logDiagnostic(
                level = Log.ERROR,
                event = "source.install",
                component = "batteryState",
                state = "error",
                "reason" to (error.message ?: error.javaClass.simpleName),
                "source" to source,
            )
            log(Log.ERROR, TAG, "Battery state source installation failed", error)
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
                onTintState = ::onTintStateUpdate,
                onSceneState = ::onSceneStateUpdate,
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
        val recoveryCompleted =
            CombinedStatusStateStore.completeMobileRecoveryIfReady(
                preferredSubscriptionId = presentation.effectiveDataSubscriptionId ?: -1,
                mobileTypeReady = presentation.networkType != null,
                mobileDataEnabled =
                    CombinedStatusPresentationStateStore
                        .snapshot()
                        .connectivity
                        .mobileDataEnabled,
            )
        if (changed != null || recoveryCompleted != null) {
            val presentationTrace = markPresentationCommitted(trace)
            if (detailedDiagnosticsEnabled && changed != null) {
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
            if (detailedDiagnosticsEnabled && recoveryCompleted != null) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "mobile.recovery",
                    component = "network",
                    state = "ready",
                    "effectiveDataSubId" to presentation.effectiveDataSubscriptionId,
                    "networkType" to presentation.networkType?.label,
                    "eventDriven" to true,
                )
            }
            onPresentationStateChanged(
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
        SystemUiNativeCombinedParticipantOwner.onState(snapshot, trace)
    }

    private fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        CombinedStatusHomeRenderSession.onPresentationStateChanged(trace)
        SystemUiNativeCombinedParticipantOwner.onPresentationStateChanged(trace)
        updateNativeNetworkSuppressionPolicy("presentation")
    }

    private fun updateNativeNetworkSuppressionPolicy(source: String) {
        val presentation =
            CombinedStatusPresentationStateStore.snapshot()
        val state = CombinedStatusStateStore.snapshot()
        val wifi = state.wifi
        SystemUiNativeNetworkSuppressionOwner.updatePolicy(
            suppressWifi =
                SystemUiNetworkRuntimeOwner.wifiReady &&
                    CombinedStatusConnectivityPolicy.wifiReplacementReady(
                        wifi = wifi,
                        connectivity = presentation.connectivity,
                    ),
            suppressMobile =
                NativeNetworkSuppressionPolicy.suppressMobile(
                    airplaneMode = state.airplaneMode,
                    presentation = presentation.mobilePresentation,
                    wasSuppressed =
                        SystemUiNativeNetworkSuppressionOwner.mobileSuppressionActive,
                ),
            source = source,
            forceRevalidate =
                source == "airplane" ||
                    source == "scene-unlocked",
        )
    }

    private fun onStatusIconPresentationChanged(
        state: CombinedStatusPresentationStateStore.StatusIconPresentation,
    ) {
        val trace = beginRenderTrace("statusIcons")
        val changed =
            CombinedStatusPresentationStateStore.updateStatusIcons(state)

        val tintSourceView = SystemUiTintStateSource.currentSourceView()
        if (tintSourceView != null) {
            SystemUiTintStateSource.currentState(tintSourceView)?.let { tintState ->
                onTintStateUpdate(
                    SystemUiTintStateSource.TintUpdate(
                        sourceView = tintSourceView,
                        state = tintState,
                    ),
                )
            }
        }

        if (changed != null) {
            val presentationTrace = markPresentationCommitted(trace)
            CombinedStatusHomeRenderSession.onPresentationStateChanged(
                presentationTrace,
            )
            SystemUiNativeCombinedParticipantOwner.onPresentationStateChanged(
                presentationTrace,
            )
            if (detailedDiagnosticsEnabled) {
                logDiagnostic(
                    level = Log.INFO,
                    event = "presentation.resolve",
                    component = "statusIcons",
                    state = "ready",
                    "tint" to
                        (
                            state.appliedTint
                                ?.toUInt()
                                ?.toString(16)
                                ?.padStart(8, '0')
                                ?: "none"
                        ),
                    "noSimVisible" to state.noSimVisible,
                    "noSimPackage" to state.noSimIcon?.packageName,
                    "noSimResId" to state.noSimIcon?.resourceId,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun onTintStateUpdate(update: SystemUiTintStateSource.TintUpdate) {
        CombinedStatusHomeRenderSession.onTintUpdate(update)
        SystemUiNativeCombinedParticipantOwner.onTintUpdate(update)
    }

    private fun onSceneStateUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
        SystemUiTintStateSource.currentState(update.sourceView)?.let { state ->
            onTintStateUpdate(
                SystemUiTintStateSource.TintUpdate(
                    sourceView = update.sourceView,
                    state = state,
                ),
            )
        }
        CombinedStatusHomeRenderSession.onSceneUpdate(update)
        SystemUiNativeCombinedParticipantOwner.onSceneUpdate(update)
        if (
            update.surface ==
                SystemUiSceneStateSource.Surface.UNLOCKED_STATUS_BAR
        ) {
            updateNativeNetworkSuppressionPolicy("scene-unlocked")
        }
    }

    private fun teardownOldGenerationForHotReload() {
        val nativeParticipantPendingCancelled =
            SystemUiNativeParticipantRuntimeOwner.cancelPending()
        CombinedStatusHomeRenderSession.detach()
        StatusBarStableSession.detach()
        SystemUiCoreRuntimeOwner.detach()
        SystemUiPresentationRuntimeOwner.resetRuntimeState()
        CombinedStatusPresentationStateStore.reset()
        SystemUiIslandMotionSource.resetRuntimeState()
        val nativeRuntimeReleased =
            SystemUiNativeCombinedParticipantOwner.releaseGenerationForHotReload()

        logDiagnostic(
            level = if (nativeRuntimeReleased) Log.INFO else Log.WARN,
            event = "runtime.teardown",
            component = "runtimeSession",
            state = if (nativeRuntimeReleased) "ready" else "partial",
            "source" to "hotReload.oldGeneration",
            "rendererDetached" to true,
            "stableStatusDetached" to true,
            "airplaneObserverDetached" to true,
            "defaultDataSubscriptionObserverDetached" to true,
            "nativeParticipantPendingCancelled" to nativeParticipantPendingCancelled,
            "nativeCombinedParticipant" to "preserved-for-main-thread-adoption",
            "nativeRuntimeReferencesReleased" to nativeRuntimeReleased,
            "mainThread" to true,
        )
        unbindRuntimeDiagnostics()
        RuntimeFeaturePreferencesOwner.unbind()
        RuntimeVisualPreferencesOwner.unbind()
    }

    private fun attachHostRuntime(
        host: Any,
        source: String,
        initialNativeHandoffActive: Boolean = false,
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
                        updateNativeNetworkSuppressionPolicy("airplane")
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
                            onPresentationStateChanged(
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
                onLatencySample = ::onRenderLatencySample,
                isDetailedDiagnosticsEnabled = { detailedDiagnosticsEnabled },
                initialNativeHandoffActive = initialNativeHandoffActive,
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

        scheduleNativeParticipantRuntime(
            host = host,
            source = source,
        )

        scheduleNativeSlotProbe(host = host, source = source)

        logDiagnostic(
            level = Log.INFO,
            event = "runtime.attach",
            component = "runtimeSession",
            state = "ready",
            "source" to source,
        )
    }

    private fun scheduleNativeParticipantRuntime(
        host: Any,
        source: String,
    ) {
        when (
            val result =
                SystemUiNativeParticipantRuntimeOwner.schedule(
                    host = host,
                    onReady = { readyHost ->
                        attachNativeCombinedParticipant(
                            host = readyHost,
                            source = source,
                        )
                        if (BuildConfig.RUNTIME_DIAGNOSTICS) {
                            runNativeParticipantDiagnostics(
                                host = readyHost,
                                source = source,
                            )
                        }
                    },
                    onFailure = { reason ->
                        logDiagnostic(
                            level = Log.WARN,
                            event = "participant.lifecycle",
                            component = "nativeParticipant",
                            state = "unavailable",
                            "source" to source,
                            "reason" to reason,
                            "nativeGeometryWrites" to 0,
                        )
                    },
                )
        ) {
            SystemUiNativeParticipantRuntimeOwner.ScheduleResult.Scheduled -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "participant.lifecycle",
                    component = "nativeParticipant",
                    state = "pending",
                    "source" to source,
                    "trigger" to "native-dark-icon-manager-registered",
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeParticipantRuntimeOwner.ScheduleResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "participant.lifecycle",
                    component = "nativeParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to result.reason,
                    "nativeGeometryWrites" to 0,
                )
            }
        }
    }

    private fun runNativeParticipantDiagnostics(
        host: Any,
        source: String,
    ) {
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
            "controllerSource" to nativeParticipant.controllerSource,
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

        if (nativeParticipant.registrationContractReady) {
            val bindableParticipant =
                NativeBindableParticipantContractProbe.inspect(host)
            log(Log.INFO, TAG, bindableParticipant.logLine)
            val bindableProbeReady =
                bindableParticipant.staticContractReady &&
                    bindableParticipant.managerBindableMapReady &&
                    bindableParticipant.viewOnlySlotsReady
            logDiagnostic(
                level = if (bindableProbeReady) Log.INFO else Log.WARN,
                event = "contract.probe",
                component = "nativeBindableParticipant",
                state = if (bindableProbeReady) "ready" else "observed",
                "source" to source,
                "available" to bindableParticipant.available,
                "reason" to bindableParticipant.reason,
                "interfaceReady" to bindableParticipant.bindableInterfaceReady,
                "creatorReady" to bindableParticipant.creatorReady,
                "registry" to bindableParticipant.registryClass,
                "registryConstructors" to
                    bindableParticipant.registryConstructors.joinToString("|"),
                "holder" to bindableParticipant.holderClass,
                "holderConstructors" to
                    bindableParticipant.holderConstructors.joinToString("|"),
                "modernView" to bindableParticipant.modernViewClass,
                "singleView" to bindableParticipant.singleBindableViewClass,
                "managerMapReady" to bindableParticipant.managerBindableMapReady,
                "managerMapCount" to bindableParticipant.managerBindableCount,
                "managerEntries" to
                    bindableParticipant.managerBindableEntries.joinToString("|"),
                "viewOnlySlotsReady" to bindableParticipant.viewOnlySlotsReady,
                "viewOnlySlots" to
                    bindableParticipant.viewOnlySlots.joinToString("|"),
                "runtimeViews" to
                    bindableParticipant.runtimeBindableViews.joinToString("|"),
                "slotOrder" to
                    bindableParticipant.runtimeSlotOrder.joinToString("|"),
                "groupClipChildren" to bindableParticipant.groupClipChildren,
                "groupClipToPadding" to bindableParticipant.groupClipToPadding,
                "groupHeight" to bindableParticipant.groupHeight,
                "staticContractReady" to bindableParticipant.staticContractReady,
                "dynamicRegistrationObserved" to
                    bindableParticipant.dynamicRegistrationObserved,
                "nativeGeometryWrites" to 0,
            )

            val visualGeometry =
                NativeBindableVisualGeometryProbe.inspect(host)
            log(Log.INFO, TAG, visualGeometry.logLine)
            logDiagnostic(
                level = if (visualGeometry.ready) Log.INFO else Log.WARN,
                event = "contract.probe",
                component = "nativeBindableVisualGeometry",
                state = if (visualGeometry.ready) "ready" else "observed",
                "source" to source,
                "available" to visualGeometry.available,
                "reason" to visualGeometry.reason,
                "reference" to visualGeometry.referenceClass,
                "referenceBounds" to visualGeometry.referenceBounds,
                "referenceLayoutWidth" to visualGeometry.referenceLayoutWidth,
                "referenceLayoutHeight" to visualGeometry.referenceLayoutHeight,
                "groupHeight" to visualGeometry.groupHeight,
                "groupClipChildren" to visualGeometry.groupClipChildren,
                "groupClipToPadding" to visualGeometry.groupClipToPadding,
                "visualWidth" to visualGeometry.visualWidth,
                "visualHeight" to visualGeometry.visualHeight,
                "shellMeasuredWidth" to visualGeometry.shellMeasuredWidth,
                "shellMeasuredHeight" to visualGeometry.shellMeasuredHeight,
                "shellClipChildren" to visualGeometry.shellClipChildren,
                "renderMeasuredWidth" to visualGeometry.renderMeasuredWidth,
                "renderMeasuredHeight" to visualGeometry.renderMeasuredHeight,
                "renderBounds" to visualGeometry.renderBounds,
                "projectedTop" to visualGeometry.projectedTop,
                "projectedBottom" to visualGeometry.projectedBottom,
                "projectedFitsGroup" to visualGeometry.projectedFitsGroup,
                "nativeGeometryWrites" to 0,
            )

        }
    }

    private fun attachNativeCombinedParticipant(
        host: Any,
        source: String,
    ) {
        when (
            val nativeCombined =
                SystemUiNativeCombinedParticipantOwner.attachHidden(
                    host = host,
                    onHandoffStateChanged = { active ->
                        val networkSuppression =
                            if (active) {
                                val presentation =
                                    CombinedStatusPresentationStateStore.snapshot()
                                val state =
                                    CombinedStatusStateStore.snapshot()
                                val wifi = state.wifi
                                SystemUiNativeNetworkSuppressionOwner.activate(
                                    host = host,
                                    suppressWifi =
                                        SystemUiNetworkRuntimeOwner.wifiReady &&
                                            CombinedStatusConnectivityPolicy
                                                .wifiReplacementReady(
                                                    wifi = wifi,
                                                    connectivity = presentation.connectivity,
                                                ),
                                    suppressMobile =
                                        NativeNetworkSuppressionPolicy.suppressMobile(
                                            airplaneMode = state.airplaneMode,
                                            presentation = presentation.mobilePresentation,
                                            wasSuppressed = false,
                                        ),
                                )
                            } else {
                                SystemUiNativeNetworkSuppressionOwner.deactivate(
                                    "native-handoff-fallback",
                                )
                            }
                        val batterySuppression =
                            if (active) {
                                SystemUiNativeBatterySuppressionOwner.activate(
                                    host = host,
                                    source = "native-handoff:" + source,
                                )
                            } else {
                                SystemUiNativeBatterySuppressionOwner.deactivate(
                                    "native-handoff-fallback",
                                )
                            }
                        CombinedStatusHomeRenderSession.setNativeHandoffActive(active)
                        val suppressionFailure =
                            active &&
                                (
                                    networkSuppression is
                                        SystemUiNativeNetworkSuppressionOwner.StateResult.Failure ||
                                        batterySuppression is
                                            SystemUiNativeBatterySuppressionOwner.StateResult.Failure
                                )
                        val batteryGeometryWrite =
                            when (batterySuppression) {
                                is SystemUiNativeBatterySuppressionOwner.StateResult.Active ->
                                    batterySuppression.changed
                                is SystemUiNativeBatterySuppressionOwner.StateResult.Inactive ->
                                    batterySuppression.changed
                                is SystemUiNativeBatterySuppressionOwner.StateResult.Failure ->
                                    false
                            }
                        logDiagnostic(
                            level = if (suppressionFailure) Log.WARN else Log.INFO,
                            event = "visibility.handoff",
                            component = "nativeCombinedParticipant",
                            state = if (active) "active" else "fallback",
                            "source" to source,
                            "nativeActive" to active,
                            "overlayActive" to !active,
                            "networkSuppression" to networkSuppression.summary,
                            "batterySuppression" to batterySuppression.summary,
                            "nativeGeometryWrites" to if (batteryGeometryWrite) 1 else 0,
                        )
                    },
                )
        ) {
            is SystemUiNativeCombinedParticipantOwner.AttachResult.Ready -> {
                logDiagnostic(
                    level = Log.INFO,
                    event = "participant.attach",
                    component = "nativeCombinedParticipant",
                    state = "ready",
                    "source" to source,
                    "slot" to SystemUiNativeCombinedParticipantOwner.SLOT,
                    "visible" to false,
                    "registryRestored" to nativeCombined.registryRestored,
                    "root" to nativeCombined.rootClass,
                    "rootVisibility" to nativeCombined.rootVisibility,
                    "iconVisible" to nativeCombined.iconVisible,
                    "layoutWidth" to nativeCombined.layoutWidth,
                    "layoutHeight" to nativeCombined.layoutHeight,
                    "renderWidth" to nativeCombined.renderWidth,
                    "renderHeight" to nativeCombined.renderHeight,
                    "renderTop" to nativeCombined.renderTop,
                    "renderBottom" to nativeCombined.renderBottom,
                    "managerEntry" to nativeCombined.managerEntry,
                    "modelReady" to nativeCombined.modelReady,
                    "tintReady" to nativeCombined.tintReady,
                    "nativeGeometryWrites" to 0,
                )
            }

            is SystemUiNativeCombinedParticipantOwner.AttachResult.Failure -> {
                logDiagnostic(
                    level = Log.WARN,
                    event = "participant.attach",
                    component = "nativeCombinedParticipant",
                    state = "unavailable",
                    "source" to source,
                    "reason" to nativeCombined.reason,
                    "visible" to false,
                    "nativeGeometryWrites" to 0,
                )
            }
        }

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
            RuntimeDiagnosticsPreferencesOwner.unbind()
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
            RuntimeDiagnosticsPreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                forceDetailed = BuildConfig.DEVELOPMENT_PROBES,
                onDetailedChanged = ::setDetailedDiagnosticsEnabled,
            )
        }.onSuccess { result ->
            logDiagnostic(
                level = Log.INFO,
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                "level" to if (result.detailedEnabled) "detailed" else "general",
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeDiagnosticsPreferencesOwner.unbind()
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

    private fun bindRuntimeFeatureSettings() {
        runCatching {
            RuntimeFeaturePreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onRuntimeFeatureSettingsChanged,
            )
        }.onSuccess { settings ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "ready",
                "combinedStatusEnabled" to settings.enabled,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeFeaturePreferencesOwner.unbind()
            onRuntimeFeatureSettingsChanged(
                RuntimeFeaturePreferencesOwner.currentSettings(),
                null,
            )
            logDiagnostic(
                level = Log.WARN,
                event = "runtimePreferences.bind",
                component = "featureSettings",
                state = "unavailable",
                "combinedStatusEnabled" to false,
                "reason" to (error.message ?: error.javaClass.simpleName),
                "fallback" to "native-systemui",
            )
        }
    }

    private fun onRuntimeFeatureSettingsChanged(
        settings: CombinedStatusFeatureSettings,
        preferenceTransportLatencyNanos: Long?,
    ) {
        if (settings.enabled) {
            SystemUiNativeCombinedParticipantOwner.onFeatureSettingsChanged(settings)
            CombinedStatusHomeRenderSession.onFeatureSettingsChanged(settings)
        } else {
            // Close the overlay gate before releasing native suppression so a
            // fallback overlay cannot become visible during the same UI turn.
            CombinedStatusHomeRenderSession.onFeatureSettingsChanged(settings)
            SystemUiNativeCombinedParticipantOwner.onFeatureSettingsChanged(settings)
        }
        logDiagnostic(
            level = Log.INFO,
            event = "featureSettings.changed",
            component = "combinedStatus",
            state = if (settings.enabled) "enabled" else "disabled",
            "combinedStatusEnabled" to settings.enabled,
            "preferenceTransportMs" to
                (
                    preferenceTransportLatencyNanos
                        ?.let { nanos -> nanos / 1_000_000.0 }
                        ?: "initial-bind"
                ),
            "eventDriven" to true,
            "fallback" to if (settings.enabled) "combined-status" else "native-systemui",
        )
    }

    private fun bindRuntimeVisualSettings() {
        runCatching {
            RuntimeVisualPreferencesOwner.bind(
                preferences = getRemotePreferences(RUNTIME_REMOTE_PREFS_NAME),
                onChanged = ::onRuntimeVisualSettingsChanged,
            )
        }.onSuccess { settings ->
            logDiagnostic(
                level = Log.INFO,
                event = "runtimePreferences.bind",
                component = "visualSettings",
                state = "ready",
                "mobileFollowsBattery" to settings.mobileFollowsBatteryColor,
                "centerFollowsBattery" to settings.centerFollowsBatteryColor,
                "transport" to "remote-preferences",
            )
        }.onFailure { error ->
            RuntimeVisualPreferencesOwner.unbind()
            logDiagnostic(
                level = Log.WARN,
                event = "runtimePreferences.bind",
                component = "visualSettings",
                state = "unavailable",
                "reason" to (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    private fun onRuntimeVisualSettingsChanged(
        settings: com.chaners.combinedstatus.settings.CombinedStatusVisualSettings,
    ) {
        CombinedStatusHomeRenderSession.onVisualSettingsChanged(settings)
        SystemUiNativeCombinedParticipantOwner.onVisualSettingsChanged(settings)
        if (detailedDiagnosticsEnabled) {
            logDiagnostic(
                level = Log.INFO,
                event = "visualSettings.changed",
                component = "renderer",
                state = "ready",
                "mobileFollowsBattery" to settings.mobileFollowsBatteryColor,
                "centerFollowsBattery" to settings.centerFollowsBatteryColor,
                "eventDriven" to true,
            )
        }
    }

    private fun logCurrentDiagnosticsHealth() {
        val state =
            when {
                !BuildConfig.RUNTIME_DIAGNOSTICS -> "disabled"
                RuntimeDiagnosticsPreferencesOwner.isBound -> "ready"
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
        RuntimeDiagnosticsPreferencesOwner.unbind()
    }

    private fun setDetailedDiagnosticsEnabled(enabled: Boolean) {
        val previous = detailedDiagnosticsEnabled
        detailedDiagnosticsEnabled = enabled
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
