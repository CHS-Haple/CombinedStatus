package com.chaners.combinedstatus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.chaners.combinedstatus.ui.screens.PreviewBatteryMode
import com.chaners.combinedstatus.ui.screens.PreviewChargingState
import com.chaners.combinedstatus.ui.screens.PreviewMobileNetwork
import com.chaners.combinedstatus.ui.screens.PreviewSandboxScreen
import com.chaners.combinedstatus.ui.screens.PreviewSandboxUiState
import com.chaners.combinedstatus.ui.screens.PreviewWifiState
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
        var previewSimPresent by rememberSaveable { mutableStateOf(true) }
        var previewAirplaneMode by rememberSaveable { mutableStateOf(false) }
        var previewMobileNetworkIndex by rememberSaveable {
            mutableIntStateOf(PreviewMobileNetwork.FIVE_G.ordinal)
        }
        var previewMobileSignalLevel by rememberSaveable { mutableIntStateOf(4) }
        var previewWifiStateIndex by rememberSaveable {
            mutableIntStateOf(PreviewWifiState.CONNECTED.ordinal)
        }
        var previewWifiSignalLevel by rememberSaveable { mutableIntStateOf(3) }
        var previewBatteryPercent by rememberSaveable { mutableIntStateOf(87) }
        var previewBatteryModeIndex by rememberSaveable {
            mutableIntStateOf(PreviewBatteryMode.BALANCED.ordinal)
        }
        var previewChargingStateIndex by rememberSaveable {
            mutableIntStateOf(PreviewChargingState.NOT_CHARGING.ordinal)
        }
        val previewState =
            PreviewSandboxUiState(
                simPresent = previewSimPresent,
                airplaneMode = previewAirplaneMode,
                mobileNetwork =
                    PreviewMobileNetwork.entries[
                        previewMobileNetworkIndex.coerceIn(
                            0,
                            PreviewMobileNetwork.entries.lastIndex,
                        )
                    ],
                mobileSignalLevel = previewMobileSignalLevel,
                wifiState =
                    PreviewWifiState.entries[
                        previewWifiStateIndex.coerceIn(
                            0,
                            PreviewWifiState.entries.lastIndex,
                        )
                    ],
                wifiSignalLevel = previewWifiSignalLevel,
                batteryPercent = previewBatteryPercent,
                batteryMode =
                    PreviewBatteryMode.entries[
                        previewBatteryModeIndex.coerceIn(
                            0,
                            PreviewBatteryMode.entries.lastIndex,
                        )
                    ],
                chargingState =
                    PreviewChargingState.entries[
                        previewChargingStateIndex.coerceIn(
                            0,
                            PreviewChargingState.entries.lastIndex,
                        )
                    ],
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
                    onSimPresentChange = { previewSimPresent = it },
                    onAirplaneModeChange = { previewAirplaneMode = it },
                    onMobileNetworkChange = {
                        previewMobileNetworkIndex = it.ordinal
                    },
                    onMobileSignalLevelChange = {
                        previewMobileSignalLevel = it
                    },
                    onWifiStateChange = {
                        previewWifiStateIndex = it.ordinal
                    },
                    onWifiSignalLevelChange = {
                        previewWifiSignalLevel = it
                    },
                    onBatteryPercentChange = {
                        previewBatteryPercent = it
                    },
                    onBatteryModeChange = {
                        previewBatteryModeIndex = it.ordinal
                    },
                    onChargingStateChange = {
                        previewChargingStateIndex = it.ordinal
                    },
                    onBack = ::navigateBack,
                )
            }
            entry<AppRoute.Diagnostics>(swipeDismiss = swipeBackDirection) {
                DiagnosticsScreen(onBack = ::navigateBack)
            }
        }
    }
}
