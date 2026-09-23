package com.chaners.combinedstatus.xposed

import android.view.View
import java.lang.reflect.Method

internal object NativeStatusBarSlotOrderingProbe {
    private const val MAX_SLOTS = 64
    private const val MAX_METHODS = 64
    private const val MAX_FIELDS = 48
    private const val MAX_SLOT_ELEMENT_TYPES = 8
    private const val MAX_SLOT_ELEMENT_MEMBERS = 24

    private val contractKeywords =
        listOf(
            "slot",
            "index",
            "view",
            "icon",
            "holder",
            "tag",
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

        val slotsRaw = readField(iconList, "mSlots") as? List<*>
        val slots =
            slotsRaw
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
        val viewOnlyList = viewOnlyRaw as? List<*>
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
        val listRelation =
            inspectListRelation(
                slots = slotsRaw,
                viewOnly = viewOnlyList,
            )

        val constructors =
            iconList.javaClass.declaredConstructors
                .map { constructor ->
                    constructor.parameterTypes.joinToString(
                        prefix = iconList.javaClass.simpleName + "(",
                        postfix = ")",
                    ) { type -> type.name }
                }
                .sorted()

        val fields =
            iconList.javaClass
                .allFields()
                .map { field ->
                    field.declaringClass.simpleName + "." +
                        field.name + ":" + field.type.name
                }
                .distinct()
                .sorted()
                .take(MAX_FIELDS)
                .toList()

        val methods =
            iconList.javaClass
                .allMethods()
                .filter(::isContractMethod)
                .map(NativeParticipantRuntimeAccess::methodSignature)
                .distinct()
                .sorted()
                .take(MAX_METHODS)
                .toList()

        val slotElementContracts =
            slotsRaw
                .orEmpty()
                .asSequence()
                .filterNotNull()
                .map { value -> value.javaClass }
                .distinctBy { clazz -> clazz.name }
                .take(MAX_SLOT_ELEMENT_TYPES)
                .map(::describeSlotElementClass)
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
                                Integer.TYPE,
                                String::class.java,
                            ),
                        ) &&
                        method.returnType == Integer.TYPE
                }
                ?.apply { isAccessible = true }

        val rawSlotIndices =
            slots.associate { entry -> entry.name to entry.index }

        val indexResults =
            candidates.map { slot ->
                val slotIndex =
                    slotIndexMethod
                        ?.let { method ->
                            runCatching {
                                (method.invoke(iconList, slot) as Number).toInt()
                            }.getOrNull()
                        }
                val rawSlotIndex = rawSlotIndices[slot]
                val viewIndexTagZero =
                    viewIndexMethod
                        ?.let { method ->
                            runCatching {
                                (method.invoke(iconList, 0, slot) as Number).toInt()
                            }.getOrNull()
                        }
                val viewIndexRawSlot =
                    rawSlotIndex
                        ?.let { index ->
                            viewIndexMethod
                                ?.let { method ->
                                    runCatching {
                                        (method.invoke(iconList, index, slot) as Number).toInt()
                                    }.getOrNull()
                                }
                        }
                slot +
                    ":rawSlotIndex=" + (rawSlotIndex?.toString() ?: "na") +
                    ",slotIndex=" + (slotIndex?.toString() ?: "na") +
                    ",viewIndexTag0=" +
                    (viewIndexTagZero?.toString() ?: "na") +
                    ",viewIndexRawSlot=" +
                    (viewIndexRawSlot?.toString() ?: "na")
            }

        val groupOrder = inspectGroup(handles.group)

        return Snapshot(
            available = true,
            reason = null,
            iconListClass = iconList.javaClass.name,
            slots = slots,
            viewOnlySlots = viewOnlySlots,
            methods = methods,
            constructors = constructors,
            fields = fields,
            slotElementContracts = slotElementContracts,
            listRelation = listRelation,
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

    private fun isContractMethod(method: Method): Boolean {
        val lower = method.name.lowercase()
        return contractKeywords.any(lower::contains)
    }

    private fun inspectListRelation(
        slots: List<*>?,
        viewOnly: List<*>?,
    ): ListRelation {
        if (slots == null || viewOnly == null) {
            return ListRelation(
                slotsClass = slots?.javaClass?.name,
                viewOnlyClass = viewOnly?.javaClass?.name,
                sameIdentity = false,
                sameSize = slots?.size == viewOnly?.size,
                sameElementIdentity = false,
                backingFields = emptyList(),
            )
        }

        val sameSize = slots.size == viewOnly.size
        val sameElementIdentity =
            sameSize &&
                slots.indices.all { index ->
                    slots[index] === viewOnly[index]
                }

        return ListRelation(
            slotsClass = slots.javaClass.name,
            viewOnlyClass = viewOnly.javaClass.name,
            sameIdentity = slots === viewOnly,
            sameSize = sameSize,
            sameElementIdentity = sameElementIdentity,
            backingFields =
                findBackingReferences(
                    target = viewOnly,
                    expected = slots,
                    maxDepth = 2,
                ),
        )
    }

    private fun findBackingReferences(
        target: Any,
        expected: Any,
        maxDepth: Int,
    ): List<String> {
        val visited = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<Any, Boolean>(),
        )

        fun scan(
            value: Any,
            path: String,
            depth: Int,
        ): List<String> {
            if (!visited.add(value) || depth > maxDepth) {
                return emptyList()
            }

            return value.javaClass
                .allFields()
                .mapNotNull { field ->
                    val fieldValue =
                        runCatching {
                            field.isAccessible = true
                            field.get(value)
                        }.getOrNull()
                        ?: return@mapNotNull null
                    val fieldPath =
                        if (path.isEmpty()) {
                            field.declaringClass.simpleName + "." + field.name
                        } else {
                            path + "." + field.declaringClass.simpleName + "." + field.name
                        }

                    when {
                        fieldValue === expected -> listOf(fieldPath)
                        depth < maxDepth &&
                            fieldValue !== value &&
                            fieldValue is Collection<*> ->
                            scan(fieldValue, fieldPath, depth + 1)
                        else -> emptyList()
                    }
                }
                .flatten()
                .distinct()
                .sorted()
                .toList()
        }

        return scan(target, "", 0)
    }

    private fun describeSlotElementClass(clazz: Class<*>): String {
        val constructors =
            clazz.declaredConstructors
                .map { constructor ->
                    constructor.parameterTypes.joinToString(
                        prefix = "(",
                        postfix = ")",
                    ) { type -> type.name }
                }
                .sorted()
                .take(8)
                .joinToString(";")

        val fields =
            clazz
                .allFields()
                .map { field -> field.name + ":" + field.type.name }
                .distinct()
                .sorted()
                .take(MAX_SLOT_ELEMENT_MEMBERS)
                .joinToString(";")

        val methods =
            clazz
                .allMethods()
                .filter(::isContractMethod)
                .map(NativeParticipantRuntimeAccess::methodSignature)
                .distinct()
                .sorted()
                .take(MAX_SLOT_ELEMENT_MEMBERS)
                .joinToString(";")

        return clazz.name +
            "{constructors=" + constructors +
            ",fields=" + fields +
            ",methods=" + methods +
            "}"
    }

    private fun inspectGroup(group: android.view.ViewGroup): List<String> =
        buildList {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                val slot = slotOf(child) ?: continue
                val screen = IntArray(2)
                runCatching { child.getLocationOnScreen(screen) }
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
                        child.measuredWidth + "x" + child.measuredHeight +
                        ",translationX=" + child.translationX +
                        ",screenX=" + screen[0],
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

    private fun Class<*>.allFields() =
        generateSequence(this) { clazz -> clazz.superclass }
            .flatMap { clazz -> clazz.declaredFields.asSequence() }
            .filterNot { field -> field.isSynthetic }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal data class ListRelation(
        val slotsClass: String?,
        val viewOnlyClass: String?,
        val sameIdentity: Boolean,
        val sameSize: Boolean,
        val sameElementIdentity: Boolean,
        val backingFields: List<String>,
    ) {
        val compact: String
            get() =
                "slotsClass=" + (slotsClass ?: "none") +
                    ",viewOnlyClass=" + (viewOnlyClass ?: "none") +
                    ",sameIdentity=" + sameIdentity +
                    ",sameSize=" + sameSize +
                    ",sameElementIdentity=" + sameElementIdentity +
                    ",backingFields=" +
                    if (backingFields.isEmpty()) "none" else backingFields.joinToString(";")

        companion object {
            fun unavailable(): ListRelation =
                ListRelation(
                    slotsClass = null,
                    viewOnlyClass = null,
                    sameIdentity = false,
                    sameSize = false,
                    sameElementIdentity = false,
                    backingFields = emptyList(),
                )
        }
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
        val constructors: List<String>,
        val fields: List<String>,
        val slotElementContracts: List<String>,
        val listRelation: ListRelation,
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
                    " constructors=" + constructors.joinToString("|") +
                    " fields=" + fields.joinToString("|") +
                    " methods=" + methods.joinToString("|") +
                    " slotElementContracts=" + slotElementContracts.joinToString("|") +
                    " listRelation=" + listRelation.compact +
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
                    constructors = emptyList(),
                    fields = emptyList(),
                    slotElementContracts = emptyList(),
                    listRelation = ListRelation.unavailable(),
                    indexResults = emptyList(),
                    groupOrder = emptyList(),
                )
        }
    }
}
