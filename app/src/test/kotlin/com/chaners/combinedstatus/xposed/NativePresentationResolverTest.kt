package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class NativePresentationResolverTest {
    @Test
    fun presentationAndDataSubscriptionIdentitiesCanDiffer() {
        val snapshot =
            NativePresentationResolver.Snapshot(
                mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptionIds = listOf(1, 4),
                presentationRootSubscriptionId = 1,
                effectiveDataSubscriptionId = 4,
                networkTypeSubscriptionId = 4,
                networkType = null,
            )

        assertEquals(1, snapshot.presentationRootSubscriptionId)
        assertEquals(4, snapshot.effectiveDataSubscriptionId)
        assertEquals(4, snapshot.networkTypeSubscriptionId)
    }

    @Test
    fun aggregatedNetworkTypeFollowsDefaultDataSubscription() {
        assertEquals(
            4,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = 1,
                boundSubscriptionIds = listOf(1, 4),
            ),
        )
    }

    @Test
    fun networkTypeFallsBackToPresentationRootWhenDefaultDataBindingIsMissing() {
        assertEquals(
            1,
            NativePresentationResolver.selectNetworkTypeSubscriptionId(
                effectiveDataSubscriptionId = 4,
                presentationRootSubscriptionId = 1,
                boundSubscriptionIds = listOf(1),
            ),
        )
    }

    @Test
    fun singlePresentationIsDetected() {
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativePresentationResolver.classify(
                boundRoots = 1,
                visibleRoots = 1,
                activeSubscriptions = 1,
            ),
        )
    }

    @Test
    fun nativeDualPresentationIsDetected() {
        assertEquals(
            NativePresentationResolver.Mode.DUAL_SEPARATE,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 2,
                activeSubscriptions = 2,
            ),
        )
    }

    @Test
    fun aggregatedDualPresentationRequiresTwoActiveSubscriptions() {
        assertEquals(
            NativePresentationResolver.Mode.DUAL_AGGREGATED,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 2,
            ),
        )
    }

    @Test
    fun preMeasureDoublePlusMatchesNativeNormalization() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "5G++",
                enhanced = false,
                beforeMeasure = true,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }

    @Test
    fun preMeasureRegularTypeDoesNotReuseStaleDoublePlusFlag() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "4G",
                enhanced = true,
                beforeMeasure = true,
            )

        assertEquals("4G", networkType?.label)
        assertEquals(false, networkType?.enhanced)
    }

    @Test
    fun postMeasureKeepsNativeDoublePlusFlag() {
        val networkType =
            NativePresentationResolver.normalizeDrawableNetworkType(
                rawLabel = "5G",
                enhanced = true,
                beforeMeasure = false,
            )

        assertEquals("5G", networkType?.label)
        assertEquals(true, networkType?.enhanced)
    }

    @Test
    fun oneActiveSubscriptionDoesNotPretendToBeAggregatedDual() {
        assertEquals(
            NativePresentationResolver.Mode.SINGLE,
            NativePresentationResolver.classify(
                boundRoots = 2,
                visibleRoots = 1,
                activeSubscriptions = 1,
            ),
        )
    }
}
