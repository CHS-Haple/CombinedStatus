package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
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
    private const val CONTROL_CENTER_HEADER_CALLBACK_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback\$1"
    private const val CONTROL_CENTER_HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController"
    private const val STATUS_BAR_ANCHOR_CLASS =
        "com.android.systemui.controlcenter.shade.StatusBarAnchorBounds"

    private const val SHADE_HOOK_ID = "combinedstatus.panel.notification.expansion"
    private const val CONTROL_CENTER_EXPANSION_HOOK_ID =
        "combinedstatus.panel.control-center.expansion"
    private const val CONTROL_CENTER_VISIBLE_HOOK_ID =
        "combinedstatus.panel.control-center.visible"

    private var shadeProbe = ProbeState()
    private var controlProbe = ProbeState()
    private var controlAnchorContract: ControlCenterAnchorContract? = null
    private var controlHeaderRef = WeakReference<Any>(null)

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

        controlAnchorContract =
            ControlCenterAnchorContract.resolve(
                classLoader = classLoader,
                delegateClass = controlClass,
            )

        val handles = ArrayList<HookHandle>(HOOK_COUNT)
        try {
            handles +=
                module
                    .hook(shadeMethod)
                    .setId(SHADE_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val fraction =
                                nativeFraction(
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

            handles +=
                module
                    .hook(controlExpansionMethod)
                    .setId(CONTROL_CENTER_EXPANSION_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val fraction =
                                nativeFraction(
                                    (chain.getArg(0) as? Number)?.toFloat(),
                                )
                            val result = chain.proceed()
                            val anchorSnapshot =
                                if (
                                    onEvent != null &&
                                    isProbeEnabled() &&
                                    shouldCaptureControlAnchor(fraction)
                                ) {
                                    captureControlCenterAnchor(chain.thisObject)
                                } else {
                                    null
                                }
                            val update =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = fraction,
                                    expanded = null,
                                    tracking = null,
                                    visible = null,
                                    controlCenterAnchor = anchorSnapshot,
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

            handles +=
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
            return handles
        } catch (error: Throwable) {
            handles.asReversed().forEach { handle ->
                runCatching { handle.unhook() }
            }
            throw error
        }
    }

    fun resetRuntimeState() {
        synchronized(this) {
            shadeProbe = ProbeState()
            controlProbe = ProbeState()
            controlAnchorContract = null
            controlHeaderRef = WeakReference(null)
        }
    }

    internal fun nativeFraction(value: Float?): Float? =
        value?.takeIf { it.isFinite() }

    internal fun diagnosticBucket(fraction: Float?): Int? =
        fraction?.let { rawValue ->
            val value = rawValue.coerceIn(0f, 1f)
            floor(value * DIAGNOSTIC_BUCKETS)
                .toInt()
                .coerceIn(0, DIAGNOSTIC_BUCKETS)
        }

    internal fun isBoundaryDiagnosticBucket(bucket: Int?): Boolean =
        bucket == 0 || bucket == 1 || bucket == 7 || bucket == 8

    @Synchronized
    private fun shouldCaptureControlAnchor(fraction: Float?): Boolean {
        val bucket = diagnosticBucket(fraction)
        return isBoundaryDiagnosticBucket(bucket) &&
            bucket != controlProbe.bucket
    }

    private fun captureControlCenterAnchor(delegate: Any?): ControlCenterAnchorSnapshot? {
        delegate ?: return null
        val contract = controlAnchorContract ?: return null
        val header =
            controlHeaderRef.get()
                ?: contract.resolveHeader(delegate)?.also { resolved ->
                    controlHeaderRef = WeakReference(resolved)
                }
                ?: return null
        return contract.snapshot(header)
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
        val anchorSummary =
            update.controlCenterAnchor?.let { snapshot ->
                " controlAnchor=" + snapshot.summary
            }.orEmpty()
        onEvent(
            "panelTransition source=" + update.source.logName +
                " fraction=" + (update.fraction ?: "none") +
                " bucket=" + (bucket ?: probe.bucket) + "/" + DIAGNOSTIC_BUCKETS +
                " expanded=" + (update.expanded ?: probe.expanded ?: "none") +
                " tracking=" + (update.tracking ?: probe.tracking ?: "none") +
                " visible=" + (update.visible ?: probe.visible ?: "none") +
                anchorSummary +
                " authority=hyperos-native-callback nativeGeometryWrites=0",
        )
    }

    internal data class Update(
        val source: Source,
        val fraction: Float?,
        val expanded: Boolean?,
        val tracking: Boolean?,
        val visible: Boolean?,
        val controlCenterAnchor: ControlCenterAnchorSnapshot? = null,
    )

    internal enum class Source(
        val logName: String,
    ) {
        NOTIFICATION_SHADE("notification"),
        CONTROL_CENTER("control-center"),
    }

    internal data class ControlCenterAnchorSnapshot(
        val systemIconsX: Int?,
        val systemIconsWidth: Int?,
        val statusIconsX: Int?,
        val statusIconsWidth: Int?,
        val batteryWidth: Int?,
        val realSystemIconsWidth: Int?,
        val normalStatusBarTranslationX: Int?,
        val normalStatusIconsTranslationX: Int?,
        val batteryWidthDiff: Int?,
        val addBatteryIsland: Boolean?,
        val controlCenterExpanding: Boolean?,
    ) {
        val summary: String
            get() =
                "{" +
                    "systemIconsX=" + (systemIconsX ?: "unknown") +
                    ",systemIconsWidth=" + (systemIconsWidth ?: "unknown") +
                    ",statusIconsX=" + (statusIconsX ?: "unknown") +
                    ",statusIconsWidth=" + (statusIconsWidth ?: "unknown") +
                    ",batteryWidth=" + (batteryWidth ?: "unknown") +
                    ",realSystemIconsWidth=" + (realSystemIconsWidth ?: "unknown") +
                    ",normalStatusBarTx=" + (normalStatusBarTranslationX ?: "unknown") +
                    ",normalStatusIconsTx=" + (normalStatusIconsTranslationX ?: "unknown") +
                    ",batteryWidthDiff=" + (batteryWidthDiff ?: "unknown") +
                    ",addBatteryIsland=" + (addBatteryIsland ?: "unknown") +
                    ",expanding=" + (controlCenterExpanding ?: "unknown") +
                    "}"
    }

    private class ControlCenterAnchorContract(
        private val callbackClass: Class<*>,
        private val callbacksField: Field,
        private val callbackOuterField: Field,
        private val statusBarAnchorField: Field,
        private val normalStatusBarTranslationXField: Field,
        private val normalStatusIconsTranslationXField: Field,
        private val batteryWidthDiffField: Field,
        private val addBatteryIslandField: Field,
        private val controlCenterExpandingField: Field,
        private val realSystemIconsField: Field,
        private val systemIconsLocationField: Field,
        private val systemIconsWidthField: Field,
        private val statusIconsLocationField: Field,
        private val statusIconsWidthField: Field,
        private val batteryWidthField: Field,
    ) {
        fun resolveHeader(delegate: Any): Any? {
            val callbacks =
                runCatching { callbacksField.get(delegate) as? Iterable<*> }
                    .getOrNull()
                    ?: return null
            val callback =
                callbacks.firstOrNull { candidate ->
                    candidate != null && callbackClass.isInstance(candidate)
                } ?: return null
            return runCatching { callbackOuterField.get(callback) }.getOrNull()
        }

        fun snapshot(header: Any): ControlCenterAnchorSnapshot? {
            val anchor =
                runCatching { statusBarAnchorField.get(header) }
                    .getOrNull()
                    ?: return null
            val realSystemIcons =
                runCatching { realSystemIconsField.get(header) as? View }
                    .getOrNull()
            val systemLocation =
                runCatching { systemIconsLocationField.get(anchor) as? IntArray }
                    .getOrNull()
            val statusLocation =
                runCatching { statusIconsLocationField.get(anchor) as? IntArray }
                    .getOrNull()
            return ControlCenterAnchorSnapshot(
                systemIconsX = systemLocation?.getOrNull(0),
                systemIconsWidth = readInt(systemIconsWidthField, anchor),
                statusIconsX = statusLocation?.getOrNull(0),
                statusIconsWidth = readInt(statusIconsWidthField, anchor),
                batteryWidth = readInt(batteryWidthField, anchor),
                realSystemIconsWidth = realSystemIcons?.width,
                normalStatusBarTranslationX =
                    readInt(normalStatusBarTranslationXField, header),
                normalStatusIconsTranslationX =
                    readInt(normalStatusIconsTranslationXField, header),
                batteryWidthDiff = readInt(batteryWidthDiffField, header),
                addBatteryIsland = readBoolean(addBatteryIslandField, header),
                controlCenterExpanding =
                    readBoolean(controlCenterExpandingField, header),
            )
        }

        private fun readInt(field: Field, target: Any): Int? =
            runCatching { field.getInt(target) }.getOrNull()

        private fun readBoolean(field: Field, target: Any): Boolean? =
            runCatching { field.getBoolean(target) }.getOrNull()

        companion object {
            fun resolve(
                classLoader: ClassLoader,
                delegateClass: Class<*>,
            ): ControlCenterAnchorContract? =
                runCatching {
                    val callbackClass =
                        Class.forName(
                            CONTROL_CENTER_HEADER_CALLBACK_CLASS,
                            false,
                            classLoader,
                        )
                    val headerClass =
                        Class.forName(
                            CONTROL_CENTER_HEADER_CLASS,
                            false,
                            classLoader,
                        )
                    val anchorClass =
                        Class.forName(
                            STATUS_BAR_ANCHOR_CLASS,
                            false,
                            classLoader,
                        )
                    ControlCenterAnchorContract(
                        callbackClass = callbackClass,
                        callbacksField =
                            delegateClass.getDeclaredField("callbacks").accessible(),
                        callbackOuterField =
                            callbackClass.getDeclaredField("this\$0").accessible(),
                        statusBarAnchorField =
                            headerClass.getDeclaredField("statusBarAnchor").accessible(),
                        normalStatusBarTranslationXField =
                            headerClass
                                .getDeclaredField("normalControlStatusBarTranslationX")
                                .accessible(),
                        normalStatusIconsTranslationXField =
                            headerClass
                                .getDeclaredField("normalControlStatusIconsTranslationX")
                                .accessible(),
                        batteryWidthDiffField =
                            headerClass.getDeclaredField("batteryWidthDiff").accessible(),
                        addBatteryIslandField =
                            headerClass.getDeclaredField("isAddBatteryIsland").accessible(),
                        controlCenterExpandingField =
                            headerClass
                                .getDeclaredField("isControlCenterExpanding")
                                .accessible(),
                        realSystemIconsField =
                            headerClass.getDeclaredField("realSystemIcons").accessible(),
                        systemIconsLocationField =
                            anchorClass
                                .getDeclaredField("systemIconsLocationOnScreen")
                                .accessible(),
                        systemIconsWidthField =
                            anchorClass.getDeclaredField("systemIconsWidth").accessible(),
                        statusIconsLocationField =
                            anchorClass
                                .getDeclaredField("statusIconsLocationInWindow")
                                .accessible(),
                        statusIconsWidthField =
                            anchorClass.getDeclaredField("statusIconsWidth").accessible(),
                        batteryWidthField =
                            anchorClass.getDeclaredField("batteryWidth").accessible(),
                    )
                }.getOrNull()
        }
    }

    private fun Field.accessible(): Field =
        apply { isAccessible = true }

    private data class ProbeState(
        var bucket: Int = -1,
        var expanded: Boolean? = null,
        var tracking: Boolean? = null,
        var visible: Boolean? = null,
    )

    private const val DIAGNOSTIC_BUCKETS = 8
}
