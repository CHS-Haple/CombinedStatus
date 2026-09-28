package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
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
    fun overlayVisibilityDoesNotDependOnControlCenterSceneState() {
        assertTrue(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                nativeHandoffActive = false,
            ),
        )
    }

    @Test
    fun overlayVisibilityHonorsFeatureAndHandoffGates() {
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            CombinedStatusHomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                nativeHandoffActive = true,
            ),
        )
    }

    @Test
    fun transferredTintWinsWithoutReadingTransientLiveState() {
        var liveReads = 0
        val transferred =
            CombinedStatusTintState(
                appliedTint = 0xbf112233.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val seed =
            CombinedStatusHomeRenderSession.resolveInitialTintSeed(
                transferred = transferred,
                allowLiveSeed = false,
                liveState = {
                    liveReads += 1
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                    )
                },
            )

        assertEquals("hotReloadTransfer", seed?.source)
        assertEquals(transferred, seed?.state)
        assertEquals(0, liveReads)
    }

    @Test
    fun invalidTransferredTintFallsBackToLiveNativeSeed() {
        var liveReads = 0
        val live =
            CombinedStatusTintState(
                appliedTint = 0xe6ffffff.toInt(),
            )

        val seed =
            CombinedStatusHomeRenderSession.resolveInitialTintSeed(
                transferred =
                    CombinedStatusTintState(
                        appliedTint = 0x00112233,
                    ),
                allowLiveSeed = true,
                liveState = {
                    liveReads += 1
                    live
                },
            )

        assertEquals("seed", seed?.source)
        assertEquals(live, seed?.state)
        assertEquals(1, liveReads)
    }

    @Test
    fun legacyHotReloadWithoutTransferredTintWaitsForNativeEvent() {
        var liveReads = 0

        val seed =
            CombinedStatusHomeRenderSession.resolveInitialTintSeed(
                transferred = null,
                allowLiveSeed = false,
                liveState = {
                    liveReads += 1
                    CombinedStatusTintState(
                        appliedTint = 0xbf000000.toInt(),
                    )
                },
            )

        assertEquals(null, seed)
        assertEquals(0, liveReads)
    }
}
