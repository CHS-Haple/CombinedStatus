package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppLanguage
import com.chaners.combinedstatus.ui.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    onNavigate: (AppRoute) -> Unit,
) {
    HubPage(
        title = stringResource(R.string.features_title),
        sectionTitle = stringResource(R.string.section_hyperos_display),
        bottomContentPadding = bottomContentPadding,
    ) {
        ArrowPreference(
            title = stringResource(R.string.status_bar_title),
            summary = stringResource(R.string.status_bar_summary),
            onClick = { onNavigate(AppRoute.StatusBar) },
        )
        ArrowPreference(
            title = stringResource(R.string.keyguard_aod_title),
            summary = stringResource(R.string.keyguard_aod_summary),
            onClick = { onNavigate(AppRoute.Keyguard) },
        )
        ArrowPreference(
            title = stringResource(R.string.charging_title),
            summary = stringResource(R.string.charging_summary),
            onClick = { onNavigate(AppRoute.Charging) },
        )
    }
}

@Composable
internal fun SettingsHubScreen(
    bottomContentPadding: Dp,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val languageOptions = listOf(
        stringResource(R.string.language_system),
        stringResource(R.string.language_english),
        stringResource(R.string.language_simplified_chinese),
    )

    HubPage(
        title = stringResource(R.string.settings_title),
        sectionTitle = stringResource(R.string.section_settings),
        bottomContentPadding = bottomContentPadding,
    ) {
        ArrowPreference(
            title = stringResource(R.string.appearance_title),
            summary = stringResource(R.string.appearance_summary),
            onClick = { onNavigate(AppRoute.Appearance) },
        )
        OverlayDropdownPreference(
            items = languageOptions,
            selectedIndex = appLanguage.ordinal,
            title = stringResource(R.string.language_title),
            summary = stringResource(R.string.language_summary),
            showValue = false,
            onSelectedIndexChange = { index ->
                AppLanguage.entries.getOrNull(index)?.let(onAppLanguageChange)
            },
        )
        SwitchPreference(
            title = stringResource(R.string.hide_launcher_icon),
            summary = stringResource(R.string.hide_launcher_icon_summary),
            checked = launcherIconHidden,
            onCheckedChange = onLauncherIconHiddenChange,
        )
        ArrowPreference(
            title = stringResource(R.string.diagnostics_title),
            summary = stringResource(R.string.diagnostics_summary),
            onClick = { onNavigate(AppRoute.Diagnostics) },
        )
    }
}

@Composable
private fun HubPage(
    title: String,
    sectionTitle: String,
    bottomContentPadding: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = title) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SmallTitle(sectionTitle)
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    content = content,
                )
            }
            item { Spacer(Modifier.height(bottomContentPadding + 16.dp)) }
        }
    }
}
