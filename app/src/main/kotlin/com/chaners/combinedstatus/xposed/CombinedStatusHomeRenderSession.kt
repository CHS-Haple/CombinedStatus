package com.chaners.combinedstatus.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettings
import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings
import java.lang.ref.WeakReference

internal object CombinedStatusHomeRenderSession {
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: Any,
        onEvent: (String) -> Unit,
        onLatencySample: ((RuntimeRenderLatencySample) -> Unit)? = null,
        isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        initialNativeHandoffActive: Boolean = false,
    ): AttachResult {
        val hostView = host as? ViewGroup
            ?: return AttachResult.Failure("host-not-view-group")
        val batteryContainer = hostView.directChild(BATTERY_CONTAINER_CLASS_NAME)
            ?: return AttachResult.Failure("battery-container-missing")
        val batteryView = batteryContainer.directChild(BATTERY_VIEW_CLASS_NAME)
            ?: return AttachResult.Failure("battery-view-missing")

        val existing = current
        if (existing?.matches(hostView, batteryContainer, batteryView) == true) {
            existing.update(CombinedStatusStateStore.snapshot())
            return AttachResult.Ready
        }

        existing?.stop()
        val session = Session(
            host = hostView,
            batteryContainer = batteryContainer,
            batteryView = batteryView,
            onEvent = onEvent,
            onLatencySample = onLatencySample,
            isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
            initialNativeHandoffActive = initialNativeHandoffActive,
            initialFeatureEnabled =
                RuntimeFeaturePreferencesOwner.currentSettings().enabled,
        )
        current = session
        session.start()
        session.update(CombinedStatusStateStore.snapshot())
        return AttachResult.Ready
    }

    @Synchronized
    fun onState(
        snapshot: CombinedStatusStateStore.Snapshot,
        trace: RuntimeRenderTrace? = null,
    ) {
        current?.update(snapshot, trace)
    }

    @Synchronized
    fun onPresentationStateChanged(trace: RuntimeRenderTrace? = null) {
        current?.update(CombinedStatusStateStore.snapshot(), trace)
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
    fun onSceneUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
        current?.updateScene(update)
    }

    @Synchronized
    fun setNativeHandoffActive(active: Boolean) {
        current?.setNativeHandoffActive(active)
    }

    @Synchronized
    fun detach(preserveVisual: Boolean = false) {
        current?.stop(removeVisual = !preserveVisual)
        current = null
    }

    internal fun resolveOverlayVisible(
        featureEnabled: Boolean,
        sceneAllowsOverlay: Boolean,
        nativeHandoffActive: Boolean,
    ): Boolean =
        featureEnabled &&
            sceneAllowsOverlay &&
            !nativeHandoffActive

    private fun ViewGroup.directChild(className: String): ViewGroup? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child as? ViewGroup
            }
        }
        return null
    }

    private class Session(
        host: ViewGroup,
        batteryContainer: ViewGroup,
        batteryView: ViewGroup,
        private val onEvent: (String) -> Unit,
        private val onLatencySample: ((RuntimeRenderLatencySample) -> Unit)?,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        initialNativeHandoffActive: Boolean,
        initialFeatureEnabled: Boolean,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private val probeView =
            CombinedStatusRenderView(host.context) { latencyMs, committedOnMainThread, sample ->
                if (sample != null && onLatencySample != null) {
                    onLatencySample.invoke(sample)
                } else {
                    emitEvent {
                        "homeRenderLatency stateToDrawMs=" + latencyMs +
                            " commitMainThread=" + committedOnMainThread +
                            " scheduling=sameFramePreferred"
                    }
                }
            }
        private val renderController = CombinedStatusRenderController(probeView)
        private var readyLogged = false
        private var layoutLogged = false
        private var tintLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        private var sceneSurface = SystemUiSceneStateSource.Surface.UNKNOWN
        private var nativeHandoffActive = initialNativeHandoffActive
        private var featureEnabled = initialFeatureEnabled
        private val anchorRect = Rect()

        private val batteryLayoutListener =
            View.OnLayoutChangeListener {
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                ->
                layoutProbe()
            }

        fun matches(
            host: ViewGroup,
            batteryContainer: ViewGroup,
            batteryView: ViewGroup,
        ): Boolean =
            this.host.get() === host &&
                this.batteryContainer.get() === batteryContainer &&
                this.batteryView.get() === batteryView

        fun start() {
            val hostView = host.get() ?: return
            batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return

            hostView.addOnAttachStateChangeListener(this)
            battery.addOnLayoutChangeListener(batteryLayoutListener)
            probeView.visibility = View.GONE
            hostView.overlay.add(probeView)
            renderController.updateVisualSettings(
                RuntimeVisualPreferencesOwner.currentSettings(),
            )
            SystemUiSceneStateSource.currentState(battery)?.let {
                applySceneState(it, "seed")
            }
            SystemUiTintStateSource.currentState(battery)?.let {
                applyTintState(it, "seed")
            }
            layoutProbe()
        }

        fun stop(removeVisual: Boolean = true) {
            host.get()?.removeOnAttachStateChangeListener(this)
            batteryView.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            if (removeVisual) {
                host.get()?.overlay?.remove(probeView)
            }
        }

        fun updateScene(update: SystemUiSceneStateSource.SceneUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) {
                return
            }
            applySceneState(update, "updateState")
        }

        private fun applySceneState(
            update: SystemUiSceneStateSource.SceneUpdate,
            source: String,
        ) {
            if (sceneSurface == update.surface) {
                return
            }

            sceneSurface = update.surface
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    sceneAllowsOverlay =
                        SystemUiSceneStateSource.allowsHomeOverlay(update.surface),
                    nativeHandoffActive = nativeHandoffActive,
                )
            probeView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                probeView.invalidate()
            } else {
                probeView.clearPendingLatency()
            }

            emitEvent {
                "homeRenderScene source=" + source +
                    " raw=" + update.rawState +
                    " surface=" + update.surface.name +
                    " visible=" + visible +
                    " policy=failClosedOutsideUnlockedStatusBar " +
                    " nativeGeometryWrites=0"
            }
        }

        fun setFeatureEnabled(enabled: Boolean) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post {
                    setFeatureEnabled(enabled)
                }
                return
            }
            if (featureEnabled == enabled) {
                return
            }
            featureEnabled = enabled
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    sceneAllowsOverlay =
                        SystemUiSceneStateSource.allowsHomeOverlay(sceneSurface),
                    nativeHandoffActive = nativeHandoffActive,
                )
            probeView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                probeView.invalidate()
            } else {
                probeView.clearPendingLatency()
            }
            emitEvent {
                "homeRenderFeature enabled=" + featureEnabled +
                    " overlayVisible=" + visible +
                    " scene=" + sceneSurface.name +
                    " nativeHandoffActive=" + nativeHandoffActive +
                    " nativeGeometryWrites=0"
            }
        }

        fun setNativeHandoffActive(active: Boolean) {
            if (nativeHandoffActive == active) {
                return
            }
            nativeHandoffActive = active
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    sceneAllowsOverlay =
                        SystemUiSceneStateSource.allowsHomeOverlay(sceneSurface),
                    nativeHandoffActive = nativeHandoffActive,
                )
            probeView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                probeView.invalidate()
            } else {
                probeView.clearPendingLatency()
            }
            emitEvent {
                "homeRenderHandoff nativeActive=" + nativeHandoffActive +
                    " overlayVisible=" + visible +
                    " scene=" + sceneSurface.name +
                    " nativeGeometryWrites=0"
            }
        }

        fun updateVisualSettings(settings: CombinedStatusVisualSettings) {
            renderController.updateVisualSettings(settings)
        }

        fun updateTint(update: SystemUiTintStateSource.TintUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) {
                return
            }
            applyTintState(update.state, "darkReceiver")
        }

        private fun applyTintState(
            state: CombinedStatusTintState,
            source: String,
        ) {
            val update = renderController.updateTint(state)

            if (update.rejectedInvalidCandidate && !rejectedTintLogged) {
                rejectedTintLogged = true
                emitEvent {
                    "homeRenderTint deferred source=" + source +
                        " applied=#" +
                        state.appliedTint.toUInt().toString(16).padStart(8, '0') +
                        " reason=transparent retainStable=true"
                }
            }

            if (update.changed && !tintLogged) {
                val resolved = update.resolved ?: return
                tintLogged = true
                emitEvent {
                    "homeRenderTint source=" + source +
                        " applied=#" +
                        resolved.appliedTint.toUInt().toString(16).padStart(8, '0') +
                        " eventDriven=true stable=true"
                }
            }
        }

        fun update(
            snapshot: CombinedStatusStateStore.Snapshot,
            trace: RuntimeRenderTrace? = null,
        ) {
            val visibleTrace =
                trace?.takeIf {
                    layoutLogged &&
                        SystemUiSceneStateSource.allowsHomeOverlay(sceneSurface)
                }
            val update =
                renderController.update(
                    snapshot = snapshot,
                    trace = visibleTrace,
                )

            if (!update.candidateComplete) {
                if (update.retainedStable && !deferredStateLogged) {
                    deferredStateLogged = true
                    emitEvent {
                        "homeRenderState deferred incomplete=true " +
                            "retainStable=true"
                    }
                }
                return
            }

            val model = update.model
            if (model != null && !readyLogged) {
                readyLogged = true
                emitEvent {
                    "homeRenderProbe ready " +
                        "battery=" + model.batteryPercent +
                        " charging=" + model.charging +
                        " batteryMode=" + model.batteryVisualMode.name +
                        " center=" + model.centerIndicator.javaClass.simpleName +
                        " mobileLevel=" + (model.mobileLevel ?: -1) +
                        " effectiveDataSubId=" + model.effectiveDataSubscriptionId +
                        " defaultDataSubId=" + update.defaultDataSubscriptionId
                }
            }
        }

        override fun onViewAttachedToWindow(view: View) {
            batteryView.get()?.let { battery ->
                SystemUiSceneStateSource.currentState(battery)?.let {
                    applySceneState(it, "reattach")
                }
            }
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        private fun layoutProbe() {
            if (!resolveNativeAnchor(anchorRect)) {
                return
            }
            applyAnchorBounds(anchorRect)

            if (!layoutLogged) {
                layoutLogged = true
                emitEvent {
                    "homeRenderProbe attached " +
                        "slot=homeHostOverlay anchor=battery " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " size=" + anchorRect.width() + "x" + anchorRect.height() +
                        " opacity=" + RENDER_OPACITY +
                        " ancestorVisibilityIndependent=true " +
                        "originalsHidden=false nativeGeometryWrites=0"
                }
            }
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) {
                onEvent(message())
            }
        }

        private fun resolveNativeAnchor(out: Rect): Boolean {
            val hostView = host.get() ?: return false
            val battery = batteryView.get() ?: return false
            if (
                !hostView.isLaidOut ||
                !battery.isLaidOut ||
                battery.width <= 0 ||
                battery.height <= 0
            ) {
                return false
            }

            out.set(0, 0, battery.width, battery.height)
            hostView.offsetDescendantRectToMyCoords(
                battery,
                out,
            )
            return true
        }

        private fun applyAnchorBounds(bounds: Rect) {
            if (
                probeView.measuredWidth != bounds.width() ||
                probeView.measuredHeight != bounds.height()
            ) {
                val widthSpec = View.MeasureSpec.makeMeasureSpec(
                    bounds.width(),
                    View.MeasureSpec.EXACTLY,
                )
                val heightSpec = View.MeasureSpec.makeMeasureSpec(
                    bounds.height(),
                    View.MeasureSpec.EXACTLY,
                )
                probeView.measure(widthSpec, heightSpec)
            }
            probeView.layout(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
            )
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    private const val RENDER_OPACITY = 1f
}
