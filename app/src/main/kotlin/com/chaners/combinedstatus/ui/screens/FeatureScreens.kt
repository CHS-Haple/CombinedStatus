package com.chaners.combinedstatus.ui.screens

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.system.DiagnosticsReportBuilder
import com.chaners.combinedstatus.system.DiagnosticsReportFiles
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun AppearanceScreen(
    settings: AppearanceSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onFloatingNavigationBlurEnabledChange: (Boolean) -> Unit,
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
                title = stringResource(R.string.floating_navigation_blur),
                summary = stringResource(R.string.floating_navigation_blur_summary),
                checked = settings.floatingNavigationBlurEnabled,
                onCheckedChange = onFloatingNavigationBlurEnabledChange,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }

    val buildSummary = listOf(
        stringResource(R.string.target_platform_value),
        stringResource(R.string.version_value, BuildConfig.VERSION_NAME),
        stringResource(R.string.build_value, BuildConfig.BUILD_ID),
        stringResource(R.string.package_value, BuildConfig.APPLICATION_ID),
    ).joinToString("\n")
    val diagnosticsModeSummary = stringResource(
        if (BuildConfig.DEBUG) {
            R.string.diagnostics_mode_detailed
        } else {
            R.string.diagnostics_mode_basic
        },
    )
    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)

    fun buildReport(onReady: suspend (String) -> Unit) {
        if (reportInProgress) return
        reportInProgress = true
        scope.launch {
            try {
                onReady(DiagnosticsReportBuilder.build())
            } finally {
                reportInProgress = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        exportPickerOpen = false
        if (uri != null) {
            buildReport { report ->
                val success = DiagnosticsReportFiles.writeExport(
                    context = context,
                    uri = uri,
                    report = report,
                )
                Toast.makeText(
                    context,
                    if (success) exportSucceededMessage else exportFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    SettingsPage(title = stringResource(R.string.diagnostics_title), onBack = onBack) {
        Section(R.string.section_current_build) {
            BasicComponent(
                title = stringResource(R.string.product_name),
                summary = buildSummary,
            )
        }
        Section(R.string.section_runtime_stage) {
            BasicComponent(
                title = stringResource(R.string.runtime_framework_title),
                summary = stringResource(R.string.runtime_framework_summary),
            )
            BasicComponent(
                title = stringResource(R.string.runtime_target_title),
                summary = stringResource(R.string.runtime_target_summary),
            )
            BasicComponent(
                title = stringResource(R.string.diagnostics_mode_title),
                summary = diagnosticsModeSummary,
            )
            if (BuildConfig.DEBUG) {
                BasicComponent(
                    title = stringResource(R.string.runtime_inventory_title),
                    summary = stringResource(R.string.runtime_inventory_summary),
                )
            }
        }
        Section(R.string.section_diagnostic_report) {
            BasicComponent(
                title = stringResource(R.string.export_diagnostic_report),
                summary = stringResource(R.string.export_diagnostic_report_summary),
                onClick = {
                    if (!reportInProgress && !exportPickerOpen) {
                        exportPickerOpen = true
                        exportLauncher.launch(DiagnosticsReportFiles.suggestedFileName())
                    }
                },
            )
            BasicComponent(
                title = stringResource(R.string.share_diagnostic_report),
                summary = stringResource(R.string.share_diagnostic_report_summary),
                onClick = {
                    if (!reportInProgress && !exportPickerOpen) {
                        buildReport { report ->
                        val prepared = DiagnosticsReportFiles.prepareShare(
                            context = context,
                            report = report,
                        )
                        if (prepared == null) {
                            Toast.makeText(
                                context,
                                shareFailedMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@buildReport
                        }

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, prepared.uri)
                            clipData = ClipData.newRawUri(reportShareTitle, prepared.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooserIntent = Intent.createChooser(
                            sendIntent,
                            reportShareTitle,
                        ).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        runCatching {
                            context.startActivity(chooserIntent)
                        }.onFailure {
                            DiagnosticsReportFiles.discardShare(context, prepared)
                            Toast.makeText(
                                context,
                                shareFailedMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    }
                },
            )
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
            contentPadding = pageContentPadding(
                innerPadding = paddingValues,
                extraBottom = 12.dp,
            ),
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
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}
