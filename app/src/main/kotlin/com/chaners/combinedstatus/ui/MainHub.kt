package com.chaners.combinedstatus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.chaners.combinedstatus.R
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
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec

private const val TopLevelPageCount = 3

@Composable
internal fun MainHub(onNavigate: (AppRoute) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { TopLevelPageCount })
    val scope = rememberCoroutineScope()
    val blurActive = isRuntimeShaderSupported()
    val backdrop = rememberLayerBackdrop {
        drawRect(MiuixTheme.colorScheme.background)
        drawContent()
    }

    val items = listOf(
        NavigationItem(stringResource(R.string.nav_home), MiuixIcons.Home),
        NavigationItem(stringResource(R.string.nav_features), MiuixIcons.Tune),
        NavigationItem(stringResource(R.string.nav_settings), MiuixIcons.Settings),
    )

    Scaffold(
        bottomBar = {
            FloatingNavigationBar(
                color = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
                modifier = if (blurActive) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
                        blurRadius = 24f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
                                ),
                            ),
                        ),
                    )
                } else {
                    Modifier
                },
            ) {
                items.forEachIndexed { index, item ->
                    FloatingNavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = PagerNavigationSpringSpec,
                                    )
                                }
                            }
                        },
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
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun TopLevelPager(
    pagerState: PagerState,
    bottomPadding: PaddingValues,
    onNavigate: (AppRoute) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
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
                onNavigate = onNavigate,
            )
        }
    }
}
