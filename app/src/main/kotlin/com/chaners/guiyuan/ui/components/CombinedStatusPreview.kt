package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.chaners.guiyuan.xposed.CombinedStatusRenderModel
import com.chaners.guiyuan.xposed.CombinedStatusRenderView
import com.chaners.guiyuan.xposed.CombinedStatusTintState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CombinedStatusPreview(
    model: CombinedStatusRenderModel,
    modifier: Modifier = Modifier,
) {
    val tint = MiuixTheme.colorScheme.onSurfaceContainer.toArgb()

    AndroidView(
        factory = { context ->
            CombinedStatusRenderView(context).apply {
                setScaleMobileTypeWithCanvas(true)
                setTintState(
                    CombinedStatusTintState(
                        appliedTint = tint,
                        statusIconTint = tint,
                    ),
                )
                setModel(model)
            }
        },
        modifier = modifier,
        update = { view ->
            view.setScaleMobileTypeWithCanvas(true)
            view.setTintState(
                CombinedStatusTintState(
                    appliedTint = tint,
                    statusIconTint = tint,
                ),
            )
            view.setModel(model)
        },
    )
}
