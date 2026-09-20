package com.chaners.combinedstatus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.settings.AppearanceSettingsRepository
import com.chaners.combinedstatus.ui.navigation.AppRoute
import com.chaners.combinedstatus.ui.screens.AppearanceScreen
import com.chaners.combinedstatus.ui.screens.ChargingScreen
import com.chaners.combinedstatus.ui.screens.DiagnosticsScreen
import com.chaners.combinedstatus.ui.screens.KeyguardScreen
import com.chaners.combinedstatus.ui.screens.StatusBarScreen
import com.chaners.combinedstatus.ui.theme.CombinedStatusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CombinedStatusApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppearanceSettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = AppearanceSettings())
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val darkAppearance = when (settings.themeMode) {
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
        AppThemeMode.System,
        AppThemeMode.Dynamic,
        -> systemDark
    }

    CombinedStatusTheme(themeMode = settings.themeMode) {
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
                backdropColor = MiuixTheme.colorScheme.background,
            ),
        ) {
            entry<AppRoute.Home> {
                MainHub(
                    settings = settings,
                    darkAppearance = darkAppearance,
                    onNavigate = ::navigate,
                )
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(
                    settings = settings,
                    onThemeModeChange = { mode ->
                        scope.launch { repository.setThemeMode(mode) }
                    },
                    onBlurEnabledChange = { enabled ->
                        scope.launch { repository.setBlurEnabled(enabled) }
                    },
                    onGlassBottomBarEnabledChange = { enabled ->
                        scope.launch { repository.setGlassBottomBarEnabled(enabled) }
                    },
                    onSwipeBackEnabledChange = { enabled ->
                        scope.launch { repository.setSwipeBackEnabled(enabled) }
                    },
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.StatusBar>(swipeDismiss = swipeBackDirection) {
                StatusBarScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.Keyguard>(swipeDismiss = swipeBackDirection) {
                KeyguardScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.Charging>(swipeDismiss = swipeBackDirection) {
                ChargingScreen(onBack = ::navigateBack)
            }
            entry<AppRoute.Diagnostics>(swipeDismiss = swipeBackDirection) {
                DiagnosticsScreen(onBack = ::navigateBack)
            }
        }
    }
}
