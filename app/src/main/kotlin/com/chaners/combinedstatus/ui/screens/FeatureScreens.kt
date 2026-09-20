package com.chaners.combinedstatus.ui.screens

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
import androidx.compose.ui.unit.dp
import com.chaners.combinedstatus.BuildConfig
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceScreen(onBack: () -> Unit) {
    var followSystem by rememberSaveable { mutableStateOf(true) }
    var compactPreview by rememberSaveable { mutableStateOf(false) }
    SettingsPage(title = "外观", onBack = onBack) {
        Section("主题") {
            SwitchPreference(
                title = "跟随系统主题",
                summary = "根据系统浅色或深色模式切换",
                checked = followSystem,
                onCheckedChange = { followSystem = it },
            )
            SwitchPreference(
                title = "紧凑预览",
                summary = "仅影响当前界面预览，不写入模块配置",
                checked = compactPreview,
                onCheckedChange = { compactPreview = it },
            )
        }
    }
}

@Composable
internal fun StatusBarScreen(onBack: () -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var smoothTransition by rememberSaveable { mutableStateOf(true) }
    SettingsPage(title = "状态栏", onBack = onBack) {
        Section("三合一图标") {
            SwitchPreference(
                title = "启用三合一图标",
                summary = "UI 预览开关，当前版本尚未接入 SystemUI",
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
            SwitchPreference(
                title = "过渡效果",
                summary = "预留状态栏与控制中心过渡设置",
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
    SettingsPage(title = "锁屏与 AOD", onBack = onBack) {
        Section("显示范围") {
            SwitchPreference(
                title = "锁屏显示",
                summary = "预留锁屏稳定态与过渡控制",
                checked = keyguard,
                onCheckedChange = { keyguard = it },
            )
            SwitchPreference(
                title = "AOD 显示",
                summary = "预留息屏显示状态同步",
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
    SettingsPage(title = "充电显示", onBack = onBack) {
        Section("充电状态") {
            SwitchPreference(
                title = "显示充电状态",
                summary = "预留充电状态图标切换",
                checked = chargingIcon,
                onCheckedChange = { chargingIcon = it },
            )
            SwitchPreference(
                title = "区分超级快充",
                summary = "后续与系统状态栏判定逻辑保持一致",
                checked = superCharging,
                onCheckedChange = { superCharging = it },
                enabled = chargingIcon,
            )
        }
    }
}

@Composable
internal fun DiagnosticsScreen(onBack: () -> Unit) {
    SettingsPage(title = "诊断与关于", onBack = onBack) {
        item {
            SmallTitle("当前构建")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                    Text("CombinedStatus", style = MiuixTheme.textStyles.headline2)
                    Spacer(Modifier.height(8.dp))
                    Text("版本：${BuildConfig.VERSION_NAME}")
                    Text("构建：${BuildConfig.BUILD_ID}")
                    Text("包名：${BuildConfig.APPLICATION_ID}")
                }
            }
        }
        item {
            SmallTitle("运行阶段")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                    Text("MIUIX 0.9.4 UI 壳")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "当前未启用 Xposed Hook、SystemUI 读取或后台任务。",
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
                        Icon(MiuixIcons.Back, contentDescription = "返回")
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
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        SmallTitle(title)
        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
            content = content,
        )
    }
    item { Spacer(Modifier.height(12.dp)) }
}
