package com.chaners.combinedstatus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chaners.combinedstatus.settings.AppThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
internal fun CombinedStatusTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = when (themeMode) {
        AppThemeMode.System -> ColorSchemeMode.System
        AppThemeMode.Light -> ColorSchemeMode.Light
        AppThemeMode.Dark -> ColorSchemeMode.Dark
        AppThemeMode.Dynamic -> ColorSchemeMode.MonetSystem
    }
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    MiuixTheme(controller = controller, content = content)
}
