package com.chaners.combinedstatus.system

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeEnvironmentInfoTest {
    @Test
    fun appendsSoftwareUpdateRevisionToHyperOsVersion() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D01",
            RuntimeEnvironmentInfo.composeOsVersion(
                baseVersion = "OS4.0.0.14.XOBCNXM",
                primaryRevision = "D01",
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun keepsAlreadySuffixedVersionUnchanged() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D01",
            RuntimeEnvironmentInfo.composeOsVersion(
                baseVersion = "4.0.0.14.XOBCNXM.D01",
                primaryRevision = "D01",
                secondaryRevision = "",
            ),
        )
    }

    @Test
    fun choosesNewerRevisionWithSamePrefix() {
        assertEquals(
            "4.0.0.14.XOBCNXM.D02",
            RuntimeEnvironmentInfo.composeOsVersion(
                baseVersion = "4.0.0.14.XOBCNXM",
                primaryRevision = "D01",
                secondaryRevision = "D02",
            ),
        )
    }

    @Test
    fun ignoresInvalidRevision() {
        assertEquals(
            "4.0.0.14.XOBCNXM",
            RuntimeEnvironmentInfo.composeOsVersion(
                baseVersion = "OS4.0.0.14.XOBCNXM",
                primaryRevision = "invalid",
                secondaryRevision = "",
            ),
        )
    }
}
