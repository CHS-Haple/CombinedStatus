package com.chaners.combinedstatus.xposed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        private val probeView = ProbeView(host.context, batteryView)
        private var readyLogged = false
        private var layoutLogged = false

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
            val container = batteryContainer.get() ?: return
            val battery = batteryView.get() ?: return

            hostView.addOnAttachStateChangeListener(this)
            battery.addOnLayoutChangeListener(batteryLayoutListener)
            container.overlay.add(probeView)
            layoutProbe()
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            batteryView.get()?.removeOnLayoutChangeListener(batteryLayoutListener)
            batteryContainer.get()?.overlay?.remove(probeView)
        }

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            val defaultDataSubscriptionId =
                runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
                    .getOrDefault(-1)
            val model = CombinedStatusRenderModel.from(
                snapshot = snapshot,
                defaultDataSubscriptionId = defaultDataSubscriptionId,
            )
            probeView.setModel(model)

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
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        private fun layoutProbe() {
            val battery = batteryView.get() ?: return
            if (
                !battery.isLaidOut ||
                battery.width <= 0 ||
                battery.height <= 0
            ) {
                return
            }

            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                battery.width,
                View.MeasureSpec.EXACTLY,
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(
                battery.height,
                View.MeasureSpec.EXACTLY,
            )
            probeView.measure(widthSpec, heightSpec)
            probeView.layout(
                battery.left,
                battery.top,
                battery.right,
                battery.bottom,
            )

            if (!layoutLogged) {
                layoutLogged = true
                onEvent(
                    "homeRenderProbe attached " +
                        "slot=batteryOverlay bounds=" +
                        battery.left + "," + battery.top + "-" +
                        battery.right + "," + battery.bottom +
                        " size=" + battery.width + "x" + battery.height +
                        " opacity=" + PROBE_OPACITY +
                        " originalsHidden=false nativeGeometryWrites=0",
                )
            }
        }
    }

    private class ProbeView(
        context: Context,
        sourceView: ViewGroup,
    ) : View(context) {
        private val sourceView = WeakReference(sourceView)
        private val painter = LegacyCombinedStatusPainter()

        @Volatile
        private var model: CombinedStatusRenderModel? = null

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

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val current = model ?: return
            painter.draw(
                canvas = canvas,
                width = width,
                height = height,
                model = current,
                tint = resolveTint(sourceView.get()),
                opacity = PROBE_OPACITY,
            )
        }

        private fun resolveTint(root: View?): Int {
            if (root == null) {
                return Color.WHITE
            }

            if (root is ImageView) {
                root.imageTintList?.defaultColor?.let { color ->
                    if (Color.alpha(color) != 0) {
                        return color
                    }
                }
            }

            if (root is TextView) {
                val color = root.currentTextColor
                if (Color.alpha(color) != 0) {
                    return color
                }
            }

            if (root is ViewGroup) {
                for (index in 0 until root.childCount) {
                    val child = root.getChildAt(index)
                    if (child is ImageView) {
                        child.imageTintList?.defaultColor?.let { color ->
                            if (Color.alpha(color) != 0) {
                                return color
                            }
                        }
                    }
                    if (child is TextView) {
                        val color = child.currentTextColor
                        if (Color.alpha(color) != 0) {
                            return color
                        }
                    }
                    if (child is ViewGroup) {
                        val color = resolveTint(child)
                        if (color != Color.WHITE) {
                            return color
                        }
                    }
                }
            }

            return Color.WHITE
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    private const val PROBE_OPACITY = 0.72f
}
