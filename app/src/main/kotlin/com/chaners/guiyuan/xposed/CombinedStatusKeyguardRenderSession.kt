package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.os.Looper
import android.view.View
import com.chaners.guiyuan.settings.CombinedStatusFeatureSettings
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import java.lang.ref.WeakReference

internal object CombinedStatusKeyguardRenderSession {
    private var current: Session? = null

    @Synchronized
    fun attach(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean = { true },
        onPresentationReadinessChanged: ((Boolean) -> Unit)? = null,
    ): AttachResult {
        val existing = current
        if (existing?.matches(resolved) == true) {
            existing.update(CombinedStatusStateStore.snapshot())
            return AttachResult.Ready
        }

        existing?.stop()
        val settings = RuntimeFeaturePreferencesOwner.currentSettings()
        val initialAod =
            SystemUiKeyguardAodStateSource.currentState(resolved.battery)
                ?: return AttachResult.Failure("aod-state-unavailable")
        val session =
            Session(
                resolved = resolved,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                initialFeatureEnabled = settings.enabled && settings.keyguardEnabled,
                initialAodBlocked = initialAod.blocksProjection,
                onPresentationReadinessChanged = onPresentationReadinessChanged,
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
    fun onPresentationStateChanged() {
        current?.update(CombinedStatusStateStore.snapshot())
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun onFeatureSettingsChanged(settings: CombinedStatusFeatureSettings) {
        current?.setFeatureEnabled(settings.enabled && settings.keyguardEnabled)
    }

    @Synchronized
    fun onVisualSettingsChanged(settings: CombinedStatusVisualSettings) {
        current?.updateVisualSettings(settings)
    }

    @Synchronized
    fun onAodState(update: SystemUiKeyguardAodStateSource.AodUpdate) {
        current?.updateAodState(update)
    }

    @Synchronized
    fun setNativeHandoffActive(active: Boolean) {
        current?.setNativeHandoffActive(active)
    }

    @Synchronized
    fun currentTransitionSourceView(): View? = current?.transitionSourceView()

    @Synchronized
    fun detach() {
        current?.stop()
        current = null
    }

    internal fun resolveOverlayVisible(
        featureEnabled: Boolean,
        nativeHandoffActive: Boolean,
        aodBlocked: Boolean,
    ): Boolean =
        featureEnabled && !nativeHandoffActive && !aodBlocked

    internal fun resolveOwnerReady(
        featureEnabled: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        aodBlocked: Boolean,
    ): Boolean =
        featureEnabled &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached &&
            !aodBlocked

    private class Session(
        resolved: SystemUiKeyguardHostResolver.ResolvedHost,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        initialFeatureEnabled: Boolean,
        initialAodBlocked: Boolean,
        private val onPresentationReadinessChanged: ((Boolean) -> Unit)?,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(resolved.host)
        private val systemIcons = WeakReference(resolved.systemIcons)
        private val batteryView = WeakReference(resolved.battery)
        private val batteryCarrier = WeakReference(resolved.batteryCarrier)
        private val renderView = CombinedStatusRenderView(resolved.host.context)
        private val renderController = CombinedStatusRenderController(renderView)
        private val anchorRect = Rect()

        private var featureEnabled = initialFeatureEnabled
        private var nativeHandoffActive = true
        private var aodBlocked = initialAodBlocked
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var layoutLogged = false
        private var readyLogged = false
        private var rejectedTintLogged = false
        private var lastPresentationReady = false

        private val overlayHostLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProbe()
            }
        private val carrierLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProbe()
            }

        fun matches(resolved: SystemUiKeyguardHostResolver.ResolvedHost): Boolean =
            host.get() === resolved.host &&
                systemIcons.get() === resolved.systemIcons &&
                batteryView.get() === resolved.battery &&
                batteryCarrier.get() === resolved.batteryCarrier

        fun transitionSourceView(): View? =
            renderView.takeIf { view ->
                CombinedStatusScenePolicy.retainedTransitionSourceWitnessAvailable(
                    widthPx = view.width,
                    heightPx = view.height,
                    hostAttached = systemIcons.get()?.isAttachedToWindow == true,
                )
            }

        fun start() {
            val overlayHost = systemIcons.get() ?: return
            val battery = batteryView.get() ?: return
            val carrier = batteryCarrier.get() ?: return

            overlayHost.addOnAttachStateChangeListener(this)
            overlayHost.addOnLayoutChangeListener(overlayHostLayoutListener)
            carrier.addOnLayoutChangeListener(carrierLayoutListener)
            renderView.visibility = View.GONE
            overlayHost.overlay.add(renderView)
            renderController.updateVisualSettings(
                RuntimeVisualPreferencesOwner.currentSettings(),
            )
            SystemUiTintStateSource.currentState(battery)?.let { state ->
                applyTintState(
                    CombinedStatusTintAuthority.resolveBatteryEvent(
                        batteryState = state,
                        liveStatusIconTint = null,
                    ),
                    "seed",
                )
            }
            layoutProbe()
        }

        fun stop() {
            layoutReady = false
            dispatchPresentationReadiness("stop")
            systemIcons.get()?.removeOnAttachStateChangeListener(this)
            systemIcons.get()?.removeOnLayoutChangeListener(overlayHostLayoutListener)
            batteryCarrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            systemIcons.get()?.overlay?.remove(renderView)
        }

        fun setFeatureEnabled(enabled: Boolean) {
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                host.get()?.post { setFeatureEnabled(enabled) }
                return
            }
            if (featureEnabled == enabled) return
            featureEnabled = enabled
            val visible = applyResolvedVisibility()
            emitEvent {
                "keyguardRenderFeature enabled=" + featureEnabled +
                    " overlayVisible=" + visible +
                    " nativeHandoffActive=" + nativeHandoffActive +
                    " nativeGeometryWrites=0"
            }
            dispatchPresentationReadiness("feature")
        }

        fun setNativeHandoffActive(active: Boolean) {
            if (nativeHandoffActive == active) return
            nativeHandoffActive = active
            val visible = applyResolvedVisibility()
            emitEvent {
                "keyguardRenderHandoff nativeActive=" + nativeHandoffActive +
                    " overlayVisible=" + visible +
                    " nativeGeometryWrites=0"
            }
        }

        fun updateVisualSettings(settings: CombinedStatusVisualSettings) {
            renderController.updateVisualSettings(settings)
        }

        fun updateAodState(update: SystemUiKeyguardAodStateSource.AodUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) return
            if (aodBlocked == update.blocksProjection) return

            aodBlocked = update.blocksProjection
            val visible = applyResolvedVisibility()
            emitEvent {
                "keyguardRenderAod blocked=" + aodBlocked +
                    " source=" + update.source +
                    " toAod=" + update.toAod +
                    " isAodAnimate=" + update.isAodAnimate +
                    " animToAod=" + (update.animToAod ?: "unavailable") +
                    " overlayVisible=" + visible +
                    " nativeGeometryWrites=0"
            }
            dispatchPresentationReadiness("aod:" + update.source)
        }

        fun updateTint(update: SystemUiTintStateSource.TintUpdate) {
            val battery = batteryView.get() ?: return
            if (update.sourceView !== battery) return
            applyTintState(
                CombinedStatusTintAuthority.resolveBatteryEvent(
                    batteryState = update.state,
                    liveStatusIconTint = null,
                ),
                "keyguard-battery",
            )
        }

        private fun applyTintState(
            state: CombinedStatusTintState,
            source: String,
        ) {
            val update = renderController.updateTint(state)
            if (update.resolved != null) {
                tintReady = true
            }
            if (update.rejectedInvalidCandidate && !rejectedTintLogged) {
                rejectedTintLogged = true
                emitEvent {
                    "keyguardRenderTint deferred source=" + source +
                        " reason=transparent retainStable=true"
                }
            }
            if (update.changed) {
                val resolved = update.resolved
                if (resolved != null) {
                    emitEvent {
                        "keyguardRenderTint source=" + source +
                            " applied=#" +
                            resolved.appliedTint.toUInt().toString(16).padStart(8, '0') +
                            " authority=keyguard-battery eventDriven=true stable=true"
                    }
                }
            }
            dispatchPresentationReadiness("tint:" + source)
        }

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            val update = renderController.update(snapshot)
            if (!update.candidateComplete) return
            val model = update.model
            if (model != null) {
                modelReady = true
                if (!readyLogged) {
                    readyLogged = true
                    emitEvent {
                        "keyguardRender ready battery=" + model.batteryPercent +
                            " charging=" + model.charging +
                            " center=" + model.centerIndicator.javaClass.simpleName +
                            " nativeGeometryWrites=0"
                    }
                }
            }
            dispatchPresentationReadiness("model")
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProbe()
        }

        override fun onViewDetachedFromWindow(view: View) {
            if (layoutReady) {
                layoutReady = false
                dispatchPresentationReadiness("host-detached")
            }
        }

        private fun layoutProbe() {
            if (!resolveNativeAnchor(anchorRect)) {
                if (layoutReady) {
                    layoutReady = false
                    dispatchPresentationReadiness("layout-unavailable")
                }
                return
            }
            applyAnchorBounds(anchorRect)
            layoutReady = true
            if (!layoutLogged) {
                layoutLogged = true
                emitEvent {
                    "keyguardRender attached carrier=MiuiStatusBatteryContainer.overlay " +
                        "carrierAuthority=battery_icon_container " +
                        "motion=keyguard-system-icons-inherited " +
                        "bounds=" + anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " size=" + anchorRect.width() + "x" + anchorRect.height() +
                        " nativeVisibilityInherited=true nativeAlphaInherited=true " +
                        "nativeTranslationInherited=true nativeGeometryWrites=0"
                }
            }
            dispatchPresentationReadiness("layout")
        }

        private fun resolveNativeAnchor(out: Rect): Boolean {
            val overlayHost = systemIcons.get() ?: return false
            val carrier = batteryCarrier.get() ?: return false
            val hostWidth = overlayHost.width
            val hostHeight = overlayHost.height
            val baseCarrierWidth =
                SystemUiHomeCarrierMetrics
                    .resolveCarrierWidthPx(carrier)
                    ?.coerceAtMost(hostWidth)
                    ?: return false
            if (
                !overlayHost.isLaidOut ||
                hostWidth <= 0 ||
                hostHeight <= 0 ||
                baseCarrierWidth <= 0
            ) {
                return false
            }

            val resolved =
                CombinedStatusSteadyLayoutResolver.resolve(
                    hostWidthPx = hostWidth,
                    hostHeightPx = hostHeight,
                    baseCarrierWidthPx = baseCarrierWidth,
                    isRtl = overlayHost.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: return false
            if (!resolved.renderCombined) return false

            out.set(
                resolved.slotLeftPx.toInt(),
                0,
                resolved.slotRightPx.toInt(),
                hostHeight,
            )
            return out.width() > 0 && out.height() > 0
        }

        private fun applyAnchorBounds(bounds: Rect) {
            if (
                renderView.measuredWidth != bounds.width() ||
                renderView.measuredHeight != bounds.height()
            ) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(bounds.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(bounds.height(), View.MeasureSpec.EXACTLY),
                )
            }
            renderView.layout(bounds.left, bounds.top, bounds.right, bounds.bottom)
        }

        private fun applyResolvedVisibility(): Boolean {
            val visible =
                resolveOverlayVisible(
                    featureEnabled = featureEnabled,
                    nativeHandoffActive = nativeHandoffActive,
                    aodBlocked = aodBlocked,
                )
            renderView.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                renderView.invalidate()
            } else {
                renderView.clearPendingLatency()
            }
            return visible
        }

        private fun dispatchPresentationReadiness(source: String) {
            val ready =
                resolveOwnerReady(
                    featureEnabled = featureEnabled,
                    modelReady = modelReady,
                    tintReady = tintReady,
                    layoutReady = layoutReady,
                    hostAttached = systemIcons.get()?.isAttachedToWindow == true,
                    aodBlocked = aodBlocked,
                )
            if (ready == lastPresentationReady) return
            lastPresentationReady = ready
            emitEvent {
                "keyguardRenderReadiness source=" + source +
                    " ownerReady=" + ready +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " featureEnabled=" + featureEnabled +
                    " aodBlocked=" + aodBlocked +
                    " aodOwned=false nativeGeometryWrites=0"
            }
            onPresentationReadinessChanged?.invoke(ready)
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) {
                onEvent(message())
            }
        }
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult
        data class Failure(val reason: String) : AttachResult
    }
}
