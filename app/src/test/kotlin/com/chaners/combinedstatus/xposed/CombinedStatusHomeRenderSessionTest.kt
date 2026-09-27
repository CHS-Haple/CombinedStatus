package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusHomeRenderSessionTest {
    @Test
    fun ownerReadinessDependsOnlyOnStructuralHomeRequirements() {
        assertTrue(
            CombinedStatusHomeRenderSession.resolveOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOwnerReady(
                featureEnabled = false,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = false,
                hostAttached = true,
            ),
        )
    }

    @Test
    fun overlayVisibilityRequiresSettledNotificationShade() {
        assertTrue(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )

        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = false,
                controlCenterAllowsHome = true,
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
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = false,
                notificationShadeAllowsHome = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                sceneAllowsOverlay = true,
                notificationShadeAllowsHome = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = true,
            ),
        )
    }
}
