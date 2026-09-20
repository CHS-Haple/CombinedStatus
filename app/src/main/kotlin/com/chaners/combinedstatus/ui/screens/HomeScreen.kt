package com.chaners.combinedstatus.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import com.chaners.combinedstatus.ui.AppScreen
import com.chaners.combinedstatus.ui.components.CombinedStatusPreview
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference

@Composable
internal fun HomeScreen(onNavigate: (AppScreen) -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "三合一状态图标",
                subtitle = "for HyperOS · v${BuildConfig.VERSION_NAME} · ${BuildConfig.BUILD_ID}",
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
                SmallTitle("HyperOS 显示")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = "外观",
                        summary = "MIUIX 主题、尺寸与视觉样式",
                        onClick = { onNavigate(AppScreen.Appearance) },
                    )
                    ArrowPreference(
                        title = "状态栏",
                        summary = "HyperOS SystemUI 三合一显示与状态栏行为",
                        onClick = { onNavigate(AppScreen.StatusBar) },
                    )
                    ArrowPreference(
                        title = "锁屏与 AOD",
                        summary = "HyperOS 锁屏及息屏显示预览",
                        onClick = { onNavigate(AppScreen.Keyguard) },
                    )
                    ArrowPreference(
                        title = "充电显示",
                        summary = "HyperOS 普通充电与超级快充样式",
                        onClick = { onNavigate(AppScreen.Charging) },
                    )
                }
            }
            item {
                SmallTitle("开发")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = "诊断与关于",
                        summary = "目标平台、版本、运行阶段与接口状态",
                        onClick = { onNavigate(AppScreen.Diagnostics) },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
