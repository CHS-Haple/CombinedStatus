package com.chaners.guiyuan.xposed

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.chaners.guiyuan.settings.CombinedStatusFeatureSettings
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import java.lang.ref.WeakReference
import java.util.ArrayDeque

internal object CombinedStatusControlCenterRenderSession {
    private const val FAKE_ROOT_CLASS_NAME =
        "com.android.systemui.controlcenter.phone.widget.ControlCenterFakeStatusIcons"
    private const val BATTERY_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val MAX_PREARM_LAYOUT_ATTEMPTS = 2

    private var current: Session? = null
    private var pendingPrearm: PendingPrearm? = null
    private var sceneEligible = false
    private var transitionEndpoints: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints? = null
    private var nativeExpansionProgress: Float? = null

    @Synchronized
    fun prearmAfterNextNativeLayout(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): PrearmResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return PrearmResult.Failure("main-thread-required")
        }
        if (host.javaClass.name != FAKE_ROOT_CLASS_NAME) {
            return PrearmResult.Failure("fake-root-type-mismatch")
        }
        if (!host.isAttachedToWindow) {
            return PrearmResult.Failure("fake-root-not-attached")
        }

        val existing = pendingPrearm
        if (existing?.matches(host) == true) {
            return PrearmResult.Scheduled(reused = true)
        }

        existing?.cancel()
        val pending =
            PendingPrearm(
                host = host,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
            )
        pendingPrearm = pending
        pending.start()
        return PrearmResult.Scheduled(reused = false)
    }

    @Synchronized
    fun attach(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
    ): AttachResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return AttachResult.Failure("main-thread-required")
        }
        if (host.javaClass.name != FAKE_ROOT_CLASS_NAME) {
            return AttachResult.Failure("fake-root-type-mismatch")
        }
        val statusBarArea =
            host.uniqueDescendant(BATTERY_CONTAINER_CLASS_NAME)
                ?: return AttachResult.Failure("fake-status-bar-area-unresolved")
        val statusIcons =
            statusBarArea.directChild(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup
                ?: return AttachResult.Failure("status-icons-missing")
        val battery =
            statusBarArea.directChild(BATTERY_VIEW_CLASS_NAME) as? ViewGroup
                ?: return AttachResult.Failure("battery-view-missing")
        val carrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return AttachResult.Failure("battery-core-carrier-missing")
        val existing = current
        if (
            existing?.matches(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
            ) == true
        ) {
            existing.setSceneEligible(sceneEligible)
            existing.refresh()
            return existing.prepareNativePresentation(reused = true)
        }

        if (existing != null) {
            SystemUiHomePresentationOwner.deactivateControlCenter("host-replaced")
            existing.stop("host-replaced")
        }

        val session =
            Session(
                host = host,
                statusBarArea = statusBarArea,
                statusIcons = statusIcons,
                battery = battery,
                carrier = carrier,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
                initialSceneEligible = sceneEligible,
            )
        current = session
        session.start()
        return session.prepareNativePresentation(reused = false)
    }

    @Synchronized
    fun setRequestedVisible(visible: Boolean): Boolean =
        current?.setRequestedVisible(visible) ?: false

    @Synchronized
    fun setTransitionEndpoints(
        endpoints: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints?,
    ) {
        transitionEndpoints = endpoints
        current?.setTransitionEndpoints(endpoints)
    }

    @Synchronized
    fun onNativeExpansionProgress(progress: Float?) {
        nativeExpansionProgress = progress
        current?.setNativeExpansionProgress(progress)
    }

    @Synchronized
    fun setSceneEligible(eligible: Boolean) {
        sceneEligible = eligible
        val session = current
        session?.setSceneEligible(eligible)
        if (!eligible) {
            SystemUiHomePresentationOwner.deactivateControlCenter("scene-ineligible")
        } else {
            session?.prepareNativePresentation(reused = true)
        }
    }

    @Synchronized
    fun currentAttachedHostForHotReload(): ViewGroup? =
        current?.attachedHost()
            ?: pendingPrearm?.host()?.takeIf { candidate -> candidate.isAttachedToWindow }

    @Synchronized
    fun currentNativePresentationReadyForHotReload(): Boolean =
        current?.nativePresentationReadyForHotReload() == true

    @Synchronized
    fun restoreLaidOutHostAfterHotReload(
        host: ViewGroup,
        onEvent: (String) -> Unit,
        isDetailedDiagnosticsEnabled: () -> Boolean,
        onProjectionReadinessChanged: (Boolean) -> Unit,
        transferredCompactReady: Boolean = false,
    ): AttachResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return AttachResult.Failure("main-thread-required")
        }
        if (
            !shouldRestoreLaidOutHostAfterHotReload(
                attached = host.isAttachedToWindow,
                inLayout = host.isInLayout,
                width = host.width,
                height = host.height,
            )
        ) {
            return AttachResult.Failure("fake-root-hot-reload-layout-unavailable")
        }

        pendingPrearm?.cancel()
        pendingPrearm = null
        val result =
            attach(
                host = host,
                onEvent = onEvent,
                isDetailedDiagnosticsEnabled = isDetailedDiagnosticsEnabled,
                onProjectionReadinessChanged = onProjectionReadinessChanged,
            )
        if (
            result == AttachResult.Ready &&
            shouldAdoptTransferredCompactReadiness(transferredCompactReady)
        ) {
            when (SystemUiHomePresentationOwner.adoptControlCenterLayoutCutoverFromHotReload()) {
                is SystemUiHomePresentationOwner.ControlCenterStateResult.Active -> {
                    if (isDetailedDiagnosticsEnabled()) {
                        onEvent(
                            "controlCenterProjection hotReloadRestore state=adopted-compact " +
                                "source=transferred-laid-out-fake-root " +
                                "next=native-status-icons-layout-refresh nativeGeometryWrites=0",
                        )
                    }
                    return AttachResult.Ready
                }

                else -> Unit
            }
        }
        if (result == AttachResult.Ready && isDetailedDiagnosticsEnabled()) {
            onEvent(
                "controlCenterProjection hotReloadRestore state=prepared " +
                    "source=transferred-laid-out-fake-root " +
                    "transferredCompactReady=" + transferredCompactReady +
                    " layoutRequestBoundary=outside-native-layout " +
                    "next=native-status-icons-layout nativeGeometryWrites=0",
            )
        }
        return result
    }

    internal fun shouldAdoptTransferredCompactReadiness(
        transferredCompactReady: Boolean,
    ): Boolean = transferredCompactReady

    internal fun shouldRestoreLaidOutHostAfterHotReload(
        attached: Boolean,
        inLayout: Boolean,
        width: Int,
        height: Int,
    ): Boolean =
        attached && !inLayout && width > 0 && height > 0

    @Synchronized
    fun onState(snapshot: CombinedStatusStateStore.Snapshot) {
        current?.update(snapshot)
    }

    @Synchronized
    fun onPresentationStateChanged() {
        current?.refresh()
    }

    @Synchronized
    fun onTintUpdate(update: SystemUiTintStateSource.TintUpdate) {
        current?.updateTint(update)
    }

    @Synchronized
    fun currentProjectionGeometryDiagnostic(): String =
        current?.geometryDiagnostic() ?: "projection=unavailable"

    @Synchronized
    fun onFeatureSettingsChanged(settings: CombinedStatusFeatureSettings) {
        val session = current
        session?.setFeatureEnabled(settings.enabled)
        if (!settings.enabled || !sceneEligible) {
            SystemUiHomePresentationOwner.deactivateControlCenter(
                if (!settings.enabled) "feature-disabled" else "scene-ineligible",
            )
        } else {
            session?.prepareNativePresentation(reused = true)
        }
    }

    @Synchronized
    fun onVisualSettingsChanged(settings: CombinedStatusVisualSettings) {
        current?.updateVisualSettings(settings)
    }

    @Synchronized
    fun detach(
        source: String = "detach",
        releaseNativePresentation: Boolean = true,
    ) {
        pendingPrearm?.cancel()
        pendingPrearm = null
        if (releaseNativePresentation) {
            SystemUiHomePresentationOwner.deactivateControlCenter(source)
        }
        current?.stop(source)
        current = null
        nativeExpansionProgress = null
        transitionEndpoints = null
    }

    @Synchronized
    private fun onPendingPrearmLayout(pending: PendingPrearm) {
        if (pendingPrearm !== pending) return
        val host = pending.host()
        if (host == null || !host.isAttachedToWindow) {
            pending.cancel()
            pendingPrearm = null
            return
        }

        val attempt = pending.nextAttempt()
        when (
            val result =
                attach(
                    host = host,
                    onEvent = pending.onEvent,
                    isDetailedDiagnosticsEnabled = pending.isDetailedDiagnosticsEnabled,
                    onProjectionReadinessChanged = pending.onProjectionReadinessChanged,
                )
        ) {
            AttachResult.Ready -> {
                pending.cancel()
                pendingPrearm = null
                pending.emit(
                    "controlCenterProjection prearm state=armed " +
                        "source=fake-root-first-layout attempt=" + attempt +
                        " nativeGeometryWrites=0",
                )
            }

            is AttachResult.Failure -> {
                val retry =
                    isFirstLayoutRetryable(result.reason) &&
                        attempt < MAX_PREARM_LAYOUT_ATTEMPTS
                if (retry) {
                    pending.emit(
                        "controlCenterProjection prearm state=deferred " +
                            "source=fake-root-first-layout attempt=" + attempt +
                            " reason=" + result.reason +
                            " next=native-root-layout nativeGeometryWrites=0",
                    )
                    host.requestLayout()
                } else {
                    pending.cancel()
                    pendingPrearm = null
                    pending.emit(
                        "controlCenterProjection prearm state=failed " +
                            "source=fake-root-first-layout attempt=" + attempt +
                            " reason=" + result.reason +
                            " fallback=native-qs-fake nativeGeometryWrites=0",
                    )
                }
            }
        }
    }

    @Synchronized
    private fun onPendingPrearmDetached(pending: PendingPrearm) {
        if (pendingPrearm !== pending) return
        pending.cancel()
        pendingPrearm = null
    }

    internal fun isFirstLayoutRetryable(reason: String): Boolean =
        reason in
            setOf(
                "fake-status-bar-area-unresolved",
                "status-icons-missing",
                "battery-view-missing",
                "battery-core-carrier-missing",
                "battery-core-width-unavailable",
                "ignored-slots-list-unavailable",
                "hooks-not-ready",
            )

    private class PendingPrearm(
        host: ViewGroup,
        val onEvent: (String) -> Unit,
        val isDetailedDiagnosticsEnabled: () -> Boolean,
        val onProjectionReadinessChanged: (Boolean) -> Unit,
    ) : View.OnLayoutChangeListener,
        View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private var started = false
        private var attempts = 0

        fun host(): ViewGroup? = host.get()

        fun nextAttempt(): Int {
            attempts += 1
            return attempts
        }

        fun matches(candidate: ViewGroup): Boolean =
            host.get() === candidate

        fun start() {
            if (started) return
            started = true
            val root = host.get() ?: return
            root.addOnLayoutChangeListener(this)
            root.addOnAttachStateChangeListener(this)
            // Cold start will normally hit the initial native layout naturally.
            // Hot Reload may restore an already-laid-out root, so request exactly
            // one native layout cycle through the same lifecycle boundary.
            root.requestLayout()
            emit(
                "controlCenterProjection prearm state=scheduled " +
                    "source=fake-root-attached next=native-root-layout " +
                    "nativeGeometryWrites=0",
            )
        }

        fun cancel() {
            if (!started) return
            started = false
            host.get()?.let { root ->
                root.removeOnLayoutChangeListener(this)
                root.removeOnAttachStateChangeListener(this)
            }
        }

        fun emit(message: String) {
            if (isDetailedDiagnosticsEnabled()) onEvent(message)
        }

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
            CombinedStatusControlCenterRenderSession.onPendingPrearmLayout(this)
        }

        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            CombinedStatusControlCenterRenderSession.onPendingPrearmDetached(this)
        }
    }

    private fun geometrySummary(view: View?): String {
        if (view == null) return "none"
        val location = IntArray(2)
        val hasLocation =
            runCatching {
                view.getLocationOnScreen(location)
                true
            }.getOrDefault(false)
        return view.javaClass.simpleName +
            "(v=" + view.visibility +
            ",a=" + view.alpha +
            ",l=" + view.left +
            ",t=" + view.top +
            ",w=" + view.width +
            ",h=" + view.height +
            ",tx=" + view.translationX +
            ",ty=" + view.translationY +
            ",sx=" + (if (hasLocation) location[0] else "na") +
            ",sy=" + (if (hasLocation) location[1] else "na") +
            ",parent=" + (view.parent?.javaClass?.simpleName ?: "none") +
            ")"
    }

    internal fun shouldRetainNativePresentationOnLayoutUnavailable(
        hostAttached: Boolean,
        nativePresentationReady: Boolean,
    ): Boolean =
        hostAttached && nativePresentationReady

    internal fun resolveProjectionReady(
        featureEnabled: Boolean,
        sceneEligible: Boolean,
        modelReady: Boolean,
        tintReady: Boolean,
        layoutReady: Boolean,
        hostAttached: Boolean,
        nativePresentationReady: Boolean,
    ): Boolean =
        featureEnabled &&
            sceneEligible &&
            modelReady &&
            tintReady &&
            layoutReady &&
            hostAttached &&
            nativePresentationReady

    private class Session(
        host: ViewGroup,
        statusBarArea: ViewGroup,
        statusIcons: ViewGroup,
        battery: ViewGroup,
        carrier: View,
        private val onEvent: (String) -> Unit,
        private val isDetailedDiagnosticsEnabled: () -> Boolean,
        private val onProjectionReadinessChanged: (Boolean) -> Unit,
        initialSceneEligible: Boolean,
    ) : View.OnAttachStateChangeListener {
        private val host = WeakReference(host)
        private val statusBarArea = WeakReference(statusBarArea)
        private val statusIcons = WeakReference(statusIcons)
        private val battery = WeakReference(battery)
        private val carrier = WeakReference(carrier)
        private val renderView = CombinedStatusRenderView(host.context)
        private val renderController = CombinedStatusRenderController(renderView)
        private val transitionView = TransitionProjectionView(host.context)
        private val anchorRect = Rect()

        private var currentModel: CombinedStatusRenderModel? = null
        private var currentTint: CombinedStatusTintState? = null
        private var currentVisualSettings = RuntimeVisualPreferencesOwner.currentSettings()
        private var transitionEndpoints =
            CombinedStatusControlCenterRenderSession.transitionEndpoints
        private var nativeExpansionProgress =
            CombinedStatusControlCenterRenderSession.nativeExpansionProgress

        private var requestedVisible = false
        private var featureEnabled = RuntimeFeaturePreferencesOwner.currentSettings().enabled
        private var sceneEligible = initialSceneEligible
        private var modelReady = false
        private var tintReady = false
        private var layoutReady = false
        private var nativePresentationReady = false
        private var lastProjectionReady: Boolean? = null

        private val hostLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val statusAreaLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }
        private val carrierLayoutListener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                layoutProjection()
            }

        fun matches(
            host: ViewGroup,
            statusBarArea: ViewGroup,
            statusIcons: ViewGroup,
            battery: ViewGroup,
            carrier: View,
        ): Boolean =
            this.host.get() === host &&
                this.statusBarArea.get() === statusBarArea &&
                this.statusIcons.get() === statusIcons &&
                this.battery.get() === battery &&
                this.carrier.get() === carrier

        fun attachedHost(): ViewGroup? =
            host.get()?.takeIf { candidate -> candidate.isAttachedToWindow }

        fun nativePresentationReadyForHotReload(): Boolean =
            nativePresentationReady && attachedHost() != null

        fun geometryDiagnostic(): String =
            "projection={" +
                "root=" + geometrySummary(host.get()) +
                ",area=" + geometrySummary(statusBarArea.get()) +
                ",statusIcons=" + geometrySummary(statusIcons.get()) +
                ",battery=" + geometrySummary(battery.get()) +
                ",carrier=" + geometrySummary(carrier.get()) +
                ",render=" + geometrySummary(renderView) +
                "}"

        fun start() {
            val hostView = host.get() ?: return
            hostView.addOnAttachStateChangeListener(this)
            hostView.addOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.addOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.addOnLayoutChangeListener(carrierLayoutListener)
            renderView.visibility = View.GONE
            transitionView.visibility = View.GONE
            hostView.overlay.add(renderView)
            hostView.overlay.add(transitionView)
            renderController.updateVisualSettings(currentVisualSettings)
            transitionView.setVisualSettings(currentVisualSettings)
            transitionView.setEndpoints(transitionEndpoints)
            transitionView.setNativeProgress(nativeExpansionProgress)
            update(CombinedStatusStateStore.snapshot())
            refreshTint()
            layoutProjection()
            dispatchReadiness("start")
        }

        fun stop(source: String) {
            val hostView = host.get()
            hostView?.removeOnAttachStateChangeListener(this)
            hostView?.removeOnLayoutChangeListener(hostLayoutListener)
            statusBarArea.get()?.removeOnLayoutChangeListener(statusAreaLayoutListener)
            carrier.get()?.removeOnLayoutChangeListener(carrierLayoutListener)
            hostView?.overlay?.remove(renderView)
            hostView?.overlay?.remove(transitionView)
            SystemUiHomePresentationOwner.setControlCenterTransitionProjectedSlots(emptySet())
            requestedVisible = false
            layoutReady = false
            nativePresentationReady = false
            if (lastProjectionReady == true) {
                lastProjectionReady = false
                onProjectionReadinessChanged(false)
            }
            emitEvent {
                "controlCenterProjection cleanup source=" + source +
                    " nativeCompactRestored=true nativeGeometryWrites=0 " +
                    "nativeAlphaWrites=0 nativeVisibilityWrites=0"
            }
        }

        fun setRequestedVisible(visible: Boolean): Boolean {
            requestedVisible = visible
            syncPresentation("visibility")
            return projectionReady()
        }

        fun prepareNativePresentation(reused: Boolean): AttachResult {
            if (!featureEnabled || !sceneEligible) {
                nativePresentationReady = false
                syncPresentation(
                    if (!featureEnabled) "feature-ineligible" else "scene-ineligible",
                )
                return AttachResult.Ready
            }
            val statusArea =
                statusBarArea.get()
                    ?: return AttachResult.Failure("fake-status-bar-area-released")
            val statusIconGroup =
                statusIcons.get()
                    ?: return AttachResult.Failure("status-icons-released")
            val batteryView =
                battery.get()
                    ?: return AttachResult.Failure("battery-view-released")
            val carrierView =
                carrier.get()
                    ?: return AttachResult.Failure("battery-core-carrier-released")

            return when (
                val result =
                    SystemUiHomePresentationOwner.activateControlCenter(
                        host = statusArea,
                        statusIcons = statusIconGroup,
                        batteryContainer = statusArea,
                        battery = batteryView,
                        batteryCarrier = carrierView,
                        onEvent = onEvent,
                        onFailNative = { reason ->
                            setNativePresentationReady(
                                ready = false,
                                maskedViews = 0,
                                source = "fail-native:" + reason,
                            )
                        },
                        onReady = { active ->
                            setNativePresentationReady(
                                ready = true,
                                maskedViews = active.maskedViews,
                                source = "native-layout",
                            )
                        },
                    )
            ) {
                is SystemUiHomePresentationOwner.ControlCenterStateResult.Active -> {
                    setNativePresentationReady(
                        ready = true,
                        maskedViews = result.maskedViews,
                        source = if (reused) "prearm-reuse" else "prearm-activation",
                    )
                    AttachResult.Ready
                }

                is SystemUiHomePresentationOwner.ControlCenterStateResult.Prepared -> {
                    emitEvent {
                        "controlCenterProjection prearm state=prepared " +
                            "reused=" + reused +
                            " requestedVisible=" + requestedVisible +
                            " nativeGeometryWrites=0"
                    }
                    AttachResult.Ready
                }

                is SystemUiHomePresentationOwner.ControlCenterStateResult.Failure -> {
                    setNativePresentationReady(
                        ready = false,
                        maskedViews = 0,
                        source = "prepare-failed:" + result.reason,
                    )
                    AttachResult.Failure(result.reason)
                }

                is SystemUiHomePresentationOwner.ControlCenterStateResult.Inactive -> {
                    setNativePresentationReady(
                        ready = false,
                        maskedViews = 0,
                        source = "prepare-inactive",
                    )
                    AttachResult.Failure("compact-presentation-inactive")
                }
            }
        }

        fun setNativePresentationReady(
            ready: Boolean,
            maskedViews: Int,
            source: String,
        ) {
            nativePresentationReady = ready
            if (ready) {
                layoutProjection()
            }
            applyVisibility()
            emitEvent {
                "controlCenterProjection compact ready=" + ready +
                    " source=" + source +
                    " maskedViews=" + maskedViews +
                    " stableBatterySlot=true batteryWidthDiffConsumed=false"
            }
            dispatchReadiness("compact:" + source)
        }

        fun update(snapshot: CombinedStatusStateStore.Snapshot) {
            val result = renderController.update(snapshot)
            currentModel = result.model
            modelReady = result.model != null
            refreshTint()
            syncTransitionState()
            syncPresentation("state")
        }

        fun refresh() =
            update(CombinedStatusStateStore.snapshot())

        fun updateTint(update: SystemUiTintStateSource.TintUpdate) {
            val batteryView = battery.get() ?: return
            if (update.sourceView !== batteryView) return
            applyTint(update.state, "battery")
        }

        fun setSceneEligible(eligible: Boolean) {
            sceneEligible = eligible
            if (!eligible) {
                nativePresentationReady = false
            }
            syncPresentation("scene")
        }

        fun setFeatureEnabled(enabled: Boolean) {
            featureEnabled = enabled
            if (!enabled) {
                nativePresentationReady = false
            }
            syncPresentation("feature")
        }

        fun updateVisualSettings(settings: CombinedStatusVisualSettings) {
            currentVisualSettings = settings
            renderController.updateVisualSettings(settings)
            transitionView.setVisualSettings(settings)
            syncTransitionState()
        }

        fun setTransitionEndpoints(
            endpoints: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints?,
        ) {
            transitionEndpoints = endpoints
            transitionView.setEndpoints(endpoints)
            refreshTransitionProjectedSlots()
            syncPresentation("transition-endpoints")
        }

        fun setNativeExpansionProgress(progress: Float?) {
            nativeExpansionProgress = progress
            transitionView.setNativeProgress(progress)
            syncPresentation("transition-progress")
        }

        private fun refreshTint() {
            val batteryView = battery.get() ?: return
            val state = SystemUiTintStateSource.currentState(batteryView) ?: return
            applyTint(state, "surface")
        }

        private fun applyTint(
            batteryState: CombinedStatusTintState,
            source: String,
        ) {
            val peerTint =
                statusIcons.get()?.let(
                    SystemUiNativeNetworkSuppressionOwner::currentAppliedStatusIconTintForGroup,
                )
            val resolved =
                CombinedStatusTintAuthority.resolveBatteryEvent(
                    batteryState,
                    peerTint,
                )
            val tintUpdate = renderController.updateTint(resolved)
            currentTint = tintUpdate.resolved
            tintReady = tintUpdate.resolved != null
            syncTransitionState()
            syncPresentation("tint:" + source)
        }

        private fun layoutProjection() {
            val hostView = host.get() ?: return markLayoutUnavailable()
            val statusArea = statusBarArea.get() ?: return markLayoutUnavailable()
            val carrierView = carrier.get() ?: return markLayoutUnavailable()
            val carrierWidth =
                SystemUiHomeCarrierMetrics.resolveCarrierWidthPx(carrierView)
                    ?: return markLayoutUnavailable()
            val resolved =
                CombinedStatusHomeLayoutResolver.resolve(
                    hostWidthPx = statusArea.width,
                    hostHeightPx = statusArea.height,
                    baseCarrierWidthPx = carrierWidth,
                    isRtl = statusArea.layoutDirection == View.LAYOUT_DIRECTION_RTL,
                ) ?: return markLayoutUnavailable()
            if (!resolved.renderCombined) return markLayoutUnavailable()

            val hostLocation = IntArray(2)
            val areaLocation = IntArray(2)
            hostView.getLocationInWindow(hostLocation)
            statusArea.getLocationInWindow(areaLocation)
            val offsetX = areaLocation[0] - hostLocation[0]
            val offsetY = areaLocation[1] - hostLocation[1]
            val left = offsetX + resolved.slotLeftPx.toInt()
            val right = offsetX + resolved.slotRightPx.toInt()
            val top = offsetY
            val bottom = offsetY + statusArea.height
            anchorRect.set(left, top, right, bottom)
            if (anchorRect.width() <= 0 || anchorRect.height() <= 0) {
                return markLayoutUnavailable()
            }

            if (
                renderView.measuredWidth != anchorRect.width() ||
                renderView.measuredHeight != anchorRect.height()
            ) {
                renderView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.width(),
                        View.MeasureSpec.EXACTLY,
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        anchorRect.height(),
                        View.MeasureSpec.EXACTLY,
                    ),
                )
            }
            renderView.layout(
                anchorRect.left,
                anchorRect.top,
                anchorRect.right,
                anchorRect.bottom,
            )

            if (
                transitionView.measuredWidth != hostView.width ||
                transitionView.measuredHeight != hostView.height
            ) {
                transitionView.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        hostView.width,
                        View.MeasureSpec.EXACTLY,
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        hostView.height,
                        View.MeasureSpec.EXACTLY,
                    ),
                )
            }
            transitionView.layout(0, 0, hostView.width, hostView.height)
            transitionView.setCombinedAnchor(anchorRect)
            transitionView.setEndpoints(transitionEndpoints)
            transitionView.setNativeProgress(nativeExpansionProgress)
            refreshTransitionProjectedSlots()

            val firstReady = !layoutReady
            layoutReady = true
            syncPresentation("layout")
            if (firstReady) {
                emitEvent {
                    "controlCenterProjection attached carrier=ControlCenterFakeStatusIcons.overlay " +
                        "geometrySource=MiuiStatusBatteryContainer bounds=" +
                        anchorRect.left + "," + anchorRect.top + "-" +
                        anchorRect.right + "," + anchorRect.bottom +
                        " target=stable-battery-slot motion=root-alpha-translation-inherited " +
                        "nativeGeometryWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0"
                }
            }
        }

        private fun markLayoutUnavailable() {
            if (!layoutReady) return
            layoutReady = false
            renderView.visibility = View.GONE
            transitionView.visibility = View.GONE
            SystemUiHomePresentationOwner.setControlCenterTransitionProjectedSlots(emptySet())

            val hostAttached = host.get()?.isAttachedToWindow == true
            val retainNativePresentation =
                shouldRetainNativePresentationOnLayoutUnavailable(
                    hostAttached = hostAttached,
                    nativePresentationReady = nativePresentationReady,
                )
            if (!retainNativePresentation) {
                nativePresentationReady = false
                SystemUiHomePresentationOwner.deactivateControlCenter(
                    "projection-layout-unavailable-detached",
                )
            }
            emitEvent {
                "controlCenterProjection layoutUnavailable action=pause-render " +
                    "compactPresentationRetained=" + retainNativePresentation +
                    " hostAttached=" + hostAttached +
                    " nativeGeometryWrites=0"
            }
            dispatchReadiness("layout-unavailable")
        }

        private fun projectionReady(): Boolean =
            resolveProjectionReady(
                featureEnabled = featureEnabled,
                sceneEligible = sceneEligible,
                modelReady = modelReady,
                tintReady = tintReady,
                layoutReady = layoutReady,
                hostAttached = host.get()?.isAttachedToWindow == true,
                nativePresentationReady = nativePresentationReady,
            )

        private fun syncPresentation(source: String) {
            syncTransitionState()
            applyVisibility()
            dispatchReadiness(source)
        }

        private fun syncTransitionState() {
            val model = currentModel
            val tint = currentTint
            if (model == null || tint == null) {
                transitionView.clearRenderState()
                return
            }
            transitionView.setRenderState(
                model = model,
                colors =
                    CombinedStatusColorPolicy.resolve(
                        model = model,
                        tintState = tint,
                        visualSettings = currentVisualSettings,
                    ),
            )
        }

        private fun refreshTransitionProjectedSlots() {
            val slots =
                if (transitionView.hasUsableEndpoints()) {
                    transitionView.commonPeerSlots()
                } else {
                    emptySet()
                }
            SystemUiHomePresentationOwner.setControlCenterTransitionProjectedSlots(slots)
        }

        private fun applyVisibility() {
            val visible = requestedVisible && projectionReady()
            val transitionVisible = visible && transitionView.hasUsableEndpoints()
            transitionView.visibility =
                if (transitionVisible) View.VISIBLE else View.GONE
            renderView.visibility =
                if (visible && !transitionVisible) View.VISIBLE else View.GONE
            if (transitionVisible) {
                transitionView.invalidate()
                renderView.clearPendingLatency()
            } else if (visible) {
                renderView.invalidate()
            } else {
                renderView.clearPendingLatency()
            }
        }

        private fun dispatchReadiness(source: String) {
            val ready = requestedVisible && projectionReady()
            if (ready == lastProjectionReady) return
            lastProjectionReady = ready
            emitEvent {
                "controlCenterProjection readiness source=" + source +
                    " ready=" + ready +
                    " requestedVisible=" + requestedVisible +
                    " sceneEligible=" + sceneEligible +
                    " modelReady=" + modelReady +
                    " tintReady=" + tintReady +
                    " layoutReady=" + layoutReady +
                    " nativePresentationReady=" + nativePresentationReady +
                    " rootAlphaInherited=true nativeGeometryWrites=0"
            }
            onProjectionReadinessChanged(ready)
        }

        override fun onViewAttachedToWindow(view: View) {
            layoutProjection()
            refreshTint()
            syncPresentation("attach")
        }

        override fun onViewDetachedFromWindow(view: View) {
            requestedVisible = false
            layoutReady = false
            nativePresentationReady = false
            renderView.visibility = View.GONE
            transitionView.visibility = View.GONE
            SystemUiHomePresentationOwner.setControlCenterTransitionProjectedSlots(emptySet())
            SystemUiHomePresentationOwner.deactivateControlCenter(
                "fake-root-detached",
            )
            dispatchReadiness("detach")
        }

        private inline fun emitEvent(message: () -> String) {
            if (isDetailedDiagnosticsEnabled()) onEvent(message())
        }
    }

    private class TransitionProjectionView(
        context: android.content.Context,
    ) : View(context) {
        private val painter = CombinedStatusPainter(context)
        private val combinedAnchor = Rect()
        private var endpoints: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints? = null
        private var nativeProgress: Float? = null
        private var model: CombinedStatusRenderModel? = null
        private var colors: CombinedStatusColors? = null
        private var visualSettings = CombinedStatusVisualSettings()

        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setWillNotDraw(false)
        }

        fun setCombinedAnchor(rect: Rect) {
            if (combinedAnchor == rect) return
            combinedAnchor.set(rect)
            invalidate()
        }

        fun setEndpoints(
            value: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints?,
        ) {
            if (
                endpoints?.sourceSystemIcons === value?.sourceSystemIcons &&
                endpoints?.finalPresentationRoot === value?.finalPresentationRoot
            ) {
                return
            }
            endpoints = value
            invalidate()
        }

        fun setNativeProgress(value: Float?) {
            if (nativeProgress == value) return
            nativeProgress = value
            postInvalidateOnAnimation()
        }

        fun setRenderState(
            model: CombinedStatusRenderModel,
            colors: CombinedStatusColors,
        ) {
            this.model = model
            this.colors = colors
            invalidate()
        }

        fun setVisualSettings(settings: CombinedStatusVisualSettings) {
            visualSettings = settings
        }

        fun clearRenderState() {
            model = null
            colors = null
            invalidate()
        }

        fun hasUsableEndpoints(): Boolean =
            endpoints?.let { value ->
                value.sourceSystemIcons.isAttachedToWindow &&
                    value.finalPresentationRoot.isAttachedToWindow &&
                    finalSystemIcons(value.finalPresentationRoot) != null
            } == true &&
                combinedAnchor.width() > 0 &&
                combinedAnchor.height() > 0

        fun commonPeerSlots(): Set<String> {
            val value = endpoints ?: return emptySet()
            val sourceGroup = statusIconGroup(value.sourceSystemIcons) ?: return emptySet()
            val finalIcons =
                finalSystemIcons(value.finalPresentationRoot) ?: return emptySet()
            val targetGroup = statusIconGroup(finalIcons) ?: return emptySet()
            val source = slotViews(sourceGroup).filterValues(::eligiblePeer)
            val target = slotViews(targetGroup).filterValues(::eligiblePeer)
            return source.keys
                .intersect(target.keys)
                .filterTo(linkedSetOf()) { slot ->
                    slot !in SystemUiHomePresentationOwner.representedSlots
                }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val value = endpoints ?: return
            val currentModel = model ?: return
            val currentColors = colors ?: return
            val sourceGroup = statusIconGroup(value.sourceSystemIcons) ?: return
            val finalIcons =
                finalSystemIcons(value.finalPresentationRoot) ?: return
            val targetGroup = statusIconGroup(finalIcons) ?: return
            val progress =
                ControlCenterTransitionProjectionPolicy.geometryProgress(
                    nativeProgress ?: 0f,
                )
            val sourcePeers = slotViews(sourceGroup)
            val targetPeers = slotViews(targetGroup)

            commonPeerSlots().forEach { slot ->
                val source = sourcePeers[slot] ?: return@forEach
                val target = targetPeers[slot] ?: return@forEach
                drawProjectedView(
                    canvas = canvas,
                    source = source,
                    sourceRect = localRect(source),
                    targetRect = localRect(target),
                    progress = progress,
                )
            }

            val sourceCombined = RectF(combinedAnchor)
            val batteryTarget =
                finalIcons
                    .directChildByClass(BATTERY_VIEW_CLASS_NAME)
                    ?.let(::localRect)
                    ?: sourceCombined
            val mobileTarget =
                targetPeers["mobile"]
                    ?.let(::mobileSignalTargetRect)
                    ?: targetPeers["stacked_mobile"]?.let(::mobileSignalTargetRect)
                    ?: sourceCombined
            val centerTarget =
                when (currentModel.centerIndicator) {
                    is CenterIndicator.Wifi ->
                        targetPeers["wifi"]?.let(::localRect)
                    is CenterIndicator.MobileType ->
                        targetPeers["mobile"]
                            ?.let(::mobileTypeTargetRect)
                            ?: targetPeers["stacked_mobile"]?.let(::mobileTypeTargetRect)
                    CenterIndicator.Airplane ->
                        targetPeers["airplane"]?.let(::localRect)
                    is CenterIndicator.NoSim ->
                        targetPeers["no_sim"]?.let(::localRect)
                    CenterIndicator.Empty -> null
                } ?: sourceCombined

            drawCombinedComponent(
                canvas = canvas,
                source = sourceCombined,
                target = batteryTarget,
                progress = progress,
                model = currentModel,
                colors = currentColors,
                component = CombinedStatusPainter.TransitionComponent.BATTERY,
            )
            drawCombinedComponent(
                canvas = canvas,
                source = sourceCombined,
                target = centerTarget,
                progress = progress,
                model = currentModel,
                colors = currentColors,
                component = CombinedStatusPainter.TransitionComponent.CENTER,
            )
            drawCombinedComponent(
                canvas = canvas,
                source = sourceCombined,
                target = mobileTarget,
                progress = progress,
                model = currentModel,
                colors = currentColors,
                component = CombinedStatusPainter.TransitionComponent.MOBILE,
            )
        }

        private fun drawCombinedComponent(
            canvas: Canvas,
            source: RectF,
            target: RectF,
            progress: Float,
            model: CombinedStatusRenderModel,
            colors: CombinedStatusColors,
            component: CombinedStatusPainter.TransitionComponent,
        ) {
            val rect =
                ControlCenterTransitionProjectionPolicy.interpolate(
                    source = source,
                    target = target,
                    progress = progress,
                )
            if (rect.width() <= 0f || rect.height() <= 0f) return
            val save = canvas.save()
            canvas.translate(rect.left, rect.top)
            painter.drawTransitionComponent(
                canvas = canvas,
                width = rect.width().toInt().coerceAtLeast(1),
                height = rect.height().toInt().coerceAtLeast(1),
                model = model,
                colors = colors,
                component = component,
            )
            canvas.restoreToCount(save)
        }

        private fun drawProjectedView(
            canvas: Canvas,
            source: View,
            sourceRect: RectF,
            targetRect: RectF,
            progress: Float,
        ) {
            if (
                source.width <= 0 ||
                source.height <= 0 ||
                sourceRect.width() <= 0f ||
                sourceRect.height() <= 0f ||
                targetRect.width() <= 0f ||
                targetRect.height() <= 0f
            ) {
                return
            }
            val rect =
                ControlCenterTransitionProjectionPolicy.interpolate(
                    source = sourceRect,
                    target = targetRect,
                    progress = progress,
                )
            val save = canvas.save()
            canvas.translate(rect.left, rect.top)
            canvas.scale(
                rect.width() / source.width.toFloat(),
                rect.height() / source.height.toFloat(),
            )
            source.draw(canvas)
            canvas.restoreToCount(save)
        }

        private fun localRect(view: View): RectF {
            val hostLocation = IntArray(2)
            val viewLocation = IntArray(2)
            getLocationOnScreen(hostLocation)
            view.getLocationOnScreen(viewLocation)
            val left = (viewLocation[0] - hostLocation[0]).toFloat()
            val top = (viewLocation[1] - hostLocation[1]).toFloat()
            return RectF(
                left,
                top,
                left + view.width,
                top + view.height,
            )
        }

        private fun mobileTypeTargetRect(view: View): RectF {
            val textCandidate =
                view.descendants()
                    .firstOrNull { candidate ->
                        candidate is android.widget.TextView &&
                            candidate.width > 0 &&
                            candidate.height > 0 &&
                            candidate.visibility == View.VISIBLE &&
                            candidate.text.isNotBlank()
                    }
            return textCandidate?.let(::localRect) ?: localRect(view)
        }

        private fun mobileSignalTargetRect(view: View): RectF {
            val nonText =
                view.descendants()
                    .filter { candidate ->
                        candidate !is android.widget.TextView &&
                            candidate.width > 0 &&
                            candidate.height > 0 &&
                            candidate.visibility == View.VISIBLE
                    }
                    .maxByOrNull { candidate -> candidate.width * candidate.height }
            return nonText?.let(::localRect) ?: localRect(view)
        }

        private fun finalSystemIcons(root: ViewGroup): ViewGroup? =
            root.uniqueDescendantByClass(BATTERY_CONTAINER_CLASS_NAME)

        private fun statusIconGroup(systemIcons: ViewGroup): ViewGroup? =
            systemIcons.directChildByClass(STATUS_ICON_CONTAINER_CLASS_NAME) as? ViewGroup

        private fun slotViews(group: ViewGroup): Map<String, View> =
            buildMap {
                for (index in 0 until group.childCount) {
                    val child = group.getChildAt(index)
                    val slot = NativeParticipantRuntimeAccess.slotOf(child) ?: continue
                    put(slot, child)
                }
            }

        private fun eligiblePeer(view: View): Boolean =
            view.width > 0 &&
                view.height > 0 &&
                NativeParticipantRuntimeAccess.iconVisible(view) != false

        private fun View.descendants(): Sequence<View> =
            sequence {
                if (this@descendants is ViewGroup) {
                    val queue = ArrayDeque<ViewGroup>()
                    queue.add(this@descendants)
                    while (queue.isNotEmpty()) {
                        val group = queue.removeFirst()
                        for (index in 0 until group.childCount) {
                            val child = group.getChildAt(index)
                            yield(child)
                            if (child is ViewGroup) queue.add(child)
                        }
                    }
                }
            }

        private fun ViewGroup.directChildByClass(className: String): View? {
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child.javaClass.name == className) return child
            }
            return null
        }

        private fun ViewGroup.uniqueDescendantByClass(className: String): ViewGroup? {
            var found: ViewGroup? = null
            val queue = ArrayDeque<ViewGroup>()
            queue.add(this)
            while (queue.isNotEmpty()) {
                val parent = queue.removeFirst()
                for (index in 0 until parent.childCount) {
                    val child = parent.getChildAt(index)
                    if (child is ViewGroup) {
                        if (child.javaClass.name == className) {
                            if (found != null && found !== child) return null
                            found = child
                        }
                        queue.add(child)
                    }
                }
            }
            return found
        }

        private companion object {
            const val BATTERY_CONTAINER_CLASS_NAME =
                "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
            const val STATUS_ICON_CONTAINER_CLASS_NAME =
                "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
            const val BATTERY_VIEW_CLASS_NAME =
                "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
        }
    }

    internal object ControlCenterTransitionProjectionPolicy {
        fun geometryProgress(nativeProgress: Float): Float =
            nativeProgress
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 0f

        fun interpolate(
            source: RectF,
            target: RectF,
            progress: Float,
        ): RectF {
            val p = geometryProgress(progress)
            return RectF(
                lerp(source.left, target.left, p),
                lerp(source.top, target.top, p),
                lerp(source.right, target.right, p),
                lerp(source.bottom, target.bottom, p),
            )
        }

        private fun lerp(
            start: Float,
            end: Float,
            progress: Float,
        ): Float = start + (end - start) * progress
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) return child
        }
        return null
    }

    private fun ViewGroup.uniqueDescendant(className: String): ViewGroup? {
        var found: ViewGroup? = null
        val queue = ArrayDeque<ViewGroup>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (child is ViewGroup) {
                    if (child.javaClass.name == className) {
                        if (found != null && found !== child) return null
                        found = child
                    }
                    queue.add(child)
                }
            }
        }
        return found
    }

    internal sealed interface PrearmResult {
        data class Scheduled(
            val reused: Boolean,
        ) : PrearmResult

        data class Failure(
            val reason: String,
        ) : PrearmResult
    }

    internal sealed interface AttachResult {
        data object Ready : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }
}
