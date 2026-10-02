package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusControlCenterTransitionOwnerTest {
    @Test
    fun targetTypographyStyleConvergesBeforeNativeHandoff() {
        assertEquals(
            0f,
            CombinedStatusPainter.TransitionTypographyPolicy.styleProgress(0.42f),
            0.0001f,
        )
        assertTrue(
            CombinedStatusPainter.TransitionTypographyPolicy.styleProgress(0.70f) in 0f..1f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.TransitionTypographyPolicy.styleProgress(0.88f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.TransitionTypographyPolicy.styleProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun exactTextGeometryReachesNativeBasisInsteadOfSimilarityEnvelope() {
        val source = geometry(centerX = 10f, centerY = 20f, width = 10f, height = 20f)
        val target = geometry(centerX = 100f, centerY = 200f, width = 30f, height = 24f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy.interpolateGeometry(
                source = source,
                target = target,
                progress = 1f,
            )

        target.forEachIndexed { index, value ->
            assertEquals(value, result[index], 0.0001f)
        }
    }

    @Test
    fun chargingGlyphHidesLateThenMovesQuicklyOnlyWithTarget() {
        val policy = CombinedStatusPainter.BatteryNumberFollowerPolicy

        // Keep the glyph fully visible until the ring is already well into retract.
        assertEquals(
            1f,
            policy.chargingOpacity(
                progress = 0.18f,
                targetAvailable = true,
            ),
            0.0001f,
        )
        assertTrue(
            policy.chargingOpacity(
                progress = 0.205f,
                targetAvailable = true,
            ) in 0f..1f,
        )

        // Full disappearance precedes all visible target travel.
        assertEquals(
            0f,
            policy.chargingOpacity(
                progress = 0.225f,
                targetAvailable = true,
            ),
            0.0001f,
        )
        assertTrue(policy.chargingMotionProgress(0.225f) > 0f)

        // Hidden travel is intentionally short; target reveal only occurs near its end.
        assertTrue(policy.chargingMotionProgress(0.30f) > 0.8f)
        assertTrue(
            policy.chargingOpacity(
                progress = 0.30f,
                targetAvailable = true,
            ) > 0f,
        )

        // Fail-native target policy: no target means fade-out only, never guessed motion/reveal.
        assertEquals(
            0f,
            policy.chargingOpacity(
                progress = 0.30f,
                targetAvailable = false,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            policy.chargingMotionProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            policy.chargingOpacity(
                progress = 0.35f,
                targetAvailable = true,
            ),
            0.0001f,
        )
    }

    @Test
    fun chargingGlyphNeverMovesWhileAnySourceOpacityRemains() {
        val policy = CombinedStatusPainter.BatteryNumberFollowerPolicy
        var observedSourceFade = false

        for (sample in 0..400) {
            val progress = sample / 1000f
            val sourceOpacity = policy.chargingSourceOpacity(progress)
            if (sourceOpacity in 0.0001f..0.9999f) {
                observedSourceFade = true
            }
            if (sourceOpacity > 0f) {
                assertEquals(
                    0f,
                    policy.chargingMotionProgress(progress),
                    0.0001f,
                )
            }
        }

        assertTrue(observedSourceFade)
        assertEquals(0f, policy.chargingSourceOpacity(0.225f), 0.0001f)
        assertTrue(policy.chargingMotionProgress(0.225f) > 0f)
    }

    @Test
    fun mobileTypeWeightInterpolatesToNativeTarget() {
        assertEquals(
            800,
            CombinedStatusPainter.MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 0f,
            ),
        )
        assertEquals(
            650,
            CombinedStatusPainter.MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 0.5f,
            ),
        )
        assertEquals(
            500,
            CombinedStatusPainter.MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 1f,
            ),
        )
    }

    @Test
    fun mobileTypeWeightFailsNativeWhenTargetTypographyIsUnavailable() {
        assertEquals(
            800,
            CombinedStatusPainter.MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = null,
                progress = 1f,
            ),
        )
    }

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
    fun fourBarSnapshotTargetsRemainOrderedAndBounded() {
        val snapshot =
            CombinedStatusParticipantVisualSnapshot.Snapshot(
                envelope =
                    CombinedStatusParticipantVisualSnapshot.NormalizedRect(
                        left = 0.1f,
                        top = 0.2f,
                        right = 0.9f,
                        bottom = 0.9f,
                    ),
                components =
                    listOf(
                        CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.1f, 0.55f, 0.2f, 0.9f),
                        CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.3f, 0.45f, 0.4f, 0.9f),
                        CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.5f, 0.35f, 0.6f, 0.9f),
                        CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.7f, 0.2f, 0.8f, 0.9f),
                    ),
                topology =
                    CombinedStatusParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            )

        val bars = requireNotNull(snapshot.fourVerticalBarsWithinEnvelope())
        assertEquals(4, bars.size)
        assertTrue(bars.zipWithNext().all { (left, right) -> left.centerX < right.centerX })
        assertTrue(bars.all { bar -> bar.left >= 0f && bar.right <= 1f })
        assertTrue(bars.all { bar -> bar.top >= 0f && bar.bottom <= 1f })
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
    fun transitionReservationInterpolatesTotalWidthFromNativeProgress() {
        val spans =
            listOf(
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -10f,
                    sourceRight = 0f,
                    targetLeft = -30f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            10,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0f,
                ),
        )
        assertEquals(
            20,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0.5f,
                ),
        )
        assertEquals(
            30,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 1f,
                ),
        )
    }

    @Test
    fun totalWidthInterpolationAvoidsOverlappingSpanDeadZone() {
        val spans =
            listOf(
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        val oldGeometryUnion =
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.25f,
            )
        val transitionWidth =
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )

        assertEquals(105, oldGeometryUnion)
        assertTrue(transitionWidth > 105)
        assertTrue(transitionWidth < 180)
    }

    @Test
    fun latentRevealRequiresRealVisualReservationAndTargetProximity() {
        val target = geometry(centerX = 100f, centerY = 100f, width = 20f, height = 20f)

        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealOpacity(
                current = geometry(centerX = 95f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealOpacity(
                current = geometry(centerX = 79f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealOpacity(
                current = geometry(centerX = 90f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealOpacity(
                current = geometry(centerX = 93f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0.35f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealOpacity(
                current = target,
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
    }

    @Test
    fun latentRevealAcceleratesAfterOccupancyUnlockWithoutChangingTheGate() {
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0f),
            0.0001f,
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0.2f) > 0.5f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun latentReservationProgressTracksVisibleEnvelopeCoverage() {
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 100,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 150,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 200,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetCompensatesInsideSimilarityBasis() {
        val outerScale =
            CombinedStatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 1.5f,
                targetHeightRatio = 2f,
            )
        assertEquals(1f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 1.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 2f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetPreservesUniformOuterShrinkAndCompensatesAxes() {
        val outerScale =
            CombinedStatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 0.75f,
                targetHeightRatio = 0.5f,
            )
        assertEquals(0.5f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.75f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
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
    fun fakeCapacityLeaseDoesNotChangeEndAnchoredMotionCarrierCenter() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CombinedStatusControlCenterTransitionOwner.Policy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = false,
                )
        requireNotNull(logicalCarrier)

        assertEquals(997f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)

        val source = floatArrayOf(1240f, 55f, 105f, 0f, 0f, 108f)
        val sourceCarrier = floatArrayOf(997f, 54f, 478f, 0f, 0f, 108f)
        val carried =
            CombinedStatusControlCenterTransitionOwner.Policy
                .rebaseSourceToCurrentCarrier(
                    source = source,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = logicalCarrier,
                )
        assertEquals(1240f, carried[0], 0.0001f)
    }

    @Test
    fun fakeCapacityLeaseKeepsRtlMotionCarrierStartAnchored() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CombinedStatusControlCenterTransitionOwner.Policy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = true,
                )
        requireNotNull(logicalCarrier)
        assertEquals(747f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)
    }

    @Test
    fun carriedSourceUsesNativeCarrierMotionBeforeRootTargetInterpolation() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 100f)
        val currentCarrier =
            floatArrayOf(130f, 120f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0.5f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(195f, result[0], 0.0001f)
        assertEquals(145f, result[1], 0.0001f)
        assertEquals(15f, result[2], 0.0001f)
        assertEquals(15f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceDoesNotRescaleSourceOffsetAtGestureStart() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 65f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(85f, result[1], 0.0001f)
        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceFollowsLiveFakeCarrierAtStart() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 58f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(58f, result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun carriedSourceLandsOnAbsoluteRootTargetEvenWhenCarriersDoNotConverge() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(target[2], result[2], 0.0001f)
        assertEquals(target[5], result[5], 0.0001f)
    }

    @Test
    fun latentSingleIconCanLandOnRootTargetWithoutGrowingToLargeSlotBox() {
        val source =
            floatArrayOf(75f, 50f, 20f, 0f, 0f, 20f)
        val target =
            floatArrayOf(235f, 150f, 75f, 0f, 0f, 75f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun latentAdditionalMobileKeepsShrinkOnlyPathBasis() {
        val source = geometry(width = 20f, height = 20f)
        val target = geometry(width = 75f, height = 75f)

        val result =
            CombinedStatusControlCenterTransitionOwner.Policy
                .interpolateSimilarityGeometry(
                    source = source,
                    target = target,
                    progress = 1f,
                    scalePolicy = CombinedStatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
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
        centerX: Float = 0f,
        centerY: Float = 0f,
        width: Float,
        height: Float,
    ): FloatArray =
        floatArrayOf(
            centerX,
            centerY,
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
    fun tinySecondaryComponentsRemainVisibleToTopologyClassifier() {
        val fourBars =
            listOf(
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        val tinyDots =
            listOf(
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.12f, 0.05f, 0.14f, 0.07f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.42f, 0.05f, 0.44f, 0.07f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.72f, 0.05f, 0.74f, 0.07f),
            )

        val retained =
            CombinedStatusParticipantVisualSnapshot.filterProbeComponents(
                components = fourBars + tinyDots,
                probeWidth = 96,
                probeHeight = 96,
            )

        assertEquals(7, retained.size)
        assertEquals(
            CombinedStatusParticipantVisualSnapshot.Topology.COMPOSITE,
            CombinedStatusParticipantVisualSnapshot.classifyComponents(retained),
        )
    }

    @Test
    fun dualRowCompositeCannotExposeExactFourBarCapability() {
        val components =
            listOf(
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.55f, 0.05f, 0.65f, 0.12f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.80f, 0.05f, 0.90f, 0.12f),
            )
        val envelope =
            CombinedStatusParticipantVisualSnapshot.NormalizedRect(
                left = 0.05f,
                top = 0.05f,
                right = 0.90f,
                bottom = 0.95f,
            )
        val snapshot =
            CombinedStatusParticipantVisualSnapshot.Snapshot(
                envelope = envelope,
                components = components,
                topology = CombinedStatusParticipantVisualSnapshot.classifyComponents(components),
            )

        assertEquals(
            CombinedStatusParticipantVisualSnapshot.Topology.COMPOSITE,
            snapshot.topology,
        )
        assertNull(snapshot.fourVerticalBarsWithinEnvelope())
    }

    @Test
    fun participantVisualTopologyDistinguishesFourBarsFromComposite() {
        val fourBars =
            listOf(
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        assertEquals(
            CombinedStatusParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            CombinedStatusParticipantVisualSnapshot.classifyComponents(fourBars),
        )

        val composite =
            fourBars +
                listOf(
                    CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                    CombinedStatusParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                )
        assertEquals(
            CombinedStatusParticipantVisualSnapshot.Topology.COMPOSITE,
            CombinedStatusParticipantVisualSnapshot.classifyComponents(composite),
        )
    }

    @Test
    fun batteryIslandReservationAuthorityUsesExactNativeContract() {
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = false,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = false,
                    nativeBatteryIslandActive = true,
                ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = true,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    charging = false,
                    nativeBatteryIslandActive = null,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    charging = true,
                    nativeBatteryIslandActive = true,
                ),
        )
    }

    @Test
    fun genericHomeIslandOnlyGuardsNativePaddingExpansion() {
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = true,
                    fakeIslandReservationBridgeReady = false,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = true,
                    fakeIslandReservationBridgeReady = true,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = false,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.HOME,
                    genericIslandShowing = null,
                ),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.KEYGUARD,
                    genericIslandShowing = true,
                ),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .allowsNativeTransitionPaddingExpansion(
                    sourceScene = CombinedStatusSourceScene.UNKNOWN,
                    genericIslandShowing = false,
                ),
        )
    }

    @Test
    fun fakeIslandWidthCompensationPreservesRelativeCollisionBoundary() {
        assertEquals(
            220,
            CombinedStatusControlCenterTransitionOwner.Policy
                .compensateFakeIslandWidth(
                    nativeIslandWidthPx = 220,
                    transitionPaddingDeltaPx = 0,
                ),
        )
        assertEquals(
            130,
            CombinedStatusControlCenterTransitionOwner.Policy
                .compensateFakeIslandWidth(
                    nativeIslandWidthPx = 220,
                    transitionPaddingDeltaPx = 90,
                ),
        )
        assertEquals(
            0,
            CombinedStatusControlCenterTransitionOwner.Policy
                .compensateFakeIslandWidth(
                    nativeIslandWidthPx = 220,
                    transitionPaddingDeltaPx = 250,
                ),
        )
        assertEquals(
            -1,
            CombinedStatusControlCenterTransitionOwner.Policy
                .compensateFakeIslandWidth(
                    nativeIslandWidthPx = -1,
                    transitionPaddingDeltaPx = 90,
                ),
        )
    }

    @Test
    fun verifiedSourceScenesKeepSemanticReservationThroughProjection() {
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.HOME),
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.KEYGUARD),
        )
        assertTrue(
            !CombinedStatusControlCenterTransitionOwner.Policy
                .usesSemanticTransitionReservation(CombinedStatusSourceScene.UNKNOWN),
        )
    }

    @Test
    fun nativeFinalAppearanceConsumesOnlyRemainingOutwardDistance() {
        assertEquals(
            0.696f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.2f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            0.9316f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 0.82f,
                finalAppearanceActive = true,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.handoffMotionProgress(
                expansionProgress = 0.62f,
                finalAppearanceAlpha = 1f,
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
