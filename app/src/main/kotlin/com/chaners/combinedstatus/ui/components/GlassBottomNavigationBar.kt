package com.chaners.combinedstatus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GlassBottomNavigationBar(
    items: List<NavigationItem>,
    pagerState: PagerState,
    backdrop: LayerBackdrop,
    darkAppearance: Boolean,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val shape = androidx.compose.foundation.shape.CircleShape
    val navigationBarInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding =
        if (navigationBarInset > 0.dp) navigationBarInset + 10.dp else 28.dp

    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer
    val primaryColor = MiuixTheme.colorScheme.primary
    val contentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val glassSurface = surfaceColor.copy(alpha = if (darkAppearance) 0.46f else 0.58f)
    val indicatorSurface = primaryColor.copy(alpha = if (darkAppearance) 0.18f else 0.12f)
    val glassHighlight =
        if (darkAppearance) Highlight.GlassStrokeSmallDark else Highlight.GlassStrokeSmallLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(64.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        padding = maxOf(padding, 24.dp.toPx())
                        blur(18.dp.toPx())
                        colorControls(
                            contrast = 1.04f,
                            saturation = 1.12f,
                        )
                    },
                    highlight = { glassHighlight.copy(alpha = 0.82f) },
                    onDrawSurface = { drawRect(glassSurface) },
                ),
        ) {
            var contentWidthPx by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .onSizeChanged { contentWidthPx = it.width.toFloat() },
            ) {
                if (contentWidthPx > 0f) {
                    val itemWidthPx = contentWidthPx / items.size
                    val itemWidth = with(density) { itemWidthPx.toDp() }

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val pagePosition = (
                                    pagerState.currentPage +
                                        pagerState.currentPageOffsetFraction
                                    ).coerceIn(0f, items.lastIndex.toFloat())
                                val physicalPage =
                                    if (isLtr) pagePosition else items.lastIndex - pagePosition
                                translationX = physicalPage * itemWidthPx
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = {
                                    blur(8.dp.toPx())
                                    colorControls(
                                        contrast = 1.03f,
                                        saturation = 1.16f,
                                    )
                                },
                                highlight = { glassHighlight.copy(alpha = 0.62f) },
                                onDrawSurface = { drawRect(indicatorSurface) },
                            ),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = pagerState.currentPage == index
                        val tint =
                            if (selected) primaryColor else contentColor.copy(alpha = 0.62f)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .selectable(
                                    selected = selected,
                                    onClick = { onSelected(index) },
                                    role = Role.Tab,
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = tint,
                            )
                            Text(
                                text = item.label,
                                color = tint,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
