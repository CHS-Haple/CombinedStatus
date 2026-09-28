package com.chaners.combinedstatus.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettings
import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings
import java.lang.ref.WeakReference
import java.util.ArrayDeque

internal object CombinedStatusControlCenterRenderSession {
    private const val FAKE_ROOT_CLASS_NAME =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private val representedSlots =
        setOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): AttachResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return AttachResult.Failure("main-thread-required")
        }
        if (host.javaClass.name != FAKE_ROOT_CLASS_NAME) {
            return AttachResult.Failure("fake-root-type-mismatch")
        }
        val statusBarArea =
            host.uniqueDescendant(BATTERY_CONTAINER_CLASS_NAME)
                ?: return AttachResult.Failure("fake-status-bar-area-unresolved")
        val statusIcons =
            statusBarArea.directChild(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup
                ?: return AttachResult.Failure("status-icons-missing")
        val battery =
            statusBarArea.directChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
                ?: return AttachResult.Failure("battery-view-missing")
        val carrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return AttachResult.Failure("battery-core-carrier-missing")

        val existing = current
        if (
            existing?.matches(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
            ) == true
        ) {
            existing.refresh()
            return AttachResult.Ready
        }

        existing?.stop("host-replaced")
        current =
            Session(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
            ).also { it.start() }
        return AttachResult.Ready
    }

    @Synchronized
    fun setRequestedVisible(visible: Boolean): Boolean =
        current?.setRequestedVisible(visible) ?: false

    @Synchronized
    fun onState(snapshot: CombinedStatusStateStore.Snapshot) {
        current?.update(snapshot)
    }

    @Synchronized
    fun onPresentationStateChanged() {
        current?.refresh()
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun onFeatureSettingsChanged(settings: CombinedStatusFeatureSettings) {
        current?.setFeatureEnabled(settings.enabled)
    }

    @Synchronized
    fun onVisualSettingsChanged(settings: CombinedStatusVisualSettings) {
        current?.updateVisualSettings(settings)
    }

    @Synchronized
    fun detach(source: String = "detach") {
        current?.stop(source)
        current = null
    }

    internal fun resolveProjectionReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        maskReady: Boolean,
    ): Boolean =
        featureEnabled &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached &&
            maskReady

    private class Session(
        host: ViewGroup,
        statusBarArea: ViewGroup,
        statusIcons: ViewGroup,
        battery: ViewGroup,
        carrier: View,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        private val onProjectionReadinessChanged: (Boolean) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusBarArea = WeakReference(statusBarArea)
        private val statusIcons = WeakReference(statusIcons)
        private val battery = WeakReference(battery)
        private val carrier = WeakReference(carrier)
        private val renderView = CombinedStatusRenderView(host.context)
        private val renderController = CombinedStatusRenderController(renderView)
        private val anchorRect = Rect()
        private val clipStates = mutableListOf<ClipState>()

        private var requestedVisible = false
        private var featureEnabled = RuntimeFeaturePreferencesOwner.currentSettings().enabled
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var maskReady = false
        private var lastProjectionReady: Boolean? = null

        private val hostLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val statusAreaLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val carrierLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val statusIconsLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (requestedVisible && baseReady()) {
                    maskReady = refreshClipMasks()
                    applyVisibility()
                    dispatchReadiness("native-layout")
                }
            }

        fun matches(
            host: ViewGroup,
            statusBarArea: ViewGroup,
            statusIcons: ViewGroup,
            battery: ViewGroup,
            carrier: View,
        ): Boolean =
            this.host.get() === host &&
                this.statusBarArea.get() === statusBarArea &&
                this.statusIcons.get() === statusIcons &&
                this.battery.get() === battery &&
                this.carrier.get() === carrier

        fun start() {
            val hostView = host.get() ?: return
            hostView.addOnAttachStateChangeListener(this)
            hostView.addOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.addOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
            statusIcons.get()?.addOnLayoutChangeListener(statusIconsLayoutListener)
            renderView.visibility = View.GONE
            hostView.overlay.add(renderView)
            renderController.updateVisualSettings(RuntimeVisualPreferencesOwner.currentSettings())
            update(CombinedStatusStateStore.snapshot())
            refreshTint()
            layoutProjection()
            dispatchReadiness("start")
        }

        fun stop(source: String) {
            val hostView = host.get()
            restoreClipMasks()
            hostView?.removeOnAttachStateChangeListener(this)
            hostView?.removeOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.removeOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            statusIcons.get()?.removeOnLayoutChangeListener(statusIconsLayoutListener)
            hostView?.overlay?.remove(renderView)
            requestedVisible = false
            layoutReady = false
            maskReady = false
            if (lastProjectionReady == true) {
                lastProjectionReady = false
                onProjectionReadinessChanged(false)
            }
            emitEvent {
                "controlCenterProjection cleanup source=" + source +
                    " restoredClipBounds=true nativeGeometryWrites=0 " +
                    "nativeAlphaWrites=0 nativeVisibilityWrites=0"
            }
        }

        fun setRequestedVisible(visible: Boolean): Boolean {
            requestedVisible = visible
            syncPresentation("visibility")
            return projectionReady()
        }

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            modelReady = renderController.update(snapshot).model != null
            refreshTint()
            syncPresentation("state")
        }

        fun refresh() =
            update(CombinedStatusStateStore.snapshot())

        fun updateTint(update: SystemUiTintStateSource.TintUpdate) {
            val batteryView = battery.get() ?: return
            if (update.sourceView !== batteryView) return
            applyTint(update.state, "battery")
        }

        fun setFeatureEnabled(enabled: Boolean) {
            featureEnabled = enabled
            syncPresentation("feature")
        }

        fun updateVisualSettings(settings: CombinedStatusVisualSettings) {
            renderController.updateVisualSettings(settings)
        }

        private fun refreshTint() {
            val batteryView = battery.get() ?: return
            val state = SystemUiTintStateSource.currentState(batteryView) ?: return
            applyTint(state, "surface")
        }

        private fun applyTint(
            batteryState: CombinedStatusTintState,
            source: String,
        ) {
            val peerTint =
                statusIcons.get()?.let(
                    SystemUiNativeNetworkSuppressionOwner::currentAppliedStatusIconTintForGroup,
                )
            val resolved =
                CombinedStatusTintAuthority.resolveBatteryEvent(
                    batteryState,
                    peerTint,
                )
            tintReady = renderController.updateTint(resolved).resolved != null
            syncPresentation("tint:" + source)
        }

        private fun layoutProjection() {
            val hostView = host.get() ?: return markLayoutUnavailable()
            val statusArea = statusBarArea.get() ?: return markLayoutUnavailable()
            val carrierView = carrier.get() ?: return markLayoutUnavailable()
            val carrierWidth =
                SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(carrierView)
                    ?: return markLayoutUnavailable()
            val resolved =
                CombinedStatusHomeLayoutResolver.resolve(
                    hostWidthPx = statusArea.width,
                    hostHeightPx = statusArea.height,
                    baseCarrierWidthPx = carrierWidth,
                    isRtl = statusArea.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: return markLayoutUnavailable()
            if (!resolved.renderCombined) return markLayoutUnavailable()

            val hostLocation = IntArray(2)
            val areaLocation = IntArray(2)
            hostView.getLocationInWindow(hostLocation)
            statusArea.getLocationInWindow(areaLocation)
            val offsetX = areaLocation[0] - hostLocation[0]
            val offsetY = areaLocation[1] - hostLocation[1]
            val left = offsetX + resolved.slotLeftPx.toInt()
            val right = offsetX + resolved.slotRightPx.toInt()
            val top = offsetY
            val bottom = offsetY + statusArea.height
            anchorRect.set(left, top, right, bottom)
            if (anchorRect.width() <= 0 || anchorRect.height() <= 0) {
                return markLayoutUnavailable()
            }

            if (
                renderView.measuredWidth != anchorRect.width() ||
                renderView.measuredHeight != anchorRect.height()
            ) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.width(),
                        View.MeasureSpec.EXACTLY,
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.height(),
                        View.MeasureSpec.EXACTLY,
                    ),
                )
            }
            renderView.layout(
                anchorRect.left,
                anchorRect.top,
                anchorRect.right,
                anchorRect.bottom,
            )

            val firstReady = !layoutReady
            layoutReady = true
            syncPresentation("layout")
            if (firstReady) {
                emitEvent {
                    "controlCenterProjection attached carrier=ControlCenterFakeStatusIcons.overlay " +
                        "geometrySource=MiuiStatusBatteryContainer bounds=" +
                        anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " motion=root-alpha-translation-inherited " +
                        "nativeGeometryWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0"
                }
            }
        }

        private fun markLayoutUnavailable() {
            if (!layoutReady && !maskReady) return
            layoutReady = false
            restoreClipMasks()
            maskReady = false
            renderView.visibility = View.GONE
            dispatchReadiness("layout-unavailable")
        }

        private fun baseReady(): Boolean {
            val hostView = host.get()
            return featureEnabled &&
                modelReady &&
                tintReady &&
                layoutReady &&
                hostView?.isAttachedToWindow == true
        }

        private fun projectionReady(): Boolean =
            resolveProjectionReady(
                featureEnabled = featureEnabled,
                modelReady = modelReady,
                tintReady = tintReady,
                layoutReady = layoutReady,
                hostAttached = host.get()?.isAttachedToWindow == true,
                maskReady = maskReady,
            )

        private fun syncPresentation(source: String) {
            if (requestedVisible && baseReady()) {
                maskReady = refreshClipMasks()
            } else {
                restoreClipMasks()
                maskReady = false
            }
            applyVisibility()
            dispatchReadiness(source)
        }

        private fun applyVisibility() {
            val visible = requestedVisible && projectionReady()
            renderView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                renderView.invalidate()
            } else {
                renderView.clearPendingLatency()
            }
        }

        private fun refreshClipMasks(): Boolean {
            val group = statusIcons.get() ?: return failMasks("status-icons-released")
            val batteryView = battery.get() ?: return failMasks("battery-released")

            val targets = linkedSetOf<View>()
            targets += batteryView
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (NativeParticipantRuntimeAccess.slotOf(child) in representedSlots) {
                    targets += child
                }
            }

            val iterator = clipStates.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null && view.clipBounds == state.appliedClip) {
                        view.clipBounds = state.nativeClip?.let(::Rect)
                    }
                    iterator.remove()
                } else if (view.clipBounds != state.appliedClip) {
                    return failMasks("clip-writer-conflict")
                }
            }

            targets.forEach { view ->
                if (clipStates.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    clipStates +=
                        ClipState(
                            view = WeakReference(view),
                            nativeClip = nativeClip,
                            appliedClip = applied,
                        )
                }
            }

            val ready =
                clipStates.any { state -> state.view.get() === batteryView } &&
                    clipStates.all { state ->
                        state.view.get()?.clipBounds == state.appliedClip
                    }
            if (ready) {
                emitEvent {
                    "controlCenterProjection mask active maskedViews=" + clipStates.size +
                        " owner=fake-status-area-clipBounds " +
                        "nativeLayoutWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0"
                }
            }
            return ready
        }

        private fun failMasks(reason: String): Boolean {
            restoreClipMasks()
            emitEvent {
                "controlCenterProjection mask failNative reason=" + reason +
                    " restoredNative=true"
            }
            return false
        }

        private fun restoreClipMasks(): Int {
            val states = clipStates.toList()
            clipStates.clear()
            var restored = 0
            states.forEach { state ->
                val view = state.view.get() ?: return@forEach
                if (view.clipBounds == state.appliedClip) {
                    view.clipBounds = state.nativeClip?.let(::Rect)
                    restored += 1
                }
            }
            return restored
        }

        private fun dispatchReadiness(source: String) {
            val ready = requestedVisible && projectionReady()
            if (ready == lastProjectionReady) return
            lastProjectionReady = ready
            emitEvent {
                "controlCenterProjection readiness source=" + source +
                    " ready=" + ready +
                    " requestedVisible=" + requestedVisible +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " maskReady=" + maskReady +
                    " rootAlphaInherited=true nativeGeometryWrites=0"
            }
            onProjectionReadinessChanged(ready)
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProjection()
            refreshTint()
            syncPresentation("attach")
        }

        override fun onViewDetachedFromWindow(view: View) {
            layoutReady = false
            restoreClipMasks()
            maskReady = false
            renderView.visibility = View.GONE
            dispatchReadiness("detach")
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) onEvent(message())
        }
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun ViewGroup.uniqueDescendant(className: String): ViewGroup? {
        var found: ViewGroup? = null
        val queue = ArrayDeque<ViewGroup>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (child is ViewGroup) {
                    if (child.javaClass.name == className) {
                        if (found != null && found !== child) return null
                        found = child
                    }
                    queue.add(child)
                }
            }
        }
        return found
    }

    private data class ClipState(
        val view: WeakReference<View>,
        val nativeClip: Rect?,
        val appliedClip: Rect,
    )

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }
}
