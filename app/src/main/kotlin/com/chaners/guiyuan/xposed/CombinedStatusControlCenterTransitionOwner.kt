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
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal object CombinedStatusControlCenterTransitionOwner {
    private const val STATUS_ICON_CONTAINER_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private var visible = false
    private var sceneEligible = false
    private var projectionReady = false
    private var nativeProgress: Float? = null
    private var nativeAppearance = false
    private var nativeAppearanceAnimated = false
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
                endpoints = null
            }
        }
        update.fraction?.let { nativeProgress = it }
        update.controlCenterAppearance?.let { nativeAppearance = it }
        update.controlCenterAppearanceAnimated?.let { nativeAppearanceAnimated = it }
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
                progress < 1f &&
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
        )
    }

    internal object Policy {
        fun geometryProgress(raw: Float): Float {
            if (!raw.isFinite()) return 0f
            val normalized = (raw / 0.82f).coerceIn(0f, 1f)
            return smoothstep(normalized)
        }

        fun releaseProgress(
            raw: Float,
            policy: CombinedStatusPainter.TransitionReleasePolicy,
        ): Float {
            if (!raw.isFinite()) return 0f
            val span = policy.endProgress - policy.startProgress
            if (span <= 0f) {
                return if (raw >= policy.endProgress) 1f else 0f
            }
            val normalized =
                ((raw - policy.startProgress) / span)
                    .coerceIn(0f, 1f)
            return smoothstep(normalized)
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

        private fun smoothstep(value: Float): Float =
            value * value * (3f - 2f * value)
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
        private val targetMasks = ArrayList<MaskState>()
        private val sourceColors = sourceSnapshot.colors

        private var currentSnapshot = sourceSnapshot
        private var progress = 0f
        private var nativeAppearance = false
        private var nativeAppearanceAnimated = false
        private var started = false
        private var lastStateVersion = sourceSnapshot.stateVersion

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
                reconcileTargetMasks(currentSnapshot.model)
                drawable.setBounds(0, 0, rootView.width, rootView.height)
                drawable.invalidateSelf()
                true
            }

        fun diagnostic(): String =
            "transitionOwner={progress=" + progress +
                ",appearance=" + nativeAppearance +
                ",appearanceAnimated=" + nativeAppearanceAnimated +
                ",nativePeers=systemui" +
                ",targetMasks=" + targetMasks.size +
                ",release=" +
                (
                    painter
                        .transitionComponentSpecs(
                            sourceViewRef.get()?.width ?: 0,
                            sourceViewRef.get()?.height ?: 0,
                            currentSnapshot.model,
                        )
                        .firstOrNull()
                        ?.let { Policy.releaseProgress(progress, it.releasePolicy) }
                        ?: 0f
                ) +
                ",sourceAnchor=" + (sourceAnchorRef.get()?.javaClass?.simpleName ?: "none") +
                ",sourceStateVersion=" + lastStateVersion +
                ",root=" + (rootRef.get()?.javaClass?.simpleName ?: "none") +
                ",fake=" + (fakeRootRef.get()?.javaClass?.simpleName ?: "none") +
                ",final=" + (finalRootRef.get()?.javaClass?.simpleName ?: "none") +
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
            reconcileTargetMasks(currentSnapshot.model)
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
        ) {
            this.progress = progress.coerceIn(0f, 1f)
            this.currentSnapshot = sourceSnapshot
            this.nativeAppearance = nativeAppearance
            this.nativeAppearanceAnimated = nativeAppearanceAnimated
            if (started) {
                reconcileTargetMasks(sourceSnapshot.model)
            }
            drawable.invalidateSelf()
        }

        fun stop(source: String) {
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
            restoreTargetMasks()
        }

        fun draw(canvas: Canvas) {
            val rootView = rootRef.get() ?: return
            val fake = fakeRootRef.get() ?: return
            val final = finalRootRef.get() ?: return
            val sourceView = sourceViewRef.get() ?: return
            val sourceAnchor = sourceAnchorRef.get() ?: return
            if (sourceView.width <= 0 || sourceView.height <= 0) return

            val sourceSample =
                sample(
                    view = sourceAnchor,
                    root = rootView,
                ) ?: return
            val model = currentSnapshot.model
            val specs =
                painter.transitionComponentSpecs(
                    width = sourceView.width,
                    height = sourceView.height,
                    model = model,
                )
            if (specs.isEmpty()) return

            val geometryProgress = Policy.geometryProgress(progress)
            val envelope =
                (endpointAlpha(fake) + endpointAlpha(final))
                    .coerceIn(0f, 1f)
            if (envelope <= 0f) return

            val targetRelease = linkedMapOf<View, Float>()
            specs.forEach { spec ->
                val sourceGeometry =
                    Policy.componentGeometry(
                        parentGeometry = sourceSample.geometry,
                        parentWidth = sourceView.width,
                        parentHeight = sourceView.height,
                        bounds = spec.sourceBounds,
                    ) ?: return@forEach
                val target = resolveTarget(spec.target)
                val targetSample = target?.let { sample(it, rootView) }
                val targetGeometry = targetSample?.geometry ?: sourceGeometry
                val release =
                    if (targetSample == null) {
                        0f
                    } else {
                        Policy.releaseProgress(progress, spec.releasePolicy)
                    }
                val geometry =
                    Policy.interpolateGeometry(
                        source = sourceGeometry,
                        target = targetGeometry,
                        progress = geometryProgress,
                    )
                val matrix =
                    matrixForBoundsGeometry(
                        geometry = geometry,
                        bounds = spec.sourceBounds,
                    ) ?: return@forEach
                val compactOpacity = envelope * (1f - release)
                if (compactOpacity > 0f) {
                    val save =
                        canvas.saveLayerAlpha(
                            null,
                            (255f * compactOpacity.coerceIn(0f, 1f)).roundToInt(),
                        )
                    canvas.concat(matrix)
                    painter.drawTransitionComponent(
                        canvas = canvas,
                        width = sourceView.width,
                        height = sourceView.height,
                        model = model,
                        colors = sourceColors,
                        component = spec.component,
                        opacity = 1f,
                        morphProgress = geometryProgress,
                    )
                    canvas.restoreToCount(save)
                }
                if (target != null && release > 0f) {
                    targetRelease[target] =
                        maxOf(targetRelease[target] ?: 0f, release)
                }
            }

            targetRelease.forEach { (target, release) ->
                drawNativeTarget(
                    canvas = canvas,
                    target = target,
                    rootView = rootView,
                    alpha = envelope * release,
                )
            }
        }

        private fun resolveTarget(
            target: CombinedStatusPainter.TransitionTarget,
        ): View? =
            when (target) {
                CombinedStatusPainter.TransitionTarget.Battery -> finalBattery
                is CombinedStatusPainter.TransitionTarget.Slots ->
                    target.preferredSlots.firstNotNullOfOrNull { slot ->
                        slotViews(finalStatusIcons, slot).firstOrNull()
                    }
            }

        private fun reconcileTargetMasks(model: CombinedStatusRenderModel) {
            if (!started) return
            val source = sourceViewRef.get() ?: return
            val targets =
                painter
                    .transitionComponentSpecs(
                        width = source.width,
                        height = source.height,
                        model = model,
                    )
                    .mapNotNull { spec -> resolveTarget(spec.target) }
                    .distinctBy(System::identityHashCode)
                    .toSet()

            val iterator = targetMasks.iterator()
            while (iterator.hasNext()) {
                val state = iterator.next()
                val view = state.view.get()
                if (view == null || view !in targets) {
                    if (view != null) restoreMask(state)
                    iterator.remove()
                }
            }
            targets.forEach { view ->
                if (targetMasks.none { state -> state.view.get() === view }) {
                    val nativeClip = view.clipBounds?.let(::Rect)
                    val applied = Rect(0, 0, 0, 0)
                    view.clipBounds = applied
                    targetMasks +=
                        MaskState(
                            view = WeakReference(view),
                            nativeClip = nativeClip,
                            appliedClip = applied,
                        )
                }
            }
        }

        private fun restoreTargetMasks() {
            val states = targetMasks.toList()
            targetMasks.clear()
            states.forEach(::restoreMask)
        }

        private fun restoreMask(state: MaskState) {
            val view = state.view.get() ?: return
            if (view.clipBounds == state.appliedClip) {
                view.clipBounds = state.nativeClip?.let(::Rect)
            }
        }

        private fun drawNativeTarget(
            canvas: Canvas,
            target: View,
            rootView: View,
            alpha: Float,
        ) {
            if (alpha <= 0f) return
            val sample = sample(target, rootView) ?: return
            val matrix =
                matrixForGeometry(
                    geometry = sample.geometry,
                    width = target.width,
                    height = target.height,
                ) ?: return
            val save =
                canvas.saveLayerAlpha(
                    null,
                    (255f * alpha.coerceIn(0f, 1f)).roundToInt(),
                )
            canvas.concat(matrix)
            targetMasks
                .firstOrNull { state -> state.view.get() === target }
                ?.nativeClip
                ?.let(canvas::clipRect)
            target.draw(canvas)
            canvas.restoreToCount(save)
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

        private data class Sample(
            val geometry: FloatArray,
        )

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
