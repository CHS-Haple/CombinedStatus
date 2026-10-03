package com.chaners.guiyuan.xposed

import android.graphics.drawable.Drawable
import io.github.libxposed.api.XposedModule

internal object SystemUiPresentationRuntimeOwner {
    private var current: AttachResult? = null

    val installedHookCount: Int
        @Synchronized get() =
            current?.let {
                it.tintHooks +
                    it.sceneHooks +
                    it.mobileTypeHooks +
                    it.keyguardAodHooks +
                    it.keyguardFullAodHooks
            } ?: 0

    val keyguardAodReady: Boolean
        @Synchronized get() = current?.keyguardAodReady == true

    val keyguardFullAodReady: Boolean
        @Synchronized get() = current?.keyguardFullAodReady == true

    internal data class AttachResult(
        val tintHooks: Int,
        val sceneHooks: Int,
        val mobileTypeHooks: Int,
        val keyguardAodHooks: Int,
        val keyguardFullAodHooks: Int,
    ) {
        val tintReady: Boolean
            get() = tintHooks == SystemUiTintStateSource.HOOK_COUNT
        val sceneReady: Boolean
            get() = sceneHooks == SystemUiSceneStateSource.HOOK_COUNT
        val mobileTypeReady: Boolean
            get() = mobileTypeHooks == SystemUiMobileTypeStateSource.HOOK_COUNT
        val keyguardAodReady: Boolean
            get() = keyguardAodHooks == SystemUiKeyguardAodStateSource.HOOK_COUNT
        val keyguardFullAodReady: Boolean
            get() =
                keyguardFullAodHooks ==
                    SystemUiKeyguardFullAodTransitionSource.HOOK_COUNT
    }

    @Synchronized
    fun attach(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (SystemUiTintStateSource.TintUpdate) -> Unit,
        onSceneState: (SystemUiSceneStateSource.SceneUpdate) -> Unit,
        onKeyguardAodState: (SystemUiKeyguardAodStateSource.AodUpdate) -> Unit,
        onKeyguardFullAodTransition: () -> Unit,
        onMobileTypeChanged: (Drawable) -> Unit,
        onTintEvent: ((String) -> Unit)?,
        onSceneEvent: ((String) -> Unit)?,
        onKeyguardAodEvent: ((String) -> Unit)?,
    ): AttachResult {
        val tintHooks =
            SystemUiTintStateSource.install(
                module = module,
                classLoader = classLoader,
                onTintState = onTintState,
                onEvent = onTintEvent,
            ).size
        val keyguardAodHooks =
            runCatching {
                SystemUiKeyguardAodStateSource.install(
                    module = module,
                    classLoader = classLoader,
                    onAodState = onKeyguardAodState,
                    onEvent = onKeyguardAodEvent,
                ).size
            }.getOrElse { error ->
                onKeyguardAodEvent?.invoke(
                    "keyguardAod install=unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=native-keyguard",
                )
                0
            }
        val keyguardFullAodHooks =
            runCatching {
                SystemUiKeyguardFullAodTransitionSource.install(
                    module = module,
                    classLoader = classLoader,
                    onTransition = onKeyguardFullAodTransition,
                    onEvent = onKeyguardAodEvent,
                ).size
            }.getOrElse { error ->
                onKeyguardAodEvent?.invoke(
                    "keyguardFullAod install=unavailable reason=" +
                        (error.message ?: error.javaClass.simpleName) +
                        " fallback=status-icons-alpha",
                )
                0
            }
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
            keyguardAodHooks = keyguardAodHooks,
            keyguardFullAodHooks = keyguardFullAodHooks,
        ).also { current = it }
    }

    @Synchronized
    fun resetRuntimeState() {
        current = null
        SystemUiTintStateSource.resetRuntimeState()
        SystemUiSceneStateSource.resetRuntimeState()
        SystemUiKeyguardAodStateSource.resetRuntimeState()
        SystemUiKeyguardHostProbe.resetRuntimeState()
    }
}
