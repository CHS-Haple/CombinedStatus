package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import com.chaners.combinedstatus.ui.components.MiuixBlurredTopBar
import com.chaners.combinedstatus.ui.components.rememberTopBarBackdrop
import com.chaners.combinedstatus.ui.components.topBarBackdropSource
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import com.chaners.combinedstatus.xposed.CenterIndicator
import com.chaners.combinedstatus.xposed.CombinedStatusBatterySemanticState
import com.chaners.combinedstatus.xposed.CombinedStatusRenderModel
import com.chaners.combinedstatus.xposed.InternetState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

internal data class PreviewSandboxUiState(
    val centerIndex: Int = 0,
    val signalIndex: Int = 0,
    val batteryIndex: Int = 0,
)

@Composable
internal fun PreviewSandboxScreen(
    state: PreviewSandboxUiState,
    onCenterIndexChange: (Int) -> Unit,
    onSignalIndexChange: (Int) -> Unit,
    onBatteryIndexChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val centerOptions =
        listOf(
            stringResource(R.string.home_preview_center_wifi),
            stringResource(R.string.home_preview_center_5g),
            stringResource(R.string.home_preview_center_empty),
        )
    val signalOptions =
        listOf(
            stringResource(R.string.home_preview_signal_strong),
            stringResource(R.string.home_preview_signal_medium),
            stringResource(R.string.home_preview_signal_weak),
            stringResource(R.string.home_preview_signal_unavailable),
        )
    val batteryOptions =
        listOf(
            stringResource(R.string.home_preview_battery_normal),
            stringResource(R.string.home_preview_battery_charging),
            stringResource(R.string.home_preview_battery_power_save),
        )

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
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_preview_sandbox_title),
                            summary = stringResource(R.string.home_preview_sandbox_summary),
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        )
                        CombinedStatusPreview(
                            model = state.toRenderModel(),
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(116.dp)
                                    .padding(bottom = 12.dp),
                        )
                    }
                }

                item {
                    SmallTitle(stringResource(R.string.home_preview_section_state))
                    Card(
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    ) {
                        OverlayDropdownPreference(
                            items = centerOptions,
                            selectedIndex = state.centerIndex,
                            title = stringResource(R.string.home_preview_center_title),
                            showValue = true,
                            insideMargin = CompactPreferencePadding,
                            onSelectedIndexChange = onCenterIndexChange,
                        )
                        OverlayDropdownPreference(
                            items = signalOptions,
                            selectedIndex = state.signalIndex,
                            title = stringResource(R.string.home_preview_signal_title),
                            showValue = true,
                            insideMargin = CompactPreferencePadding,
                            onSelectedIndexChange = onSignalIndexChange,
                        )
                        OverlayDropdownPreference(
                            items = batteryOptions,
                            selectedIndex = state.batteryIndex,
                            title = stringResource(R.string.home_preview_battery_title),
                            showValue = true,
                            insideMargin = CompactPreferencePadding,
                            onSelectedIndexChange = onBatteryIndexChange,
                        )
                    }
                }
            }
        }
    }
}

internal fun PreviewSandboxUiState.toRenderModel(): CombinedStatusRenderModel {
    val center =
        when (centerIndex) {
            0 ->
                CenterIndicator.Wifi(
                    segments = 3,
                    internet = InternetState.VALIDATED,
                )
            1 ->
                CenterIndicator.MobileType(
                    label = "5G",
                    enhanced = false,
                    internet = InternetState.VALIDATED,
                )
            else -> CenterIndicator.Empty
        }
    val mobileLevel =
        when (signalIndex) {
            0 -> 4
            1 -> 3
            2 -> 2
            else -> null
        }
    val batteryPercent =
        when (batteryIndex) {
            0 -> 87
            1 -> 68
            else -> 42
        }
    val semanticState =
        when (batteryIndex) {
            1 -> CombinedStatusBatterySemanticState.CHARGING
            2 -> CombinedStatusBatterySemanticState.POWER_SAVE
            else -> CombinedStatusBatterySemanticState.NORMAL
        }

    return CombinedStatusRenderModel(
        batteryPercent = batteryPercent,
        charging = batteryIndex == 1,
        centerIndicator = center,
        mobileLevel = mobileLevel,
        mobileUnavailableMark = signalIndex == 3,
        effectiveDataSubscriptionId = -1,
        batterySemanticState = semanticState,
    )
}

private val CompactPreferencePadding =
    PaddingValues(
        horizontal = 16.dp,
        vertical = 10.dp,
    )
