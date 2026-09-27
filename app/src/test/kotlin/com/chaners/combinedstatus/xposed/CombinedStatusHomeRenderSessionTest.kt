package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusHomeRenderSessionTest {
    @Test
    fun overlayVisibilityRequiresSettledNotificationShade() {
        assertTrue(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )

        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = false,
                nativeHandoffActive = false,
            ),
        )
    }

    @Test
    fun overlayVisibilityStillHonorsExistingFeatureSceneAndHandoffGates() {
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = false,
                notificationShadeAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = true,
                nativeHandoffActive = true,
            ),
        )
    }
}
