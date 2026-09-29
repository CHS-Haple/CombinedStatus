package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import com.chaners.combinedstatus.ui.components.MiuixBlurredTopBar
import com.chaners.combinedstatus.ui.components.rememberTopBarBackdrop
import com.chaners.combinedstatus.ui.components.topBarBackdropSource
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreviewSandboxScreen(
    state: PreviewSandboxUiState,
    onSimPresentChange: (Boolean) -> Unit,
    onAirplaneModeChange: (Boolean) -> Unit,
    onNetworkModeChange: (PreviewNetworkMode) -> Unit,
    onMobileNetworkChange: (PreviewMobileNetwork) -> Unit,
    onMobileSignalLevelChange: (Int) -> Unit,
    onWifiStateChange: (PreviewWifiState) -> Unit,
    onWifiSignalLevelChange: (Int) -> Unit,
    onBatteryPercentChange: (Int) -> Unit,
    onBatteryModeChange: (PreviewBatteryMode) -> Unit,
    onChargingStateChange: (PreviewChargingState) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val resourceResolver =
        remember(context.applicationContext) {
            PreviewSystemUiResourceResolver(context.applicationContext)
        }
    val renderModel = state.toRenderModel(resourceResolver)

    val networkModeOptions =
        listOf(
            stringResource(R.string.home_preview_network_mode_mobile),
            stringResource(R.string.home_preview_network_mode_wifi),
        )
    val simOptions =
        listOf(
            stringResource(R.string.home_preview_sim_present),
            stringResource(R.string.home_preview_sim_absent),
        )
    val mobileNetworkOptions =
        listOf(
            stringResource(R.string.home_preview_network_none),
            stringResource(R.string.home_preview_network_4g),
            stringResource(R.string.home_preview_network_5g),
            stringResource(R.string.home_preview_network_5ga),
        )
    val wifiOptions =
        listOf(
            stringResource(R.string.home_preview_wifi_connected),
            stringResource(R.string.home_preview_wifi_no_internet),
            stringResource(R.string.home_preview_wifi_hotspot),
        )
    val batteryModeOptions =
        listOf(
            stringResource(R.string.home_preview_battery_mode_balanced),
            stringResource(R.string.home_preview_battery_mode_power_save),
            stringResource(R.string.home_preview_battery_mode_performance),
            stringResource(R.string.home_preview_battery_mode_super_power_save),
        )
    val chargingOptions =
        listOf(
            stringResource(R.string.home_preview_charging_none),
            stringResource(R.string.home_preview_charging_normal),
            stringResource(R.string.home_preview_charging_super_fast),
        )

    val mobileDisabledSummary =
        when {
            state.airplaneMode -> stringResource(R.string.home_preview_mobile_disabled_airplane)
            !state.simPresent -> stringResource(R.string.home_preview_mobile_disabled_no_sim)
            else -> null
        }

    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                SmallTopAppBar(
                    title = stringResource(R.string.home_preview_sandbox_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .topBarBackdropSource(topBarBackdrop),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding =
                    pageContentPadding(
                        innerPadding = paddingValues,
                        extraBottom = 12.dp,
                    ),
            ) {
                item {
                    SmallTitle(stringResource(R.string.home_preview_section_preview))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_preview_live_title),
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        Text(
                            text = stringResource(R.string.home_preview_sandbox_summary),
                            modifier = Modifier.padding(top = 3.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(132.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CombinedStatusPreview(
                                model = renderModel,
                                modifier = Modifier.size(120.dp),
                            )
                        }
                        Text(
                            text = previewNetworkSummary(state),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        Text(
                            text = previewBatterySummary(state),
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 1.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }

                item {
                    SmallTitle(stringResource(R.string.home_preview_section_network))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(vertical = 12.dp),
                    ) {
                        TabRowWithContour(
                            tabs = networkModeOptions,
                            selectedTabIndex = state.networkMode.ordinal,
                            onTabSelected = { index ->
                                onNetworkModeChange(PreviewNetworkMode.entries[index])
                            },
                            modifier =
                                Modifier
                                    .widthIn(max = 244.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .padding(horizontal = 6.dp),
                        )

                        if (state.networkMode == PreviewNetworkMode.MOBILE) {
                            if (state.mobileOptionsVisible) {
                                SandboxSegmentedField(
                                    title = stringResource(R.string.home_preview_mobile_network_title),
                                    options = mobileNetworkOptions,
                                    selectedIndex = state.mobileNetwork.ordinal,
                                    onSelected = { index ->
                                        onMobileNetworkChange(PreviewMobileNetwork.entries[index])
                                    },
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                                SliderPreference(
                                    value = state.mobileSignalLevel.toFloat(),
                                    onValueChange = { value ->
                                        onMobileSignalLevelChange(value.roundToInt().coerceIn(0, 4))
                                    },
                                    title = stringResource(R.string.home_preview_mobile_signal_title),
                                    valueText = signalValueText(state.mobileSignalLevel),
                                    valueRange = 0f..4f,
                                    steps = 3,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0f, 1f, 2f, 3f, 4f),
                                    insideMargin = CompactPreferenceMargin,
                                )
                            } else {
                                Text(
                                    text = mobileDisabledSummary.orEmpty(),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                )
                            }
                        } else {
                            SandboxSegmentedField(
                                title = stringResource(R.string.home_preview_wifi_state_title),
                                options = wifiOptions,
                                selectedIndex = state.wifiState.ordinal,
                                onSelected = { index ->
                                    onWifiStateChange(PreviewWifiState.entries[index])
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            SliderPreference(
                                value = state.wifiSignalLevel.toFloat(),
                                onValueChange = { value ->
                                    onWifiSignalLevelChange(value.roundToInt().coerceIn(0, 3))
                                },
                                title = stringResource(R.string.home_preview_wifi_signal_title),
                                valueText = signalValueText(state.wifiSignalLevel),
                                valueRange = 0f..3f,
                                steps = 2,
                                showKeyPoints = true,
                                keyPoints = listOf(0f, 1f, 2f, 3f),
                                insideMargin = CompactPreferenceMargin,
                            )
                        }

                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_sim_title),
                            options = simOptions,
                            selectedIndex = if (state.simPresent) 0 else 1,
                            onSelected = { onSimPresentChange(it == 0) },
                            maxWidth = 244.dp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        SwitchPreference(
                            checked = state.airplaneMode,
                            onCheckedChange = onAirplaneModeChange,
                            title = stringResource(R.string.home_preview_airplane_title),
                            summary = stringResource(R.string.home_preview_airplane_summary),
                            insideMargin = CompactPreferenceMargin,
                        )
                    }
                }

                item {
                    SmallTitle(stringResource(R.string.home_preview_section_battery))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        insideMargin = PaddingValues(vertical = 10.dp),
                    ) {
                        SliderPreference(
                            value = state.batteryPercent.toFloat(),
                            onValueChange = { value ->
                                onBatteryPercentChange(value.roundToInt().coerceIn(0, 100))
                            },
                            title = stringResource(R.string.home_preview_battery_level_title),
                            valueText =
                                stringResource(
                                    R.string.home_preview_battery_percent,
                                    state.batteryPercent,
                                ),
                            valueRange = 0f..100f,
                            insideMargin = CompactPreferenceMargin,
                        )
                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_battery_mode_title),
                            options = batteryModeOptions,
                            selectedIndex = state.batteryMode.ordinal,
                            onSelected = { index ->
                                onBatteryModeChange(PreviewBatteryMode.entries[index])
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        SandboxSegmentedField(
                            title = stringResource(R.string.home_preview_charging_state_title),
                            options = chargingOptions,
                            selectedIndex = state.chargingState.ordinal,
                            onSelected = { index ->
                                onChargingStateChange(PreviewChargingState.entries[index])
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SandboxSegmentedField(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 360.dp,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
        TabRowWithContour(
            tabs = options,
            selectedTabIndex = selectedIndex,
            onTabSelected = onSelected,
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp),
        )
    }
}

@Composable
internal fun previewNetworkSummary(
    state: PreviewSandboxUiState,
): String {
    if (state.networkMode == PreviewNetworkMode.WIFI) {
        val wifi =
            when (state.wifiState) {
                PreviewWifiState.CONNECTED ->
                    stringResource(
                        R.string.home_preview_network_summary_wifi,
                        signalValueText(state.wifiSignalLevel),
                    )
                PreviewWifiState.NO_INTERNET ->
                    stringResource(
                        R.string.home_preview_network_summary_wifi_no_internet,
                        signalValueText(state.wifiSignalLevel),
                    )
                PreviewWifiState.HOTSPOT ->
                    stringResource(
                        R.string.home_preview_network_summary_hotspot,
                        signalValueText(state.wifiSignalLevel),
                    )
            }
        val withAirplane =
            if (state.airplaneMode) {
                stringResource(R.string.home_preview_network_summary_airplane_wifi, wifi)
            } else {
                wifi
            }
        return if (!state.simPresent) {
            stringResource(
                R.string.home_preview_network_summary_with_sim_state,
                withAirplane,
                stringResource(R.string.home_preview_sim_absent),
            )
        } else {
            withAirplane
        }
    }

    if (state.airplaneMode) {
        return if (!state.simPresent) {
            stringResource(
                R.string.home_preview_network_summary_with_sim_state,
                stringResource(R.string.home_preview_airplane_title),
                stringResource(R.string.home_preview_sim_absent),
            )
        } else {
            stringResource(R.string.home_preview_airplane_title)
        }
    }
    if (!state.simPresent) {
        return stringResource(R.string.home_preview_sim_absent)
    }

    val mobileType =
        when (state.mobileNetwork) {
            PreviewMobileNetwork.NONE -> stringResource(R.string.home_preview_network_none)
            PreviewMobileNetwork.FOUR_G -> stringResource(R.string.home_preview_network_4g)
            PreviewMobileNetwork.FIVE_G -> stringResource(R.string.home_preview_network_5g)
            PreviewMobileNetwork.FIVE_GA -> stringResource(R.string.home_preview_network_5ga)
        }
    return stringResource(
        R.string.home_preview_network_summary_mobile,
        mobileType,
        signalValueText(state.mobileSignalLevel),
    )
}

@Composable
internal fun previewBatterySummary(
    state: PreviewSandboxUiState,
): String {
    val mode =
        when (state.batteryMode) {
            PreviewBatteryMode.BALANCED ->
                stringResource(R.string.home_preview_battery_mode_balanced)
            PreviewBatteryMode.POWER_SAVE ->
                stringResource(R.string.home_preview_battery_mode_power_save)
            PreviewBatteryMode.PERFORMANCE ->
                stringResource(R.string.home_preview_battery_mode_performance)
            PreviewBatteryMode.SUPER_POWER_SAVE ->
                stringResource(R.string.home_preview_battery_mode_super_power_save)
        }
    val charging =
        when (state.chargingState) {
            PreviewChargingState.NOT_CHARGING ->
                stringResource(R.string.home_preview_charging_none)
            PreviewChargingState.CHARGING ->
                stringResource(R.string.home_preview_charging_normal)
            PreviewChargingState.SUPER_FAST_CHARGING ->
                stringResource(R.string.home_preview_charging_super_fast)
        }

    return stringResource(
        R.string.home_preview_battery_summary_format,
        state.batteryPercent,
        mode,
        charging,
    )
}

@Composable
private fun signalValueText(level: Int): String =
    if (level <= 0) {
        stringResource(R.string.home_preview_signal_none)
    } else {
        stringResource(R.string.home_preview_signal_level, level)
    }

private val CompactPreferenceMargin =
    PaddingValues(horizontal = 18.dp, vertical = 7.dp)
