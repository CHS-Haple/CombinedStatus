package com.chaners.combinedstatus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chaners.combinedstatus.R
import com.chaners.combinedstatus.settings.AppLanguage
import com.chaners.combinedstatus.settings.AppearanceSettings
import com.chaners.combinedstatus.ui.navigation.AppRoute
import com.chaners.combinedstatus.ui.screens.FeaturesScreen
import com.chaners.combinedstatus.ui.screens.HomeScreen
import com.chaners.combinedstatus.ui.screens.SettingsHubScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private const val TopLevelPageCount = 3

@Composable
internal fun MainHub(
    settings: AppearanceSettings,
    darkMode: Boolean,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val scope = rememberCoroutineScope()
    val blurActive =
        settings.floatingNavigationBlurEnabled &&
            isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val items = listOf(
        NavigationItem(stringResource(R.string.nav_home), MiuixIcons.Home),
        NavigationItem(stringResource(R.string.nav_features), MiuixIcons.Tune),
        NavigationItem(stringResource(R.string.nav_settings), MiuixIcons.Settings),
    )

    val floatingHighlight = remember(darkMode) {
        if (darkMode) {
            Highlight.GlassStrokeMiddleDark
        } else {
            Highlight.GlassStrokeMiddleLight
        }
    }

    fun selectPage(index: Int) {
        if (pagerState.currentPage != index) {
            scope.launch {
                pagerState.springAnimateToPage(index)
            }
        }
    }

    TopLevelBackHandler(
        pagerState = pagerState,
        onBackToHome = { selectPage(0) },
    )

    val navigationBarModifier =
        if (blurActive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(
                            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                        ),
                    ),
                ),
                highlight = floatingHighlight,
            )
        } else {
            Modifier
        }

    Scaffold(
        bottomBar = {
            FloatingNavigationBar(
                modifier = navigationBarModifier,
                color = if (blurActive) {
                    Color.Transparent
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            ) {
                items.forEachIndexed { index, item ->
                    FloatingNavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { selectPage(index) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            TopLevelPager(
                pagerState = pagerState,
                bottomPadding = innerPadding,
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                swipeBackEnabled = settings.swipeBackEnabled,
                onAppLanguageChange = onAppLanguageChange,
                onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun TopLevelBackHandler(
    pagerState: PagerState,
    onBackToHome: () -> Unit,
) {
    val isBackEnabled by remember {
        derivedStateOf { pagerState.currentPage != 0 }
    }
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = isBackEnabled,
        onBackCompleted = onBackToHome,
    )
}

@Composable
private fun TopLevelPager(
    pagerState: PagerState,
    bottomPadding: PaddingValues,
    appLanguage: AppLanguage,
    launcherIconHidden: Boolean,
    swipeBackEnabled: Boolean,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onLauncherIconHiddenChange: (Boolean) -> Unit,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        flingBehavior = flingBehavior,
    ) { page ->
        val bottom = bottomPadding.calculateBottomPadding()
        when (page) {
            0 -> HomeScreen(bottomContentPadding = bottom)
            1 -> FeaturesScreen(
                bottomContentPadding = bottom,
                onNavigate = onNavigate,
            )
            2 -> SettingsHubScreen(
                bottomContentPadding = bottom,
                appLanguage = appLanguage,
                launcherIconHidden = launcherIconHidden,
                swipeBackEnabled = swipeBackEnabled,
                onAppLanguageChange = onAppLanguageChange,
                onLauncherIconHiddenChange = onLauncherIconHiddenChange,
                onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                onNavigate = onNavigate,
            )
        }
    }
}
