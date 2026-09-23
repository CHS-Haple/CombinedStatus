package com.chaners.combinedstatus.xposed

import android.view.View
import java.lang.reflect.Method

internal object NativeStatusBarSlotOrderingProbe {
    private const val MAX_SLOTS = 64
    private val INTERESTING_METHOD_NAMES =
        setOf(
            "getSlotIndex",
            "getSlotName",
            "getViewIndex",
        )

    fun inspect(host: Any): Snapshot {
        val resolution = NativeParticipantRuntimeAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return Snapshot.unavailable(resolution.reason)
            }

        val iconList =
            readField(handles.controller, "mStatusBarIconList")
                ?: return Snapshot.unavailable("status-bar-icon-list-missing")

        val slots =
            (readField(iconList, "mSlots") as? List<*>)
                ?.take(MAX_SLOTS)
                ?.mapIndexed { index, value ->
                    SlotEntry(
                        index = index,
                        name = slotName(value),
                        raw = value?.toString() ?: "null",
                    )
                }
                .orEmpty()

        val viewOnlyRaw = readField(iconList, "mViewOnlySlots")
        val viewOnlySlots =
            when (viewOnlyRaw) {
                is Iterable<*> ->
                    viewOnlyRaw
                        .take(MAX_SLOTS)
                        .mapIndexed { index, value ->
                            index.toString() + ":" + slotName(value)
                        }
                else -> emptyList()
            }

        val methods =
            iconList.javaClass
                .allMethods()
                .filter { method -> method.name in INTERESTING_METHOD_NAMES }
                .map(NativeParticipantRuntimeAccess::methodSignature)
                .distinct()
                .sorted()
                .toList()

        val slotNames = slots.map { entry -> entry.name }.toSet()
        val candidates =
            listOf(
                SystemUiNativeCombinedParticipantOwner.SLOT,
                "stacked_mobile",
                "network_speed",
                "volume",
                "mobile",
                "demo_mobile",
                "no_sim",
                "demo_wifi",
                "wifi",
            ).filter { candidate -> candidate in slotNames }

        val slotIndexMethod =
            iconList.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "getSlotIndex" &&
                        method.parameterTypes.contentEquals(
                            arrayOf<Class<*>>(String::class.java),
                        ) &&
                        method.returnType == Integer.TYPE
                }
                ?.apply { isAccessible = true }

        val viewIndexMethod =
            iconList.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "getViewIndex" &&
                        method.parameterTypes.contentEquals(
                            arrayOf<Class<*>>(
                                String::class.java,
                                Integer.TYPE,
                            ),
                        ) &&
                        method.returnType == Integer.TYPE
                }
                ?.apply { isAccessible = true }

        val indexResults =
            candidates.map { slot ->
                val slotIndex =
                    slotIndexMethod
                        ?.let { method ->
                            runCatching {
                                (method.invoke(iconList, slot) as Number).toInt()
                            }.getOrNull()
                        }
                val viewIndex =
                    viewIndexMethod
                        ?.let { method ->
                            runCatching {
                                (method.invoke(iconList, slot, 0) as Number).toInt()
                            }.getOrNull()
                        }
                slot +
                    ":slotIndex=" + (slotIndex?.toString() ?: "na") +
                    ",viewIndex=" + (viewIndex?.toString() ?: "na")
            }

        val groupOrder = inspectGroup(handles.group)

        return Snapshot(
            available = true,
            reason = null,
            iconListClass = iconList.javaClass.name,
            slots = slots,
            viewOnlySlots = viewOnlySlots,
            methods = methods,
            indexResults = indexResults,
            groupOrder = groupOrder,
        )
    }

    fun inspectLaidOutGroup(host: Any): List<String> {
        val resolution = NativeParticipantRuntimeAccess.resolve(host)
        val handles =
            (resolution as? NativeParticipantRuntimeAccess.ResolveResult.Ready)
                ?.handles
                ?: return emptyList()
        return inspectGroup(handles.group)
    }

    private fun inspectGroup(group: android.view.ViewGroup): List<String> =
        buildList {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                val slot = slotOf(child) ?: continue
                add(
                    "index=" + index +
                        ",slot=" + slot +
                        ",visibility=" + visibilityName(child.visibility) +
                        ",iconVisible=" +
                        (NativeParticipantRuntimeAccess.iconVisible(child)
                            ?.toString() ?: "unknown") +
                        ",bounds=" +
                        child.left + "," + child.top + "-" +
                        child.right + "," + child.bottom +
                        ",size=" + child.width + "x" + child.height +
                        ",measured=" +
                        child.measuredWidth + "x" + child.measuredHeight,
                )
                if (size >= MAX_SLOTS) {
                    break
                }
            }
        }

    private fun slotName(value: Any?): String {
        if (value == null) return "null"
        if (value is String) return value

        val fieldNames =
            listOf(
                "mName",
                "name",
                "slot",
                "mSlot",
            )
        fieldNames.forEach { name ->
            val candidate = readField(value, name) as? String
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }

        val accessor =
            value.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.returnType == String::class.java &&
                        method.name in setOf("getName", "getSlot", "getSlotName")
                }
        val fromAccessor =
            accessor?.let { method ->
                runCatching {
                    method.isAccessible = true
                    method.invoke(value) as? String
                }.getOrNull()
            }
        if (!fromAccessor.isNullOrBlank()) {
            return fromAccessor
        }

        return value.toString()
    }

    private fun slotOf(view: View): String? {
        val accessor =
            view.javaClass
                .allMethods()
                .firstOrNull { method ->
                    method.name == "getSlot" &&
                        method.parameterCount == 0 &&
                        method.returnType == String::class.java
                }
                ?: return null
        return runCatching {
            accessor.isAccessible = true
            accessor.invoke(view) as? String
        }.getOrNull()
    }

    private fun readField(target: Any, name: String): Any? {
        val field =
            generateSequence(target.javaClass) { clazz -> clazz.superclass }
                .mapNotNull { clazz ->
                    clazz.declaredFields.firstOrNull { candidate ->
                        candidate.name == name
                    }
                }
                .firstOrNull()
                ?: return null
        return runCatching {
            field.isAccessible = true
            field.get(target)
        }.getOrNull()
    }

    private fun Class<*>.allMethods(): Sequence<Method> =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredMethods.asSequence() }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal data class SlotEntry(
        val index: Int,
        val name: String,
        val raw: String,
    ) {
        val compact: String
            get() = index.toString() + ":" + name
    }

    internal data class Snapshot(
        val available: Boolean,
        val reason: String?,
        val iconListClass: String?,
        val slots: List<SlotEntry>,
        val viewOnlySlots: List<String>,
        val methods: List<String>,
        val indexResults: List<String>,
        val groupOrder: List<String>,
    ) {
        val ready: Boolean
            get() =
                available &&
                    slots.isNotEmpty() &&
                    methods.any { method -> method.startsWith("getViewIndex(") }

        val logLine: String
            get() =
                "nativeSlotOrdering available=" + available +
                    " reason=" + (reason ?: "none") +
                    " iconList=" + (iconListClass ?: "none") +
                    " slots=" + slots.joinToString("|") { entry -> entry.compact } +
                    " viewOnly=" + viewOnlySlots.joinToString("|") +
                    " methods=" + methods.joinToString("|") +
                    " indices=" + indexResults.joinToString("|") +
                    " groupOrder=" + groupOrder.joinToString("|") +
                    " nativeGeometryWrites=0"

        companion object {
            fun unavailable(reason: String): Snapshot =
                Snapshot(
                    available = false,
                    reason = reason,
                    iconListClass = null,
                    slots = emptyList(),
                    viewOnlySlots = emptyList(),
                    methods = emptyList(),
                    indexResults = emptyList(),
                    groupOrder = emptyList(),
                )
        }
    }
}
