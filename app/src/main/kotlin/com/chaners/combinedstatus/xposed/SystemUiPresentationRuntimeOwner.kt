package com.chaners.combinedstatus.xposed

import android.graphics.drawable.Drawable
import io.github.libxposed.api.XposedModule

internal object SystemUiPresentationRuntimeOwner {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() =
            current?.let { it.tintHooks + it.sceneHooks + it.mobileTypeHooks } ?: 0

    internal data class AttachResult(
        val tintHooks: Int,
        val sceneHooks: Int,
        val mobileTypeHooks: Int,
    ) {
        val tintReady: Boolean
            get() = tintHooks == SystemUiTintStateSource.HOOK_COUNT
        val sceneReady: Boolean
            get() = sceneHooks == SystemUiSceneStateSource.HOOK_COUNT
        val mobileTypeReady: Boolean
            get() = mobileTypeHooks == SystemUiMobileTypeStateSource.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (SystemUiTintStateSource.TintUpdate) -> Unit,
        onSceneState: (SystemUiSceneStateSource.SceneUpdate) -> Unit,
        onMobileTypeChanged: (Drawable) -> Unit,
        onTintEvent: ((String) -> Unit)?,
        onSceneEvent: ((String) -> Unit)?,
    ): AttachResult {
        val tintHooks =
            SystemUiTintStateSource.install(
                module = module,
                classLoader = classLoader,
                onTintState = onTintState,
                onEvent = onTintEvent,
            ).size
        val sceneHooks =
            SystemUiSceneStateSource.install(
                module = module,
                classLoader = classLoader,
                onSceneState = onSceneState,
                onEvent = onSceneEvent,
            ).size
        val mobileTypeHooks =
            SystemUiMobileTypeStateSource.install(
                module = module,
                classLoader = classLoader,
                onChanged = onMobileTypeChanged,
            ).size

        return AttachResult(
            tintHooks = tintHooks,
            sceneHooks = sceneHooks,
            mobileTypeHooks = mobileTypeHooks,
        ).also { current = it }
    }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SystemUiTintStateSource.resetRuntimeState()
        SystemUiSceneStateSource.resetRuntimeState()
    }
}
