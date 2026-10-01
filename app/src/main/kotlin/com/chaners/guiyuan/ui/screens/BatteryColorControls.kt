package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusIosStyleBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusRecommendedBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
) {
    val detail = selectedSlot != null
    OverlayBottomSheet(
        show = show,
        title =
            if (selectedSlot == null) {
                stringResource(R.string.battery_colors)
            } else {
                stringResource(batteryColorSlotLabel(selectedSlot))
            },
        startAction =
            if (detail) {
                {
                    TextButton(
                        text = stringResource(R.string.back),
                        onClick = onBackToOverview,
                    )
                }
            } else {
                null
            },
        onDismissRequest = {
            if (detail) {
                onBackToOverview()
            } else {
                onDismiss()
            }
        },
    ) {
        if (selectedSlot == null) {
            BatteryColorOverview(
                settings = settings,
                onPresetChange = onPresetChange,
                onSlotSelected = onSlotSelected,
            )
        } else {
            BatteryColorModeDetail(
                slot = selectedSlot,
                settings = settings,
                onModeChange = onModeChange,
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
                onClick = {
                    onModeChange(
                        slot,
                        CombinedStatusBatteryColorMode.CUSTOM,
                    )
                },
                radioButtonLocation = RadioButtonLocation.End,
                endActions = {
                    BatteryColorDot(
                        color = settings.batteryColorOverrides.colorFor(slot),
                    )
                },
            )
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
) {
    val modifier =
        Modifier
            .size(14.dp)
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

internal fun batteryColorPreviewColor(
    settings: CombinedStatusVisualSettings,
    slot: CombinedStatusBatteryColorSlot,
): Int? {
    fun presetColor(): Int? =
        when (settings.batteryColorPreset) {
            CombinedStatusBatteryColorPreset.RECOMMENDED ->
                CombinedStatusRecommendedBatteryPalette.colorFor(slot)
            CombinedStatusBatteryColorPreset.HYPEROS -> null
            CombinedStatusBatteryColorPreset.IOS_STYLE ->
                CombinedStatusIosStyleBatteryPalette.colorFor(slot)
        }

    return when (settings.batteryColorModes.modeFor(slot)) {
        CombinedStatusBatteryColorMode.PRESET -> presetColor()
        CombinedStatusBatteryColorMode.FOLLOW_SYSTEM -> null
        CombinedStatusBatteryColorMode.CUSTOM ->
            settings.batteryColorOverrides.colorFor(slot) ?: presetColor()
    }
}

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
