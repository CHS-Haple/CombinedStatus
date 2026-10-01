package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorModes
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorOverrides
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryColorControlsTest {
    @Test
    fun recommendedAndIosPreviewUseTheirFixedPaletteColors() {
        assertEquals(
            0xFF3FA760.toInt(),
            batteryColorPreviewColor(
                settings = CombinedStatusVisualSettings(),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
            ),
        )
        assertEquals(
            0xFFFF3B30.toInt(),
            batteryColorPreviewColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorPreset = CombinedStatusBatteryColorPreset.IOS_STYLE,
                    ),
                slot = CombinedStatusBatteryColorSlot.LOW,
            ),
        )
    }

    @Test
    fun hyperosAndFollowSystemRemainDynamicInPreview() {
        assertNull(
            batteryColorPreviewColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
            ),
        )
        assertNull(
            batteryColorPreviewColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorModes =
                            CombinedStatusBatteryColorModes(
                                charging = CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
                            ),
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
            ),
        )
    }

    @Test
    fun customPreviewUsesStoredColorAndFallsBackToPresetWhenUnset() {
        val custom = 0xFF2468AC.toInt()
        assertEquals(
            custom,
            batteryColorPreviewColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorModes =
                            CombinedStatusBatteryColorModes(
                                charging = CombinedStatusBatteryColorMode.CUSTOM,
                            ),
                        batteryColorOverrides =
                            CombinedStatusBatteryColorOverrides(
                                charging = custom,
                            ),
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
            ),
        )
        assertEquals(
            0xFF3FA760.toInt(),
            batteryColorPreviewColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorModes =
                            CombinedStatusBatteryColorModes(
                                charging = CombinedStatusBatteryColorMode.CUSTOM,
                            ),
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
            ),
        )
    }

    @Test
    fun normalRecommendedPreviewIsDynamicBecauseItFollowsStatusTint() {
        assertNull(
            batteryColorPreviewColor(
                settings = CombinedStatusVisualSettings(),
                slot = CombinedStatusBatteryColorSlot.NORMAL,
            ),
        )
    }
}
