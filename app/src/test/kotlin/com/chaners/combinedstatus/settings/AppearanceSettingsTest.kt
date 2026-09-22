package com.chaners.combinedstatus.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun legacyDynamicModeMigratesToSystemWithDynamicColor() {
        val result =
            decodeThemeSelection(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = null,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertTrue(result.dynamicColorEnabled)
    }

    @Test
    fun explicitDynamicPreferenceOverridesLegacyFallback() {
        val result =
            decodeThemeSelection(
                storedMode = "Dynamic",
                storedDynamicColorEnabled = false,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertFalse(result.dynamicColorEnabled)
    }

    @Test
    fun lightModeRemainsIndependentFromDynamicColor() {
        val result =
            decodeThemeSelection(
                storedMode = "Light",
                storedDynamicColorEnabled = true,
            )

        assertEquals(AppThemeMode.Light, result.mode)
        assertTrue(result.dynamicColorEnabled)
    }

    @Test
    fun unknownStoredModeFallsBackToSystem() {
        val result =
            decodeThemeSelection(
                storedMode = "Unknown",
                storedDynamicColorEnabled = null,
            )

        assertEquals(AppThemeMode.System, result.mode)
        assertFalse(result.dynamicColorEnabled)
    }
}
