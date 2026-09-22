package com.chaners.combinedstatus.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal object StatusBarStableSession {
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
        onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
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
            return AttachResult.Ready
        }

        existing?.stop()

        val session = Session(
            host = hostView,
            batteryContainer = batteryContainer,
            batteryView = batteryView,
            onBatteryState = onBatteryState,
            onEvent = onEvent,
        )
        current = session
        session.start()

        return AttachResult.Ready
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
        private val onBatteryState: (CombinedStatusStateStore.BatteryState) -> Unit,
        private val onEvent: (String) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private var anchorCaptured = false
        private var anchorLayoutListener: View.OnLayoutChangeListener? = null
        private var receiverContext: Context? = null
        private var receiverRegistered = false
        private var lastBatteryState: CombinedStatusStateStore.BatteryState? = null

        private val batteryReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    intent?.let(::acceptBatteryIntent)
                }
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
            val view = host.get() ?: return
            view.addOnAttachStateChangeListener(this)
            scheduleAnchorCapture()
            if (view.isAttachedToWindow) {
                registerBatteryReceiver(view.context)
            }
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            clearAnchorLayoutListener()
            unregisterBatteryReceiver()
        }

        override fun onViewAttachedToWindow(view: View) {
            scheduleAnchorCapture()
            registerBatteryReceiver(view.context)
        }

        override fun onViewDetachedFromWindow(view: View) {
            clearAnchorLayoutListener()
            unregisterBatteryReceiver()
        }

        private fun scheduleAnchorCapture() {
            if (anchorCaptured || captureAnchorIfReady("ready")) {
                return
            }

            val view = batteryView.get() ?: return
            if (anchorLayoutListener != null) {
                return
            }

            val listener =
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        captureAnchorIfReady("layout")
                    }
                }

            anchorLayoutListener = listener
            view.addOnLayoutChangeListener(listener)
            captureAnchorIfReady("ready")
        }

        private fun captureAnchorIfReady(source: String): Boolean {
            if (anchorCaptured) {
                return true
            }

            val hostView = host.get() ?: return false
            val container = batteryContainer.get() ?: return false
            val view = batteryView.get() ?: return false

            if (
                !view.isLaidOut ||
                view.width <= 0 ||
                view.height <= 0 ||
                view.measuredWidth <= 0 ||
                view.measuredHeight <= 0
            ) {
                return false
            }

            anchorCaptured = true
            clearAnchorLayoutListener()

            onEvent(
                Anchor(
                    source = source,
                    hostIdentity = System.identityHashCode(hostView),
                    batteryContainerIndex = hostView.indexOfChild(container),
                    batteryIndex = container.indexOfChild(view),
                    batteryWidth = view.width,
                    batteryHeight = view.height,
                    batteryMeasuredWidth = view.measuredWidth,
                    batteryMeasuredHeight = view.measuredHeight,
                ).logLine,
            )

            val statusIcons = container.directChild(STATUS_ICON_CONTAINER_CLASS_NAME)
            val layoutParams = view.layoutParams
            val margins = layoutParams as? ViewGroup.MarginLayoutParams
            onEvent(
                SlotMetrics(
                    batteryPaddingStart = view.paddingStart,
                    batteryPaddingEnd = view.paddingEnd,
                    batteryPaddingTop = view.paddingTop,
                    batteryPaddingBottom = view.paddingBottom,
                    batteryMinimumWidth = view.minimumWidth,
                    batteryLayoutWidth = layoutParams?.width ?: Int.MIN_VALUE,
                    batteryLayoutHeight = layoutParams?.height ?: Int.MIN_VALUE,
                    batteryMarginStart = margins?.marginStart ?: 0,
                    batteryMarginEnd = margins?.marginEnd ?: 0,
                    containerPaddingStart = container.paddingStart,
                    containerPaddingEnd = container.paddingEnd,
                    statusIconsWidth = statusIcons?.width ?: -1,
                    statusIconsRight = statusIcons?.right ?: -1,
                    batteryLeft = view.left,
                    batteryRight = view.right,
                    adjacentGap =
                        statusIcons?.let { icons -> view.left - icons.right } ?: -1,
                    batteryClipChildren = view.clipChildren,
                    containerClipChildren = container.clipChildren,
                    layoutRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                    batteryTranslationX = view.translationX,
                ).logLine,
            )
            return true
        }

        private fun clearAnchorLayoutListener() {
            val listener = anchorLayoutListener ?: return
            batteryView.get()?.removeOnLayoutChangeListener(listener)
            anchorLayoutListener = null
        }

        private fun registerBatteryReceiver(context: Context) {
            if (receiverRegistered) {
                return
            }

            val targetContext = context.applicationContext ?: context
            val stickyIntent =
                try {
                    targetContext.registerReceiver(
                        batteryReceiver,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        Context.RECEIVER_NOT_EXPORTED,
                    ).also {
                        receiverContext = targetContext
                        receiverRegistered = true
                    }
                } catch (error: RuntimeException) {
                    onEvent(
                        "stableStatus batteryListener=failed reason=" +
                            error.javaClass.simpleName,
                    )
                    null
                }

            stickyIntent?.let(::acceptBatteryIntent)
        }

        private fun unregisterBatteryReceiver() {
            val context = receiverContext
            if (!receiverRegistered || context == null) {
                receiverContext = null
                receiverRegistered = false
                return
            }

            runCatching {
                context.unregisterReceiver(batteryReceiver)
            }
            receiverContext = null
            receiverRegistered = false
        }

        private fun acceptBatteryIntent(intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) {
                return
            }

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent =
                if (level >= 0 && scale > 0) {
                    ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
                } else {
                    -1
                }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val state = CombinedStatusStateStore.BatteryState(
                percent = percent,
                charging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL,
                plugged = plugged,
            )

            if (state == lastBatteryState) {
                return
            }

            lastBatteryState = state
            onBatteryState(state)
            onEvent(
                "stableStatus battery=" +
                    "percent=${state.percent} charging=${state.charging} plugged=${state.plugged}",
            )
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    internal data class Anchor(
        val source: String,
        val hostIdentity: Int,
        val batteryContainerIndex: Int,
        val batteryIndex: Int,
        val batteryWidth: Int,
        val batteryHeight: Int,
        val batteryMeasuredWidth: Int,
        val batteryMeasuredHeight: Int,
    ) {
        val logLine: String
            get() =
                "stableStatus anchor source=$source hostId=$hostIdentity " +
                    "containerIndex=$batteryContainerIndex batteryIndex=$batteryIndex " +
                    "batterySize=${batteryWidth}x$batteryHeight " +
                    "batteryMeasured=${batteryMeasuredWidth}x$batteryMeasuredHeight " +
                    "nativeGeometryWrites=0"
    }

    internal data class SlotMetrics(
        val batteryPaddingStart: Int,
        val batteryPaddingEnd: Int,
        val batteryPaddingTop: Int,
        val batteryPaddingBottom: Int,
        val batteryMinimumWidth: Int,
        val batteryLayoutWidth: Int,
        val batteryLayoutHeight: Int,
        val batteryMarginStart: Int,
        val batteryMarginEnd: Int,
        val containerPaddingStart: Int,
        val containerPaddingEnd: Int,
        val statusIconsWidth: Int,
        val statusIconsRight: Int,
        val batteryLeft: Int,
        val batteryRight: Int,
        val adjacentGap: Int,
        val batteryClipChildren: Boolean,
        val containerClipChildren: Boolean,
        val layoutRtl: Boolean,
        val batteryTranslationX: Float,
    ) {
        val logLine: String
            get() =
                "stableStatus slotMetrics " +
                    "batteryPadding=$batteryPaddingStart,$batteryPaddingEnd," +
                    "$batteryPaddingTop,$batteryPaddingBottom " +
                    "batteryMinWidth=$batteryMinimumWidth " +
                    "layout=${batteryLayoutWidth}x$batteryLayoutHeight " +
                    "margins=$batteryMarginStart,$batteryMarginEnd " +
                    "containerPadding=$containerPaddingStart,$containerPaddingEnd " +
                    "statusIconsWidth=$statusIconsWidth statusIconsRight=$statusIconsRight " +
                    "batteryBounds=$batteryLeft-$batteryRight adjacentGap=$adjacentGap " +
                    "clipChildren=$batteryClipChildren,$containerClipChildren " +
                    "rtl=$layoutRtl translationX=$batteryTranslationX " +
                    "nativeGeometryWrites=0"
    }

}
