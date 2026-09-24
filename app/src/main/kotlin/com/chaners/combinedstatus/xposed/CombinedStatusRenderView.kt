package com.chaners.combinedstatus.xposed

import android.content.Context
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.View

internal class CombinedStatusRenderView(
    context: Context,
    private val onStateRendered: (
        latencyMs: Long,
        committedOnMainThread: Boolean,
        sample: RuntimeRenderLatencySample?,
    ) -> Unit = { _, _, _ -> },
) : View(context) {
    private val painter = CombinedStatusPainter()

    @Volatile
    private var model: CombinedStatusRenderModel? = null

    @Volatile
    private var tintState: CombinedStatusTintState? = null

    @Volatile
    private var pendingStateUptimeMs: Long = 0

    @Volatile
    private var pendingStateCommittedOnMainThread: Boolean = false

    @Volatile
    private var pendingTrace: RuntimeRenderTrace? = null

    @Volatile
    private var pendingModelCommittedNanos: Long = 0L

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

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
            opacity = 1f,
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
