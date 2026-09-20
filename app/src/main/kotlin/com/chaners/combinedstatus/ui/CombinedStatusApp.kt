package com.chaners.combinedstatus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.chaners.combinedstatus.ui.screens.AppearanceScreen
import com.chaners.combinedstatus.ui.screens.ChargingScreen
import com.chaners.combinedstatus.ui.screens.DiagnosticsScreen
import com.chaners.combinedstatus.ui.screens.HomeScreen
import com.chaners.combinedstatus.ui.screens.KeyguardScreen
import com.chaners.combinedstatus.ui.screens.StatusBarScreen
import com.chaners.combinedstatus.ui.theme.CombinedStatusTheme

internal enum class AppScreen {
    Home,
    Appearance,
    StatusBar,
    Keyguard,
    Charging,
    Diagnostics,
}

@Composable
fun CombinedStatusApp() {
    var screenName by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    val screen = AppScreen.valueOf(screenName)
    val navigate: (AppScreen) -> Unit = { screenName = it.name }
    val navigateHome = { screenName = AppScreen.Home.name }

    BackHandler(enabled = screen != AppScreen.Home, onBack = navigateHome)

    CombinedStatusTheme {
        when (screen) {
            AppScreen.Home -> HomeScreen(onNavigate = navigate)
            AppScreen.Appearance -> AppearanceScreen(onBack = navigateHome)
            AppScreen.StatusBar -> StatusBarScreen(onBack = navigateHome)
            AppScreen.Keyguard -> KeyguardScreen(onBack = navigateHome)
            AppScreen.Charging -> ChargingScreen(onBack = navigateHome)
            AppScreen.Diagnostics -> DiagnosticsScreen(onBack = navigateHome)
        }
    }
}
