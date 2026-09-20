package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun HomeScreen(bottomContentPadding: Dp) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(
                    R.string.home_subtitle,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_ID,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                CombinedStatusPreview(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            item {
                SmallTitle(stringResource(R.string.section_current_build))
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                        Text(
                            text = stringResource(R.string.product_name),
                            style = MiuixTheme.textStyles.headline2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.target_platform_value))
                        Text(stringResource(R.string.version_value, BuildConfig.VERSION_NAME))
                        Text(stringResource(R.string.build_value, BuildConfig.BUILD_ID))
                    }
                }
            }
            item { Spacer(Modifier.height(bottomContentPadding + 16.dp)) }
        }
    }
}
