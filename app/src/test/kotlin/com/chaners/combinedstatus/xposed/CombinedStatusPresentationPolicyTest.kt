package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusPresentationPolicyTest {
    @Test
    fun incompleteCandidateKeepsLastStableModel() {
        val previous = model(wifiSegments = 3, mobileLevel = 4)

        val resolved =
            CombinedStatusPresentationPolicy.resolveModel(
                previous = previous,
                candidate = null,
            )

        assertSame(previous, resolved)
    }

    @Test
    fun explicitHiddenOrUnavailableModelStillCommits() {
        val previous = model(wifiSegments = 3, mobileLevel = 4)
        val candidate = model(wifiSegments = null, mobileLevel = null)

        val resolved =
            CombinedStatusPresentationPolicy.resolveModel(
                previous = previous,
                candidate = candidate,
            )

        assertEquals(candidate, resolved)
    }

    @Test
    fun initialIncompleteStateRemainsNotRenderable() {
        assertNull(
            CombinedStatusPresentationPolicy.resolveModel(
                previous = null,
                candidate = null,
            ),
        )
    }

    @Test
    fun transparentTintDoesNotReplaceLastStableTint() {
        val previous = CombinedStatusTintState(0xe6ffffff.toInt())
        val transparent = CombinedStatusTintState(0x00000000)

        val resolved =
            CombinedStatusPresentationPolicy.resolveTint(
                previous = previous,
                candidate = transparent,
            )

        assertSame(previous, resolved)
    }

    @Test
    fun initialTransparentTintIsRejected() {
        assertNull(
            CombinedStatusPresentationPolicy.resolveTint(
                previous = null,
                candidate = CombinedStatusTintState(0x00000000),
            ),
        )
    }

    @Test
    fun nonTransparentTintCommitsImmediately() {
        val candidate = CombinedStatusTintState(0xbf000000.toInt())

        val resolved =
            CombinedStatusPresentationPolicy.resolveTint(
                previous = CombinedStatusTintState(0xe6ffffff.toInt()),
                candidate = candidate,
            )

        assertEquals(candidate, resolved)
        assertTrue(CombinedStatusPresentationPolicy.isValidTint(candidate))
    }

    private fun model(
        wifiSegments: Int?,
        mobileLevel: Int?,
    ) =
        CombinedStatusRenderModel(
            batteryPercent = 83,
            charging = false,
            wifiSegments = wifiSegments,
            mobileLevel = mobileLevel,
            mobileSubscriptionId = 4,
        )
}
