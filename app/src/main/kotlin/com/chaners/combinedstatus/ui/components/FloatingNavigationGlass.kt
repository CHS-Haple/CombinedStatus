package com.chaners.combinedstatus.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val FloatingNavigationBlurRadius = 22f
internal const val FloatingNavigationBlendAlpha = 0.45f

@Composable
internal fun Modifier.floatingNavigationGlass(
    backdrop: Backdrop,
    darkMode: Boolean,
): Modifier =
    textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
        blurRadius = FloatingNavigationBlurRadius,
        colors =
            BlurDefaults.blurColors(
                blendColors =
                    listOf(
                        BlendColorEntry(
                            color =
                                MiuixTheme.colorScheme.surfaceContainer.copy(
                                    alpha = FloatingNavigationBlendAlpha,
                                ),
                        ),
                    ),
            ),
        highlight =
            if (darkMode) {
                Highlight.GlassStrokeSmallDark
            } else {
                Highlight.GlassStrokeSmallLight
            },
    )
