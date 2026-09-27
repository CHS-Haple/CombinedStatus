package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field

internal object SystemUiNotificationHeaderProbe {
    const val HOOK_COUNT = 1

    private const val CONTROLLER_CLASS_NAME =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController"
    private const val CALLBACK_CLASS_NAME =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1"
    private const val EXPANSION_METHOD_NAME = "onExpansionChanged"
    private const val HOOK_ID = "combinedstatus.panel.notification.header.probe"

    private val scalarFieldNames =
        listOf(
            "notificationTranslationX",
            "notificationTranslationY",
        )

    private var lastBucket: Int? = null
    private var inventoryLogged = false

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
    ): List<HookHandle> {
        val controllerClass =
            Class.forName(
                CONTROLLER_CLASS_NAME,
                false,
                classLoader,
            )
        val callbackClass =
            Class.forName(
                CALLBACK_CLASS_NAME,
                false,
                classLoader,
            )
        val expansionMethod =
            callbackClass
                .getDeclaredMethod(
                    EXPANSION_METHOD_NAME,
                    Float::class.javaPrimitiveType,
                )
                .apply { isAccessible = true }
        val controllerField =
            callbackClass.declaredFields
                .firstOrNull { field ->
                    controllerClass.isAssignableFrom(field.type)
                }
                ?.apply { isAccessible = true }
                ?: error("notification-header-controller-field-missing")

        val scalarFields =
            scalarFieldNames.mapNotNull { name ->
                runCatching {
                    controllerClass
                        .getDeclaredField(name)
                        .apply { isAccessible = true }
                }.getOrNull()?.let { name to it }
            }
        val viewFields =
            controllerClass.declaredFields
                .filter { field ->
                    View::class.java.isAssignableFrom(field.type)
                }
                .sortedBy { field -> field.name }
                .take(MAX_VIEW_FIELDS)
                .onEach { field -> field.isAccessible = true }
        val fieldInventory =
            controllerClass.declaredFields
                .sortedBy { field -> field.name }
                .take(MAX_FIELD_INVENTORY)
                .joinToString(",") { field ->
                    field.name + ":" + field.type.simpleName
                }

        val handle =
            module
                .hook(expansionMethod)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val fraction =
                            SystemUiPanelTransitionSource.nativeFraction(
                                (chain.getArg(0) as? Number)?.toFloat(),
                            )
                        val result = chain.proceed()
                        if (onEvent == null || !isProbeEnabled()) {
                            return@Hooker result
                        }

                        val bucket =
                            SystemUiPanelTransitionSource.diagnosticBucket(fraction)
                        val capture =
                            synchronized(this) {
                                val previous = lastBucket
                                lastBucket = bucket
                                shouldCaptureBoundary(previous, bucket)
                            }
                        if (!capture) {
                            return@Hooker result
                        }

                        val controller =
                            runCatching {
                                controllerField.get(chain.thisObject)
                            }.getOrNull()
                        if (controller == null) {
                            onEvent(
                                "notificationHeaderProbe fraction=" + fraction +
                                    " bucket=" + bucket + "/8" +
                                    " controller=unavailable" +
                                    " readOnly=true nativeGeometryWrites=0",
                            )
                            return@Hooker result
                        }

                        val scalarSummary =
                            scalarFields.joinToString(",") { (name, field) ->
                                name + "=" + readScalar(field, controller)
                            }
                        val viewSummary =
                            viewFields.joinToString(";") { field ->
                                field.name + "=" +
                                    (
                                        runCatching {
                                            field.get(controller) as? View
                                        }.getOrNull()?.let(::viewSummary)
                                            ?: "null"
                                    )
                            }
                        val includeInventory =
                            synchronized(this) {
                                if (inventoryLogged) {
                                    false
                                } else {
                                    inventoryLogged = true
                                    true
                                }
                            }

                        onEvent(
                            "notificationHeaderProbe fraction=" + fraction +
                                " bucket=" + bucket + "/8" +
                                " controller=" + controller.javaClass.simpleName +
                                " scalars={" + scalarSummary + "}" +
                                " views=[" + viewSummary + "]" +
                                if (includeInventory) {
                                    " fields=[" + fieldInventory + "]"
                                } else {
                                    ""
                                } +
                                " readOnly=true nativeGeometryWrites=0",
                        )
                        result
                    },
                )

        return listOf(handle)
    }

    fun resetRuntimeState() {
        synchronized(this) {
            lastBucket = null
            inventoryLogged = false
        }
    }

    internal fun shouldCaptureBoundary(
        previousBucket: Int?,
        currentBucket: Int?,
    ): Boolean =
        currentBucket != null &&
            currentBucket != previousBucket &&
            SystemUiPanelTransitionSource.isBoundaryDiagnosticBucket(currentBucket)

    private fun readScalar(
        field: Field,
        target: Any,
    ): String =
        runCatching {
            field.get(target)?.toString() ?: "null"
        }.getOrElse {
            "unavailable"
        }

    private fun viewSummary(view: View): String {
        val location = IntArray(2)
        runCatching {
            view.getLocationOnScreen(location)
        }
        val idName =
            if (view.id == View.NO_ID) {
                "none"
            } else {
                runCatching {
                    view.resources.getResourceEntryName(view.id)
                }.getOrNull() ?: "0x" + view.id.toUInt().toString(16)
            }
        val parentName =
            view.parent?.javaClass?.simpleName ?: "none"
        return view.javaClass.simpleName +
            "{id=" + idName +
            ",parent=" + parentName +
            ",screen=" + location[0] + "," + location[1] +
            ",bounds=" + view.left + "," + view.top + "-" +
            view.right + "," + view.bottom +
            ",size=" + view.width + "x" + view.height +
            ",measured=" + view.measuredWidth + "x" + view.measuredHeight +
            ",translation=" + view.translationX + "," + view.translationY +
            ",alpha=" + view.alpha +
            ",visibility=" + view.visibility +
            ",attached=" + view.isAttachedToWindow +
            "}"
    }

    private const val MAX_VIEW_FIELDS = 16
    private const val MAX_FIELD_INVENTORY = 32
}
