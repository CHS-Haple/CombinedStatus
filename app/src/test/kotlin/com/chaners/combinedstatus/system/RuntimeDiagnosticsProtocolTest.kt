package com.chaners.combinedstatus.system

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeDiagnosticsProtocolTest {
    @Test
    fun formattedEventRoundTripsStructuredFields() {
        val line =
            RuntimeDiagnosticsProtocol.format(
                event = "source.install",
                component = "network",
                state = "ready",
                fields =
                    mapOf(
                        "source" to "cold start",
                        "hooks" to "4",
                    ),
            )

        val parsed = requireNotNull(RuntimeDiagnosticsProtocol.parse(line))

        assertEquals("source.install", parsed.event)
        assertEquals("network", parsed.component)
        assertEquals("ready", parsed.state)
        assertEquals("cold start", parsed.fields["source"])
        assertEquals("4", parsed.fields["hooks"])
    }

    @Test
    fun healthSnapshotUsesLatestEventForEachComponent() {
        val lines =
            listOf(
                RuntimeDiagnosticsProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "error",
                ),
                RuntimeDiagnosticsProtocol.format(
                    event = "source.install",
                    component = "network",
                    state = "ready",
                    fields = mapOf("hooks" to "4"),
                ),
            )

        val network =
            RuntimeHealthSnapshot
                .fromLines(lines)
                .components
                .first { it.component == "network" }

        assertEquals("ready", network.state)
        assertEquals("4", network.fields["hooks"])
    }

    @Test
    fun missingCoreComponentsNeverReportHealthy() {
        val snapshot =
            RuntimeHealthSnapshot.fromLines(
                listOf(
                    RuntimeDiagnosticsProtocol.format(
                        event = "module.loaded",
                        component = "module",
                        state = "ready",
                    ),
                ),
            )

        assertEquals("degraded", snapshot.overall)
        assertEquals(
            "unknown",
            snapshot.components.first { it.component == "renderer" }.state,
        )
    }
}
