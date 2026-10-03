package com.chaners.guiyuan.xposed

import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

internal object SystemUiIslandMotionSource {
    const val HOOK_COUNT = 1


    private const val INJECTOR_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector"
    private const val LISTENER_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.shared.ui.binder.HomeStatusBarViewBinderInjector\$islandListener\$1"
    private const val STATUS_METHOD_NAME = "onIslandStatusChanged"
    private const val HOOK_ID = "combinedstatus.island.home.status"
    private const val BATTERY_VIEW_FIELD = "mBatteryView"
    private const val ISLAND_CONTROLLER_FIELD = "islandController"
    private const val ISLAND_STATE_HANDLER_FIELD = "islandStateHandler"
    private const val ISLAND_RECT_FIELD = "islandRect"
    private const val GEOMETRY_PROBE_MAX_ENTRIES = 32
    private const val GEOMETRY_PROBE_MAX_OBJECTS = 12
    private const val GEOMETRY_PROBE_MAX_DEPTH = 2

    private val diagnosticTrackedNames =
        listOf(
            "mStatusContainer",
            "mEndSideContent",
            "mStatusBarIcons",
            "mBatteryContainer",
            BATTERY_VIEW_FIELD,
        )

    @Volatile
    private var injectorRef = WeakReference<Any>(null)
    private var diagnosticFields: List<Pair<String, Field>> = emptyList()
    @Volatile
    private var islandControllerField: Field? = null
    @Volatile
    private var islandStateHandlerField: Field? = null
    @Volatile
    private var islandRectField: Field? = null
    @Volatile
    private var islandStateHandlerRef = WeakReference<Any>(null)
    @Volatile
    private var islandShowing: Boolean? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onStatusChanged: ((Boolean) -> Unit)? = null,
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
        val diagnosticsEnabled = onEvent != null
        val outerField =
            listenerClass.declaredFields
                .firstOrNull { it.type == injectorClass }
                ?.apply { isAccessible = true }
        val resolvedDiagnosticFields =
            if (diagnosticsEnabled) {
                diagnosticTrackedNames.mapNotNull { name ->
                    runCatching {
                        injectorClass.getDeclaredField(name).apply { isAccessible = true }
                    }.getOrNull()?.let { name to it }
                }
            } else {
                emptyList()
            }
        val resolvedIslandControllerField =
            runCatching {
                injectorClass
                    .getDeclaredField(ISLAND_CONTROLLER_FIELD)
                    .apply { isAccessible = true }
            }.getOrNull()
        synchronized(this) {
            diagnosticFields = resolvedDiagnosticFields
            islandControllerField = resolvedIslandControllerField
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
                        val injector =
                            outerField?.let { field ->
                                runCatching { field.get(chain.thisObject) }.getOrNull()
                            }
                        synchronized(this) {
                            islandShowing = showing
                            if (injector != null) {
                                injectorRef = WeakReference(injector)
                            }
                        }
                        runCatching {
                            onStatusChanged?.invoke(showing)
                        }
                        if (onEvent == null || !isProbeEnabled()) {
                            return@Hooker result
                        }

                        if (injector != null) {
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
                        }
                        result
                    },
                )
        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    @Synchronized
    fun currentIslandShowing(): Boolean? = islandShowing

    /**
     * Copies HyperOS' live island rectangle into [out].
     *
     * The exact field path was verified on the pinned target by Build 664:
     * HomeStatusBarViewBinderInjector.islandController
     *   -> StatusBarIslandControllerImpl.islandStateHandler
     *   -> islandRect.
     *
     * Field discovery occurs only once on first successful access. The hot path
     * performs direct cached Field.get calls and Rect.set only.
     */
    fun copyCurrentIslandRect(out: Rect): Boolean {
        val injector = injectorRef.get() ?: return false
        val controllerField = islandControllerField ?: return false
        val controller =
            runCatching { controllerField.get(injector) }.getOrNull()
                ?: return false

        var handler = islandStateHandlerRef.get()
        if (handler == null) {
            val handlerField =
                islandStateHandlerField
                    ?: runCatching {
                        controller.javaClass
                            .getDeclaredField(ISLAND_STATE_HANDLER_FIELD)
                            .apply { isAccessible = true }
                    }.getOrNull()
                        ?.also { islandStateHandlerField = it }
                    ?: return false
            handler =
                runCatching { handlerField.get(controller) }.getOrNull()
                    ?: return false
            islandStateHandlerRef = WeakReference(handler)
        }

        val rectField =
            islandRectField
                ?: runCatching {
                    handler.javaClass
                        .getDeclaredField(ISLAND_RECT_FIELD)
                        .apply { isAccessible = true }
                }.getOrNull()
                    ?.also { islandRectField = it }
                ?: return false
        val liveRect =
            runCatching { rectField.get(handler) as? Rect }.getOrNull()
                ?: return false
        if (liveRect.isEmpty) return false
        out.set(liveRect)
        return true
    }

    @Synchronized
    fun currentOwnerSnapshot(): OwnerSnapshot? {
        val injector = injectorRef.get() ?: return null
        val views =
            diagnosticFields.mapNotNull { (name, field) ->
                (runCatching { field.get(injector) as? View }.getOrNull())
                    ?.let { name to viewSnapshot(it) }
            }.toMap()
        val geometryProbe =
            runCatching { islandGeometryProbe(injector) }
                .getOrNull()
        if (views.isEmpty() && geometryProbe == null) {
            return null
        }
        return OwnerSnapshot(
            views = views,
            geometryProbe = geometryProbe,
        )
    }

    fun resetRuntimeState() {
        synchronized(this) {
            injectorRef = WeakReference(null)
            diagnosticFields = emptyList()
            islandControllerField = null
            islandStateHandlerField = null
            islandRectField = null
            islandStateHandlerRef = WeakReference(null)
            islandShowing = null
        }
    }

    internal data class OwnerSnapshot(
        val views: Map<String, MotionViewSnapshot>,
        val geometryProbe: IslandGeometryProbe? = null,
    ) {
        val summary: String
            get() {
                val parts =
                    diagnosticTrackedNames
                        .mapNotNull { name ->
                            views[name]?.let { snapshot ->
                                name + "=" + snapshot.summary
                            }
                        }
                        .toMutableList()
                geometryProbe?.let { probe ->
                    parts += "islandGeometry=" + probe.summary
                }
                return "{" + parts.joinToString(",") + "}"
            }
    }

    internal data class MotionViewSnapshot(
        val className: String,
        val screenX: Int,
        val screenY: Int = 0,
        val width: Int,
        val height: Int = 0,
        val translationX: Float,
        val translationY: Float = 0f,
        val alpha: Float,
        val visibility: Int,
    ) {
        val summary: String
            get() =
                className +
                    "(x=" + screenX +
                    ",y=" + screenY +
                    ",w=" + width +
                    ",h=" + height +
                    ",tx=" + translationX +
                    ",ty=" + translationY +
                    ",a=" + alpha +
                    ",v=" + visibility +
                    ")"
    }

    internal data class IslandGeometryProbe(
        val rootClassName: String,
        val entries: List<String>,
    ) {
        val summary: String
            get() =
                "{root=" + rootClassName +
                    ",entries=[" + entries.joinToString(";") + "]}"
    }

    private fun viewSnapshot(view: View): MotionViewSnapshot {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return MotionViewSnapshot(
            className = view.javaClass.simpleName,
            screenX = location[0],
            screenY = location[1],
            width = view.width,
            height = view.height,
            translationX = view.translationX,
            translationY = view.translationY,
            alpha = view.alpha,
            visibility = view.visibility,
        )
    }

    private fun islandGeometryProbe(injector: Any): IslandGeometryProbe? {
        val controller =
            islandControllerField
                ?.let { field ->
                    runCatching { field.get(injector) }.getOrNull()
                }
                ?: return null
        val entries = ArrayList<String>()
        val seen =
            Collections.newSetFromMap(
                IdentityHashMap<Any, Boolean>(),
            )
        val queue = ArrayDeque<ProbeNode>()
        queue.add(ProbeNode("controller", controller, 0))

        while (
            queue.isNotEmpty() &&
            entries.size < GEOMETRY_PROBE_MAX_ENTRIES &&
            seen.size < GEOMETRY_PROBE_MAX_OBJECTS
        ) {
            val node = queue.removeFirst()
            if (!seen.add(node.value)) continue
            fieldsOf(node.value.javaClass).forEach { field ->
                if (
                    entries.size >= GEOMETRY_PROBE_MAX_ENTRIES ||
                    Modifier.isStatic(field.modifiers)
                ) {
                    return@forEach
                }
                val key = node.path + "." + field.name
                val fieldTypeName = field.type.name
                val relevant =
                    isGeometryProbeRelevant(
                        name = field.name,
                        typeName = fieldTypeName,
                    )
                val geometryTyped =
                    Rect::class.java.isAssignableFrom(field.type) ||
                        RectF::class.java.isAssignableFrom(field.type) ||
                        View::class.java.isAssignableFrom(field.type)
                if (!relevant && !geometryTyped) {
                    return@forEach
                }
                val value =
                    runCatching {
                        field.isAccessible = true
                        field.get(node.value)
                    }.getOrNull()
                        ?: return@forEach

                summarizeProbeValue(
                    key = key,
                    value = value,
                    relevant = relevant,
                )?.let(entries::add)

                val flowValue =
                    if (relevant) {
                        readFlowValue(value)
                    } else {
                        null
                    }
                if (flowValue != null) {
                    summarizeProbeValue(
                        key = key + ".value",
                        value = flowValue,
                        relevant = true,
                    )?.let(entries::add)
                }

                if (
                    node.depth < GEOMETRY_PROBE_MAX_DEPTH &&
                    relevant &&
                    shouldTraverseProbeObject(value)
                ) {
                    queue.add(
                        ProbeNode(
                            path = key,
                            value = value,
                            depth = node.depth + 1,
                        ),
                    )
                }
            }
        }

        if (entries.isEmpty()) return null
        return IslandGeometryProbe(
            rootClassName = controller.javaClass.name,
            entries = entries,
        )
    }

    internal fun isGeometryProbeRelevant(
        name: String,
        typeName: String,
    ): Boolean {
        val token = (name + " " + typeName).lowercase()
        return listOf(
            "island",
            "rect",
            "bound",
            "space",
            "translation",
            "monitor",
            "container",
            "area",
        ).any(token::contains)
    }

    private fun summarizeProbeValue(
        key: String,
        value: Any,
        relevant: Boolean,
    ): String? =
        when (value) {
            is Rect ->
                key + "=Rect(" +
                    value.left + "," + value.top + "," +
                    value.right + "," + value.bottom + ")"
            is RectF ->
                key + "=RectF(" +
                    value.left + "," + value.top + "," +
                    value.right + "," + value.bottom + ")"
            is View ->
                runCatching { key + "=" + viewSnapshot(value).summary }
                    .getOrNull()
            is Number,
            is Boolean,
            is Enum<*>,
            -> if (relevant) key + "=" + value else null
            else -> null
        }

    private fun readFlowValue(value: Any): Any? {
        val method =
            runCatching {
                generateSequence<Class<*>>(value.javaClass) { clazz -> clazz.superclass }
                    .mapNotNull { clazz ->
                        runCatching {
                            clazz.declaredMethods.firstOrNull { candidate ->
                                candidate.name == "getValue" &&
                                    candidate.parameterCount == 0
                            }
                        }.getOrNull()
                    }
                    .firstOrNull()
            }.getOrNull()
                ?: return null
        return runCatching {
            method.isAccessible = true
            method.invoke(value)
        }.getOrNull()
    }

    private fun shouldTraverseProbeObject(value: Any): Boolean =
        value !is Rect &&
            value !is RectF &&
            value !is View &&
            value !is Number &&
            value !is Boolean &&
            value !is String &&
            value !is Enum<*>

    private fun fieldsOf(clazz: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        generateSequence<Class<*>>(clazz) { current -> current.superclass }
            .takeWhile { current -> current != Any::class.java }
            .forEach { current ->
                runCatching { current.declaredFields.toList() }
                    .getOrDefault(emptyList())
                    .forEach(fields::add)
            }
        return fields
    }

    private data class ProbeNode(
        val path: String,
        val value: Any,
        val depth: Int,
    )
}
