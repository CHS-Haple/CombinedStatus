package com.chaners.combinedstatus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.chaners.combinedstatus.ui.navigation.AppRoute
import com.chaners.combinedstatus.ui.screens.AppearanceScreen
import com.chaners.combinedstatus.ui.screens.ChargingScreen
import com.chaners.combinedstatus.ui.screens.DiagnosticsScreen
import com.chaners.combinedstatus.ui.screens.HomeScreen
import com.chaners.combinedstatus.ui.screens.KeyguardScreen
import com.chaners.combinedstatus.ui.screens.StatusBarScreen
import com.chaners.combinedstatus.ui.theme.CombinedStatusTheme
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CombinedStatusApp() {
    CombinedStatusTheme {
        val backStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
        val swipeBackDirection =
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
                NavSwipeDirection.LeftToRight
            } else {
                NavSwipeDirection.RightToLeft
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
                HomeScreen(onNavigate = ::navigate)
            }
            entry<AppRoute.Appearance>(swipeDismiss = swipeBackDirection) {
                AppearanceScreen(onBack = ::navigateBack)
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
