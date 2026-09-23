package com.chaners.combinedstatus.xposed

internal object SystemUiNativeSlotOrderRuntimeOwner {
    private const val STATUS_BAR_ICON_LIST_FIELD = "mStatusBarIconList"
    private const val SLOTS_FIELD = "mSlots"
    private const val VIEW_ONLY_SLOTS_FIELD = "mViewOnlySlots"
    private const val ICON_GROUPS_FIELD = "mIconGroups"

    @Synchronized
    fun moveSlotToTail(
        controller: Any,
        slot: String,
    ): ReorderResult {
        val iconGroups =
            readField(controller, ICON_GROUPS_FIELD) as? Collection<*>
                ?: return ReorderResult.Failure("icon-groups-unreadable")
        if (iconGroups.isNotEmpty()) {
            return ReorderResult.Failure("icon-groups-already-registered")
        }

        val iconList =
            readField(controller, STATUS_BAR_ICON_LIST_FIELD)
                ?: return ReorderResult.Failure("status-bar-icon-list-missing")

        @Suppress("UNCHECKED_CAST")
        val slots =
            readField(iconList, SLOTS_FIELD) as? MutableList<Any?>
                ?: return ReorderResult.Failure("slot-list-unreadable")
        val viewOnlySlots =
            readField(iconList, VIEW_ONLY_SLOTS_FIELD) as? List<*>
                ?: return ReorderResult.Failure("view-only-slot-list-unreadable")

        if (slots.size != viewOnlySlots.size) {
            return ReorderResult.Failure("slot-list-size-mismatch")
        }

        val fromIndex = slots.indexOfFirst { value -> slotName(value) == slot }
        if (fromIndex < 0) {
            return ReorderResult.Failure("slot-missing")
        }

        val original = slots.toList()
        val target = slots[fromIndex]
        val toIndex = slots.lastIndex

        if (fromIndex != toIndex) {
            val mutation =
                runCatching {
                    slots.removeAt(fromIndex)
                    slots.add(target)
                }
            if (mutation.isFailure) {
                restore(slots, original)
                return ReorderResult.Failure(
                    "slot-list-mutation-" +
                        (mutation.exceptionOrNull()?.javaClass?.simpleName ?: "failed"),
                )
            }
        }

        val slotsSynced =
            slots.size == original.size &&
                slotName(slots.lastOrNull()) == slot
        val viewOnlySynced =
            viewOnlySlots.size == slots.size &&
                slotName(viewOnlySlots.lastOrNull()) == slot

        if (!slotsSynced || !viewOnlySynced) {
            val restored = restore(slots, original)
            return ReorderResult.Failure(
                if (restored) {
                    "post-mutation-verification-failed"
                } else {
                    "post-mutation-verification-and-rollback-failed"
                },
            )
        }

        return ReorderResult.Ready(
            fromIndex = fromIndex,
            toIndex = slots.lastIndex,
            slotCount = slots.size,
            iconGroups = iconGroups.size,
            viewOnlySynced = true,
        )
    }

    private fun restore(
        slots: MutableList<Any?>,
        original: List<Any?>,
    ): Boolean =
        runCatching {
            slots.clear()
            slots.addAll(original)
            slots.size == original.size &&
                slots.indices.all { index -> slots[index] === original[index] }
        }.getOrDefault(false)

    private fun slotName(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        listOf("mName", "name", "slot", "mSlot").forEach { fieldName ->
            val candidate = readField(value, fieldName) as? String
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }

        val accessor =
            generateSequence(value.javaClass) { clazz -> clazz.superclass }
                .flatMap { clazz -> clazz.declaredMethods.asSequence() }
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        method.returnType == String::class.java &&
                        method.name in setOf("getName", "getSlot", "getSlotName")
                }
                ?: return null

        return runCatching {
            accessor.isAccessible = true
            accessor.invoke(value) as? String
        }.getOrNull()
    }

    private fun readField(
        target: Any,
        name: String,
    ): Any? {
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

    internal sealed interface ReorderResult {
        val logLine: String

        data class Ready(
            val fromIndex: Int,
            val toIndex: Int,
            val slotCount: Int,
            val iconGroups: Int,
            val viewOnlySynced: Boolean,
        ) : ReorderResult {
            override val logLine: String
                get() =
                    "nativeSlotOrder reorder slot=" +
                        SystemUiNativeCombinedParticipantOwner.SLOT +
                        " from=" + fromIndex +
                        " to=" + toIndex +
                        " slots=" + slotCount +
                        " iconGroups=" + iconGroups +
                        " viewOnlySynced=" + viewOnlySynced +
                        " mode=controller-post-init nativeGeometryWrites=0"
        }

        data class Failure(
            val reason: String,
        ) : ReorderResult {
            override val logLine: String
                get() =
                    "nativeSlotOrder unchanged slot=" +
                        SystemUiNativeCombinedParticipantOwner.SLOT +
                        " reason=" + reason +
                        " mode=controller-post-init nativeGeometryWrites=0"
        }
    }
}
