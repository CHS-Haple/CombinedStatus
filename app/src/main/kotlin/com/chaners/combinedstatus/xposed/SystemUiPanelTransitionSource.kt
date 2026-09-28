package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.floor

internal object SystemUiPanelTransitionSource {
    const val CONTROL_CENTER_RUNTIME_HOOK_COUNT = 1
    const val CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT = 1
    const val HOOK_COUNT =
        CONTROL_CENTER_RUNTIME_HOOK_COUNT +
            CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT

    private const val CONTROL_CENTER_CLASS =
        "com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate"
    private const val CONTROL_CENTER_EXPANSION_METHOD = "onExpansionChanged"
    private const val CONTROL_CENTER_VISIBLE_METHOD = "onVisibleChanged"
    private const val CONTROL_CENTER_HEADER_CALLBACK_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController\$controlCenterCallback\$1"
    private const val CONTROL_CENTER_HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.ControlCenterHeaderExpandController"
    private const val COMBINED_HEADER_CLASS =
        "com.android.systemui.controlcenter.shade.CombinedHeaderController"
    private const val CONTROL_CENTER_FAKE_STATUS_BAR_CLASS =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
    private const val CC_FAKE_STATUS_BAR_ICONS_CLASS =
        "com.android.systemui.controlcenter.header.CcFakeStatusBarIcons"
    private const val DAGGER_LAZY_CLASS = "dagger.Lazy"
    private const val STATUS_BAR_ANCHOR_CLASS =
        "com.android.systemui.controlcenter.shade.StatusBarAnchorBounds"

    private const val CONTROL_CENTER_EXPANSION_HOOK_ID =
        "combinedstatus.panel.control-center.expansion"
    private const val CONTROL_CENTER_VISIBLE_HOOK_ID =
        "combinedstatus.panel.control-center.visible"

    private var controlProbe = ProbeState()
    private var controlAnchorContract: ControlCenterAnchorContract? = null
    private var controlHeaderRef = WeakReference<Any>(null)

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onUpdate: ((Update) -> Unit)? = null,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
        includeControlCenterDiagnostics: Boolean = true,
    ): List<HookHandle> {
        val controlClass =
            Class.forName(CONTROL_CENTER_CLASS, false, classLoader)
        val controlVisibleMethod =
            controlClass
                .getDeclaredMethod(
                    CONTROL_CENTER_VISIBLE_METHOD,
                    Boolean::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
        val controlExpansionMethod =
            if (includeControlCenterDiagnostics) {
                controlClass
                    .getDeclaredMethod(
                        CONTROL_CENTER_EXPANSION_METHOD,
                        Float::class.javaPrimitiveType,
                    )
                    .apply { isAccessible = true }
            } else {
                null
            }

        controlAnchorContract =
            ControlCenterAnchorContract.resolve(
                classLoader = classLoader,
                delegateClass = controlClass,
            )

        val handles =
            ArrayList<HookHandle>(
                expectedHookCount(includeControlCenterDiagnostics),
            )
        try {
            handles +=
                module
                    .hook(controlVisibleMethod)
                    .setId(CONTROL_CENTER_VISIBLE_HOOK_ID)
                    .intercept(
                        Hooker { chain ->
                            val visible = chain.getArg(0) as? Boolean
                            val result = chain.proceed()
                            val sourceCarrier =
                                if (visible == true) {
                                    resolveControlCenterRealSystemIcons(chain.thisObject)
                                } else {
                                    null
                                }
                            val transitionCarrier =
                                if (visible == true) {
                                    resolveControlCenterFakeSystemIcons(chain.thisObject)
                                } else {
                                    null
                                }
                            val update =
                                Update(
                                    source = Source.CONTROL_CENTER,
                                    fraction = null,
                                    expanded = null,
                                    tracking = null,
                                    visible = visible,
                                    controlCenterCarrier = transitionCarrier,
                                    controlCenterSourceCarrier = sourceCarrier,
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

            if (includeControlCenterDiagnostics) {
                val expansionMethod = checkNotNull(controlExpansionMethod)

                handles +=
                    module
                        .hook(expansionMethod)
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
                                        homeMotion =
                                            if (anchorSnapshot != null) {
                                                SystemUiIslandMotionSource.currentOwnerSnapshot()
                                            } else {
                                                null
                                            },
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
            }

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
            controlProbe = ProbeState()
            controlAnchorContract = null
            controlHeaderRef = WeakReference(null)
        }
    }

    internal fun nativeFraction(value: Float?): Float? =
        value?.takeIf { it.isFinite() }

    internal fun expectedHookCount(includeControlCenterDiagnostics: Boolean): Int =
        CONTROL_CENTER_RUNTIME_HOOK_COUNT +
            if (includeControlCenterDiagnostics) CONTROL_CENTER_DIAGNOSTIC_HOOK_COUNT else 0

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

    private fun resolveControlCenterHeader(delegate: Any?): Any? {
        delegate ?: return null
        val contract = controlAnchorContract ?: return null
        return controlHeaderRef.get()
            ?: contract.resolveHeader(delegate)?.also { resolved ->
                controlHeaderRef = WeakReference(resolved)
            }
    }

    private fun resolveControlCenterRealSystemIcons(delegate: Any?): ViewGroup? {
        val contract = controlAnchorContract ?: return null
        val header = resolveControlCenterHeader(delegate) ?: return null
        return contract.realSystemIcons(header)
    }

    private fun resolveControlCenterFakeSystemIcons(delegate: Any?): ViewGroup? {
        val contract = controlAnchorContract ?: return null
        val header = resolveControlCenterHeader(delegate) ?: return null
        return contract.fakeSystemIcons(header)
    }

    private fun captureControlCenterAnchor(delegate: Any?): ControlCenterAnchorSnapshot? {
        val contract = controlAnchorContract ?: return null
        val header = resolveControlCenterHeader(delegate) ?: return null
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
        val probe = controlProbe
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
        val homeMotionSummary =
            update.homeMotion?.let { snapshot ->
                " homeMotion=" + snapshot.summary
            }.orEmpty()
        onEvent(
            "panelTransition source=" + update.source.logName +
                " fraction=" + (update.fraction ?: "none") +
                " bucket=" + (bucket ?: probe.bucket) + "/" + DIAGNOSTIC_BUCKETS +
                " expanded=" + (update.expanded ?: probe.expanded ?: "none") +
                " tracking=" + (update.tracking ?: probe.tracking ?: "none") +
                " visible=" + (update.visible ?: probe.visible ?: "none") +
                anchorSummary +
                homeMotionSummary +
                " authority=hyperos-native-callback" +
                " nativeGeometryWrites=0",
        )
    }

    internal data class Update(
        val source: Source,
        val fraction: Float?,
        val expanded: Boolean?,
        val tracking: Boolean?,
        val visible: Boolean?,
        val controlCenterCarrier: ViewGroup? = null,
        val controlCenterSourceCarrier: ViewGroup? = null,
        val controlCenterAnchor: ControlCenterAnchorSnapshot? = null,
        val homeMotion: SystemUiIslandMotionSource.OwnerSnapshot? = null,
    )

    internal enum class Source(
        val logName: String,
    ) {
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
        private val headerControllerField: Field,
        private val lazyGetMethod: Method,
        private val controlCenterFakeStatusBarField: Field,
        private val fakeDelegateField: Field,
        private val fakeStatusBarAreaField: Field,
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

        fun realSystemIcons(header: Any): ViewGroup? =
            runCatching { realSystemIconsField.get(header) as? ViewGroup }
                .getOrNull()

        fun fakeSystemIcons(header: Any): ViewGroup? {
            val lazy =
                runCatching { headerControllerField.get(header) }
                    .getOrNull()
                    ?: return null
            val combinedHeader =
                runCatching { lazyGetMethod.invoke(lazy) }
                    .getOrNull()
                    ?: return null
            val fakeStatusBar =
                runCatching { controlCenterFakeStatusBarField.get(combinedHeader) }
                    .getOrNull()
                    ?: return null
            val delegate =
                runCatching { fakeDelegateField.get(fakeStatusBar) }
                    .getOrNull()
                    ?: return null
            return runCatching { fakeStatusBarAreaField.get(delegate) as? ViewGroup }
                .getOrNull()
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
                    val combinedHeaderClass =
                        Class.forName(
                            COMBINED_HEADER_CLASS,
                            false,
                            classLoader,
                        )
                    val fakeStatusBarClass =
                        Class.forName(
                            CONTROL_CENTER_FAKE_STATUS_BAR_CLASS,
                            false,
                            classLoader,
                        )
                    val fakeStatusBarIconsClass =
                        Class.forName(
                            CC_FAKE_STATUS_BAR_ICONS_CLASS,
                            false,
                            classLoader,
                        )
                    val lazyClass =
                        Class.forName(
                            DAGGER_LAZY_CLASS,
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
                        headerControllerField =
                            headerClass.getDeclaredField("headerController").accessible(),
                        lazyGetMethod =
                            lazyClass.getDeclaredMethod("get").apply {
                                isAccessible = true
                            },
                        controlCenterFakeStatusBarField =
                            combinedHeaderClass
                                .getDeclaredField("controlCenterFakeStatusBar")
                                .accessible(),
                        fakeDelegateField =
                            fakeStatusBarClass.getDeclaredField("delegate").accessible(),
                        fakeStatusBarAreaField =
                            fakeStatusBarIconsClass.getDeclaredField("statusBarArea").accessible(),
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
