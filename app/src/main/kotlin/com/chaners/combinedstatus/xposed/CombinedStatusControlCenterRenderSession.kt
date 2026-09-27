package com.chaners.combinedstatus.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettings
import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings
import java.lang.ref.WeakReference

internal object CombinedStatusControlCenterRenderSession {
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): AttachResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) return AttachResult.Failure("main-thread-required")
        if (host.javaClass.name != BATTERY_CONTAINER_CLASS_NAME) return AttachResult.Failure("real-system-icons-type-mismatch")
        if (!SystemUiHomePresentationOwner.ownsBatteryContainer(host)) return AttachResult.Failure("real-system-icons-not-home-owned-container")
        val statusIcons = host.directChild(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup
            ?: return AttachResult.Failure("status-icons-missing")
        val battery = host.directChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
            ?: return AttachResult.Failure("battery-view-missing")
        val carrier = SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
            ?: return AttachResult.Failure("battery-core-carrier-missing")

        val existing = current
        if (existing?.matches(host, statusIcons, battery, carrier) == true) {
            existing.refresh()
            return AttachResult.Ready
        }
        existing?.stop("host-replaced")
        current = Session(
            host = host,
            statusIcons = statusIcons,
            battery = battery,
            carrier = carrier,
            onEvent = onEvent,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            onProjectionReadinessChanged = onProjectionReadinessChanged,
        ).also { it.start() }
        return AttachResult.Ready
    }

    @Synchronized fun setRequestedVisible(visible: Boolean): Boolean =
        current?.setRequestedVisible(visible) ?: false
    @Synchronized fun onState(snapshot: CombinedStatusStateStore.Snapshot) { current?.update(snapshot) }
    @Synchronized fun onPresentationStateChanged() { current?.refresh() }
    @Synchronized fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) { current?.updateTint(update) }
    @Synchronized fun onFeatureSettingsChanged(settings: CombinedStatusFeatureSettings) { current?.setFeatureEnabled(settings.enabled) }
    @Synchronized fun onVisualSettingsChanged(settings: CombinedStatusVisualSettings) { current?.updateVisualSettings(settings) }
    @Synchronized fun detach(source: String = "detach") { current?.stop(source); current = null }

    internal fun resolveProjectionReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        homePresentationOwnsCarrier: Boolean,
    ): Boolean =
        featureEnabled && modelReady && tintReady && layoutReady && hostAttached && homePresentationOwnsCarrier

    private class Session(
        host: ViewGroup,
        statusIcons: ViewGroup,
        battery: ViewGroup,
        carrier: View,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        private val onProjectionReadinessChanged: (Boolean) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusIcons = WeakReference(statusIcons)
        private val battery = WeakReference(battery)
        private val carrier = WeakReference(carrier)
        private val renderView = CombinedStatusRenderView(host.context)
        private val renderController = CombinedStatusRenderController(renderView)
        private val anchorRect = Rect()
        private var requestedVisible = false
        private var featureEnabled = RuntimeFeaturePreferencesOwner.currentSettings().enabled
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var lastProjectionReady: Boolean? = null

        private val hostLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layoutProjection() }
        private val carrierLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layoutProjection() }

        fun matches(host: ViewGroup, statusIcons: ViewGroup, battery: ViewGroup, carrier: View): Boolean =
            this.host.get() === host && this.statusIcons.get() === statusIcons &&
                this.battery.get() === battery && this.carrier.get() === carrier

        fun start() {
            val hostView = host.get() ?: return
            hostView.addOnAttachStateChangeListener(this)
            hostView.addOnLayoutChangeListener(hostLayoutListener)
            carrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
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
            hostView?.removeOnAttachStateChangeListener(this)
            hostView?.removeOnLayoutChangeListener(hostLayoutListener)
            carrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            hostView?.overlay?.remove(renderView)
            requestedVisible = false
            layoutReady = false
            if (lastProjectionReady == true) {
                lastProjectionReady = false
                onProjectionReadinessChanged(false)
            }
            emitEvent { "controlCenterProjection cleanup source=" + source + " nativeGeometryWrites=0 nativeVisibilityWrites=0" }
        }

        fun setRequestedVisible(visible: Boolean): Boolean {
            requestedVisible = visible
            val ready = projectionReady()
            applyVisibility(ready)
            dispatchReadiness("visibility")
            return ready
        }

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            modelReady = renderController.update(snapshot).model != null
            refreshTint()
            val ready = projectionReady()
            applyVisibility(ready)
            dispatchReadiness("state")
        }

        fun refresh() = update(CombinedStatusStateStore.snapshot())

        fun updateTint(update: SystemUiTintStateSource.TintUpdate) {
            val batteryView = battery.get() ?: return
            if (update.sourceView !== batteryView) return
            applyTint(update.state, "battery")
        }

        fun setFeatureEnabled(enabled: Boolean) {
            featureEnabled = enabled
            val ready = projectionReady()
            applyVisibility(ready)
            dispatchReadiness("feature")
        }

        fun updateVisualSettings(settings: CombinedStatusVisualSettings) {
            renderController.updateVisualSettings(settings)
        }

        private fun refreshTint() {
            val batteryView = battery.get() ?: return
            val state = SystemUiTintStateSource.currentState(batteryView) ?: return
            applyTint(state, "surface")
        }

        private fun applyTint(batteryState: CombinedStatusTintState, source: String) {
            val peerTint = statusIcons.get()?.let(SystemUiNativeNetworkSuppressionOwner::currentAppliedStatusIconTintForGroup)
            val resolved = CombinedStatusTintAuthority.resolveBatteryEvent(batteryState, peerTint)
            tintReady = renderController.updateTint(resolved).resolved != null
            val ready = projectionReady()
            applyVisibility(ready)
            dispatchReadiness("tint:" + source)
        }

        private fun layoutProjection() {
            val hostView = host.get() ?: return
            val carrierView = carrier.get() ?: return
            val carrierWidth = SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(carrierView) ?: return markLayoutUnavailable()
            val resolved = CombinedStatusHomeLayoutResolver.resolve(
                hostWidthPx = hostView.width,
                hostHeightPx = hostView.height,
                baseCarrierWidthPx = carrierWidth,
                isRtl = hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL,
            ) ?: return markLayoutUnavailable()
            if (!resolved.renderCombined) return markLayoutUnavailable()
            val left = if (hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL) 0 else resolved.slotLeftPx.toInt()
            val right = if (hostView.layoutDirection == View.LAYOUT_DIRECTION_RTL) resolved.slotRightPx.toInt() else resolved.slotRightPx.toInt()
            anchorRect.set(left, 0, right, hostView.height)
            if (anchorRect.width() <= 0 || anchorRect.height() <= 0) return markLayoutUnavailable()
            if (renderView.measuredWidth != anchorRect.width() || renderView.measuredHeight != anchorRect.height()) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(anchorRect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(anchorRect.height(), View.MeasureSpec.EXACTLY),
                )
            }
            renderView.layout(anchorRect.left, anchorRect.top, anchorRect.right, anchorRect.bottom)
            val firstReady = !layoutReady
            layoutReady = true
            val ready = projectionReady()
            applyVisibility(ready)
            if (firstReady) {
                emitEvent {
                    "controlCenterProjection attached carrier=realSystemIcons.overlay bounds=" +
                        anchorRect.left + "," + anchorRect.top + "-" + anchorRect.right + "," + anchorRect.bottom +
                        " owner=ControlCenterHeaderExpandController.realSystemIcons motion=system-ui-inherited nativeGeometryWrites=0"
                }
            }
            dispatchReadiness("layout")
        }

        private fun markLayoutUnavailable() {
            if (!layoutReady) return
            layoutReady = false
            renderView.visibility = View.GONE
            dispatchReadiness("layout-unavailable")
        }

        private fun projectionReady(): Boolean {
            val hostView = host.get()
            return resolveProjectionReady(
                featureEnabled = featureEnabled,
                modelReady = modelReady,
                tintReady = tintReady,
                layoutReady = layoutReady,
                hostAttached = hostView?.isAttachedToWindow == true,
                homePresentationOwnsCarrier = hostView?.let(SystemUiHomePresentationOwner::ownsBatteryContainer) == true,
            )
        }

        private fun applyVisibility(ready: Boolean) {
            renderView.visibility = if (requestedVisible && ready) View.VISIBLE else View.GONE
            if (renderView.visibility == View.VISIBLE) renderView.invalidate() else renderView.clearPendingLatency()
        }

        private fun dispatchReadiness(source: String) {
            val ready = requestedVisible && projectionReady()
            if (ready == lastProjectionReady) return
            lastProjectionReady = ready
            applyVisibility(ready)
            emitEvent {
                "controlCenterProjection readiness source=" + source + " ready=" + ready +
                    " requestedVisible=" + requestedVisible + " modelReady=" + modelReady +
                    " tintReady=" + tintReady + " layoutReady=" + layoutReady + " nativeGeometryWrites=0"
            }
            onProjectionReadinessChanged(ready)
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProjection()
            refreshTint()
            dispatchReadiness("attach")
        }

        override fun onViewDetachedFromWindow(view: View) {
            layoutReady = false
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

    internal sealed interface AttachResult {
        data object Ready : AttachResult
        data class Failure(val reason: String) : AttachResult
    }
}
