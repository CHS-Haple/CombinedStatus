package com.chaners.guiyuan.xposed

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object CombinedStatusControlCenterTransitionOwner {
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val BATTERY_SLOT = "battery"
    private const val MOBILE_SLOT = "mobile"
    private const val STACKED_MOBILE_SLOT = "stacked_mobile"

    private var visible = false
    private var sceneEligible = false
    private var projectionReady = false
    private var nativeProgress: Float? = null
    private var nativeAppearance = false
    private var nativeAppearanceAnimated = false
    private var sourceScene = CombinedStatusSourceScene.UNKNOWN
    private var endpoints: SystemUiPanelTransitionSource.ControlCenterTransitionEndpoints? = null
    private var current: Session? = null

    @Synchronized
    fun onPanelUpdate(update: SystemUiPanelTransitionSource.Update) {
        update.visible?.let { nextVisible ->
            visible = nextVisible
            if (!nextVisible) {
                nativeProgress = null
                nativeAppearance = false
                nativeAppearanceAnimated = false
                sourceScene = CombinedStatusSourceScene.UNKNOWN
                endpoints = null
            }
        }
        update.fraction?.let { nativeProgress = it }
        update.controlCenterAppearance?.let { nativeAppearance = it }
        update.controlCenterAppearanceAnimated?.let { nativeAppearanceAnimated = it }
        update.controlCenterSourceScene?.let { sourceScene = it }
        update.controlCenterTransitionEndpoints?.let { endpoints = it }
        sync("panel-update")
    }

    @Synchronized
    fun setSceneEligible(eligible: Boolean) {
        sceneEligible = eligible
        sync("scene")
    }

    @Synchronized
    fun onProjectionReadinessChanged(ready: Boolean) {
        projectionReady = ready
        sync("projection-readiness")
    }

    @Synchronized
    fun currentDiagnostic(): String =
        current?.diagnostic()
            ?: "transitionOwner=inactive appearance=" + nativeAppearance

    @Synchronized
    fun detach(source: String = "detach") {
        current?.stop(source)
        current = null
        visible = false
        projectionReady = false
        nativeProgress = null
        nativeAppearance = false
        nativeAppearanceAnimated = false
        sourceScene = CombinedStatusSourceScene.UNKNOWN
        endpoints = null
    }

    private fun sync(source: String) {
        val progress = nativeProgress
        val endpoint = endpoints
        val shouldRun =
            visible &&
                sceneEligible &&
                projectionReady &&
                progress != null &&
                progress > 0f &&
                progress <= 1f &&
                endpoint != null &&
                endpoint.fakeRoot.isAttachedToWindow &&
                endpoint.finalRoot.isAttachedToWindow

        if (!shouldRun) {
            current?.stop("inactive:" + source)
            current = null
            return
        }

        val sourceSnapshot =
            CombinedStatusControlCenterRenderSession.currentTransitionSourceSnapshot()
                ?: run {
                    current?.stop("source-unavailable")
                    current = null
                    return
                }
        val root = endpoint.fakeRoot.rootView as? ViewGroup
            ?: run {
                current?.stop("root-view-unavailable")
                current = null
                return
            }
        if (endpoint.finalRoot.rootView !== root) {
            current?.stop("root-view-mismatch")
            current = null
            return
        }

        val existing = current
        if (
            existing == null ||
            !existing.matches(
                root = root,
                fakeRoot = endpoint.fakeRoot,
                finalRoot = endpoint.finalRoot,
                sourceView = sourceSnapshot.view,
                sourceAnchor = sourceSnapshot.anchorView,
            )
        ) {
            existing?.stop("endpoints-changed")
            val created =
                Session.create(
                    root = root,
                    fakeRoot = endpoint.fakeRoot,
                    finalRoot = endpoint.finalRoot,
                    sourceSnapshot = sourceSnapshot,
                ) ?: run {
                    current = null
                    return
                }
            current = created
            created.start()
        }

        current?.update(
            progress = progress,
            sourceSnapshot = sourceSnapshot,
            nativeAppearance = nativeAppearance,
            nativeAppearanceAnimated = nativeAppearanceAnimated,
            transitionReservationEnabled =
                Policy.usesProgressSynchronousReservation(sourceScene),
        )
    }

    internal object Policy {
        fun geometryProgress(raw: Float): Float =
            if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f

        fun motionProgress(raw: Float): Float =
            geometryProgress(raw)

        fun mobileSignalShapeProgress(rawProgress: Float): Float {
            val p = geometryProgress(rawProgress)
            return p * p
        }

        fun unmatchedExitOpacity(rawProgress: Float): Float {
            val remaining = 1f - geometryProgress(rawProgress)
            return remaining * remaining
        }

        fun unmatchedExitScale(rawProgress: Float): Float =
            1f - 0.06f * geometryProgress(rawProgress)

        fun usesProgressSynchronousReservation(
            sourceScene: CombinedStatusSourceScene,
        ): Boolean = sourceScene == CombinedStatusSourceScene.HOME

        data class ReservationSpan(
            val sourceLeft: Float,
            val sourceRight: Float,
            val targetLeft: Float,
            val targetRight: Float,
        )

        fun resolveReservationWidth(
            compactWidthPx: Int,
            spans: List<ReservationSpan>,
            progress: Float,
        ): Int {
            val compact = compactWidthPx.coerceAtLeast(0)
            if (compact == 0) return 0
            val p = geometryProgress(progress)
            var left = -compact.toFloat()
            var right = 0f
            spans.forEach { span ->
                val currentLeft =
                    span.sourceLeft +
                        (span.targetLeft - span.sourceLeft) * p
                val currentRight =
                    span.sourceRight +
                        (span.targetRight - span.sourceRight) * p
                left = min(left, currentLeft)
                right = maxOf(right, currentRight)
            }
            return kotlin.math.ceil((right - left).coerceAtLeast(compact.toFloat())).toInt()
        }

        fun interpolateGeometry(
            source: FloatArray,
            target: FloatArray,
            progress: Float,
        ): FloatArray {
            require(source.size == 6 && target.size == 6)
            val p = progress.coerceIn(0f, 1f)
            return FloatArray(6) { index ->
                source[index] + (target[index] - source[index]) * p
            }
        }

        fun interpolateSimilarityGeometry(
            source: FloatArray,
            target: FloatArray,
            progress: Float,
            scalePolicy: CombinedStatusPainter.TransitionScalePolicy,
        ): FloatArray {
            require(source.size == 6 && target.size == 6)
            val p = progress.coerceIn(0f, 1f)
            val sourceWidth = vectorLength(source[2], source[3])
            val sourceHeight = vectorLength(source[4], source[5])
            val targetWidth = vectorLength(target[2], target[3])
            val targetHeight = vectorLength(target[4], target[5])
            if (
                sourceWidth <= 0f ||
                sourceHeight <= 0f ||
                targetWidth <= 0f ||
                targetHeight <= 0f
            ) {
                return source.copyOf()
            }
            val rawTargetScale =
                min(
                    targetWidth / sourceWidth,
                    targetHeight / sourceHeight,
                )
            val targetScale =
                when (scalePolicy) {
                    CombinedStatusPainter.TransitionScalePolicy.TARGET ->
                        rawTargetScale

                    CombinedStatusPainter.TransitionScalePolicy.SHRINK_ONLY ->
                        min(rawTargetScale, 1f)
                }
            val scale = 1f + (targetScale - 1f) * p
            return floatArrayOf(
                source[0] + (target[0] - source[0]) * p,
                source[1] + (target[1] - source[1]) * p,
                source[2] * scale,
                source[3] * scale,
                source[4] * scale,
                source[5] * scale,
            )
        }

        fun composeSourceGeometry(
            positionAuthority: FloatArray,
            basisAuthority: FloatArray,
        ): FloatArray {
            require(positionAuthority.size == 6 && basisAuthority.size == 6)
            return floatArrayOf(
                positionAuthority[0],
                positionAuthority[1],
                basisAuthority[2],
                basisAuthority[3],
                basisAuthority[4],
                basisAuthority[5],
            )
        }

        fun semanticFallbackBounds(
            preferredChildEntries: List<String>,
            isRtl: Boolean,
        ): CombinedStatusPainter.TransitionNormalizedBounds? {
            val logical =
                when {
                    preferredChildEntries.any { entry ->
                        entry == "mobile_type_single" || entry == "mobile_type"
                    } ->
                        CombinedStatusPainter.TransitionNormalizedBounds(
                            left = 0f,
                            top = 0f,
                            right = 0.42f,
                            bottom = 1f,
                        )

                    preferredChildEntries.contains("mobile_signal") ->
                        CombinedStatusPainter.TransitionNormalizedBounds(
                            left = 0.48f,
                            top = 0f,
                            right = 1f,
                            bottom = 1f,
                        )

                    preferredChildEntries.contains("wifi_signal") ->
                        CombinedStatusPainter.TransitionNormalizedBounds(
                            left = 0f,
                            top = 0f,
                            right = 1f,
                            bottom = 1f,
                        )

                    else -> null
                } ?: return null
            if (!isRtl || (logical.left == 0f && logical.right == 1f)) {
                return logical
            }
            return CombinedStatusPainter.TransitionNormalizedBounds(
                left = 1f - logical.right,
                top = logical.top,
                right = 1f - logical.left,
                bottom = logical.bottom,
            )
        }

        fun isHyperCeilerDualSignalStructure(
            nativeSignalVisible: Boolean,
            candidateVisible: Boolean,
            candidateHasResourceEntry: Boolean,
            directChildCount: Int,
            directImageChildCount: Int,
        ): Boolean =
            !nativeSignalVisible &&
                candidateVisible &&
                !candidateHasResourceEntry &&
                directChildCount >= 2 &&
                directChildCount == directImageChildCount

        fun scaleGeometry(
            source: FloatArray,
            scale: Float,
        ): FloatArray {
            require(source.size == 6)
            val normalized = scale.coerceAtLeast(0f)
            return floatArrayOf(
                source[0],
                source[1],
                source[2] * normalized,
                source[3] * normalized,
                source[4] * normalized,
                source[5] * normalized,
            )
        }

        fun relativeGeometryHeight(
            target: FloatArray,
            current: FloatArray,
        ): Float? {
            require(target.size == 6)
            require(current.size == 6)
            val targetHeight = vectorLength(target[4], target[5])
            val currentHeight = vectorLength(current[4], current[5])
            if (targetHeight <= 0f || currentHeight <= 0f) return null
            return targetHeight / currentHeight
        }

        private fun vectorLength(
            x: Float,
            y: Float,
        ): Float = sqrt(x * x + y * y)

        fun componentGeometry(
            parentGeometry: FloatArray,
            parentWidth: Int,
            parentHeight: Int,
            bounds: CombinedStatusPainter.TransitionBounds,
        ): FloatArray? {
            if (
                parentGeometry.size != 6 ||
                parentWidth <= 0 ||
                parentHeight <= 0 ||
                bounds.width <= 0f ||
                bounds.height <= 0f
            ) {
                return null
            }
            val normalizedCenterX =
                bounds.centerX / parentWidth.toFloat() - 0.5f
            val normalizedCenterY =
                bounds.centerY / parentHeight.toFloat() - 0.5f
            val widthScale = bounds.width / parentWidth.toFloat()
            val heightScale = bounds.height / parentHeight.toFloat()
            return floatArrayOf(
                parentGeometry[0] +
                    parentGeometry[2] * normalizedCenterX +
                    parentGeometry[4] * normalizedCenterY,
                parentGeometry[1] +
                    parentGeometry[3] * normalizedCenterX +
                    parentGeometry[5] * normalizedCenterY,
                parentGeometry[2] * widthScale,
                parentGeometry[3] * widthScale,
                parentGeometry[4] * heightScale,
                parentGeometry[5] * heightScale,
            )
        }
    }

    private class Session(
        root: ViewGroup,
        fakeRoot: ViewGroup,
        finalRoot: ViewGroup,
        sourceView: View,
        sourceAnchor: View,
        sourceSnapshot: CombinedStatusControlCenterRenderSession.TransitionSourceSnapshot,
        private val finalStatusIcons: ViewGroup,
        private val finalBattery: View,
    ) {
        private val rootRef = WeakReference(root)
        private val fakeRootRef = WeakReference(fakeRoot)
        private val finalRootRef = WeakReference(finalRoot)
        private val sourceViewRef = WeakReference(sourceView)
        private val sourceAnchorRef = WeakReference(sourceAnchor)
        private val painter = CombinedStatusPainter(root.context)
        private val drawable = TransitionDrawable(this)
        private val sourceMask = MaskState(
            view = WeakReference(sourceView),
            nativeClip = sourceView.clipBounds?.let(::Rect),
            appliedClip = Rect(0, 0, 0, 0),
        )
        private val mobileSubIdCache = WeakHashMap<View, Int?>()
        private val targetCache = HashMap<TargetCacheKey, TargetWitness>()

        private var currentSnapshot = sourceSnapshot
        private var progress = 0f
        private var nativeAppearance = false
        private var nativeAppearanceAnimated = false
        private var started = false
        private var lastStateVersion = sourceSnapshot.stateVersion
        private var lastWitnessSummary = "pending"
        private var cachedNativePeerTint: Int? = null
        private var frozenReservationSpans: List<Policy.ReservationSpan>? = null
        private var lastReservationWidthPx: Int? = null
        private var transitionReservationEnabled = false

        private val preDrawListener =
            ViewTreeObserver.OnPreDrawListener {
                val rootView = rootRef.get()
                val fake = fakeRootRef.get()
                val final = finalRootRef.get()
                val source = sourceViewRef.get()
                val sourceAnchor = sourceAnchorRef.get()
                if (
                    rootView == null ||
                    fake == null ||
                    final == null ||
                    source == null ||
                    sourceAnchor == null ||
                    !rootView.isAttachedToWindow ||
                    !fake.isAttachedToWindow ||
                    !final.isAttachedToWindow ||
                    !source.isAttachedToWindow ||
                    !sourceAnchor.isAttachedToWindow
                ) {
                    CombinedStatusControlCenterTransitionOwner.detach("pre-draw-detached")
                    return@OnPreDrawListener true
                }

                val latest =
                    CombinedStatusControlCenterRenderSession.currentTransitionSourceSnapshot()
                if (
                    latest != null &&
                    latest.view === source &&
                    latest.anchorView === sourceAnchor
                ) {
                    currentSnapshot = latest
                    lastStateVersion = latest.stateVersion
                }
                if (cachedNativePeerTint == null) {
                    refreshNativePeerTint()
                }
                syncTransitionReservation()
                drawable.setBounds(0, 0, rootView.width, rootView.height)
                drawable.invalidateSelf()
                true
            }

        fun diagnostic(): String =
            "transitionOwner={progress=" + progress +
                ",appearance=" + nativeAppearance +
                ",appearanceAnimated=" + nativeAppearanceAnimated +
                ",nativePeers=systemui" +
                ",sourceAnchor=" + (sourceAnchorRef.get()?.javaClass?.simpleName ?: "none") +
                ",sourceStateVersion=" + lastStateVersion +
                ",root=" + (rootRef.get()?.javaClass?.simpleName ?: "none") +
                ",fake=" + (fakeRootRef.get()?.javaClass?.simpleName ?: "none") +
                ",final=" + (finalRootRef.get()?.javaClass?.simpleName ?: "none") +
                ",witness=" + lastWitnessSummary +
                ",reservation=" + (lastReservationWidthPx ?: -1) +
                ",reservationMode=" +
                (if (transitionReservationEnabled) "progress-padding" else "native-peer-motion") +
                "}"

        fun matches(
            root: ViewGroup,
            fakeRoot: ViewGroup,
            finalRoot: ViewGroup,
            sourceView: View,
            sourceAnchor: View,
        ): Boolean =
            rootRef.get() === root &&
                fakeRootRef.get() === fakeRoot &&
                finalRootRef.get() === finalRoot &&
                sourceViewRef.get() === sourceView &&
                sourceAnchorRef.get() === sourceAnchor

        fun start() {
            if (started) return
            val rootView = rootRef.get() ?: return
            val source = sourceViewRef.get() ?: return
            started = true
            source.clipBounds = sourceMask.appliedClip
            refreshNativePeerTint()
            syncTransitionReservation()
            rootView.overlay.add(drawable)
            rootView.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            drawable.setBounds(0, 0, rootView.width, rootView.height)
            drawable.invalidateSelf()
        }

        fun update(
            progress: Float,
            sourceSnapshot: CombinedStatusControlCenterRenderSession.TransitionSourceSnapshot,
            nativeAppearance: Boolean,
            nativeAppearanceAnimated: Boolean,
            transitionReservationEnabled: Boolean,
        ) {
            val appearanceChanged =
                this.nativeAppearance != nativeAppearance ||
                    this.nativeAppearanceAnimated != nativeAppearanceAnimated
            this.progress = progress.coerceIn(0f, 1f)
            this.currentSnapshot = sourceSnapshot
            this.nativeAppearance = nativeAppearance
            this.nativeAppearanceAnimated = nativeAppearanceAnimated
            this.transitionReservationEnabled = transitionReservationEnabled
            if (appearanceChanged) {
                refreshNativePeerTint()
            }
            syncTransitionReservation()
            drawable.invalidateSelf()
        }

        fun stop(source: String) {
            SystemUiHomePresentationOwner.clearControlCenterTransitionReservation(
                "transition-" + source,
            )
            if (!started) return
            started = false
            val rootView = rootRef.get()
            if (rootView?.viewTreeObserver?.isAlive == true) {
                rootView.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
            }
            rootView?.overlay?.remove(drawable)
            sourceViewRef.get()?.let { sourceView ->
                if (sourceView.clipBounds == sourceMask.appliedClip) {
                    sourceView.clipBounds = sourceMask.nativeClip?.let(::Rect)
                }
            }
        }

        fun draw(canvas: Canvas) {
            val rootView = rootRef.get() ?: return
            val fake = fakeRootRef.get() ?: return
            val sourceView = sourceViewRef.get() ?: return
            val sourceAnchor = sourceAnchorRef.get() ?: return
            if (sourceView.width <= 0 || sourceView.height <= 0) return

            val sourcePositionSample =
                sample(
                    view = sourceAnchor,
                    root = rootView,
                ) ?: return
            val sourceBasisSample =
                sample(
                    view = sourceView,
                    root = rootView,
                ) ?: return
            val sourceParentGeometry =
                Policy.composeSourceGeometry(
                    positionAuthority = sourcePositionSample.geometry,
                    basisAuthority = sourceBasisSample.geometry,
                )
            val model = currentSnapshot.model
            val specs =
                painter.transitionComponentSpecs(
                    width = sourceView.width,
                    height = sourceView.height,
                    model = model,
                )
            if (specs.isEmpty()) return

            val nativeProgress = Policy.geometryProgress(progress)
            val motionProgress = Policy.motionProgress(nativeProgress)
            val opacity = endpointAlpha(fake)
            val mobileSignalShapeProgress =
                Policy.mobileSignalShapeProgress(nativeProgress)
            if (opacity <= 0f) return

            val transitionColors =
                cachedNativePeerTint
                    ?.let { tint ->
                        currentSnapshot.colors.copy(
                            centerTint = tint,
                            mobileTint = tint,
                        )
                    }
                    ?: currentSnapshot.colors

            val preferredMobileSubId =
                CombinedStatusPresentationStateStore
                    .snapshot()
                    .mobilePresentation
                    ?.presentationRootSubscriptionId
            val refreshWitnessDiagnostic =
                lastWitnessSummary == "pending" ||
                    lastWitnessSummary.contains("unresolved") ||
                    lastWitnessSummary.contains(":0x0")
            val witnessDescriptions =
                if (refreshWitnessDiagnostic) {
                    ArrayList<String>(specs.size)
                } else {
                    null
                }

            specs.forEach { spec ->
                val sourceGeometry =
                    Policy.componentGeometry(
                        parentGeometry = sourceParentGeometry,
                        parentWidth = sourceView.width,
                        parentHeight = sourceView.height,
                        bounds = spec.sourceBounds,
                    ) ?: return@forEach
                val witness =
                    resolveTarget(
                        target = spec.target,
                        preferredMobileSubId = preferredMobileSubId,
                    )
                val targetGeometry =
                    witness?.let { target ->
                        resolveTargetGeometry(
                            witness = target,
                            root = rootView,
                            sourceGeometry = sourceGeometry,
                            targetOpticalBounds = spec.targetOpticalBounds,
                        )
                    }
                val geometry =
                    if (targetGeometry != null) {
                        Policy.interpolateSimilarityGeometry(
                            source = sourceGeometry,
                            target = targetGeometry,
                            progress = motionProgress,
                            scalePolicy = spec.scalePolicy,
                        )
                    } else {
                        Policy.scaleGeometry(
                            source = sourceGeometry,
                            scale = Policy.unmatchedExitScale(nativeProgress),
                        )
                    }
                val componentOpacity =
                    if (targetGeometry != null) {
                        opacity
                    } else {
                        opacity * Policy.unmatchedExitOpacity(nativeProgress)
                    }
                if (componentOpacity <= 0f) return@forEach
                val matrix =
                    matrixForBoundsGeometry(
                        geometry = geometry,
                        bounds = spec.sourceBounds,
                    ) ?: return@forEach

                val save =
                    canvas.saveLayerAlpha(
                        null,
                        (255f * componentOpacity.coerceIn(0f, 1f)).roundToInt(),
                    )
                canvas.concat(matrix)
                painter.drawTransitionComponent(
                    canvas = canvas,
                    width = sourceView.width,
                    height = sourceView.height,
                    model = model,
                    colors = transitionColors,
                    component = spec.component,
                    shapePolicy = spec.shapePolicy,
                    opacity = 1f,
                    motionProgress = motionProgress,
                    shapeProgress =
                        when (spec.shapePolicy) {
                            CombinedStatusPainter.TransitionShapePolicy.BATTERY_FOLD ->
                                motionProgress

                            CombinedStatusPainter.TransitionShapePolicy.MOBILE_SIGNAL ->
                                mobileSignalShapeProgress

                            CombinedStatusPainter.TransitionShapePolicy.RIGID ->
                                0f
                        },
                    mobileTargetHeightRatio =
                        if (
                            spec.shapePolicy ==
                            CombinedStatusPainter.TransitionShapePolicy.MOBILE_SIGNAL &&
                            targetGeometry != null
                        ) {
                            Policy.relativeGeometryHeight(
                                target = targetGeometry,
                                current = sourceGeometry,
                            )
                        } else {
                            null
                        },
                )
                canvas.restoreToCount(save)

                witnessDescriptions?.add(
                    witness?.summary ?: (spec.component.name.lowercase() + ":unresolved"),
                )
            }

            witnessDescriptions?.let { descriptions ->
                lastWitnessSummary = descriptions.joinToString("|")
            }
        }

        private fun refreshNativePeerTint() {
            cachedNativePeerTint =
                SystemUiNativeNetworkSuppressionOwner
                    .currentAppliedStatusIconTintForGroup(finalStatusIcons)
        }

        private fun syncTransitionReservation() {
            if (!transitionReservationEnabled) {
                if (lastReservationWidthPx != null) {
                    SystemUiHomePresentationOwner.clearControlCenterTransitionReservation(
                        "transition-source-native-peer-motion",
                    )
                    lastReservationWidthPx = null
                }
                return
            }
            val source = sourceViewRef.get() ?: return
            if (source.width <= 0) return
            val spans =
                frozenReservationSpans
                    ?: resolveReservationSpans()
                        ?.also { resolved ->
                            frozenReservationSpans = resolved
                        }
                    ?: return
            val requestedWidth =
                Policy.resolveReservationWidth(
                    compactWidthPx = source.width,
                    spans = spans,
                    progress = progress,
                )
            if (lastReservationWidthPx == requestedWidth) return
            if (
                SystemUiHomePresentationOwner.updateControlCenterTransitionReservation(
                    requestedSlotWidthPx = requestedWidth,
                )
            ) {
                lastReservationWidthPx = requestedWidth
            }
        }

        private fun resolveReservationSpans(): List<Policy.ReservationSpan>? {
            val source = sourceViewRef.get() ?: return null
            if (source.width <= 0 || source.height <= 0) return null
            if (!isUsableSlotView(finalBattery)) return null

            val specs =
                painter.transitionComponentSpecs(
                    width = source.width,
                    height = source.height,
                    model = currentSnapshot.model,
                )
            if (specs.isEmpty()) return null

            val sourceRtl =
                source.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val targetRtl =
                finalBattery.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (sourceRtl != targetRtl) return null

            val batteryLocation = IntArray(2)
            finalBattery.getLocationInWindow(batteryLocation)
            val finalEndPhysical =
                if (targetRtl) {
                    batteryLocation[0].toFloat()
                } else {
                    (batteryLocation[0] + finalBattery.width).toFloat()
                }
            fun logicalTargetX(physicalX: Float): Float =
                (if (targetRtl) -physicalX else physicalX) -
                    (if (targetRtl) -finalEndPhysical else finalEndPhysical)
            fun logicalSourceX(localX: Float): Float {
                val sourceEnd =
                    if (sourceRtl) 0f else source.width.toFloat()
                return (if (sourceRtl) -localX else localX) -
                    (if (sourceRtl) -sourceEnd else sourceEnd)
            }

            val preferredMobileSubId =
                CombinedStatusPresentationStateStore
                    .snapshot()
                    .mobilePresentation
                    ?.presentationRootSubscriptionId

            val result = ArrayList<Policy.ReservationSpan>(specs.size)
            specs.forEach { spec ->
                val witness =
                    resolveTarget(
                        target = spec.target,
                        preferredMobileSubId = preferredMobileSubId,
                    ) ?: return@forEach
                val slot = witness.slotView
                if (!isUsableSlotView(slot)) return@forEach
                val targetLocation = IntArray(2)
                slot.getLocationInWindow(targetLocation)

                val sourceA = logicalSourceX(spec.sourceBounds.left)
                val sourceB = logicalSourceX(spec.sourceBounds.right)
                val targetA = logicalTargetX(targetLocation[0].toFloat())
                val targetB =
                    logicalTargetX(
                        (targetLocation[0] + slot.width).toFloat(),
                    )
                result +=
                    Policy.ReservationSpan(
                        sourceLeft = min(sourceA, sourceB),
                        sourceRight = maxOf(sourceA, sourceB),
                        targetLeft = min(targetA, targetB),
                        targetRight = maxOf(targetA, targetB),
                    )
            }
            return result.takeIf { it.isNotEmpty() }
        }

        private fun resolveTarget(
            target: CombinedStatusPainter.TransitionTarget,
            preferredMobileSubId: Int?,
        ): TargetWitness? {
            val mobileSubId =
                preferredMobileSubId.takeIf {
                    target is CombinedStatusPainter.TransitionTarget.Slots &&
                        target.preferredSlots.any { slot ->
                            slot == MOBILE_SLOT || slot == STACKED_MOBILE_SLOT
                        }
                }
            val key =
                TargetCacheKey(
                    target = target,
                    mobileSubId = mobileSubId,
                )
            val opticalRequired =
                target is CombinedStatusPainter.TransitionTarget.Slots &&
                    target.preferredChildEntries.isNotEmpty()

            targetCache[key]
                ?.takeIf { witness ->
                    val slotStillSemantic =
                        target !is CombinedStatusPainter.TransitionTarget.Slots ||
                            witness.slotView.visibility == View.VISIBLE
                    witness.slotView.isAttachedToWindow &&
                        slotStillSemantic &&
                        (
                            !opticalRequired ||
                                witness.opticalView?.let(::isReliableSemanticTarget) == true ||
                                witness.fallbackBounds != null
                        )
                }
                ?.let { return it }

            val resolved =
                when (target) {
                    CombinedStatusPainter.TransitionTarget.BatteryIcon ->
                        TargetWitness(
                            slot = BATTERY_SLOT,
                            slotView = finalBattery,
                            opticalView = resolveBatteryIconTarget(finalBattery),
                            subscriptionId = null,
                            requiresOpticalGeometry = false,
                            fallbackBounds = null,
                            opticalSource = "battery",
                        ).takeIf { witness -> isUsableSlotView(witness.slotView) }

                    is CombinedStatusPainter.TransitionTarget.Slots ->
                        target.preferredSlots.firstNotNullOfOrNull { slot ->
                            val slotRoot =
                                selectSlotView(
                                    candidates = slotViews(finalStatusIcons, slot),
                                    preferredMobileSubId =
                                        mobileSubId.takeIf {
                                            slot == MOBILE_SLOT ||
                                                slot == STACKED_MOBILE_SLOT
                                        },
                                ) ?: return@firstNotNullOfOrNull null

                            val nativeOptical =
                                target.preferredChildEntries
                                    .firstNotNullOfOrNull { entry ->
                                        findDescendantByResourceEntry(
                                            root = slotRoot,
                                            entryName = entry,
                                        )?.takeIf(::isReliableSemanticTarget)
                                    }
                            val compatibilityOptical =
                                if (nativeOptical == null) {
                                    resolveCompatibilityOpticalTarget(
                                        slotRoot = slotRoot,
                                        preferredChildEntries = target.preferredChildEntries,
                                    )
                                } else {
                                    null
                                }
                            val optical = nativeOptical ?: compatibilityOptical?.view
                            val fallbackBounds =
                                if (opticalRequired && optical == null) {
                                    Policy.semanticFallbackBounds(
                                        preferredChildEntries = target.preferredChildEntries,
                                        isRtl =
                                            slotRoot.layoutDirection ==
                                                View.LAYOUT_DIRECTION_RTL,
                                    )
                                } else {
                                    null
                                }
                            if (opticalRequired && optical == null && fallbackBounds == null) {
                                return@firstNotNullOfOrNull null
                            }

                            TargetWitness(
                                slot = slot,
                                slotView = slotRoot,
                                opticalView = optical,
                                subscriptionId = readMobileSubId(slotRoot),
                                requiresOpticalGeometry = opticalRequired,
                                fallbackBounds = fallbackBounds,
                                opticalSource =
                                    when {
                                        nativeOptical != null -> "native"
                                        compatibilityOptical != null -> compatibilityOptical.source
                                        fallbackBounds != null -> "slot-estimate"
                                        else -> "slot"
                                    },
                            )
                        }
                }

            if (resolved != null) {
                targetCache[key] = resolved
            } else {
                targetCache.remove(key)
            }
            return resolved
        }

        private fun resolveTargetGeometry(
            witness: TargetWitness,
            root: View,
            sourceGeometry: FloatArray,
            targetOpticalBounds: CombinedStatusPainter.TransitionNormalizedBounds?,
        ): FloatArray? {
            val opticalView = witness.opticalView
            if (opticalView != null) {
                val opticalSample = sample(opticalView, root)
                if (opticalSample != null) {
                    return targetOpticalBounds
                        ?.let { bounds ->
                            Policy.componentGeometry(
                                parentGeometry = opticalSample.geometry,
                                parentWidth = opticalView.width,
                                parentHeight = opticalView.height,
                                bounds =
                                    CombinedStatusPainter.TransitionBounds(
                                        left = bounds.left * opticalView.width,
                                        top = bounds.top * opticalView.height,
                                        right = bounds.right * opticalView.width,
                                        bottom = bounds.bottom * opticalView.height,
                                    ),
                            )
                        }
                        ?: opticalSample.geometry
                }

                syntheticOpticalGeometry(
                    witness = witness,
                    root = root,
                    targetOpticalBounds = targetOpticalBounds,
                )?.let { return it }

            }

            val slotSample = sample(witness.slotView, root) ?: return sourceGeometry
            val slot = witness.slotView
            val contentBounds =
                CombinedStatusPainter.TransitionBounds(
                    left = slot.paddingLeft.toFloat(),
                    top = slot.paddingTop.toFloat(),
                    right = (slot.width - slot.paddingRight).toFloat(),
                    bottom = (slot.height - slot.paddingBottom).toFloat(),
                )
            val targetBounds =
                witness.fallbackBounds
                    ?.let { fallback ->
                        CombinedStatusPainter.TransitionBounds(
                            left =
                                contentBounds.left +
                                    contentBounds.width * fallback.left,
                            top =
                                contentBounds.top +
                                    contentBounds.height * fallback.top,
                            right =
                                contentBounds.left +
                                    contentBounds.width * fallback.right,
                            bottom =
                                contentBounds.top +
                                    contentBounds.height * fallback.bottom,
                        )
                    }
                    ?: if (witness.requiresOpticalGeometry) {
                        return null
                    } else {
                        contentBounds
                    }
            return Policy.componentGeometry(
                parentGeometry = slotSample.geometry,
                parentWidth = slot.width,
                parentHeight = slot.height,
                bounds = targetBounds,
            ) ?: slotSample.geometry
        }

        private fun syntheticOpticalGeometry(
            witness: TargetWitness,
            root: View,
            targetOpticalBounds: CombinedStatusPainter.TransitionNormalizedBounds?,
        ): FloatArray? {
            val image = witness.opticalView as? ImageView ?: return null
            val drawable = image.drawable ?: return null
            val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
            val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
            val slot = witness.slotView
            val slotSample = sample(slot, root) ?: return null
            val contentLeft = slot.paddingLeft.toFloat()
            val contentTop = slot.paddingTop.toFloat()
            val contentRight = (slot.width - slot.paddingRight).toFloat()
            val contentBottom = (slot.height - slot.paddingBottom).toFloat()
            val contentWidth = (contentRight - contentLeft).coerceAtLeast(1f)
            val contentHeight = (contentBottom - contentTop).coerceAtLeast(1f)
            val fit =
                min(
                    contentWidth / intrinsicWidth.toFloat(),
                    contentHeight / intrinsicHeight.toFloat(),
                ).coerceAtMost(1f)
            val frameWidth = intrinsicWidth * fit
            val frameHeight = intrinsicHeight * fit
            val localCenterX =
                if (image.left != 0 || image.right != 0) {
                    image.left + frameWidth / 2f
                } else {
                    (contentLeft + contentRight) / 2f
                }
            val localCenterY =
                if (image.top != 0 || image.bottom != 0) {
                    image.top + frameHeight / 2f
                } else {
                    (contentTop + contentBottom) / 2f
                }
            val optical = targetOpticalBounds
            val frameLeft = localCenterX - frameWidth / 2f
            val frameTop = localCenterY - frameHeight / 2f
            val localBounds =
                if (optical != null) {
                    CombinedStatusPainter.TransitionBounds(
                        left = frameLeft + optical.left * frameWidth,
                        top = frameTop + optical.top * frameHeight,
                        right = frameLeft + optical.right * frameWidth,
                        bottom = frameTop + optical.bottom * frameHeight,
                    )
                } else {
                    CombinedStatusPainter.TransitionBounds(
                        left = frameLeft,
                        top = frameTop,
                        right = frameLeft + frameWidth,
                        bottom = frameTop + frameHeight,
                    )
                }
            return Policy.componentGeometry(
                parentGeometry = slotSample.geometry,
                parentWidth = slot.width,
                parentHeight = slot.height,
                bounds = localBounds,
            )
        }

        private fun resolveBatteryIconTarget(battery: View): View? {
            val fieldNames =
                if (readIntField(battery, "mBatteryStyle") == 1) {
                    listOf("mHollowBatteryIconView", "mBatteryIconView")
                } else {
                    listOf("mBatteryIconView", "mHollowBatteryIconView")
                }
            return fieldNames.firstNotNullOfOrNull { fieldName ->
                readViewField(
                    target = battery,
                    fieldName = fieldName,
                )
            }
                ?: findDescendantByResourceEntry(battery, "battery_icon")
                ?: findDescendantByResourceEntry(battery, "battery_icon_container")
        }

        private fun readViewField(
            target: Any,
            fieldName: String,
        ): View? =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { field -> field.name == fieldName }
                }
                .firstOrNull()
                ?.let { field ->
                    runCatching {
                        field.isAccessible = true
                        field.get(target) as? View
                    }.getOrNull()
                }

        private fun readIntField(
            target: Any,
            fieldName: String,
        ): Int? =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { field -> field.name == fieldName }
                }
                .firstOrNull()
                ?.let { field ->
                    runCatching {
                        field.isAccessible = true
                        field.getInt(target)
                    }.getOrNull()
                }

        private fun resolveCompatibilityOpticalTarget(
            slotRoot: View,
            preferredChildEntries: List<String>,
        ): CompatibilityOpticalTarget? {
            if (!preferredChildEntries.contains("mobile_signal")) return null
            val signalContainer =
                findDescendantByResourceEntry(
                    root = slotRoot,
                    entryName = "mobile_signal_container",
                ) as? ViewGroup ?: return null
            val nativeSignal =
                findDescendantByResourceEntry(
                    root = slotRoot,
                    entryName = "mobile_signal",
                ) ?: return null

            for (index in 0 until signalContainer.childCount) {
                val candidate = signalContainer.getChildAt(index) as? FrameLayout ?: continue
                if (!isReliableSemanticTarget(candidate)) continue
                var imageChildren = 0
                for (childIndex in 0 until candidate.childCount) {
                    if (candidate.getChildAt(childIndex) is ImageView) {
                        imageChildren += 1
                    }
                }
                val isHyperCeilerDual =
                    Policy.isHyperCeilerDualSignalStructure(
                        nativeSignalVisible = nativeSignal.visibility == View.VISIBLE,
                        candidateVisible = candidate.visibility == View.VISIBLE,
                        candidateHasResourceEntry =
                            NativeParticipantRuntimeAccess.resourceEntryName(candidate) != null,
                        directChildCount = candidate.childCount,
                        directImageChildCount = imageChildren,
                    )
                if (isHyperCeilerDual) {
                    return CompatibilityOpticalTarget(
                        view = candidate,
                        source = "hyperceiler-dual-signal",
                    )
                }
            }
            return null
        }

        private fun readMobileSubId(view: View): Int? {
            if (mobileSubIdCache.containsKey(view)) {
                return mobileSubIdCache[view]
            }
            val resolved =
                generateSequence<Class<*>>(view.javaClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        clazz.declaredFields.firstOrNull { field -> field.name == "subId" }
                    }
                    .firstOrNull()
                    ?.let { field ->
                        runCatching {
                            field.isAccessible = true
                            field.getInt(view)
                        }.getOrNull()
                    }
            mobileSubIdCache[view] = resolved
            return resolved
        }

        private fun selectSlotView(
            candidates: List<View>,
            preferredMobileSubId: Int?,
        ): View? {
            if (preferredMobileSubId != null) {
                candidates.firstOrNull { view ->
                    readMobileSubId(view) == preferredMobileSubId &&
                        view.visibility == View.VISIBLE &&
                        isUsableSlotView(view)
                }?.let { return it }
            }
            return candidates.firstOrNull { view ->
                view.visibility == View.VISIBLE && isUsableSlotView(view)
            }
        }

        private fun isReliableSemanticTarget(view: View): Boolean =
            view.visibility == View.VISIBLE &&
                view.isAttachedToWindow &&
                view.width > 0 &&
                view.height > 0

        private fun isUsableSlotView(view: View): Boolean =
            view.isAttachedToWindow &&
                view.width > 0 &&
                view.height > 0

        private fun findDescendantByResourceEntry(
            root: View,
            entryName: String,
        ): View? {
            if (NativeParticipantRuntimeAccess.resourceEntryName(root) == entryName) {
                return root
            }
            val group = root as? ViewGroup ?: return null
            for (index in 0 until group.childCount) {
                findDescendantByResourceEntry(
                    root = group.getChildAt(index),
                    entryName = entryName,
                )?.let { return it }
            }
            return null
        }

        private fun endpointAlpha(view: View): Float {
            var current: View? = view
            var alpha = 1f
            val rootView = view.rootView
            while (current != null && current !== rootView) {
                if (current.visibility != View.VISIBLE) return 0f
                alpha *= current.alpha
                current = current.parent as? View
            }
            return alpha.coerceIn(0f, 1f)
        }

        private fun sample(
            view: View,
            root: View,
        ): Sample? {
            if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
                return null
            }
            val matrix = Matrix()
            view.transformMatrixToGlobal(matrix)
            root.transformMatrixToLocal(matrix)
            val values = FloatArray(9)
            matrix.getValues(values)
            return Sample(
                geometry =
                    floatArrayOf(
                        ((values[Matrix.MSCALE_X] * view.width) +
                            (values[Matrix.MSKEW_X] * view.height)) / 2f +
                            values[Matrix.MTRANS_X],
                        ((values[Matrix.MSKEW_Y] * view.width) +
                            (values[Matrix.MSCALE_Y] * view.height)) / 2f +
                            values[Matrix.MTRANS_Y],
                        values[Matrix.MSCALE_X] * view.width,
                        values[Matrix.MSKEW_Y] * view.width,
                        values[Matrix.MSKEW_X] * view.height,
                        values[Matrix.MSCALE_Y] * view.height,
                    ),
            )
        }

        private fun matrixForBoundsGeometry(
            geometry: FloatArray,
            bounds: CombinedStatusPainter.TransitionBounds,
        ): Matrix? {
            if (
                geometry.size != 6 ||
                bounds.width <= 0f ||
                bounds.height <= 0f
            ) {
                return null
            }
            val values = FloatArray(9)
            values[Matrix.MSCALE_X] = geometry[2] / bounds.width
            values[Matrix.MSKEW_X] = geometry[4] / bounds.height
            values[Matrix.MTRANS_X] =
                geometry[0] -
                    values[Matrix.MSCALE_X] * bounds.centerX -
                    values[Matrix.MSKEW_X] * bounds.centerY
            values[Matrix.MSKEW_Y] = geometry[3] / bounds.width
            values[Matrix.MSCALE_Y] = geometry[5] / bounds.height
            values[Matrix.MTRANS_Y] =
                geometry[1] -
                    values[Matrix.MSKEW_Y] * bounds.centerX -
                    values[Matrix.MSCALE_Y] * bounds.centerY
            values[Matrix.MPERSP_0] = 0f
            values[Matrix.MPERSP_1] = 0f
            values[Matrix.MPERSP_2] = 1f
            return Matrix().apply { setValues(values) }
        }

        private fun matrixForGeometry(
            geometry: FloatArray,
            width: Int,
            height: Int,
        ): Matrix? {
            if (geometry.size != 6 || width <= 0 || height <= 0) return null
            val values = FloatArray(9)
            values[Matrix.MSCALE_X] = geometry[2] / width
            values[Matrix.MSKEW_X] = geometry[4] / height
            values[Matrix.MTRANS_X] =
                geometry[0] - ((geometry[2] + geometry[4]) / 2f)
            values[Matrix.MSKEW_Y] = geometry[3] / width
            values[Matrix.MSCALE_Y] = geometry[5] / height
            values[Matrix.MTRANS_Y] =
                geometry[1] - ((geometry[3] + geometry[5]) / 2f)
            values[Matrix.MPERSP_0] = 0f
            values[Matrix.MPERSP_1] = 0f
            values[Matrix.MPERSP_2] = 1f
            return Matrix().apply { setValues(values) }
        }

        private data class TargetCacheKey(
            val target: CombinedStatusPainter.TransitionTarget,
            val mobileSubId: Int?,
        )

        private data class Sample(
            val geometry: FloatArray,
        )

        private data class CompatibilityOpticalTarget(
            val view: View,
            val source: String,
        )

        private data class TargetWitness(
            val slot: String,
            val slotView: View,
            val opticalView: View?,
            val subscriptionId: Int?,
            val requiresOpticalGeometry: Boolean,
            val fallbackBounds: CombinedStatusPainter.TransitionNormalizedBounds?,
            val opticalSource: String,
        ) {
            val summary: String
                get() =
                    slot +
                        ":" +
                        slotView.width +
                        "x" +
                        slotView.height +
                        "/sub=" +
                        (subscriptionId ?: -1) +
                        "/opt=" +
                        opticalSource +
                        ":" +
                        (
                            opticalView?.let { view ->
                                (NativeParticipantRuntimeAccess.resourceEntryName(view)
                                    ?: view.javaClass.simpleName) +
                                    ":" + view.width + "x" + view.height
                            } ?: "none"
                        )
        }

        private data class MaskState(
            val view: WeakReference<View>,
            val nativeClip: Rect?,
            val appliedClip: Rect,
        )

        companion object {
            fun create(
                root: ViewGroup,
                fakeRoot: ViewGroup,
                finalRoot: ViewGroup,
                sourceSnapshot: CombinedStatusControlCenterRenderSession.TransitionSourceSnapshot,
            ): Session? {
                val finalStatusIcons =
                    uniqueDescendant(finalRoot, STATUS_ICON_CONTAINER_CLASS_NAME)
                        ?: return null
                val finalBattery =
                    uniqueDescendantView(finalRoot, BATTERY_VIEW_CLASS_NAME)
                        ?: return null
                return Session(
                    root = root,
                    fakeRoot = fakeRoot,
                    finalRoot = finalRoot,
                    sourceView = sourceSnapshot.view,
                    sourceAnchor = sourceSnapshot.anchorView,
                    sourceSnapshot = sourceSnapshot,
                    finalStatusIcons = finalStatusIcons,
                    finalBattery = finalBattery,
                )
            }

            private fun slotViews(
                group: ViewGroup,
                slot: String,
            ): List<View> {
                val result = ArrayList<View>()
                for (index in 0 until group.childCount) {
                    val child = group.getChildAt(index)
                    if (NativeParticipantRuntimeAccess.slotOf(child) == slot) {
                        result += child
                    }
                }
                return result
            }

            private fun uniqueDescendant(
                root: ViewGroup,
                className: String,
            ): ViewGroup? =
                uniqueDescendantView(root, className) as? ViewGroup

            private fun uniqueDescendantView(
                root: ViewGroup,
                className: String,
            ): View? {
                var found: View? = null
                val queue = ArrayDeque<ViewGroup>()
                queue.add(root)
                while (queue.isNotEmpty()) {
                    val parent = queue.removeFirst()
                    for (index in 0 until parent.childCount) {
                        val child = parent.getChildAt(index)
                        if (child.javaClass.name == className) {
                            if (found != null && found !== child) return null
                            found = child
                        }
                        if (child is ViewGroup) {
                            queue.add(child)
                        }
                    }
                }
                return found
            }
        }
    }

    private class TransitionDrawable(
        session: Session,
    ) : Drawable() {
        private val session = WeakReference(session)

        override fun draw(canvas: Canvas) {
            session.get()?.draw(canvas)
        }

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
