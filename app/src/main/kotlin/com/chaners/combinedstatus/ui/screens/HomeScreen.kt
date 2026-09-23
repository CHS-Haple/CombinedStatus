package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import com.chaners.combinedstatus.ui.components.HotReloadAction
import com.chaners.combinedstatus.ui.layout.pageContentPadding
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar

@Composable
internal fun HomeScreen(
    bottomContentPadding: Dp,
    hotReloadInProgress: Boolean,
    onHotReload: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val buildSummary = listOf(
        stringResource(R.string.target_platform_value),
        stringResource(R.string.version_value, BuildConfig.VERSION_NAME),
        stringResource(R.string.build_value, BuildConfig.BUILD_ID),
    ).joinToString("\n")

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.home_title),
                actions = {
                    HotReloadAction(
                        inProgress = hotReloadInProgress,
                        onClick = onHotReload,
                    )
                },
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
                CombinedStatusPreview(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                )
            }
            item {
                SmallTitle(stringResource(R.string.section_current_build))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    BasicComponent(
                        title = stringResource(R.string.product_name),
                        summary = buildSummary,
                    )
                }
            }
        }
    }
}
