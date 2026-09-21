package com.chaners.combinedstatus.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class CombinedStatusModule : XposedModule() {
    private var statusHostHookInstalled = false

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "Module loaded in ${param.processName} with Xposed API $apiVersion",
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
                onCaptured = ::logStatusHostCapture,
            )
        }.onSuccess {
            statusHostHookInstalled = true
        }.onFailure { error ->
            log(Log.ERROR, TAG, "Status host hook installation failed", error)
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        if (!statusHostHookInstalled) {
            log(Log.WARN, TAG, "Hot reload declined reason=status-host-hook-not-ready")
            return false
        }

        log(Log.INFO, TAG, "Hot reload preparing hooks=1")
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
                onCaptured = ::logStatusHostCapture,
            )
        }.onSuccess {
            statusHostHookInstalled = true

            var removed = 0
            oldHandles.forEach { handle ->
                if (handle !== statusHostHandle) {
                    handle.unhook()
                    removed += 1
                }
            }

            log(
                Log.INFO,
                TAG,
                "Hot reload completed statusHostHook=replaced staleHooks=$removed",
            )
        }.onFailure { error ->
            log(Log.ERROR, TAG, "Hot reload failed restartScope=true", error)
        }
    }

    private fun logStatusHostCapture(capture: SystemUiHostRegistry.Capture) {
        log(
            Log.INFO,
            TAG,
            "statusHost captured id=${capture.identity} replacement=${capture.replacement}",
        )
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
