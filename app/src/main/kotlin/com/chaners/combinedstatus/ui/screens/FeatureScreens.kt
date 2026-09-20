package com.chaners.combinedstatus.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceScreen(
    settings: AppearanceSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onGlassBottomBarEnabledChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_dynamic),
    )

    SettingsPage(title = stringResource(R.string.appearance_title), onBack = onBack) {
        Section(R.string.section_theme) {
            OverlayDropdownPreference(
                items = themeOptions,
                selectedIndex = settings.themeMode.ordinal,
                title = stringResource(R.string.theme_mode),
                summary = stringResource(R.string.theme_mode_summary),
                showValue = false,
                onSelectedIndexChange = { index ->
                    AppThemeMode.entries.getOrNull(index)?.let(onThemeModeChange)
                },
            )
        }
        Section(R.string.section_visual_effects) {
            SwitchPreference(
                title = stringResource(R.string.glass_bottom_bar),
                summary = stringResource(R.string.glass_bottom_bar_summary),
                checked = settings.glassBottomBarEnabled,
                onCheckedChange = onGlassBottomBarEnabledChange,
            )
        }
        Section(R.string.section_navigation) {
            SwitchPreference(
                title = stringResource(R.string.swipe_back),
                summary = stringResource(R.string.swipe_back_summary),
                checked = settings.swipeBackEnabled,
                onCheckedChange = onSwipeBackEnabledChange,
            )
        }
    }
}

@Composable
internal fun StatusBarScreen(onBack: () -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var smoothTransition by rememberSaveable { mutableStateOf(true) }
    SettingsPage(title = stringResource(R.string.status_bar_title), onBack = onBack) {
        Section(R.string.section_hyperos_systemui) {
            SwitchPreference(
                title = stringResource(R.string.enable_combined_icon),
                summary = stringResource(R.string.enable_combined_icon_summary),
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
            SwitchPreference(
                title = stringResource(R.string.transition_effects),
                summary = stringResource(R.string.transition_effects_summary),
                checked = smoothTransition,
                onCheckedChange = { smoothTransition = it },
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun KeyguardScreen(onBack: () -> Unit) {
    var keyguard by rememberSaveable { mutableStateOf(true) }
    var aod by rememberSaveable { mutableStateOf(true) }
    SettingsPage(title = stringResource(R.string.keyguard_aod_title), onBack = onBack) {
        Section(R.string.section_hyperos_display_scope) {
            SwitchPreference(
                title = stringResource(R.string.show_on_lock_screen),
                summary = stringResource(R.string.show_on_lock_screen_summary),
                checked = keyguard,
                onCheckedChange = { keyguard = it },
            )
            SwitchPreference(
                title = stringResource(R.string.show_on_aod),
                summary = stringResource(R.string.show_on_aod_summary),
                checked = aod,
                onCheckedChange = { aod = it },
            )
        }
    }
}

@Composable
internal fun ChargingScreen(onBack: () -> Unit) {
    var chargingIcon by rememberSaveable { mutableStateOf(true) }
    var superCharging by rememberSaveable { mutableStateOf(true) }
    SettingsPage(title = stringResource(R.string.charging_title), onBack = onBack) {
        Section(R.string.section_hyperos_charging) {
            SwitchPreference(
                title = stringResource(R.string.show_charging_state),
                summary = stringResource(R.string.show_charging_state_summary),
                checked = chargingIcon,
                onCheckedChange = { chargingIcon = it },
            )
            SwitchPreference(
                title = stringResource(R.string.distinguish_super_charging),
                summary = stringResource(R.string.distinguish_super_charging_summary),
                checked = superCharging,
                onCheckedChange = { superCharging = it },
                enabled = chargingIcon,
            )
        }
    }
}

@Composable
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    SettingsPage(title = stringResource(R.string.diagnostics_title), onBack = onBack) {
        item {
            SmallTitle(stringResource(R.string.section_current_build))
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                    Text(
                        stringResource(R.string.product_name),
                        style = MiuixTheme.textStyles.headline2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.target_platform_value))
                    Text(stringResource(R.string.version_value, BuildConfig.VERSION_NAME))
                    Text(stringResource(R.string.build_value, BuildConfig.BUILD_ID))
                    Text(stringResource(R.string.package_value, BuildConfig.APPLICATION_ID))
                }
            }
        }
        item {
            SmallTitle(stringResource(R.string.section_runtime_stage))
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                    Text(stringResource(R.string.runtime_stage_value))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.runtime_stage_summary),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            content = content,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.Section(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SmallTitle(stringResource(titleRes))
        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
            content = content,
        )
    }
    item { Spacer(Modifier.height(12.dp)) }
}
