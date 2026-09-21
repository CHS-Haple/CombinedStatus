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
            return AttachResult.Ready(existing.anchor)
        }

        existing?.stop()

        val anchor = Anchor(
            hostIdentity = System.identityHashCode(hostView),
            batteryContainerIndex = hostView.indexOfChild(batteryContainer),
            batteryIndex = batteryContainer.indexOfChild(batteryView),
            batteryWidth = batteryView.width,
            batteryHeight = batteryView.height,
            batteryMeasuredWidth = batteryView.measuredWidth,
            batteryMeasuredHeight = batteryView.measuredHeight,
        )
        val session = Session(
            host = hostView,
            batteryContainer = batteryContainer,
            batteryView = batteryView,
            anchor = anchor,
            onEvent = onEvent,
        )
        current = session
        session.start()

        return AttachResult.Ready(anchor)
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
        val anchor: Anchor,
        private val onEvent: (String) -> Unit,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val batteryContainer = WeakReference(batteryContainer)
        private val batteryView = WeakReference(batteryView)
        private var receiverContext: Context? = null
        private var receiverRegistered = false
        private var lastBatteryState: BatteryState? = null

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
            if (view.isAttachedToWindow) {
                registerBatteryReceiver(view.context)
            }
        }

        fun stop() {
            host.get()?.removeOnAttachStateChangeListener(this)
            unregisterBatteryReceiver()
        }

        override fun onViewAttachedToWindow(view: View) {
            registerBatteryReceiver(view.context)
        }

        override fun onViewDetachedFromWindow(view: View) {
            unregisterBatteryReceiver()
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
            val state = BatteryState(
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
            onEvent(
                "stableStatus battery=" +
                    "percent=${state.percent} charging=${state.charging} plugged=${state.plugged}",
            )
        }
    }

    internal sealed interface AttachResult {
        data class Ready(
            val anchor: Anchor,
        ) : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    internal data class Anchor(
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
                "stableStatus anchor hostId=$hostIdentity " +
                    "containerIndex=$batteryContainerIndex batteryIndex=$batteryIndex " +
                    "batterySize=${batteryWidth}x$batteryHeight " +
                    "batteryMeasured=${batteryMeasuredWidth}x$batteryMeasuredHeight " +
                    "nativeGeometryWrites=0"
    }

    private data class BatteryState(
        val percent: Int,
        val charging: Boolean,
        val plugged: Int,
    )
}
