package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeFeaturePreferencesOwnerTest {
    @Test
    fun validCrossProcessTimestampProducesTransportLatency() {
        assertEquals(
            6_000_000L,
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 1_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun missingTimestampDoesNotInventLatency() {
        assertNull(
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 0L,
                receivedAtElapsedRealtimeNanos = 1_006_000_000L,
            ),
        )
    }

    @Test
    fun invalidFutureTimestampDoesNotInventLatency() {
        assertNull(
            RuntimeFeaturePreferencesOwner.resolveTransportLatencyNanos(
                changedAtElapsedRealtimeNanos = 2_000_000_000L,
                receivedAtElapsedRealtimeNanos = 1_000_000_000L,
            ),
        )
    }
}
