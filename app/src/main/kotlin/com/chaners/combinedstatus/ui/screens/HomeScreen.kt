package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.ui.navigation.AppRoute
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun HomeScreen(onNavigate: (AppRoute) -> Unit) {
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
                SmallTitle(stringResource(R.string.section_hyperos_display))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.appearance_title),
                        summary = stringResource(R.string.appearance_summary),
                        onClick = { onNavigate(AppRoute.Appearance) },
                    )
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
            item {
                SmallTitle(stringResource(R.string.section_development))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.diagnostics_title),
                        summary = stringResource(R.string.diagnostics_summary),
                        onClick = { onNavigate(AppRoute.Diagnostics) },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
