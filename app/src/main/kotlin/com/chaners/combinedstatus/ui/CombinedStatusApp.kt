package com.chaners.combinedstatus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.combinedstatus.settings.AppLanguage
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.settings.FloatingNavigationContent
import com.chaners.combinedstatus.settings.FloatingNavigationStyle
import com.chaners.combinedstatus.ui.navigation.AppRoute
import com.chaners.combinedstatus.ui.screens.AppearanceScreen
import com.chaners.combinedstatus.ui.screens.DiagnosticsScreen
import com.chaners.combinedstatus.ui.screens.PreviewSandboxScreen
import com.chaners.combinedstatus.ui.screens.PreviewSandboxUiState
import com.chaners.combinedstatus.ui.theme.CombinedStatusTheme
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CombinedStatusApp(
    settings: AppearanceSettings,
    darkMode: Boolean,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    onHotReload: (() -> Unit) -> Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationBarEnabledChange: (Boolean) -> Unit,
    onFloatingNavigationStyleChange: (FloatingNavigationStyle) -> Unit,
    onFloatingNavigationContentChange: (FloatingNavigationContent) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
) {
    CombinedStatusTheme(
        themeMode = settings.themeMode,
        dynamicColorEnabled = settings.dynamicColorEnabled,
    ) {
        var previewCenterIndex by rememberSaveable { mutableIntStateOf(0) }
        var previewSignalIndex by rememberSaveable { mutableIntStateOf(0) }
        var previewBatteryIndex by rememberSaveable { mutableIntStateOf(0) }
        val previewState =
            PreviewSandboxUiState(
                centerIndex = previewCenterIndex,
                signalIndex = previewSignalIndex,
                batteryIndex = previewBatteryIndex,
            )

        val backStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
        val swipeBackDirection = when {
            !settings.swipeBackEnabled -> NavSwipeDirection.None
            LocalLayoutDirection.current == LayoutDirection.Ltr -> NavSwipeDirection.LeftToRight
            else -> NavSwipeDirection.RightToLeft
        }

        fun navigate(route: AppRoute) {
            if (route !in backStack) {
                backStack.add(route)
            }
        }

        fun navigateBack() {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = ::navigateBack,
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                cornerClipRadius = rememberNavSystemCornerRadius(),
                backdropColor = MiuixTheme.colorScheme.surface,
            ),
        ) {
            entry<AppRoute.Home> {
                MainHub(
                    settings = settings,
                    darkMode = darkMode,
                    appLanguage = appLanguage,
                    launcherIconHidden = launcherIconHidden,
                    onHotReload = onHotReload,
                    onAppLanguageChange = onAppLanguageChange,
                    onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                    onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                    previewState = previewState,
                    onNavigate = ::navigate,
                )
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(
                    settings = settings,
                    darkMode = darkMode,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorEnabledChange = onDynamicColorEnabledChange,
                    onFloatingNavigationBarEnabledChange =
                        onFloatingNavigationBarEnabledChange,
                    onFloatingNavigationStyleChange =
                        onFloatingNavigationStyleChange,
                    onFloatingNavigationContentChange =
                        onFloatingNavigationContentChange,
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.PreviewSandbox>(swipeDismiss = swipeBackDirection) {
                PreviewSandboxScreen(
                    state = previewState,
                    onCenterIndexChange = { previewCenterIndex = it },
                    onSignalIndexChange = { previewSignalIndex = it },
                    onBatteryIndexChange = { previewBatteryIndex = it },
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.Diagnostics>(swipeDismiss = swipeBackDirection) {
                DiagnosticsScreen(onBack = ::navigateBack)
            }
        }
    }
}
