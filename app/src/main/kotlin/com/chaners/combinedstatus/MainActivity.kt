package com.chaners.combinedstatus

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.chaners.combinedstatus.settings.AppThemeMode
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.settings.AppearanceSettingsRepository
import com.chaners.combinedstatus.ui.CombinedStatusApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val repository = remember {
                AppearanceSettingsRepository(applicationContext)
            }
            val settings by repository.settings.collectAsState(initial = AppearanceSettings())
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val darkMode = when (settings.themeMode) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System,
                AppThemeMode.Dynamic,
                -> systemDark
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose { }
            }

            CombinedStatusApp(
                settings = settings,
                darkMode = darkMode,
                onThemeModeChange = { mode ->
                    scope.launch { repository.setThemeMode(mode) }
                },
                onFloatingNavigationBlurEnabledChange = { enabled ->
                    scope.launch { repository.setFloatingNavigationBlurEnabled(enabled) }
                },
                onSwipeBackEnabledChange = { enabled ->
                    scope.launch { repository.setSwipeBackEnabled(enabled) }
                },
            )
        }
    }
}
