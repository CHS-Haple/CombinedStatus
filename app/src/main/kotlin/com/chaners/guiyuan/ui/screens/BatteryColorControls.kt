package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusIosStyleBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusRecommendedBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val COMMON_BATTERY_COLORS =
    listOf(
        0xFFFF3B30.toInt(),
        0xFFFF9500.toInt(),
        0xFFFFCC00.toInt(),
        0xFF34C759.toInt(),
        0xFF32ADE6.toInt(),
        0xFF007AFF.toInt(),
        0xFF5856D6.toInt(),
        0xFFAF52DE.toInt(),
        0xFFFF2D55.toInt(),
        0xFF8E8E93.toInt(),
    )

private val BATTERY_COLOR_PREVIEW_SLOTS =
    listOf(
        CombinedStatusBatteryColorSlot.POWER_SAVE,
        CombinedStatusBatteryColorSlot.PERFORMANCE,
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE,
        CombinedStatusBatteryColorSlot.CHARGING,
        CombinedStatusBatteryColorSlot.LOW,
    )

@Composable
internal fun BatteryColorPreference(
    settings: CombinedStatusVisualSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(R.string.battery_colors),
        summary =
            stringResource(
                R.string.battery_color_scheme_summary,
                stringResource(batteryColorPresetLabel(settings.batteryColorPreset)),
            ),
        enabled = enabled,
        onClick = onClick,
        endActions = {
            BatteryColorPreviewStrip(settings)
        },
    )
}

@Composable
internal fun BatteryColorBottomSheet(
    show: Boolean,
    selectedSlot: CombinedStatusBatteryColorSlot?,
    settings: CombinedStatusVisualSettings,
    onDismiss: () -> Unit,
    onBackToOverview: () -> Unit,
    onPresetChange: (CombinedStatusBatteryColorPreset) -> Unit,
    onSlotSelected: (CombinedStatusBatteryColorSlot) -> Unit,
    onModeChange: (CombinedStatusBatteryColorSlot, CombinedStatusBatteryColorMode) -> Unit,
    onCustomColorChange: (CombinedStatusBatteryColorSlot, Int) -> Unit,
    onResetSlot: (CombinedStatusBatteryColorSlot) -> Unit,
) {
    var editingCustom by remember(selectedSlot?.ordinal) { mutableStateOf(false) }
    val detail = selectedSlot != null
    OverlayBottomSheet(
        show = show,
        title =
            when {
                selectedSlot == null -> stringResource(R.string.battery_colors)
                editingCustom ->
                    stringResource(
                        R.string.battery_color_custom_title,
                        stringResource(batteryColorSlotLabel(selectedSlot)),
                    )
                else -> stringResource(batteryColorSlotLabel(selectedSlot))
            },
        startAction =
            if (detail) {
                {
                    TextButton(
                        text = stringResource(R.string.back),
                        onClick = {
                            if (editingCustom) {
                                editingCustom = false
                            } else {
                                onBackToOverview()
                            }
                        },
                    )
                }
            } else {
                null
            },
        onDismissRequest = {
            when {
                editingCustom -> editingCustom = false
                detail -> onBackToOverview()
                else -> onDismiss()
            }
        },
    ) {
        if (selectedSlot == null) {
            BatteryColorOverview(
                settings = settings,
                onPresetChange = onPresetChange,
                onSlotSelected = onSlotSelected,
            )
        } else if (editingCustom) {
            BatteryCustomColorEditor(
                slot = selectedSlot,
                settings = settings,
                onColorChange = onCustomColorChange,
            )
        } else {
            BatteryColorModeDetail(
                slot = selectedSlot,
                settings = settings,
                onModeChange = onModeChange,
                onEditCustom = {
                    editingCustom = true
                },
                onResetSlot = onResetSlot,
            )
        }
    }
}

@Composable
private fun BatteryColorOverview(
    settings: CombinedStatusVisualSettings,
    onPresetChange: (CombinedStatusBatteryColorPreset) -> Unit,
    onSlotSelected: (CombinedStatusBatteryColorSlot) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(stringResource(R.string.battery_color_scheme))
        Card {
            listOf(
                CombinedStatusBatteryColorPreset.RECOMMENDED,
                CombinedStatusBatteryColorPreset.HYPEROS,
                CombinedStatusBatteryColorPreset.IOS_STYLE,
            ).forEach { preset ->
                RadioButtonPreference(
                    title = stringResource(batteryColorPresetLabel(preset)),
                    summary = stringResource(batteryColorPresetSummary(preset)),
                    selected = settings.batteryColorPreset == preset,
                    onClick = { onPresetChange(preset) },
                    radioButtonLocation = RadioButtonLocation.End,
                )
            }
        }

        SmallTitle(stringResource(R.string.battery_mode_colors))
        Card {
            CombinedStatusBatteryColorSlot.entries.forEach { slot ->
                ArrowPreference(
                    title = stringResource(batteryColorSlotLabel(slot)),
                    summary = batteryColorSourceSummary(settings, slot),
                    onClick = { onSlotSelected(slot) },
                    endActions = {
                        BatteryColorDot(
                            color = batteryColorPreviewColor(settings, slot),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun BatteryColorModeDetail(
    slot: CombinedStatusBatteryColorSlot,
    settings: CombinedStatusVisualSettings,
    onModeChange: (CombinedStatusBatteryColorSlot, CombinedStatusBatteryColorMode) -> Unit,
    onEditCustom: () -> Unit,
    onResetSlot: (CombinedStatusBatteryColorSlot) -> Unit,
) {
    val selected = settings.batteryColorModes.modeFor(slot)
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(stringResource(R.string.battery_color_source))
        Card {
            RadioButtonPreference(
                title = stringResource(R.string.battery_color_source_preset),
                summary =
                    stringResource(
                        R.string.battery_color_source_preset_summary,
                        stringResource(batteryColorPresetLabel(settings.batteryColorPreset)),
                    ),
                selected = selected == CombinedStatusBatteryColorMode.PRESET,
                onClick = {
                    onModeChange(
                        slot,
                        CombinedStatusBatteryColorMode.PRESET,
                    )
                },
                radioButtonLocation = RadioButtonLocation.End,
            )
            RadioButtonPreference(
                title = stringResource(R.string.battery_color_source_system),
                summary = stringResource(R.string.battery_color_source_system_summary),
                selected = selected == CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
                onClick = {
                    onModeChange(
                        slot,
                        CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
                    )
                },
                radioButtonLocation = RadioButtonLocation.End,
            )
            RadioButtonPreference(
                title = stringResource(R.string.battery_color_source_custom),
                summary =
                    settings.batteryColorOverrides.colorFor(slot)
                        ?.let(::batteryColorHex)
                        ?: stringResource(R.string.battery_color_custom_unset),
                selected = selected == CombinedStatusBatteryColorMode.CUSTOM,
                onClick = onEditCustom,
                radioButtonLocation = RadioButtonLocation.End,
                endActions = {
                    BatteryColorDot(
                        color = settings.batteryColorOverrides.colorFor(slot),
                    )
                },
            )
        }

        SmallTitle(stringResource(R.string.section_management))
        Card {
            BasicComponent(
                title = stringResource(R.string.battery_color_restore_mode),
                summary = stringResource(R.string.battery_color_restore_mode_summary),
                onClick = { onResetSlot(slot) },
            )
        }
    }
}

@Composable
private fun BatteryCustomColorEditor(
    slot: CombinedStatusBatteryColorSlot,
    settings: CombinedStatusVisualSettings,
    onColorChange: (CombinedStatusBatteryColorSlot, Int) -> Unit,
) {
    val dynamicFallback =
        CombinedStatusRecommendedBatteryPalette.colorFor(slot)
            ?: MiuixTheme.colorScheme.onSurface.toArgb()
    val initialColor =
        remember(slot.ordinal) {
            batteryColorEditorInitialColor(
                settings = settings,
                slot = slot,
                dynamicFallback = dynamicFallback,
            )
        }
    var editingColor by remember(slot.ordinal) { mutableIntStateOf(initialColor) }
    var hexText by remember(slot.ordinal) {
        mutableStateOf(batteryColorHex(editingColor).removePrefix("#"))
    }
    val initialRgb = batteryColorRgb(editingColor)
    var redText by remember(slot.ordinal) { mutableStateOf(initialRgb.first.toString()) }
    var greenText by remember(slot.ordinal) { mutableStateOf(initialRgb.second.toString()) }
    var blueText by remember(slot.ordinal) { mutableStateOf(initialRgb.third.toString()) }

    fun applyColor(color: Int) {
        val opaque = color or 0xFF000000.toInt()
        editingColor = opaque
        hexText = batteryColorHex(opaque).removePrefix("#")
        val rgb = batteryColorRgb(opaque)
        redText = rgb.first.toString()
        greenText = rgb.second.toString()
        blueText = rgb.third.toString()
        onColorChange(slot, opaque)
    }

    val hsv = Color(editingColor).toHsv()

    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(stringResource(R.string.battery_color_current))
        Card {
            BasicComponent(
                title = batteryColorHex(editingColor),
                summary = stringResource(R.string.battery_color_opaque_summary),
                endActions = {
                    BatteryColorDot(
                        color = editingColor,
                        size = 28.dp,
                    )
                },
            )
        }

        SmallTitle(stringResource(R.string.battery_color_common))
        Card(
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            COMMON_BATTERY_COLORS.chunked(5).forEachIndexed { index, colors ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == 0) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(Color(color), CircleShape)
                                    .then(
                                        if (editingColor == color) {
                                            Modifier.border(
                                                2.dp,
                                                MiuixTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable { applyColor(color) },
                        )
                    }
                }
            }
        }

        SmallTitle(stringResource(R.string.battery_color_full_adjustment))
        Card {
            BasicComponent(
                title = stringResource(R.string.battery_color_hue),
                bottomAction = {
                    HsvHueSlider(
                        currentHue = hsv.h,
                        onHueChanged = { fraction ->
                            applyColor(
                                Hsv(
                                    fraction * 360f,
                                    hsv.s,
                                    hsv.v,
                                ).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.battery_color_saturation),
                bottomAction = {
                    HsvSaturationSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        onSaturationChanged = { saturation ->
                            applyColor(
                                Hsv(
                                    hsv.h,
                                    saturation * 100f,
                                    hsv.v,
                                ).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.battery_color_brightness),
                bottomAction = {
                    HsvValueSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        currentValue = hsv.v / 100f,
                        onValueChanged = { value ->
                            applyColor(
                                Hsv(
                                    hsv.h,
                                    hsv.s,
                                    value * 100f,
                                ).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
        }

        SmallTitle(stringResource(R.string.battery_color_precise_input))
        Card(
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            TextField(
                value = hexText,
                onValueChange = { raw ->
                    val normalized =
                        raw.removePrefix("#")
                            .uppercase()
                            .filter { it.isDigit() || it in 'A'..'F' }
                    if (normalized.length <= 6) {
                        hexText = normalized
                        batteryColorFromHex(normalized)?.let(::applyColor)
                    }
                },
                label = stringResource(R.string.battery_color_hex),
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                    ),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = redText,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all(Char::isDigit)) {
                            redText = value
                            batteryColorFromRgb(
                                redText,
                                greenText,
                                blueText,
                            )?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "R",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = greenText,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all(Char::isDigit)) {
                            greenText = value
                            batteryColorFromRgb(
                                redText,
                                greenText,
                                blueText,
                            )?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "G",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = blueText,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all(Char::isDigit)) {
                            blueText = value
                            batteryColorFromRgb(
                                redText,
                                greenText,
                                blueText,
                            )?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "B",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }
}

@Composable
private fun BatteryColorPreviewStrip(
    settings: CombinedStatusVisualSettings,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BATTERY_COLOR_PREVIEW_SLOTS.forEach { slot ->
            BatteryColorDot(
                color = batteryColorPreviewColor(settings, slot),
            )
        }
    }
}

@Composable
private fun BatteryColorDot(
    color: Int?,
    size: Dp = 14.dp,
) {
    val modifier =
        Modifier
            .size(size)
            .then(
                if (color == null) {
                    Modifier.border(
                        width = 1.dp,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                } else {
                    Modifier.background(
                        color = Color(color),
                        shape = CircleShape,
                    )
                },
            )
    Box(modifier = modifier)
}

@Composable
private fun batteryColorSourceSummary(
    settings: CombinedStatusVisualSettings,
    slot: CombinedStatusBatteryColorSlot,
): String =
    when (settings.batteryColorModes.modeFor(slot)) {
        CombinedStatusBatteryColorMode.PRESET ->
            stringResource(
                R.string.battery_color_source_preset_short,
                stringResource(batteryColorPresetLabel(settings.batteryColorPreset)),
            )
        CombinedStatusBatteryColorMode.FOLLOW_SYSTEM ->
            stringResource(R.string.battery_color_source_system)
        CombinedStatusBatteryColorMode.CUSTOM ->
            settings.batteryColorOverrides.colorFor(slot)
                ?.let(::batteryColorHex)
                ?: stringResource(R.string.battery_color_source_custom)
    }

internal fun batteryColorPresetPreviewColor(
    preset: CombinedStatusBatteryColorPreset,
    slot: CombinedStatusBatteryColorSlot,
): Int? =
    when (preset) {
        CombinedStatusBatteryColorPreset.RECOMMENDED ->
            CombinedStatusRecommendedBatteryPalette.colorFor(slot)
        CombinedStatusBatteryColorPreset.HYPEROS -> null
        CombinedStatusBatteryColorPreset.IOS_STYLE ->
            CombinedStatusIosStyleBatteryPalette.colorFor(slot)
    }

internal fun batteryColorPreviewColor(
    settings: CombinedStatusVisualSettings,
    slot: CombinedStatusBatteryColorSlot,
): Int? {
    val presetColor =
        batteryColorPresetPreviewColor(
            preset = settings.batteryColorPreset,
            slot = slot,
        )
    return when (settings.batteryColorModes.modeFor(slot)) {
        CombinedStatusBatteryColorMode.PRESET -> presetColor
        CombinedStatusBatteryColorMode.FOLLOW_SYSTEM -> null
        CombinedStatusBatteryColorMode.CUSTOM ->
            settings.batteryColorOverrides.colorFor(slot) ?: presetColor
    }
}

internal fun batteryColorEditorInitialColor(
    settings: CombinedStatusVisualSettings,
    slot: CombinedStatusBatteryColorSlot,
    dynamicFallback: Int,
): Int =
    (
        settings.batteryColorOverrides.colorFor(slot)
            ?: batteryColorPresetPreviewColor(
                preset = settings.batteryColorPreset,
                slot = slot,
            )
            ?: dynamicFallback
    ) or 0xFF000000.toInt()

internal fun batteryColorFromHex(input: String): Int? {
    val hex = input.removePrefix("#")
    if (hex.length != 6 || !hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
        return null
    }
    return runCatching {
        hex.toUInt(16).toInt() or 0xFF000000.toInt()
    }.getOrNull()
}

internal fun batteryColorFromRgb(
    red: String,
    green: String,
    blue: String,
): Int? {
    val r = red.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val g = green.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val b = blue.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
}

internal fun batteryColorRgb(color: Int): Triple<Int, Int, Int> =
    Triple(
        (color shr 16) and 0xFF,
        (color shr 8) and 0xFF,
        color and 0xFF,
    )

private fun batteryColorHex(color: Int): String =
    "#%06X".format(color and 0x00FFFFFF)

@StringRes
private fun batteryColorPresetLabel(
    preset: CombinedStatusBatteryColorPreset,
): Int =
    when (preset) {
        CombinedStatusBatteryColorPreset.RECOMMENDED -> R.string.battery_color_preset_recommended
        CombinedStatusBatteryColorPreset.HYPEROS -> R.string.battery_color_preset_hyperos
        CombinedStatusBatteryColorPreset.IOS_STYLE -> R.string.battery_color_preset_ios
    }

@StringRes
private fun batteryColorPresetSummary(
    preset: CombinedStatusBatteryColorPreset,
): Int =
    when (preset) {
        CombinedStatusBatteryColorPreset.RECOMMENDED ->
            R.string.battery_color_preset_recommended_summary
        CombinedStatusBatteryColorPreset.HYPEROS ->
            R.string.battery_color_preset_hyperos_summary
        CombinedStatusBatteryColorPreset.IOS_STYLE ->
            R.string.battery_color_preset_ios_summary
    }

@StringRes
private fun batteryColorSlotLabel(
    slot: CombinedStatusBatteryColorSlot,
): Int =
    when (slot) {
        CombinedStatusBatteryColorSlot.NORMAL -> R.string.battery_mode_normal
        CombinedStatusBatteryColorSlot.POWER_SAVE -> R.string.battery_mode_power_save
        CombinedStatusBatteryColorSlot.PERFORMANCE -> R.string.battery_mode_performance
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> R.string.battery_mode_super_power_save
        CombinedStatusBatteryColorSlot.CHARGING -> R.string.battery_mode_charging
        CombinedStatusBatteryColorSlot.LOW -> R.string.battery_mode_low
    }
