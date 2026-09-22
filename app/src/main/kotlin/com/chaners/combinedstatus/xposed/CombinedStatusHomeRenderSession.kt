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
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"

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
        private val probeView =
            ProbeView(host.context) { latencyMs, committedOnMainThread ->
                onEvent(
                    "homeRenderLatency stateToDrawMs=" + latencyMs +
                        " commitMainThread=" + committedOnMainThread +
                        " scheduling=sameFramePreferred",
                )
            }
        private var readyLogged = false
        private var layoutLogged = false
        private var tintLogged = false
        private var deferredStateLogged = false
        private var rejectedTintLogged = false
        private var stableModel: CombinedStatusRenderModel? = null
        private var stableTint: CombinedStatusTintState? = null
        private var baselinePaddingCaptured = false
        private var baselinePaddingStart = 0
        private var baselinePaddingEnd = 0
        private var baselinePaddingTop = 0
        private var baselinePaddingBottom = 0
        private var baselineBatteryWidthPx = 0
        private var ownedSlotWidthPx = 0
        private var ownedSlotApplied = false
        private var ownedSlotVerificationPending = false
        private var ownedSlotVerifiedLogged = false
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
                if (applyOwnedSlotIfReady("layout")) {
                    return@OnLayoutChangeListener
                }
                layoutProbe()
                verifyOwnedSlotIfReady()
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
            if (!applyOwnedSlotIfReady("start")) {
                layoutProbe()
                verifyOwnedSlotIfReady()
            }
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            batteryView.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            host.get()?.overlay?.remove(probeView)
            restoreOwnedSlot()
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

            if (model != stableModel) {
                stableModel = model
                probeView.setModel(model)
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

        override fun onViewAttachedToWindow(view: View) {
            if (!applyOwnedSlotIfReady("attach")) {
                layoutProbe()
                verifyOwnedSlotIfReady()
            }
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        private fun applyOwnedSlotIfReady(source: String): Boolean {
            if (ownedSlotApplied) {
                return false
            }

            val battery = batteryView.get() ?: return false
            if (
                !battery.isLaidOut ||
                battery.width <= 0 ||
                battery.height <= 0
            ) {
                return false
            }

            baselinePaddingStart = battery.paddingStart
            baselinePaddingEnd = battery.paddingEnd
            baselinePaddingTop = battery.paddingTop
            baselinePaddingBottom = battery.paddingBottom
            baselinePaddingCaptured = true
            baselineBatteryWidthPx = battery.width
            ownedSlotWidthPx = minOf(battery.width, battery.height).coerceAtLeast(1)
            ownedSlotApplied = true
            ownedSlotVerificationPending = true

            battery.setPaddingRelative(
                baselinePaddingStart + ownedSlotWidthPx,
                baselinePaddingTop,
                baselinePaddingEnd,
                baselinePaddingBottom,
            )

            onEvent(
                "homeOwnedSlot applied source=" + source +
                    " baselineBatteryWidth=" + baselineBatteryWidthPx +
                    " slotWidth=" + ownedSlotWidthPx +
                    " paddingStart=" + baselinePaddingStart + "->" + battery.paddingStart +
                    " owner=MiuiBatteryMeterView originalsHidden=false " +
                    "animationAdded=false visibilityWrites=0 translationWrites=0 " +
                    "nativeGeometryWrites=paddingStartOnly",
            )
            return true
        }

        private fun restoreOwnedSlot() {
            if (!ownedSlotApplied || !baselinePaddingCaptured) {
                return
            }

            val battery = batteryView.get() ?: return
            battery.setPaddingRelative(
                baselinePaddingStart,
                baselinePaddingTop,
                baselinePaddingEnd,
                baselinePaddingBottom,
            )
            onEvent(
                "homeOwnedSlot restored paddingStart=" + baselinePaddingStart +
                    " visibilityWrites=0 translationWrites=0",
            )
            ownedSlotApplied = false
            ownedSlotVerificationPending = false
        }

        private fun verifyOwnedSlotIfReady() {
            if (
                !ownedSlotApplied ||
                !ownedSlotVerificationPending ||
                ownedSlotVerifiedLogged
            ) {
                return
            }

            val container = batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return
            if (!battery.isLaidOut || battery.width <= 0) {
                return
            }

            var statusIcons: View? = null
            for (index in 0 until container.childCount) {
                val child = container.getChildAt(index)
                if (child.javaClass.name == STATUS_ICON_CONTAINER_CLASS_NAME) {
                    statusIcons = child
                    break
                }
            }

            ownedSlotVerificationPending = false
            ownedSlotVerifiedLogged = true
            val occupiedWidthDelta = battery.width - baselineBatteryWidthPx
            val adjacentGap =
                statusIcons?.let { icons -> battery.left - icons.right } ?: -1
            onEvent(
                "homeOwnedSlot verified " +
                    "baselineBatteryWidth=" + baselineBatteryWidthPx +
                    " currentBatteryWidth=" + battery.width +
                    " measuredWidth=" + battery.measuredWidth +
                    " occupiedWidthDelta=" + occupiedWidthDelta +
                    " requestedSlotWidth=" + ownedSlotWidthPx +
                    " paddingStart=" + battery.paddingStart +
                    " statusIconsWidth=" + (statusIcons?.width ?: -1) +
                    " statusIconsRight=" + (statusIcons?.right ?: -1) +
                    " batteryBounds=" + battery.left + "-" + battery.right +
                    " adjacentGap=" + adjacentGap +
                    " originalBatteryPreserved=true " +
                    "animationAdded=false visibilityWrites=0 translationWrites=0",
            )
        }

        private fun layoutProbe() {
            if (!ownedSlotApplied || !resolveOwnedSlot(anchorRect)) {
                return
            }
            applyAnchorBounds(anchorRect)

            if (!layoutLogged) {
                layoutLogged = true
                onEvent(
                    "homeRenderProbe attached " +
                        "slot=homeOwnedPaddingSlot anchor=batteryStart " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " size=" + anchorRect.width() + "x" + anchorRect.height() +
                        " opacity=" + PROBE_OPACITY +
                        " ancestorVisibilityIndependent=true " +
                        "originalsHidden=false nativeGeometryWrites=paddingStartOnly " +
                        "translationWrites=0 animationAdded=false",
                )
            }
        }

        private fun resolveOwnedSlot(out: Rect): Boolean {
            val hostView = host.get() ?: return false
            val battery = batteryView.get() ?: return false
            if (
                !hostView.isLaidOut ||
                !battery.isLaidOut ||
                battery.width <= 0 ||
                battery.height <= 0 ||
                ownedSlotWidthPx <= 0
            ) {
                return false
            }

            out.set(0, 0, battery.width, battery.height)
            hostView.offsetDescendantRectToMyCoords(
                battery,
                out,
            )
            if (battery.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                out.left = out.right - ownedSlotWidthPx
            } else {
                out.right = out.left + ownedSlotWidthPx
            }
            return out.width() > 0 && out.height() > 0
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
        private val onStateRendered: (latencyMs: Long, committedOnMainThread: Boolean) -> Unit,
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

        fun setModel(model: CombinedStatusRenderModel?) {
            if (this.model == model) {
                return
            }
            this.model = model
            pendingStateUptimeMs = SystemClock.uptimeMillis()
            pendingStateCommittedOnMainThread =
                Looper.myLooper() === Looper.getMainLooper()
            requestRedraw()
        }

        fun setTintState(state: CombinedStatusTintState) {
            if (tintState == state) {
                return
            }
            tintState = state
            requestRedraw()
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
                onStateRendered(
                    (SystemClock.uptimeMillis() - committedAt).coerceAtLeast(0L),
                    pendingStateCommittedOnMainThread,
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
