package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusBatteryColorPolicyTest {
    private val statusTint = 0xff555555.toInt()
    private val systemSemantic = 0xff123456.toInt()

    @Test
    fun systemDefaultKeepsNormalOnStatusIconTint() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun systemDefaultUsesNativeSemanticColorForEverySemanticState() {
        listOf(
            CombinedStatusBatterySemanticState.CHARGING,
            CombinedStatusBatterySemanticState.POWER_SAVE,
            CombinedStatusBatterySemanticState.PERFORMANCE,
            CombinedStatusBatterySemanticState.LOW,
        ).forEach { state ->
            assertEquals(
                systemSemantic,
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                ),
            )
        }
    }

    @Test
    fun everyStateCanFollowStatusIconTint() {
        CombinedStatusBatterySemanticState.entries.forEach { state ->
            assertEquals(
                statusTint,
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            CombinedStatusBatteryColorSource.FollowStatusIcon,
                        ),
                ),
            )
        }
    }

    @Test
    fun everyStateCanUseCustomColor() {
        val custom = 0xffabcdef.toInt()
        CombinedStatusBatterySemanticState.entries.forEach { state ->
            assertEquals(
                custom,
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            CombinedStatusBatteryColorSource.Custom(custom),
                        ),
                ),
            )
        }
    }

    @Test
    fun missingNativeSemanticColorFallsBackToStatusTint() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.PERFORMANCE,
                systemSemanticColor = null,
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun invalidCustomFallsBackToSystemDefault() {
        assertEquals(
            systemSemantic,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.POWER_SAVE,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences =
                    CombinedStatusBatteryColorPreferences(
                        powerSave = CombinedStatusBatteryColorSource.Custom(0x00112233),
                    ),
            ),
        )
    }

    private fun preferencesFor(
        state: CombinedStatusBatterySemanticState,
        source: CombinedStatusBatteryColorSource,
    ): CombinedStatusBatteryColorPreferences =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL ->
                CombinedStatusBatteryColorPreferences(normal = source)
            CombinedStatusBatterySemanticState.CHARGING ->
                CombinedStatusBatteryColorPreferences(charging = source)
            CombinedStatusBatterySemanticState.POWER_SAVE ->
                CombinedStatusBatteryColorPreferences(powerSave = source)
            CombinedStatusBatterySemanticState.PERFORMANCE ->
                CombinedStatusBatteryColorPreferences(performance = source)
            CombinedStatusBatterySemanticState.LOW ->
                CombinedStatusBatteryColorPreferences(low = source)
        }
}
