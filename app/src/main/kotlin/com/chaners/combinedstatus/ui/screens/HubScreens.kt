package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppLanguage
import com.chaners.combinedstatus.system.SystemUiScopeController
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import com.chaners.combinedstatus.ui.navigation.AppRoute
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
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
    swipeBackEnabled: Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val languageOptions = listOf(
        stringResource(R.string.language_system),
        stringResource(R.string.language_english),
        stringResource(R.string.language_simplified_chinese),
    )
    val scope = rememberCoroutineScope()
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showRestartFailure by rememberSaveable { mutableStateOf(false) }
    var restartInProgress by rememberSaveable { mutableStateOf(false) }

    HubPage(
        title = stringResource(R.string.settings_title),
        sectionTitle = stringResource(R.string.section_settings),
        bottomContentPadding = bottomContentPadding,
        secondarySectionTitle = stringResource(R.string.section_module),
        secondaryContent = {
            BasicComponent(
                title = stringResource(R.string.restart_scope),
                summary = stringResource(R.string.restart_scope_summary),
                enabled = !restartInProgress,
                onClick = { showRestartDialog = true },
            )
        },
        overlay = {
            OverlayDialog(
                title = stringResource(R.string.restart_scope),
                summary = stringResource(R.string.restart_scope_dialog_summary),
                show = showRestartDialog,
                onDismissRequest = { showRestartDialog = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = { showRestartDialog = false },
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.restart),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            showRestartDialog = false
                            restartInProgress = true
                            scope.launch {
                                val success = SystemUiScopeController.restart()
                                restartInProgress = false
                                if (!success) {
                                    showRestartFailure = true
                                }
                            }
                        },
                    )
                }
            }

            OverlayDialog(
                title = stringResource(R.string.restart_scope_failed),
                summary = stringResource(R.string.restart_scope_failed_summary),
                show = showRestartFailure,
                onDismissRequest = { showRestartFailure = false },
            ) {
                TextButton(
                    text = stringResource(R.string.confirm),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { showRestartFailure = false },
                )
            }
        },
    ) {
        ArrowPreference(
            title = stringResource(R.string.appearance_title),
            summary = stringResource(R.string.appearance_summary),
            onClick = { onNavigate(AppRoute.Appearance) },
        )
        SwitchPreference(
            title = stringResource(R.string.swipe_back),
            summary = stringResource(R.string.swipe_back_summary),
            checked = swipeBackEnabled,
            onCheckedChange = onSwipeBackEnabledChange,
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
    secondarySectionTitle: String? = null,
    secondaryContent: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = pageContentPadding(
                innerPadding = paddingValues,
                outerBottomPadding = bottomContentPadding,
                extraBottom = 12.dp,
            ),
        ) {
            item {
                SmallTitle(sectionTitle)
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    content = content,
                )
            }

            if (secondarySectionTitle != null && secondaryContent != null) {
                item {
                    SmallTitle(secondarySectionTitle)
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        content = secondaryContent,
                    )
                }
            }
        }

        overlay()
    }
}
