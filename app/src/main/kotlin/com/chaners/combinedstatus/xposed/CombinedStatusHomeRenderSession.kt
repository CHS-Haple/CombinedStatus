package com.chaners.combinedstatus.xposed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
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
        )
        current = session
        session.start()
        session.update(CombinedStatusStateStore.snapshot())
        return AttachResult.Ready
    }

    @Synchronized
    fun onState(snapshot: CombinedStatusStateStore.Snapshot) {
        current?.update(snapshot)
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        current?.updateTint(update)
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
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private val probeView = ProbeView(host.context)
        private var readyLogged = false
        private var layoutLogged = false
        private var tintLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        private var stableModel: CombinedStatusRenderModel? = null
        private var stableTint: CombinedStatusTintState? = null
        private var transitionProbeGeneration = 0

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
            hostView.overlay.add(probeView)
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

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            val defaultDataSubscriptionId =
                runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
                    .getOrDefault(-1)
            val candidate =
                CombinedStatusRenderModel.from(
                    snapshot = snapshot,
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

            val previousModel = stableModel
            if (model != stableModel) {
                stableModel = model
                probeView.setModel(model)
                if (readyLogged && previousModel != null && model != null) {
                    scheduleTransitionProbe(
                        previous = previousModel,
                        current = model,
                    )
                }
            }

            if (model != null && !readyLogged) {
                readyLogged = true
                onEvent(
                    "homeRenderProbe ready " +
                        "battery=" + model.batteryPercent +
                        " charging=" + model.charging +
                        " wifiSegments=" + (model.wifiSegments ?: 0) +
                        " mobileLevel=" + (model.mobileLevel ?: -1) +
                        " mobileSubId=" + model.mobileSubscriptionId +
                        " defaultDataSubId=" + defaultDataSubscriptionId,
                )
            }
        }

        private fun scheduleTransitionProbe(
            previous: CombinedStatusRenderModel,
            current: CombinedStatusRenderModel,
        ) {
            val hostView = host.get() ?: return
            transitionProbeGeneration += 1
            val generation = transitionProbeGeneration
            val reason =
                "wifi=" + (previous.wifiSegments ?: 0) + "->" +
                    (current.wifiSegments ?: 0) +
                    ",mobile=" + (previous.mobileLevel ?: -1) + "->" +
                    (current.mobileLevel ?: -1) +
                    ",charging=" + previous.charging + "->" + current.charging

            sampleTransitionFrame(
                generation = generation,
                frame = 0,
                reason = reason,
            )
            scheduleNextTransitionFrame(
                hostView = hostView,
                generation = generation,
                frame = 1,
                reason = reason,
            )
        }

        private fun scheduleNextTransitionFrame(
            hostView: View,
            generation: Int,
            frame: Int,
            reason: String,
        ) {
            if (frame >= TRANSITION_PROBE_FRAME_COUNT) {
                return
            }
            hostView.postOnAnimation {
                if (generation != transitionProbeGeneration) {
                    return@postOnAnimation
                }
                sampleTransitionFrame(
                    generation = generation,
                    frame = frame,
                    reason = reason,
                )
                scheduleNextTransitionFrame(
                    hostView = hostView,
                    generation = generation,
                    frame = frame + 1,
                    reason = reason,
                )
            }
        }

        private fun sampleTransitionFrame(
            generation: Int,
            frame: Int,
            reason: String,
        ) {
            val hostView = host.get() ?: return
            val container = batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return
            val drawSnapshot = probeView.drawSnapshot()

            onEvent(
                "homeTransitionFrame gen=" + generation +
                    " frame=" + frame +
                    " reason=" + reason +
                    " host=" + viewState(hostView) +
                    " container=" + viewState(container) +
                    " battery=" + viewState(battery) +
                    " probe=" + viewState(probeView) +
                    " probeParent=" +
                    (probeView.parent?.javaClass?.simpleName ?: "none") +
                    " drawCount=" + drawSnapshot.count +
                    " lastDrawAgeMs=" +
                    if (drawSnapshot.lastUptimeMs == 0L) {
                        -1
                    } else {
                        (SystemClock.uptimeMillis() - drawSnapshot.lastUptimeMs)
                            .coerceAtLeast(0L)
                    },
            )
        }

        private fun viewState(view: View): String =
            view.javaClass.simpleName +
                "{a=" + view.alpha +
                ",ea=" + effectiveAlpha(view) +
                ",v=" + visibilityToken(view.visibility) +
                ",shown=" + view.isShown +
                ",attached=" + view.isAttachedToWindow +
                ",windowV=" + visibilityToken(view.windowVisibility) +
                ",b=" + view.left + "," + view.top + "-" +
                view.right + "," + view.bottom +
                ",t=" + view.translationX + "," + view.translationY +
                "}"

        private fun effectiveAlpha(view: View): Float {
            var alpha = 1f
            var current: View? = view
            while (current != null) {
                alpha *= current.alpha
                current = current.parent as? View
            }
            return alpha
        }

        private fun visibilityToken(value: Int): String =
            when (value) {
                View.VISIBLE -> "V"
                View.INVISIBLE -> "I"
                View.GONE -> "G"
                else -> value.toString()
            }

        override fun onViewAttachedToWindow(view: View) {
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        private fun layoutProbe() {
            val hostView = host.get() ?: return
            val battery = batteryView.get() ?: return
            if (
                !hostView.isLaidOut ||
                !battery.isLaidOut ||
                battery.width <= 0 ||
                battery.height <= 0
            ) {
                return
            }

            val anchorBounds = Rect(0, 0, battery.width, battery.height)
            hostView.offsetDescendantRectToMyCoords(
                battery,
                anchorBounds,
            )

            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                anchorBounds.width(),
                View.MeasureSpec.EXACTLY,
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(
                anchorBounds.height(),
                View.MeasureSpec.EXACTLY,
            )
            probeView.measure(widthSpec, heightSpec)
            probeView.layout(
                anchorBounds.left,
                anchorBounds.top,
                anchorBounds.right,
                anchorBounds.bottom,
            )

            if (!layoutLogged) {
                layoutLogged = true
                onEvent(
                    "homeRenderProbe attached " +
                        "slot=homeHostOverlay anchor=battery " +
                        "bounds=" + anchorBounds.left + "," + anchorBounds.top + "-" +
                        anchorBounds.right + "," + anchorBounds.bottom +
                        " size=" + anchorBounds.width() + "x" + anchorBounds.height() +
                        " opacity=" + PROBE_OPACITY +
                        " ancestorVisibilityIndependent=true " +
                        "originalsHidden=false nativeGeometryWrites=0",
                )
            }
        }
    }

    private class ProbeView(
        context: Context,
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

        fun setModel(model: CombinedStatusRenderModel?) {
            if (this.model == model) {
                return
            }
            this.model = model
            postInvalidateOnAnimation()
        }

        fun setTintState(state: CombinedStatusTintState) {
            if (tintState == state) {
                return
            }
            tintState = state
            postInvalidateOnAnimation()
        }

        @Volatile
        private var drawCount: Long = 0

        @Volatile
        private var lastDrawUptimeMs: Long = 0

        fun drawSnapshot(): DrawSnapshot =
            DrawSnapshot(
                count = drawCount,
                lastUptimeMs = lastDrawUptimeMs,
            )

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawCount += 1
            lastDrawUptimeMs = SystemClock.uptimeMillis()
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
        }

        data class DrawSnapshot(
            val count: Long,
            val lastUptimeMs: Long,
        )
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    private const val PROBE_OPACITY = 1f
    private const val TRANSITION_PROBE_FRAME_COUNT = 8
}
