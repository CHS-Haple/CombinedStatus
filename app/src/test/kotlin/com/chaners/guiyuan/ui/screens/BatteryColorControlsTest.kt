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
    fun hexAndRgbParsingProduceOpaqueColorsAndRejectInvalidInput() {
        assertEquals(0xFF34C759.toInt(), batteryColorFromHex("#34C759"))
        assertEquals(0xFF34C759.toInt(), batteryColorFromHex("34c759"))
        assertNull(batteryColorFromHex("FF34C759"))
        assertNull(batteryColorFromHex("GG0000"))

        assertEquals(
            0xFFFF0080.toInt(),
            batteryColorFromRgb("255", "0", "128"),
        )
        assertNull(batteryColorFromRgb("256", "0", "0"))
        assertNull(batteryColorFromRgb("", "0", "0"))
    }

    @Test
    fun rgbBreakdownRoundTripsOpaqueColor() {
        assertEquals(
            Triple(36, 104, 172),
            batteryColorRgb(0xFF2468AC.toInt()),
        )
        assertEquals(
            0xFF2468AC.toInt(),
            batteryColorFromRgb("36", "104", "172"),
        )
    }

    @Test
    fun editorInitialColorPrefersStoredThenPresetThenDynamicFallback() {
        val custom = 0xFF123456.toInt()
        assertEquals(
            custom,
            batteryColorEditorInitialColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorOverrides =
                            CombinedStatusBatteryColorOverrides(charging = custom),
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = 0xFF000000.toInt(),
            ),
        )
        assertEquals(
            0xFF3FA760.toInt(),
            batteryColorEditorInitialColor(
                settings = CombinedStatusVisualSettings(),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = 0xFF000000.toInt(),
            ),
        )
        assertEquals(
            0xFF112233.toInt(),
            batteryColorEditorInitialColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = 0x00112233,
            ),
        )
    }

    @Test
    fun hexAndRgbParsersAcceptValidOpaqueColorsAndRejectInvalidInput() {
        assertEquals(
            0xFF34C759.toInt(),
            batteryColorFromHex("34C759"),
        )
        assertEquals(
            0xFF34C759.toInt(),
            batteryColorFromHex("#34c759"),
        )
        assertEquals(
            0xFF34C759.toInt(),
            batteryColorFromRgb("52", "199", "89"),
        )
        assertNull(batteryColorFromHex("34C75"))
        assertNull(batteryColorFromHex("GG0000"))
        assertNull(batteryColorFromRgb("256", "0", "0"))
        assertNull(batteryColorFromRgb("", "0", "0"))
    }

    @Test
    fun editorInitialColorPrefersStoredThenPresetThenDynamicFallback() {
        val custom = 0xFF2468AC.toInt()
        val fallback = 0xFF112233.toInt()
        assertEquals(
            custom,
            batteryColorEditorInitialColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorOverrides =
                            CombinedStatusBatteryColorOverrides(
                                charging = custom,
                            ),
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = fallback,
            ),
        )
        assertEquals(
            0xFF3FA760.toInt(),
            batteryColorEditorInitialColor(
                settings = CombinedStatusVisualSettings(),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = fallback,
            ),
        )
        assertEquals(
            fallback,
            batteryColorEditorInitialColor(
                settings =
                    CombinedStatusVisualSettings(
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
                slot = CombinedStatusBatteryColorSlot.CHARGING,
                dynamicFallback = fallback,
            ),
        )
    }

    @Test
    fun rgbRoundTripMatchesPackedColor() {
        assertEquals(
            Triple(52, 199, 89),
            batteryColorRgb(0xFF34C759.toInt()),
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
