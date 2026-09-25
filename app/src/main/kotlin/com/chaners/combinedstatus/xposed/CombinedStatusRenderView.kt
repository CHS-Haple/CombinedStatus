package com.chaners.combinedstatus.xposed

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator

internal class CombinedStatusRenderView(
    context: Context,
    private val onStateRendered: (
        latencyMs: Long,
        committedOnMainThread: Boolean,
        sample: RuntimeRenderLatencySample?,
    ) -> Unit = { _, _, _ -> },
) : View(context) {
    private val painter = CombinedStatusPainter(context)
    private val centerEnterInterpolator: Interpolator =
        AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.linear_out_slow_in,
        )
    private val centerExitInterpolator: Interpolator =
        AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.fast_out_linear_in,
        )

    @Volatile
    private var model: CombinedStatusRenderModel? = null

    private var previousCenterIndicator: CenterIndicator? = null
    private var centerTransitionFraction = 1f
    private var centerTransitionAnimator: ValueAnimator? = null

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

        val previousModel = this.model
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

        val previousCenter = previousModel?.centerIndicator
        val nextCenter = model?.centerIndicator
        if (Looper.myLooper() === Looper.getMainLooper()) {
            applyCenterTransitionPolicy(
                previous = previousCenter,
                current = nextCenter,
            )
        } else {
            post {
                if (this.model?.centerIndicator == nextCenter) {
                    applyCenterTransitionPolicy(
                        previous = previousCenter,
                        current = nextCenter,
                    )
                }
            }
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

    private fun applyCenterTransitionPolicy(
        previous: CenterIndicator?,
        current: CenterIndicator?,
    ) {
        when (
            CenterTransitionPolicy.decide(
                previous = previous,
                current = current,
                activeSource = previousCenterIndicator,
                transitionRunning = centerTransitionAnimator != null,
            )
        ) {
            CenterTransitionPolicy.Decision.START ->
                startCenterTransition(
                    previous = checkNotNull(previous),
                    current = checkNotNull(current),
                )

            CenterTransitionPolicy.Decision.KEEP ->
                invalidate()

            CenterTransitionPolicy.Decision.SNAP ->
                cancelCenterTransition()
        }
    }

    private fun startCenterTransition(
        previous: CenterIndicator,
        current: CenterIndicator,
    ) {
        centerTransitionAnimator?.cancel()
        previousCenterIndicator = previous
        centerTransitionFraction = 0f

        centerTransitionAnimator =
            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
                    duration = CENTER_TRANSITION_DURATION_MS
                    addUpdateListener { animator ->
                        centerTransitionFraction =
                            (animator.animatedValue as Float)
                                .coerceIn(0f, 1f)
                        invalidate()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                if (centerTransitionAnimator === animation) {
                                    centerTransitionAnimator = null
                                    previousCenterIndicator = null
                                    centerTransitionFraction = 1f
                                    invalidate()
                                }
                            }
                        },
                    )
                    start()
                }
    }

    private fun cancelCenterTransition() {
        centerTransitionAnimator?.cancel()
        centerTransitionAnimator = null
        previousCenterIndicator = null
        centerTransitionFraction = 1f
    }

    override fun onDetachedFromWindow() {
        cancelCenterTransition()
        super.onDetachedFromWindow()
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
        val transitionFraction =
            centerTransitionFraction.coerceIn(0f, 1f)
        painter.draw(
            canvas = canvas,
            width = width,
            height = height,
            model = current,
            colors = CombinedStatusColorPolicy.resolve(current, tint),
            opacity = 1f,
            previousCenterIndicator = previousCenterIndicator,
            centerExitAmount =
                1f -
                    centerExitInterpolator
                        .getInterpolation(transitionFraction),
            centerEnterAmount =
                centerEnterInterpolator
                    .getInterpolation(transitionFraction),
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

    private companion object {
        const val CENTER_TRANSITION_DURATION_MS = 100L
    }
}
