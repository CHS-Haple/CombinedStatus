package com.chaners.combinedstatus.xposed

import android.view.View

internal object SystemUiNativeParticipantRuntimeOwner {
    private var pending: PendingActivation? = null

    @Synchronized
    fun schedule(
        host: Any,
        onReady: (Any) -> Unit,
        onFailure: (String) -> Unit,
    ): ScheduleResult {
        cancelPendingLocked()

        val hostView =
            host as? View
                ?: return ScheduleResult.Failure("host-not-view")

        val activation =
            PendingActivation(
                hostView = hostView,
                onReady = onReady,
                onFailure = onFailure,
            )
        pending = activation

        return if (activation.start()) {
            ScheduleResult.Scheduled
        } else {
            pending = null
            ScheduleResult.Failure("host-readiness-schedule-rejected")
        }
    }

    @Synchronized
    fun cancelPending(): Boolean = cancelPendingLocked()

    @Synchronized
    private fun complete(activation: PendingActivation): Boolean {
        if (pending !== activation) {
            return false
        }
        pending = null
        return true
    }

    private fun cancelPendingLocked(): Boolean {
        val activation = pending ?: return false
        pending = null
        activation.cancel()
        return true
    }

    private class PendingActivation(
        private val hostView: View,
        private val onReady: (Any) -> Unit,
        private val onFailure: (String) -> Unit,
    ) : View.OnAttachStateChangeListener, Runnable {
        private var listeningForAttach = false

        fun start(): Boolean {
            return if (hostView.isAttachedToWindow) {
                hostView.post(this)
            } else {
                hostView.addOnAttachStateChangeListener(this)
                listeningForAttach = true
                true
            }
        }

        fun cancel() {
            if (listeningForAttach) {
                hostView.removeOnAttachStateChangeListener(this)
                listeningForAttach = false
            }
            hostView.removeCallbacks(this)
        }

        override fun onViewAttachedToWindow(view: View) {
            if (listeningForAttach) {
                view.removeOnAttachStateChangeListener(this)
                listeningForAttach = false
            }
            if (!view.post(this) && complete(this)) {
                onFailure("host-readiness-post-rejected")
            }
        }

        override fun onViewDetachedFromWindow(view: View) = Unit

        override fun run() {
            if (!complete(this)) {
                return
            }
            onReady(hostView)
        }
    }

    internal sealed interface ScheduleResult {
        data object Scheduled : ScheduleResult

        data class Failure(
            val reason: String,
        ) : ScheduleResult
    }
}
