package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.CombinedStatusApplication
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.CombinedStatusFeatureSettingsRepository
import com.chaners.combinedstatus.system.XposedRuntimeStatus
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import com.chaners.combinedstatus.ui.components.HotReloadAction
import com.chaners.combinedstatus.ui.components.MiuixBlurredTopBar
import com.chaners.combinedstatus.ui.components.rememberTopBarBackdrop
import com.chaners.combinedstatus.ui.components.topBarBackdropSource
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class RuntimeStatusTone {
    Success,
    Warning,
    Error,
    Neutral,
}

private enum class RuntimeStatusMarkKind {
    Check,
    Alert,
    Minus,
}

private data class HomeRuntimeCardState(
    val titleRes: Int,
    val summaryRes: Int,
    val tone: RuntimeStatusTone,
    val mark: RuntimeStatusMarkKind,
)

@Composable
internal fun HomeScreen(
    bottomContentPadding: Dp,
    hotReloadInProgress: Boolean,
    previewState: PreviewSandboxUiState,
    onHotReload: () -> Unit,
    onOpenPreviewSandbox: () -> Unit,
) {
    val context = LocalContext.current
    val application =
        remember(context.applicationContext) {
            context.applicationContext as CombinedStatusApplication
        }
    val featureRepository =
        remember(context.applicationContext) {
            CombinedStatusFeatureSettingsRepository(context.applicationContext)
        }
    val featureSettings by
        featureRepository.settings.collectAsState(
            initial = featureRepository.current(),
        )
    val xposedRuntimeStatus by
        application.xposedRuntimeStatus.collectAsState()

    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberTopBarBackdrop()

    Scaffold(
        topBar = {
            MiuixBlurredTopBar(
                backdrop = topBarBackdrop,
                scrollBehavior = scrollBehavior,
            ) { barColor ->
                TopAppBar(
                    title = stringResource(R.string.home_title),
                    color = barColor,
                    actions = {
                        HotReloadAction(
                            inProgress = hotReloadInProgress,
                            onClick = onHotReload,
                        )
                    },
                    scrollBehavior = scrollBehavior,
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
                        outerBottomPadding = bottomContentPadding,
                        extraBottom = 12.dp,
                    ),
            ) {
                item {
                    SmallTitle(stringResource(R.string.section_home_runtime))
                    HomeRuntimeStatusCard(
                        enabled = featureSettings.enabled,
                        runtimeStatus = xposedRuntimeStatus,
                        hotReloadInProgress = hotReloadInProgress,
                        onEnabledChange = featureRepository::setEnabled,
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    )
                }

                item {
                    SmallTitle(stringResource(R.string.section_home_preview_sandbox))
                    HomePreviewSandboxCard(
                        state = previewState,
                        onOpen = onOpenPreviewSandbox,
                        modifier =
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRuntimeStatusCard(
    enabled: Boolean,
    runtimeStatus: XposedRuntimeStatus,
    hotReloadInProgress: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        resolveHomeRuntimeCardState(
            enabled = enabled,
            runtimeStatus = runtimeStatus,
            hotReloadInProgress = hotReloadInProgress,
        )
    val accentColor =
        when (state.tone) {
            RuntimeStatusTone.Success -> RuntimeSuccessAccent
            RuntimeStatusTone.Warning -> RuntimeWarningAccent
            RuntimeStatusTone.Error -> MiuixTheme.colorScheme.error
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.onSurfaceContainerVariant
        }
    val containerColor =
        when (state.tone) {
            RuntimeStatusTone.Neutral -> MiuixTheme.colorScheme.surfaceContainer
            else ->
                accentColor
                    .copy(alpha = 0.15f)
                    .compositeOver(MiuixTheme.colorScheme.surfaceContainer)
        }

    Card(
        modifier = modifier,
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(RuntimeCardHeight)
                    .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            RuntimeStatusMark(
                kind = state.mark,
                color = accentColor,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(RuntimeStatusMarkSize),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(end = 98.dp),
            ) {
                Text(
                    text = stringResource(state.titleRes),
                    style =
                        MiuixTheme.textStyles.title3.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                )
                Text(
                    text =
                        stringResource(
                            R.string.home_version_line,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.BUILD_ID.substringAfterLast('-'),
                        ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
                Text(
                    text = stringResource(state.summaryRes),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun HomePreviewSandboxCard(
    state: PreviewSandboxUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val center =
        when (state.centerIndex) {
            0 -> stringResource(R.string.home_preview_center_wifi)
            1 -> stringResource(R.string.home_preview_center_5g)
            else -> stringResource(R.string.home_preview_center_empty)
        }
    val signal =
        when (state.signalIndex) {
            0 -> stringResource(R.string.home_preview_signal_strong)
            1 -> stringResource(R.string.home_preview_signal_medium)
            2 -> stringResource(R.string.home_preview_signal_weak)
            else -> stringResource(R.string.home_preview_signal_unavailable)
        }
    val battery =
        when (state.batteryIndex) {
            0 -> stringResource(R.string.home_preview_battery_normal)
            1 -> stringResource(R.string.home_preview_battery_charging)
            else -> stringResource(R.string.home_preview_battery_power_save)
        }
    val stateSummary =
        stringResource(
            R.string.home_preview_state_format,
            center,
            signal,
            battery,
        )

    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_preview_open_title),
            summary = stateSummary,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            startAction = {
                CombinedStatusPreview(
                    model = state.toRenderModel(),
                    modifier = Modifier.size(58.dp),
                )
            },
            onClick = onOpen,
        )
    }
}

@Composable
private fun RuntimeStatusMark(
    kind: RuntimeStatusMarkKind,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val ringColor = color.copy(alpha = 0.38f)
        val symbolColor = color.copy(alpha = 0.64f)
        val ringStrokeWidth = 5.0.dp.toPx()
        val symbolStrokeWidth = 5.6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.40f

        drawCircle(
            color = ringColor,
            radius = radius,
            center = center,
            style =
                Stroke(
                    width = ringStrokeWidth,
                    cap = StrokeCap.Round,
                ),
        )

        when (kind) {
            RuntimeStatusMarkKind.Check -> {
                val checkPath =
                    Path().apply {
                        moveTo(size.width * 0.29f, size.height * 0.52f)
                        lineTo(size.width * 0.44f, size.height * 0.66f)
                        lineTo(size.width * 0.72f, size.height * 0.35f)
                    }
                drawPath(
                    path = checkPath,
                    color = symbolColor,
                    style =
                        Stroke(
                            width = symbolStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
            }

            RuntimeStatusMarkKind.Alert -> {
                drawLine(
                    color = symbolColor,
                    start = Offset(size.width * 0.50f, size.height * 0.29f),
                    end = Offset(size.width * 0.50f, size.height * 0.56f),
                    strokeWidth = symbolStrokeWidth,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = symbolColor,
                    radius = symbolStrokeWidth * 0.58f,
                    center = Offset(size.width * 0.50f, size.height * 0.70f),
                )
            }

            RuntimeStatusMarkKind.Minus -> {
                drawLine(
                    color = symbolColor,
                    start = Offset(size.width * 0.31f, size.height * 0.50f),
                    end = Offset(size.width * 0.69f, size.height * 0.50f),
                    strokeWidth = symbolStrokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun resolveHomeRuntimeCardState(
    enabled: Boolean,
    runtimeStatus: XposedRuntimeStatus,
    hotReloadInProgress: Boolean,
): HomeRuntimeCardState {
    if (!enabled) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_disabled,
            summaryRes = R.string.home_runtime_disabled_summary,
            tone = RuntimeStatusTone.Neutral,
            mark = RuntimeStatusMarkKind.Minus,
        )
    }

    if (hotReloadInProgress) {
        return HomeRuntimeCardState(
            titleRes = R.string.home_runtime_reloading,
            summaryRes = R.string.home_runtime_reloading_summary,
            tone = RuntimeStatusTone.Warning,
            mark = RuntimeStatusMarkKind.Alert,
        )
    }

    return when (runtimeStatus) {
        XposedRuntimeStatus.Checking ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_checking,
                summaryRes = R.string.home_runtime_checking_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedRuntimeStatus.FrameworkUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_framework_unavailable,
                summaryRes = R.string.home_runtime_framework_unavailable_summary,
                tone = RuntimeStatusTone.Error,
                mark = RuntimeStatusMarkKind.Alert,
            )

        XposedRuntimeStatus.QueryUnavailable ->
            HomeRuntimeCardState(
                titleRes = R.string.home_runtime_unknown,
                summaryRes = R.string.home_runtime_unknown_summary,
                tone = RuntimeStatusTone.Warning,
                mark = RuntimeStatusMarkKind.Alert,
            )

        is XposedRuntimeStatus.Connected ->
            when {
                !runtimeStatus.systemUiInScope ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_unhooked,
                        summaryRes = R.string.home_runtime_unhooked_summary,
                        tone = RuntimeStatusTone.Error,
                        mark = RuntimeStatusMarkKind.Alert,
                    )

                runtimeStatus.systemUiRunning ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_running,
                        summaryRes = R.string.home_runtime_running_summary,
                        tone = RuntimeStatusTone.Success,
                        mark = RuntimeStatusMarkKind.Check,
                    )

                else ->
                    HomeRuntimeCardState(
                        titleRes = R.string.home_runtime_pending,
                        summaryRes = R.string.home_runtime_pending_summary,
                        tone = RuntimeStatusTone.Warning,
                        mark = RuntimeStatusMarkKind.Alert,
                    )
            }
    }
}

private val RuntimeCardHeight = 160.dp
private val RuntimeStatusMarkSize = 88.dp
private val RuntimeSuccessAccent = Color(0xFF36D167)
private val RuntimeWarningAccent = Color(0xFFFFA500)
