package com.chaners.combinedstatus.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsSettingsTest {
    @Test
    fun defaultsToGeneralWhenValueIsMissing() {
        assertEquals(
            DiagnosticsLevel.General,
            decodeDiagnosticsLevel(null),
        )
    }

    @Test
    fun restoresDetailedValue() {
        assertEquals(
            DiagnosticsLevel.Detailed,
            decodeDiagnosticsLevel(DiagnosticsLevel.Detailed.name),
        )
    }

    @Test
    fun invalidValueFallsBackToGeneral() {
        assertEquals(
            DiagnosticsLevel.General,
            decodeDiagnosticsLevel("Verbose"),
        )
    }
}
