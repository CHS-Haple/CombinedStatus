package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeParticipantRuntimeAccessTest {
    @Test
    fun classifiesHyperOsContentSlotResourceSetter() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "setIcon",
                CharSequence::class.java,
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
            )

        assertEquals(
            NativeParticipantRuntimeAccess.ResourceSetIconMode.CONTENT_SLOT_RES,
            NativeParticipantRuntimeAccess.classifyResourceSetIcon(method),
        )
    }

    @Test
    fun classifiesAospSlotResourceContentSetter() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "setIcon",
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
                CharSequence::class.java,
            )

        assertEquals(
            NativeParticipantRuntimeAccess.ResourceSetIconMode.SLOT_RES_CONTENT,
            NativeParticipantRuntimeAccess.classifyResourceSetIcon(method),
        )
    }

    @Test
    fun classifiesHyperOsRemoveAllWithPipelineFlag() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeAllIconsForSlot",
                String::class.java,
                requireNotNull(Boolean::class.javaPrimitiveType),
            )

        assertEquals(
            NativeParticipantRuntimeAccess.RemovalMode.REMOVE_ALL_SLOT_PIPELINE_FLAG,
            NativeParticipantRuntimeAccess.classifyRemoval(method),
        )
    }

    @Test
    fun classifiesTaggedRemovalFallback() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "removeIcon",
                String::class.java,
                requireNotNull(Int::class.javaPrimitiveType),
            )

        assertEquals(
            NativeParticipantRuntimeAccess.RemovalMode.REMOVE_TAGGED,
            NativeParticipantRuntimeAccess.classifyRemoval(method),
        )
    }

    @Test
    fun rejectsUnrelatedMethods() {
        val method =
            Fixture::class.java.getDeclaredMethod(
                "other",
                String::class.java,
            )

        assertNull(
            NativeParticipantRuntimeAccess.classifyResourceSetIcon(method),
        )
        assertNull(
            NativeParticipantRuntimeAccess.classifyRemoval(method),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private class Fixture {
        fun setIcon(
            contentDescription: CharSequence,
            slot: String,
            resourceId: Int,
        ) = Unit

        fun setIcon(
            slot: String,
            resourceId: Int,
            contentDescription: CharSequence,
        ) = Unit

        fun removeAllIconsForSlot(
            slot: String,
            fromNewPipeline: Boolean,
        ) = Unit

        fun removeIcon(
            slot: String,
            tag: Int,
        ) = Unit

        fun other(slot: String) = Unit
    }
}
