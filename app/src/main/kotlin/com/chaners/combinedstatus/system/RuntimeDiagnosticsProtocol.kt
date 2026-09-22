package com.chaners.combinedstatus.system

internal data class RuntimeDiagnosticEvent(
    val event: String,
    val component: String,
    val state: String,
    val fields: Map<String, String>,
)

internal data class RuntimeHealthComponent(
    val component: String,
    val state: String,
    val event: String,
    val fields: Map<String, String>,
)

internal data class RuntimeHealthSnapshot(
    val overall: String,
    val components: List<RuntimeHealthComponent>,
) {
    fun reportLines(): List<String> =
        buildList {
            add("overall=$overall")
            components.forEach { component ->
                add(
                    buildString {
                        append("component=")
                        append(component.component)
                        append(" state=")
                        append(component.state)
                        append(" event=")
                        append(component.event)
                        component.fields
                            .toSortedMap()
                            .forEach { (key, value) ->
                                append(' ')
                                append(key)
                                append('=')
                                append(RuntimeDiagnosticsProtocol.encode(value))
                            }
                    },
                )
            }
        }

    companion object {
        private val expectedComponents =
            listOf(
                "module",
                "diagnostics",
                "compatibility",
                "statusHostHook",
                "statusHost",
                "network",
                "connectivity",
                "mobileType",
                "mobilePresentation",
                "airplane",
                "tint",
                "scene",
                "stableStatus",
                "renderer",
                "runtimeSession",
                "islandMotion",
                "hotReload",
            )

        private val coreComponents =
            setOf(
                "module",
                "diagnostics",
                "compatibility",
                "statusHostHook",
                "statusHost",
                "network",
                "airplane",
                "tint",
                "scene",
                "stableStatus",
                "renderer",
                "runtimeSession",
            )

        fun fromLines(lines: List<String>): RuntimeHealthSnapshot {
            val latest = linkedMapOf<String, RuntimeDiagnosticEvent>()
            lines.forEach { line ->
                RuntimeDiagnosticsProtocol.parse(line)?.let { event ->
                    latest[event.component] = event
                }
            }

            val components =
                buildList {
                    expectedComponents.forEach { component ->
                        val event = latest[component]
                        add(
                            RuntimeHealthComponent(
                                component = component,
                                state = event?.state ?: "unknown",
                                event = event?.event ?: "not-observed",
                                fields = event?.fields.orEmpty(),
                            ),
                        )
                    }

                    latest.keys
                        .filterNot(expectedComponents::contains)
                        .sorted()
                        .forEach { component ->
                            val event = latest.getValue(component)
                            add(
                                RuntimeHealthComponent(
                                    component = component,
                                    state = event.state,
                                    event = event.event,
                                    fields = event.fields,
                                ),
                            )
                        }
                }

            val byName = components.associateBy(RuntimeHealthComponent::component)
            val moduleState = byName["module"]?.state
            val healthy =
                coreComponents.all { component ->
                    byName[component]?.state in setOf("ready", "disabled")
                }
            val overall =
                when {
                    moduleState == null || moduleState == "unknown" -> "unavailable"
                    healthy -> "healthy"
                    else -> "degraded"
                }

            return RuntimeHealthSnapshot(
                overall = overall,
                components = components,
            )
        }
    }
}

internal object RuntimeDiagnosticsProtocol {
    private const val Marker = "diag "

    fun format(
        event: String,
        component: String,
        state: String,
        fields: Map<String, String> = emptyMap(),
    ): String =
        buildString {
            append(Marker)
            append("event=")
            append(encode(event))
            append(" component=")
            append(encode(component))
            append(" state=")
            append(encode(state))
            fields
                .toSortedMap()
                .forEach { (key, value) ->
                    append(' ')
                    append(key)
                    append('=')
                    append(encode(value))
                }
        }

    fun parse(line: String): RuntimeDiagnosticEvent? {
        val markerIndex = line.indexOf(Marker)
        if (markerIndex < 0) {
            return null
        }

        val values =
            line
                .substring(markerIndex + Marker.length)
                .trim()
                .split(Regex("\\s+"))
                .mapNotNull { token ->
                    val separator = token.indexOf('=')
                    if (separator <= 0 || separator == token.lastIndex) {
                        null
                    } else {
                        token.substring(0, separator) to decode(token.substring(separator + 1))
                    }
                }
                .toMap()

        val event = values["event"] ?: return null
        val component = values["component"] ?: return null
        val state = values["state"] ?: return null

        return RuntimeDiagnosticEvent(
            event = event,
            component = component,
            state = state,
            fields = values - setOf("event", "component", "state"),
        )
    }

    internal fun encode(value: String): String =
        value
            .replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")
            .replace("\t", "%09")
            .replace(" ", "%20")
            .replace("=", "%3D")

    private fun decode(value: String): String =
        value
            .replace("%3D", "=")
            .replace("%20", " ")
            .replace("%09", "\t")
            .replace("%0A", "\n")
            .replace("%0D", "\r")
            .replace("%25", "%")
}
