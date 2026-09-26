package com.chaners.combinedstatus.xposed

import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field

internal object SystemUiIslandMotionSource {
    const val HOOK_COUNT = 1


    private const val INJECTOR_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector"
    private const val LISTENER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector\$islandListener\$1"
    private const val STATUS_METHOD_NAME = "onIslandStatusChanged"
    private const val HOOK_ID = "combinedstatus.island.home.status"
    private const val BATTERY_VIEW_FIELD = "mBatteryView"

    private val diagnosticTrackedNames =
        listOf(
            "mStatusContainer",
            "mEndSideContent",
            "mStatusBarIcons",
            "mBatteryContainer",
            BATTERY_VIEW_FIELD,
        )

    private var injectorRef = WeakReference<Any>(null)
    private var diagnosticFields: List<Pair<String, Field>> = emptyList()

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { true },
    ): List<HookHandle> {
        val injectorClass = Class.forName(INJECTOR_CLASS_NAME, false, classLoader)
        val listenerClass = Class.forName(LISTENER_CLASS_NAME, false, classLoader)
        val method =
            listenerClass.getDeclaredMethod(
                STATUS_METHOD_NAME,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }
        val outerField =
            listenerClass.declaredFields
                .firstOrNull { it.type == injectorClass }
                ?.apply { isAccessible = true }
        val resolvedDiagnosticFields =
            diagnosticTrackedNames.mapNotNull { name ->
                runCatching {
                    injectorClass.getDeclaredField(name).apply { isAccessible = true }
                }.getOrNull()?.let { name to it }
            }
        synchronized(this) {
            diagnosticFields = resolvedDiagnosticFields
        }

        val handle =
            module
                .hook(method)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val showing = chain.getArg(0) as? Boolean ?: false
                        val secondary = chain.getArg(1) as? Boolean ?: false
                        val animate = chain.getArg(2) as? Boolean ?: false
                        val result = chain.proceed()
                        if (onEvent == null || !isProbeEnabled()) {
                            return@Hooker result
                        }

                        val injector =
                            outerField?.let { field ->
                                runCatching { field.get(chain.thisObject) }.getOrNull()
                            }
                        if (injector != null) {
                            synchronized(this) {
                                injectorRef = WeakReference(injector)
                            }
                            val views =
                                resolvedDiagnosticFields.mapNotNull { (name, field) ->
                                    (runCatching { field.get(injector) as? View }.getOrNull())
                                        ?.let { name to it }
                                }.toMap()
                            onEvent(
                                "islandOwner event showing=" + showing +
                                    " secondary=" + secondary +
                                    " animate=" + animate +
                                    " fields=" + views.keys.joinToString(",") +
                                    " geometryWrites=0",
                            )
                            if (views.isNotEmpty()) {
                                DiagnosticProbe.start(
                                    views = views,
                                    onEvent = onEvent,
                                    isProbeEnabled = isProbeEnabled,
                                )
                            }
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    @Synchronized
    fun currentOwnerSnapshot(): OwnerSnapshot? {
        val injector = injectorRef.get() ?: return null
        val views =
            diagnosticFields.mapNotNull { (name, field) ->
                (runCatching { field.get(injector) as? View }.getOrNull())
                    ?.let { name to viewSnapshot(it) }
            }.toMap()
        if (views.isEmpty()) {
            return null
        }
        return OwnerSnapshot(views)
    }

    fun resetRuntimeState() {
        synchronized(this) {
            injectorRef = WeakReference(null)
            diagnosticFields = emptyList()
        }
        DiagnosticProbe.reset()
    }

    internal data class OwnerSnapshot(
        val views: Map<String, MotionViewSnapshot>,
    ) {
        val summary: String
            get() =
                "{" +
                    diagnosticTrackedNames
                        .mapNotNull { name ->
                            views[name]?.let { snapshot ->
                                name + "=" + snapshot.summary
                            }
                        }
                        .joinToString(",") +
                    "}"
    }

    internal data class MotionViewSnapshot(
        val className: String,
        val screenX: Int,
        val width: Int,
        val translationX: Float,
        val alpha: Float,
        val visibility: Int,
    ) {
        val summary: String
            get() =
                className +
                    "(x=" + screenX +
                    ",w=" + width +
                    ",tx=" + translationX +
                    ",a=" + alpha +
                    ",v=" + visibility +
                    ")"
    }

    private fun viewSnapshot(view: View): MotionViewSnapshot {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return MotionViewSnapshot(
            className = view.javaClass.simpleName,
            screenX = location[0],
            width = view.width,
            translationX = view.translationX,
            alpha = view.alpha,
            visibility = view.visibility,
        )
    }

    private object DiagnosticProbe {
        private var generation = 0
        private var activeRoot = WeakReference<View>(null)
        private var listener: ViewTreeObserver.OnPreDrawListener? = null
        private var timeout: Runnable? = null

        fun start(
            views: Map<String, View>,
            onEvent: (String) -> Unit,
            isProbeEnabled: () -> Boolean,
        ) {
            stop()
            generation += 1
            val currentGeneration = generation
            val root = views.values.first().rootView ?: return
            val observer = root.viewTreeObserver
            if (!observer.isAlive) return
            val startedAt = SystemClock.uptimeMillis()
            var frame = 0
            var samples = 0
            var previous = ""

            val nextListener =
                ViewTreeObserver.OnPreDrawListener {
                    if (!isProbeEnabled()) {
                        stop()
                        return@OnPreDrawListener true
                    }
                    frame += 1
                    val snapshot =
                        views.entries.joinToString(" ") { (name, view) ->
                            name + "=" + motion(view)
                        }
                    if (snapshot != previous && samples < MAX_SAMPLES) {
                        previous = snapshot
                        samples += 1
                        onEvent(
                            "islandOwner sample frame=" + frame +
                                " elapsedMs=" + (SystemClock.uptimeMillis() - startedAt) +
                                " " + snapshot +
                                " sample=" + samples + "/" + MAX_SAMPLES +
                                " geometryWrites=0",
                        )
                    }
                    if (
                        currentGeneration == generation &&
                        SystemClock.uptimeMillis() - startedAt >= FOLLOW_DURATION_MS
                    ) {
                        stop()
                    }
                    true
                }
            val nextTimeout =
                Runnable {
                    if (currentGeneration == generation) {
                        stop()
                    }
                }
            listener = nextListener
            timeout = nextTimeout
            activeRoot = WeakReference(root)
            observer.addOnPreDrawListener(nextListener)
            root.postDelayed(nextTimeout, FOLLOW_DURATION_MS)
        }

        fun reset() = stop()

        private fun stop() {
            val root = activeRoot.get()
            val currentListener = listener
            val currentTimeout = timeout
            if (root != null) {
                if (currentListener != null) {
                    val observer = root.viewTreeObserver
                    if (observer.isAlive) observer.removeOnPreDrawListener(currentListener)
                }
                if (currentTimeout != null) {
                    root.removeCallbacks(currentTimeout)
                }
            }
            listener = null
            timeout = null
            activeRoot = WeakReference(null)
        }

        private fun motion(view: View): String {
            val snapshot = viewSnapshot(view)
            return snapshot.className +
                "{x=" + view.x +
                ",screenX=" + snapshot.screenX +
                ",tx=" + snapshot.translationX +
                ",a=" + snapshot.alpha +
                ",v=" + snapshot.visibility +
                ",w=" + snapshot.width +
                "}"
        }

        private const val MAX_SAMPLES = 16
    }

    private const val FOLLOW_DURATION_MS = 900L
}
