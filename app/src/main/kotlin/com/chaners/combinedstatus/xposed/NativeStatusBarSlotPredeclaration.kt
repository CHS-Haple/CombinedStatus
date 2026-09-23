package com.chaners.combinedstatus.xposed

internal object NativeStatusBarSlotPredeclaration {
    const val STATUS_BAR_ICON_LIST =
        "com.android.systemui.statusbar.phone.ui.StatusBarIconList"

    private const val SLOTS_FIELD = "mSlots"
    private const val VIEW_ONLY_SLOTS_FIELD = "mViewOnlySlots"

    fun reserveTail(
        iconList: Any,
        slot: String,
    ): ReservationResult {
        if (iconList.javaClass.name != STATUS_BAR_ICON_LIST) {
            return ReservationResult.Failure(
                Result.Failure("status-bar-icon-list-type-mismatch"),
            )
        }

        @Suppress("UNCHECKED_CAST")
        val slots =
            readField(iconList, SLOTS_FIELD) as? MutableList<Any?>
                ?: return ReservationResult.Failure(
                    Result.Failure("slot-list-unreadable"),
                )
        val viewOnlySlots =
            readField(iconList, VIEW_ONLY_SLOTS_FIELD) as? List<*>
                ?: return ReservationResult.Failure(
                    Result.Failure("view-only-slot-list-unreadable"),
                )
        if (slots.size != viewOnlySlots.size) {
            return ReservationResult.Failure(
                Result.Failure("slot-list-size-mismatch"),
            )
        }

        val existingIndex =
            slots.indexOfFirst { value -> slotName(value) == slot }
        if (existingIndex >= 0) {
            val synced =
                viewOnlySlots.size == slots.size &&
                    slotName(viewOnlySlots.getOrNull(existingIndex)) == slot
            if (!synced) {
                return ReservationResult.Failure(
                    Result.Failure("existing-slot-view-sync-mismatch"),
                )
            }
            return ReservationResult.Ready(
                reservation = Reservation.noOp(),
                result =
                    Result.Ready(
                        created = false,
                        fromIndex = existingIndex,
                        toIndex = existingIndex,
                        slotCount = slots.size,
                        viewOnlySynced = true,
                    ),
            )
        }

        val original = slots.toList()
        val getSlot =
            iconList.javaClass.declaredMethods
                .firstOrNull { method ->
                    method.name == "getSlot" &&
                        method.parameterTypes.contentEquals(
                            arrayOf<Class<*>>(String::class.java),
                        )
                }
                ?: return ReservationResult.Failure(
                    Result.Failure("native-get-slot-method-missing"),
                )

        val created =
            runCatching {
                getSlot.isAccessible = true
                getSlot.invoke(iconList, slot)
            }.getOrElse { error ->
                return ReservationResult.Failure(
                    Result.Failure(
                        "native-get-slot-" +
                            (error.message ?: error.javaClass.simpleName),
                    ),
                )
            }

        val createdIndex =
            slots.indexOfFirst { value ->
                value === created || slotName(value) == slot
            }
        if (
            createdIndex < 0 ||
            slots.size != original.size + 1
        ) {
            restore(slots, original)
            return ReservationResult.Failure(
                Result.Failure("native-slot-create-verification-failed"),
            )
        }

        val mutation =
            runCatching {
                val createdSlot = slots.removeAt(createdIndex)
                slots.add(createdSlot)
            }
        if (mutation.isFailure) {
            restore(slots, original)
            return ReservationResult.Failure(
                Result.Failure(
                    "slot-tail-placement-" +
                        (mutation.exceptionOrNull()?.javaClass?.simpleName ?: "failed"),
                ),
            )
        }

        val toIndex = slots.lastIndex
        val synced =
            slots.size == original.size + 1 &&
                slotName(slots.lastOrNull()) == slot &&
                viewOnlySlots.size == slots.size &&
                slotName(viewOnlySlots.lastOrNull()) == slot
        if (!synced) {
            val restored = restore(slots, original)
            return ReservationResult.Failure(
                Result.Failure(
                    if (restored) {
                        "slot-tail-verification-failed"
                    } else {
                        "slot-tail-verification-and-rollback-failed"
                    },
                ),
            )
        }

        return ReservationResult.Ready(
            reservation = Reservation(slots, original),
            result =
                Result.Ready(
                    created = true,
                    fromIndex = createdIndex,
                    toIndex = toIndex,
                    slotCount = slots.size,
                    viewOnlySynced = true,
                ),
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

    internal class Reservation private constructor(
        private val slots: MutableList<Any?>?,
        private val original: List<Any?>?,
    ) {
        fun rollback(): Boolean {
            val targetSlots = slots ?: return true
            val targetOriginal = original ?: return true
            return restore(targetSlots, targetOriginal)
        }

        companion object {
            fun noOp(): Reservation = Reservation(null, null)
        }

        constructor(
            slots: MutableList<Any?>,
            original: List<Any?>,
        ) : this(
            slots = slots as MutableList<Any?>?,
            original = original as List<Any?>?,
        )
    }

    internal sealed interface ReservationResult {
        data class Ready(
            val reservation: Reservation,
            val result: Result.Ready,
        ) : ReservationResult

        data class Failure(
            val result: Result.Failure,
        ) : ReservationResult
    }

    internal sealed interface Result {
        val logLine: String

        data class Ready(
            val created: Boolean,
            val fromIndex: Int,
            val toIndex: Int,
            val slotCount: Int,
            val viewOnlySynced: Boolean,
        ) : Result {
            override val logLine: String
                get() =
                    "nativeSlotOrder predeclare slot=" +
                        SystemUiNativeCombinedParticipantOwner.SLOT +
                        " created=" + created +
                        " from=" + fromIndex +
                        " to=" + toIndex +
                        " slots=" + slotCount +
                        " viewOnlySynced=" + viewOnlySynced +
                        " mode=controller-pre-init nativeGeometryWrites=0"
        }

        data class Failure(
            val reason: String,
        ) : Result {
            override val logLine: String
                get() =
                    "nativeSlotOrder unchanged slot=" +
                        SystemUiNativeCombinedParticipantOwner.SLOT +
                        " reason=" + reason +
                        " mode=controller-pre-init nativeGeometryWrites=0"
        }
    }
}
