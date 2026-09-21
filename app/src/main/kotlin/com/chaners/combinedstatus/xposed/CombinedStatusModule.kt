package com.chaners.combinedstatus.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class CombinedStatusModule : XposedModule() {
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

        if (compatibility.isAvailable("statusHost")) {
            StatusBarHostCapture.install(
                module = this,
                classLoader = param.classLoader,
                onCaptured = { capture ->
                    log(
                        Log.INFO,
                        TAG,
                        "Status host captured class=${capture.className} " +
                            "id=${capture.identity} replacement=${capture.replacement}",
                    )
                },
            )
        }
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
