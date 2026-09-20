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
    }

    private companion object {
        const val TAG = "CombinedStatus"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
