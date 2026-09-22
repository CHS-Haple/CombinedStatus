package com.chaners.combinedstatus.ui.screens

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.settings.DiagnosticsLevel
import com.chaners.combinedstatus.settings.DiagnosticsSettings
import com.chaners.combinedstatus.settings.DiagnosticsSettingsRepository
import com.chaners.combinedstatus.system.DiagnosticsReportBuilder
import com.chaners.combinedstatus.system.DiagnosticsReportFiles
import com.chaners.combinedstatus.system.RuntimeEnvironmentInfo
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
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
    onDynamicColorEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBarEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBlurEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val themeOptions =
        listOf(
            stringResource(R.string.theme_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )

    SettingsPage(title = stringResource(R.string.appearance_title), onBack = onBack) {
        item {
            AppearanceThemePreview(settings)
        }

        Section(R.string.section_theme) {
            OverlayDropdownPreference(
                items = themeOptions,
                selectedIndex = settings.themeMode.ordinal,
                title = stringResource(R.string.theme_mode),
                summary = stringResource(R.string.theme_mode_summary),
                showValue = true,
                onSelectedIndexChange = { index ->
                    AppThemeMode.entries.getOrNull(index)?.let { mode ->
                        if (mode != settings.themeMode) {
                            onThemeModeChange(mode)
                        }
                    }
                },
            )
            SwitchPreference(
                title = stringResource(R.string.dynamic_color),
                summary = stringResource(R.string.dynamic_color_summary),
                checked = settings.dynamicColorEnabled,
                onCheckedChange = onDynamicColorEnabledChange,
            )
        }

        Section(R.string.section_visual_effects) {
            SwitchPreference(
                title = stringResource(R.string.floating_navigation_bar),
                summary = stringResource(R.string.floating_navigation_bar_summary),
                checked = settings.floatingNavigationBarEnabled,
                onCheckedChange = onFloatingNavigationBarEnabledChange,
            )
            AnimatedVisibility(visible = settings.floatingNavigationBarEnabled) {
                SwitchPreference(
                    title = stringResource(R.string.floating_navigation_blur),
                    summary = stringResource(R.string.floating_navigation_blur_summary),
                    checked = settings.floatingNavigationBlurEnabled,
                    onCheckedChange = onFloatingNavigationBlurEnabledChange,
                )
            }
        }
    }
}

@Composable
private fun AppearanceThemePreview(settings: AppearanceSettings) {
    val modeLabel =
        stringResource(
            when (settings.themeMode) {
                AppThemeMode.System -> R.string.theme_system
                AppThemeMode.Light -> R.string.theme_light
                AppThemeMode.Dark -> R.string.theme_dark
            },
        )
    val colorLabel =
        stringResource(
            if (settings.dynamicColorEnabled) {
                R.string.dynamic_color_on
            } else {
                R.string.dynamic_color_off
            },
        )

    Card(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = colorLabel,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Text(
                    text = modeLabel,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.28f),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.22f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.theme_preview),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ThemeColorSwatch(MiuixTheme.colorScheme.primary)
                        ThemeColorSwatch(MiuixTheme.colorScheme.secondary)
                        ThemeColorSwatch(MiuixTheme.colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeColorSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(22.dp),
        shape = RoundedCornerShape(6.dp),
        color = color,
        border =
            BorderStroke(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.42f),
            ),
    ) {}
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
    val environment by
        produceState(
            initialValue = RuntimeEnvironmentInfo.basic(),
            key1 = context.applicationContext,
        ) {
            value = RuntimeEnvironmentInfo.resolve(context.applicationContext)
        }
    val diagnosticsRepository =
        remember(context.applicationContext) {
            DiagnosticsSettingsRepository(context.applicationContext)
        }
    val diagnosticsSettings by
        diagnosticsRepository.settings.collectAsState(
            initial = DiagnosticsSettings(level = diagnosticsRepository.currentLevel()),
        )
    val diagnosticsLevelOptions =
        listOf(
            stringResource(R.string.diagnostics_mode_basic),
            stringResource(R.string.diagnostics_mode_detailed),
        )
    var reportInProgress by rememberSaveable { mutableStateOf(false) }
    var exportPickerOpen by rememberSaveable { mutableStateOf(false) }

    val reportShareTitle = stringResource(R.string.share_diagnostic_report)
    val exportSucceededMessage = stringResource(R.string.diagnostic_report_exported)
    val exportFailedMessage = stringResource(R.string.diagnostic_report_export_failed)
    val shareFailedMessage = stringResource(R.string.diagnostic_report_share_failed)

    fun buildReport(onReady: suspend (String) -> Unit) {
        if (reportInProgress) return
        reportInProgress = true
        scope.launch {
            try {
                onReady(DiagnosticsReportBuilder.build(context.applicationContext))
            } finally {
                reportInProgress = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            exportPickerOpen = false
            if (uri != null) {
                buildReport { report ->
                    val success =
                        DiagnosticsReportFiles.writeExport(
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
        Section(R.string.section_diagnostics_app) {
            DiagnosticsCardHeader(
                title = stringResource(R.string.product_name),
                subtitle = stringResource(R.string.product_summary),
            )
            DiagnosticsInfoValue(
                value = BuildConfig.VERSION_NAME,
                label = stringResource(R.string.diagnostics_version_label),
            )
            DiagnosticsInfoValue(
                value = BuildConfig.BUILD_ID,
                label = stringResource(R.string.diagnostics_build_label),
            )
            DiagnosticsInfoValue(
                value = BuildConfig.APPLICATION_ID,
                label = stringResource(R.string.diagnostics_package_label),
            )
        }

        Section(R.string.section_device_system) {
            DiagnosticsCardHeader(title = environment.deviceName)
            DiagnosticsInfoValue(
                value = environment.modelAndCodename,
                label = stringResource(R.string.device_model_label),
            )
            DiagnosticsInfoValue(
                value = environment.androidDisplay,
                label = stringResource(R.string.android_version_label),
            )
            DiagnosticsInfoValue(
                value = environment.osVersion,
                label = stringResource(R.string.os_version_label),
            )
            DiagnosticsInfoValue(
                value = environment.systemUiDisplay,
                label = stringResource(R.string.systemui_version_label),
            )
        }

        Section(R.string.section_module_runtime) {
            BasicComponent(
                title = stringResource(R.string.runtime_framework_title),
                summary = stringResource(R.string.runtime_framework_summary),
            )
            BasicComponent(
                title = stringResource(R.string.runtime_scope_title),
                summary = stringResource(R.string.runtime_scope_summary),
            )
            BasicComponent(
                title = stringResource(R.string.runtime_target_title),
                summary = stringResource(R.string.runtime_target_summary),
            )
            OverlayDropdownPreference(
                items = diagnosticsLevelOptions,
                selectedIndex = diagnosticsSettings.level.ordinal,
                title = stringResource(R.string.diagnostics_mode_title),
                summary = stringResource(R.string.diagnostics_mode_summary),
                showValue = true,
                onSelectedIndexChange = { index ->
                    DiagnosticsLevel.entries.getOrNull(index)?.let { level ->
                        if (level != diagnosticsSettings.level) {
                            diagnosticsRepository.setLevel(level)
                        }
                    }
                },
            )
            if (BuildConfig.DEVELOPMENT_PROBES) {
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
                            val prepared =
                                DiagnosticsReportFiles.prepareShare(
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

                            val sendIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = DiagnosticsReportFiles.ShareMimeType
                                    putExtra(Intent.EXTRA_STREAM, prepared.uri)
                                    clipData =
                                        ClipData.newUri(
                                            context.contentResolver,
                                            reportShareTitle,
                                            prepared.uri,
                                        )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            DiagnosticsReportFiles.logShareIntent(
                                context = context,
                                intent = sendIntent,
                                uri = prepared.uri,
                            )

                            val chooserIntent =
                                Intent.createChooser(
                                    sendIntent,
                                    reportShareTitle,
                                ).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                            runCatching {
                                context.startActivity(chooserIntent)
                            }.onSuccess {
                                DiagnosticsReportFiles.logChooserLaunch(context)
                            }.onFailure { error ->
                                DiagnosticsReportFiles.logChooserLaunch(context, error)
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
private fun DiagnosticsCardHeader(
    title: String,
    subtitle: String? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsInfoValue(
    value: String,
    label: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = value.ifBlank { "—" },
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
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
