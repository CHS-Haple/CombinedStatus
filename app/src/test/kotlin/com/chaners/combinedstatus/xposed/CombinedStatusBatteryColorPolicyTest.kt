package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusBatteryColorPolicyTest {
    private val statusTint = 0xff3c3c3c.toInt()

    @Test
    fun systemDefaultUsesStatusTintForNormalState() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.NORMAL,
                systemSemanticColor = 0xff123456.toInt(),
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun systemDefaultUsesNativeSemanticColorForEveryColoredState() {
        listOf(
            CombinedStatusBatterySemanticState.CHARGING,
            CombinedStatusBatterySemanticState.POWER_SAVE,
            CombinedStatusBatterySemanticState.PERFORMANCE,
            CombinedStatusBatterySemanticState.LOW,
        ).forEachIndexed { index, state ->
            val nativeColor = 0xff000000.toInt() or (index + 1)
            assertEquals(
                nativeColor,
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = nativeColor,
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
                    systemSemanticColor = 0xffabcdef.toInt(),
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state = state,
                            source = CombinedStatusBatteryColorSource.FollowStatusIcon,
                        ),
                ),
            )
        }
    }

    @Test
    fun everyStateCanUseCustomColor() {
        val custom = 0xff765432.toInt()
        CombinedStatusBatterySemanticState.entries.forEach { state ->
            assertEquals(
                custom,
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = 0xffabcdef.toInt(),
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state = state,
                            source = CombinedStatusBatteryColorSource.Custom(custom),
                        ),
                ),
            )
        }
    }

    @Test
    fun unavailableSemanticColorFallsBackToStatusTint() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.PERFORMANCE,
                systemSemanticColor = null,
                statusIconTint = statusTint,
            ),
        )
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.LOW,
                systemSemanticColor = 0x00112233,
                statusIconTint = statusTint,
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
