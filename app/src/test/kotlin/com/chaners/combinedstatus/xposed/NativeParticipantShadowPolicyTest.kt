package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeParticipantShadowPolicyTest {
    @Test
    fun semanticVisibilityMustBeExplicitlyFalse() {
        assertTrue(
            NativeParticipantShadowPolicy.isSemanticallyHidden(false),
        )
        assertFalse(
            NativeParticipantShadowPolicy.isSemanticallyHidden(true),
        )
        assertFalse(
            NativeParticipantShadowPolicy.isSemanticallyHidden(null),
        )
    }

    @Test
    fun hiddenOrZeroWidthRootDoesNotOccupyLayout() {
        assertTrue(
            NativeParticipantShadowPolicy.isLayoutHidden(
                rootVisible = false,
                measuredWidth = 75,
            ),
        )
        assertTrue(
            NativeParticipantShadowPolicy.isLayoutHidden(
                rootVisible = true,
                measuredWidth = 0,
            ),
        )
        assertFalse(
            NativeParticipantShadowPolicy.isLayoutHidden(
                rootVisible = true,
                measuredWidth = 75,
            ),
        )
    }
}
