package com.chaners.combinedstatus.xposed

import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import kotlin.math.floor

internal object SystemUiPanelTransitionSource {
    const val HOOK_COUNT = 3

    private const val SHADE_MANAGER_CLASS =
        "com.android.systemui.shade.ShadeExpansionStateManager"
    private const val SHADE_METHOD = "onPanelExpansionChanged"
    private const val CONTROL_CENTER_CLASS =
        "com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate"
    private const val CONTROL_CENTER_EXPANSION_METHOD = "onExpansionChanged"
    private const val CONTROL_CENTER_VISIBLE_METHOD = "onVisibleChanged"

    private const val SHADE_HOOK_ID = "combinedstatus.panel.notification.expansion"
    private const val CONTROL_CENTER_EXPANSION_HOOK_ID =
        "combinedstatus.panel.control-center.expansion"
    private const val CONTROL_CENTER_VISIBLE_HOOK_ID =
        "combinedstatus.panel.control-center.visible"

    private var shadeProbe = ProbeState()
    private var controlProbe = ProbeState()

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onUpdate: ((Update) -> Unit)? = null,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
    ): List<HookHandle> {
        val shadeClass = Class.forName(SHADE_MANAGER_CLASS, false, classLoader)
        val shadeMethod =
            shadeClass.getDeclaredMethod(
                SHADE_METHOD,
                Float::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val controlClass = Class.forName(CONTROL_CENTER_CLASS, false, classLoader)
        val controlExpansionMethod =
            controlClass.getDeclaredMethod(
                CONTROL_CENTER_EXPANSION_METHOD,
                Float::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val controlVisibleMethod =
            controlClass.getDeclaredMethod(
                CONTROL_CENTER_VISIBLE_METHOD,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        val shadeHandle =
            module
                .hook(shadeMethod)
                .setId(SHADE_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val fraction =
                            normalizeFraction(
                                (chain.getArg(0) as? Number)?.toFloat(),
                            )
                        val expanded = chain.getArg(1) as? Boolean
                        val tracking = chain.getArg(2) as? Boolean
                        val result = chain.proceed()
                        val update =
                            Update(
                                source = Source.NOTIFICATION_SHADE,
                                fraction = fraction,
                                expanded = expanded,
                                tracking = tracking,
                                visible = null,
                            )
                        onUpdate?.invoke(update)
                        emitDiagnostic(
                            update = update,
                            onEvent = onEvent,
                            isProbeEnabled = isProbeEnabled,
                        )
                        result
                    },
                )

        val controlExpansionHandle =
            module
                .hook(controlExpansionMethod)
                .setId(CONTROL_CENTER_EXPANSION_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val fraction =
                            normalizeFraction(
                                (chain.getArg(0) as? Number)?.toFloat(),
                            )
                        val result = chain.proceed()
                        val update =
                            Update(
                                source = Source.CONTROL_CENTER,
                                fraction = fraction,
                                expanded = null,
                                tracking = null,
                                visible = null,
                            )
                        onUpdate?.invoke(update)
                        emitDiagnostic(
                            update = update,
                            onEvent = onEvent,
                            isProbeEnabled = isProbeEnabled,
                        )
                        result
                    },
                )

        val controlVisibleHandle =
            module
                .hook(controlVisibleMethod)
                .setId(CONTROL_CENTER_VISIBLE_HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val visible = chain.getArg(0) as? Boolean
                        val result = chain.proceed()
                        val update =
                            Update(
                                source = Source.CONTROL_CENTER,
                                fraction = null,
                                expanded = null,
                                tracking = null,
                                visible = visible,
                            )
                        onUpdate?.invoke(update)
                        emitDiagnostic(
                            update = update,
                            onEvent = onEvent,
                            isProbeEnabled = isProbeEnabled,
                        )
                        result
                    },
                )

        return listOf(
            shadeHandle,
            controlExpansionHandle,
            controlVisibleHandle,
        )
    }

    fun resetRuntimeState() {
        synchronized(this) {
            shadeProbe = ProbeState()
            controlProbe = ProbeState()
        }
    }

    internal fun normalizeFraction(value: Float?): Float? =
        value
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f)

    internal fun diagnosticBucket(fraction: Float?): Int? =
        fraction?.let { value ->
            floor(value * DIAGNOSTIC_BUCKETS)
                .toInt()
                .coerceIn(0, DIAGNOSTIC_BUCKETS)
        }

    @Synchronized
    private fun emitDiagnostic(
        update: Update,
        onEvent: ((String) -> Unit)?,
        isProbeEnabled: () -> Boolean,
    ) {
        if (onEvent == null || !isProbeEnabled()) {
            return
        }
        val probe =
            when (update.source) {
                Source.NOTIFICATION_SHADE -> shadeProbe
                Source.CONTROL_CENTER -> controlProbe
            }
        val bucket = diagnosticBucket(update.fraction)
        val changed =
            (bucket != null && bucket != probe.bucket) ||
                (update.expanded != null && update.expanded != probe.expanded) ||
                (update.tracking != null && update.tracking != probe.tracking) ||
                (update.visible != null && update.visible != probe.visible)
        if (!changed) {
            return
        }
        if (bucket != null) {
            probe.bucket = bucket
        }
        if (update.expanded != null) {
            probe.expanded = update.expanded
        }
        if (update.tracking != null) {
            probe.tracking = update.tracking
        }
        if (update.visible != null) {
            probe.visible = update.visible
        }
        onEvent(
            "panelTransition source=" + update.source.logName +
                " fraction=" + (update.fraction ?: "none") +
                " bucket=" + (bucket ?: probe.bucket) + "/" + DIAGNOSTIC_BUCKETS +
                " expanded=" + (update.expanded ?: probe.expanded ?: "none") +
                " tracking=" + (update.tracking ?: probe.tracking ?: "none") +
                " visible=" + (update.visible ?: probe.visible ?: "none") +
                " authority=hyperos-native-callback nativeGeometryWrites=0",
        )
    }

    internal data class Update(
        val source: Source,
        val fraction: Float?,
        val expanded: Boolean?,
        val tracking: Boolean?,
        val visible: Boolean?,
    )

    internal enum class Source(
        val logName: String,
    ) {
        NOTIFICATION_SHADE("notification"),
        CONTROL_CENTER("control-center"),
    }

    private data class ProbeState(
        var bucket: Int = -1,
        var expanded: Boolean? = null,
        var tracking: Boolean? = null,
        var visible: Boolean? = null,
    )

    private const val DIAGNOSTIC_BUCKETS = 8
}
