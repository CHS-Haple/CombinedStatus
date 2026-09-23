package com.chaners.combinedstatus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TopBarProgressiveEdgeHeight = 32.dp

@Composable
internal fun rememberTopBarContentBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

internal fun Modifier.captureForTopBarBlur(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) {
        layerBackdrop(backdrop)
    } else {
        this
    }

@Composable
internal fun TopBarProgressiveEdge(
    backdrop: LayerBackdrop?,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    if (backdrop == null) return

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TopBarProgressiveEdgeHeight)
                .offset(y = topPadding)
                .progressiveTextureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    gradient = ProgressiveBlur.Top,
                ),
    )
}
