package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterRenderSessionTest {
    @Test
    fun renderPreparationDoesNotRequirePresentationOwnership() {
        assertTrue(
            CombinedStatusControlCenterRenderSession.resolveRenderReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.resolveRenderReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = false,
                layoutReady = true,
                hostAttached = true,
            ),
        )
    }

    @Test
    fun projectionVisibilityRequiresOwnedQsFakePresentation() {
        assertTrue(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                presentationOwned = true,
            ),
        )
        assertFalse(
            CombinedStatusControlCenterRenderSession.resolveProjectionReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
                presentationOwned = false,
            ),
        )
    }
}
