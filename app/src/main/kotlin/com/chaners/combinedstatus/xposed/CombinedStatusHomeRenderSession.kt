package com.chaners.combinedstatus.xposed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
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
    fun onSceneUpdate(update: SystemUiSceneStateSource.SceneUpdate) {
        current?.updateScene(update)
    }

    @Synchronized
    fun detach() {
        current?.stop()
        current = null
    }

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
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private val probeView =
            ProbeView(host.context) { latencyMs, committedOnMainThread, sample ->
                if (sample != null && onLatencySample != null) {
                    onLatencySample.invoke(sample)
                } else {
                    onEvent(
                        "homeRenderLatency stateToDrawMs=" + latencyMs +
                            " commitMainThread=" + committedOnMainThread +
                            " scheduling=sameFramePreferred",
                    )
                }
            }
        private var readyLogged = false
        private var layoutLogged = false
        private var tintLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        private var stableModel: CombinedStatusRenderModel? = null
        private var stableTint: CombinedStatusTintState? = null
        private var sceneSurface = SystemUiSceneStateSource.Surface.UNKNOWN
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
            SystemUiSceneStateSource.currentState(battery)?.let {
                applySceneState(it, "seed")
            }
            SystemUiTintStateSource.currentState(battery)?.let {
                applyTintState(it, "seed")
            }
            layoutProbe()
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            batteryView.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            host.get()?.overlay?.remove(probeView)
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
            val visible = SystemUiSceneStateSource.allowsHomeOverlay(update.surface)
            probeView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                probeView.invalidate()
            } else {
                probeView.clearPendingLatency()
            }

            onEvent(
                "homeRenderScene source=" + source +
                    " raw=" + update.rawState +
                    " surface=" + update.surface.name +
                    " visible=" + visible +
                    " policy=failClosedOutsideUnlockedStatusBar " +
                    " nativeGeometryWrites=0",
            )
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
            val resolved =
                CombinedStatusPresentationPolicy.resolveTint(
                    previous = stableTint,
                    candidate = state,
                )

            if (resolved == null || resolved == stableTint) {
                if (
                    !CombinedStatusPresentationPolicy.isValidTint(state) &&
                    !rejectedTintLogged
                ) {
                    rejectedTintLogged = true
                    onEvent(
                        "homeRenderTint deferred source=" + source +
                            " applied=#" +
                            state.appliedTint.toUInt().toString(16).padStart(8, '0') +
                            " reason=transparent retainStable=true",
                    )
                }
                return
            }

            stableTint = resolved
            probeView.setTintState(resolved)
            if (!tintLogged) {
                tintLogged = true
                onEvent(
                    "homeRenderTint source=" + source +
                        " applied=#" +
                        resolved.appliedTint.toUInt().toString(16).padStart(8, '0') +
                        " eventDriven=true stable=true",
                )
            }
        }

        fun update(
            snapshot: CombinedStatusStateStore.Snapshot,
            trace: RuntimeRenderTrace? = null,
        ) {
            val defaultDataSubscriptionId =
                runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
                    .getOrDefault(-1)
            val candidate =
                CombinedStatusRenderModel.from(
                    snapshot = snapshot,
                    presentation = CombinedStatusPresentationStateStore.snapshot(),
                    defaultDataSubscriptionId = defaultDataSubscriptionId,
                )
            val model =
                CombinedStatusPresentationPolicy.resolveModel(
                    previous = stableModel,
                    candidate = candidate,
                )

            if (candidate == null) {
                if (stableModel != null && !deferredStateLogged) {
                    deferredStateLogged = true
                    onEvent(
                        "homeRenderState deferred incomplete=true " +
                            "retainStable=true",
                    )
                }
                return
            }

            if (model != stableModel) {
                stableModel = model
                val visibleTrace =
                    trace?.takeIf {
                        layoutLogged &&
                            SystemUiSceneStateSource.allowsHomeOverlay(sceneSurface)
                    }
                probeView.setModel(model, visibleTrace)
            }

            if (model != null && !readyLogged) {
                readyLogged = true
                onEvent(
                    "homeRenderProbe ready " +
                        "battery=" + model.batteryPercent +
                        " charging=" + model.charging +
                        " center=" + model.centerIndicator.javaClass.simpleName +
                        " mobileLevel=" + (model.mobileLevel ?: -1) +
                        " effectiveDataSubId=" + model.effectiveDataSubscriptionId +
                        " defaultDataSubId=" + defaultDataSubscriptionId,
                )
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
                onEvent(
                    "homeRenderProbe attached " +
                        "slot=homeHostOverlay anchor=battery " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " size=" + anchorRect.width() + "x" + anchorRect.height() +
                        " opacity=" + PROBE_OPACITY +
                        " ancestorVisibilityIndependent=true " +
                        "originalsHidden=false nativeGeometryWrites=0",
                )
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

    private class ProbeView(
        context: Context,
        private val onStateRendered: (
            latencyMs: Long,
            committedOnMainThread: Boolean,
            sample: RuntimeRenderLatencySample?,
        ) -> Unit,
    ) : View(context) {
        private val painter = LegacyCombinedStatusPainter()

        @Volatile
        private var model: CombinedStatusRenderModel? = null

        @Volatile
        private var tintState: CombinedStatusTintState? = null

        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setWillNotDraw(false)
        }

        @Volatile
        private var pendingStateUptimeMs: Long = 0

        @Volatile
        private var pendingStateCommittedOnMainThread: Boolean = false

        @Volatile
        private var pendingTrace: RuntimeRenderTrace? = null

        @Volatile
        private var pendingModelCommittedNanos: Long = 0L

        fun setModel(
            model: CombinedStatusRenderModel?,
            trace: RuntimeRenderTrace? = null,
        ) {
            if (this.model == model) {
                return
            }
            this.model = model
            pendingStateUptimeMs = SystemClock.uptimeMillis()
            pendingStateCommittedOnMainThread =
                Looper.myLooper() === Looper.getMainLooper()
            pendingTrace = trace
            pendingModelCommittedNanos =
                if (trace == null) {
                    0L
                } else {
                    SystemClock.elapsedRealtimeNanos()
                }
            requestRedraw()
        }

        fun setTintState(state: CombinedStatusTintState) {
            if (tintState == state) {
                return
            }
            tintState = state
            requestRedraw()
        }

        fun clearPendingLatency() {
            pendingStateUptimeMs = 0L
            pendingTrace = null
            pendingModelCommittedNanos = 0L
        }

        private fun requestRedraw() {
            if (Looper.myLooper() === Looper.getMainLooper()) {
                invalidate()
            } else {
                postInvalidateOnAnimation()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val current = model ?: return
            val tint = tintState ?: return
            painter.draw(
                canvas = canvas,
                width = width,
                height = height,
                model = current,
                colors = CombinedStatusColorPolicy.resolve(current, tint),
                opacity = PROBE_OPACITY,
            )

            val committedAt = pendingStateUptimeMs
            if (committedAt != 0L) {
                pendingStateUptimeMs = 0L
                val trace = pendingTrace
                val modelCommittedNanos = pendingModelCommittedNanos
                pendingTrace = null
                pendingModelCommittedNanos = 0L
                val sample =
                    if (trace != null && modelCommittedNanos != 0L) {
                        RuntimeRenderLatencySample.from(
                            trace = trace,
                            modelCommittedNanos = modelCommittedNanos,
                            drawNanos = SystemClock.elapsedRealtimeNanos(),
                            committedOnMainThread = pendingStateCommittedOnMainThread,
                        )
                    } else {
                        null
                    }
                onStateRendered(
                    (SystemClock.uptimeMillis() - committedAt).coerceAtLeast(0L),
                    pendingStateCommittedOnMainThread,
                    sample,
                )
            }
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    private const val PROBE_OPACITY = 1f
}
