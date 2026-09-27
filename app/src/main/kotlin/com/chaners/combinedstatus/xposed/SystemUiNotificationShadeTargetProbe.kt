package com.chaners.combinedstatus.xposed

import android.view.View
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field

internal object SystemUiNotificationShadeTargetProbe {
    const val HOOK_COUNT = 1

    private const val CONTROLLER_CLASS_NAME =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController"
    private const val CALLBACK_CLASS_NAME =
        "com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1"
    private const val EXPANSION_METHOD_NAME = "onExpansionChanged"
    private const val NOTIFICATION_FIELD_NAME = "notification"
    private const val HEADER_CONTROLLER_FIELD_NAME = "headerController"
    private const val HOOK_ID = "combinedstatus.panel.notification.target.probe"

    private var lastBucket: Int? = null
    private var inventoryLogged = false

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        isProbeEnabled: () -> Boolean = { false },
    ): List<HookHandle> {
        val controllerClass =
            Class.forName(CONTROLLER_CLASS_NAME, false, classLoader)
        val callbackClass =
            Class.forName(CALLBACK_CLASS_NAME, false, classLoader)
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
        val notificationField =
            controllerClass
                .getDeclaredField(NOTIFICATION_FIELD_NAME)
                .apply { isAccessible = true }
        val headerControllerField =
            controllerClass
                .getDeclaredField(HEADER_CONTROLLER_FIELD_NAME)
                .apply { isAccessible = true }

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
                                "notificationShadeTargetProbe fraction=" + fraction +
                                    " bucket=" + bucket + "/8" +
                                    " controller=unavailable" +
                                    " readOnly=true nativeGeometryWrites=0",
                            )
                            return@Hooker result
                        }

                        val notification =
                            runCatching {
                                notificationField.get(controller)
                            }.getOrNull()
                        val headerControllerHolder =
                            runCatching {
                                headerControllerField.get(controller)
                            }.getOrNull()
                        val headerController =
                            resolveLazyValue(headerControllerHolder)

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
                            "notificationShadeTargetProbe fraction=" + fraction +
                                " bucket=" + bucket + "/8" +
                                " notification={" +
                                ownerSummary(notification, includeInventory) +
                                "}" +
                                " headerController={" +
                                ownerSummary(headerController, includeInventory) +
                                "}" +
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

    internal fun isCandidateField(
        fieldName: String,
        typeName: String,
    ): Boolean {
        val normalized = (fieldName + " " + typeName).lowercase()
        return CANDIDATE_TOKENS.any(normalized::contains)
    }

    private fun resolveLazyValue(holder: Any?): Any? {
        if (holder == null) {
            return null
        }
        val method =
            holder.javaClass.methods
                .firstOrNull { candidate ->
                    candidate.name == "getValue" &&
                        candidate.parameterCount == 0
                }
                ?: return holder
        return runCatching {
            method.isAccessible = true
            method.invoke(holder)
        }.getOrNull() ?: holder
    }

    private fun ownerSummary(
        owner: Any?,
        includeInventory: Boolean,
    ): String {
        if (owner == null) {
            return "null"
        }
        val ownerClass = owner.javaClass
        val fields =
            ownerClass.declaredFields
                .sortedBy(Field::getName)
                .take(MAX_FIELDS)
                .onEach { field -> field.isAccessible = true }
        val viewFields =
            fields
                .filter { field -> View::class.java.isAssignableFrom(field.type) }
                .take(MAX_VIEW_FIELDS)
                .joinToString(";") { field ->
                    field.name + "=" +
                        (
                            runCatching {
                                field.get(owner) as? View
                            }.getOrNull()?.let(::viewSummary)
                                ?: "null"
                        )
                }
        val candidates =
            fields
                .filter { field ->
                    !View::class.java.isAssignableFrom(field.type) &&
                        isCandidateField(field.name, field.type.name)
                }
                .take(MAX_CANDIDATE_FIELDS)
                .joinToString(";") { field ->
                    val value =
                        runCatching { field.get(owner) }
                            .getOrNull()
                    field.name + "=" +
                        (value?.javaClass?.name ?: "null") +
                        directViewSummary(value)
                }
        val inventory =
            if (includeInventory) {
                fields.joinToString(",") { field ->
                    field.name + ":" + field.type.name
                }
            } else {
                ""
            }

        return "class=" + ownerClass.name +
            " views=[" + viewFields + "]" +
            " candidates=[" + candidates + "]" +
            (
                if (includeInventory) {
                    " fields=[" + inventory + "]"
                } else {
                    ""
                }
            )
    }

    private fun directViewSummary(owner: Any?): String {
        if (owner == null) {
            return ""
        }
        val views =
            owner.javaClass.declaredFields
                .asSequence()
                .filter { field ->
                    View::class.java.isAssignableFrom(field.type)
                }
                .take(MAX_NESTED_VIEW_FIELDS)
                .map { field ->
                    field.isAccessible = true
                    field.name + "=" +
                        (
                            runCatching {
                                field.get(owner) as? View
                            }.getOrNull()?.let(::viewSummary)
                                ?: "null"
                        )
                }
                .toList()
        return if (views.isEmpty()) {
            ""
        } else {
            "{views=" + views.joinToString(";") + "}"
        }
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
        return view.javaClass.simpleName +
            "{id=" + idName +
            ",parent=" + (view.parent?.javaClass?.simpleName ?: "none") +
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

    private val CANDIDATE_TOKENS =
        listOf(
            "status",
            "icon",
            "header",
            "battery",
            "system",
            "shade",
            "clock",
            "container",
        )

    private const val MAX_FIELDS = 48
    private const val MAX_VIEW_FIELDS = 16
    private const val MAX_CANDIDATE_FIELDS = 10
    private const val MAX_NESTED_VIEW_FIELDS = 12
}
