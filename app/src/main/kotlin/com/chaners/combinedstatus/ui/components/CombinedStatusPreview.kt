package com.chaners.combinedstatus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.chaners.combinedstatus.xposed.CombinedStatusRenderModel
import com.chaners.combinedstatus.xposed.CombinedStatusRenderView
import com.chaners.combinedstatus.xposed.CombinedStatusTintState
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
