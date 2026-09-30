package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterTransitionOwnerTest {
    @Test
    fun shrinkOnlyScalePolicyNeverEnlargesSemanticGlyphs() {
        val source = geometry(width = 10f, height = 10f)
        val target = geometry(width = 30f, height = 20f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = CombinedStatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )

        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun targetScalePolicyStillAllowsWifiOpticalConvergence() {
        val source = geometry(width = 10f, height = 10f)
        val target = geometry(width = 20f, height = 20f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
            )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
    }

    @Test
    fun roundedCapsAreIncludedInsideTheNativeOpticalHeightBudget() {
        assertEquals(
            45f,
            CombinedStatusPainter.MobileSignalMorphPolicy.targetMaxBarHeight(
                sourceBoundsHeight = 40f,
                diameter = 10f,
                targetHeightRatio = 1.25f,
            ),
            0.0001f,
        )
    }

    @Test
    fun reservationFollowsNativeProgressWithoutASecondSemanticTimeline() {
        val width =
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 10,
                spans =
                    listOf(
                        CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                            sourceLeft = -10f,
                            sourceRight = 0f,
                            targetLeft = -30f,
                            targetRight = 0f,
                        ),
                    ),
                progress = 0.5f,
            )
        assertEquals(20, width)
    }

    @Test
    fun unmatchedComponentsExitFastWithoutChangingTheirScale() {
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(0f),
            0.0001f,
        )
        assertEquals(
            0.125f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitOpacity(1f),
            0.0001f,
        )
    }

    @Test
    fun carrierRelativeInterpolationInheritsOnlyNativeCarrierCenterMotion() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 100f)
        val currentCarrier =
            floatArrayOf(130f, 120f, 140f, 0f, 0f, 169f)
        val targetCarrier =
            floatArrayOf(200f, 150f, 120f, 0f, 0f, 108f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarrierRelativeGeometry(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    targetCarrier = targetCarrier,
                    progress = 0.5f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(160f, result[0], 0.0001f)
        assertEquals(130f, result[1], 0.0001f)
        assertEquals(15f, result[2], 0.0001f)
        assertEquals(15f, result[5], 0.0001f)
    }

    @Test
    fun carrierRelativeInterpolationDoesNotRescaleSourceOffsetAtGestureStart() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 65f, 140f, 0f, 0f, 169f)
        val targetCarrier =
            floatArrayOf(200f, 150f, 120f, 0f, 0f, 108f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarrierRelativeGeometry(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    targetCarrier = targetCarrier,
                    progress = 0f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(85f, result[1], 0.0001f)
        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun carrierRelativeInterpolationLandsExactlyOnTargetWhenCarriersConverge() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val targetCarrier =
            floatArrayOf(200f, 150f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarrierRelativeGeometry(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = targetCarrier,
                    targetCarrier = targetCarrier,
                    progress = 1f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(target[2], result[2], 0.0001f)
        assertEquals(target[5], result[5], 0.0001f)
    }

    @Test
    fun latentParticipantUsesFinalTargetBasisBeforeItBecomesVisible() {
        val path =
            floatArrayOf(
                42f,
                73f,
                30f,
                0f,
                0f,
                30f,
            )
        val target =
            floatArrayOf(
                80f,
                90f,
                18f,
                2f,
                -2f,
                18f,
            )

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .latentTargetSizedGeometry(
                    pathGeometry = path,
                    targetGeometry = target,
                )

        assertEquals(path[0], result[0], 0.0001f)
        assertEquals(path[1], result[1], 0.0001f)
        assertEquals(target[2], result[2], 0.0001f)
        assertEquals(target[3], result[3], 0.0001f)
        assertEquals(target[4], result[4], 0.0001f)
        assertEquals(target[5], result[5], 0.0001f)
    }

    @Test
    fun latentParticipantWaitsForOneNativeSlotThenRevealsQuickly() {
        val carriedSource = floatArrayOf(100f, 50f, 20f, 0f, 0f, 20f)
        val beforeSlot = floatArrayOf(174f, 80f, 20f, 0f, 0f, 20f)
        val slotReady = floatArrayOf(175f, 80f, 20f, 0f, 0f, 20f)
        val fastReveal = floatArrayOf(194f, 80f, 20f, 0f, 0f, 20f)

        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .latentRevealOpacity(
                    carriedSource = carriedSource,
                    current = beforeSlot,
                    nativeSlotWidth = 75f,
                ),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .latentRevealOpacity(
                    carriedSource = carriedSource,
                    current = slotReady,
                    nativeSlotWidth = 75f,
                ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .latentRevealOpacity(
                    carriedSource = carriedSource,
                    current = fastReveal,
                    nativeSlotWidth = 75f,
                ),
            0.0001f,
        )
    }

    @Test
    fun nativeTargetHeightCanBoundLocalShapeWithoutOwningItsExactScale() {
        val ratio =
            CombinedStatusControlCenterTransitionOwner.Policy.relativeGeometryHeight(
                target = geometry(width = 20f, height = 50f),
                current = geometry(width = 10f, height = 20f),
            )

        assertEquals(2.5f, ratio ?: -1f, 0.0001f)
    }

    private fun geometry(
        width: Float,
        height: Float,
    ): FloatArray =
        floatArrayOf(
            0f,
            0f,
            width,
            0f,
            0f,
            height,
        )

    @Test
    fun steadyTransitionSourceUsesHostEndSlotInsteadOfInnerBatteryCenter() {
        val host = floatArrayOf(300f, 54f, 600f, 0f, 0f, 108f)

        val ltr =
            CombinedStatusControlCenterTransitionOwner.Policy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = false,
            )
        requireNotNull(ltr)
        assertEquals(547.5f, ltr[0], 0.0001f)
        assertEquals(54f, ltr[1], 0.0001f)
        assertEquals(105f, ltr[2], 0.0001f)
        assertEquals(108f, ltr[5], 0.0001f)

        val rtl =
            CombinedStatusControlCenterTransitionOwner.Policy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = true,
            )
        requireNotNull(rtl)
        assertEquals(52.5f, rtl[0], 0.0001f)
    }

    @Test
    fun sourceGeometryKeepsNativePositionButUsesStableRenderBasis() {
        val nativePosition = floatArrayOf(100f, 200f, 60f, 0f, 0f, 40f)
        val stableRenderBasis = floatArrayOf(900f, 900f, 105f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.composeSourceGeometry(
                positionAuthority = nativePosition,
                basisAuthority = stableRenderBasis,
            )

        assertEquals(100f, result[0], 0.0001f)
        assertEquals(200f, result[1], 0.0001f)
        assertEquals(105f, result[2], 0.0001f)
        assertEquals(169f, result[5], 0.0001f)
    }

    @Test
    fun semanticFallbackSeparatesMobileTypeAndSignalInsteadOfSharingSlotCenter() {
        val type =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type_single", "mobile_type"),
                isRtl = false,
            )
        val signal =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_signal"),
                isRtl = false,
            )
        requireNotNull(type)
        requireNotNull(signal)

        assertTrue(type.right < signal.left)

        val rtlType =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type"),
                isRtl = true,
            )
        val rtlSignal =
            CombinedStatusControlCenterTransitionOwner.Policy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_signal"),
                isRtl = true,
            )
        requireNotNull(rtlType)
        requireNotNull(rtlSignal)
        assertTrue(rtlSignal.right < rtlType.left)
    }

    @Test
    fun hyperCeilerDualSignalCompatibilityRequiresItsStructuralSignature() {
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = false,
                candidateVisible = true,
                candidateHasResourceEntry = false,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = true,
                candidateVisible = true,
                candidateHasResourceEntry = false,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy.isHyperCeilerDualSignalStructure(
                nativeSignalVisible = false,
                candidateVisible = true,
                candidateHasResourceEntry = true,
                directChildCount = 2,
                directImageChildCount = 2,
            ),
        )
    }

    @Test
    fun chargingIslandReservationAuthorityIsSceneSpecific() {
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeIslandShowing = true,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeIslandShowing = false,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = false,
                    nativeIslandShowing = true,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    charging = true,
                    nativeIslandShowing = true,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(CombinedStatusSourceScene.HOME),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(CombinedStatusSourceScene.KEYGUARD),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .usesProgressSynchronousReservation(CombinedStatusSourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceCanOnlyCatchOutwardGeometryUp() {
        assertEquals(
            0.62f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.82f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.62f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = false,
            ),
            0.0001f,
        )
    }
}
