package com.chaners.combinedstatus.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun pageContentPadding(
    innerPadding: PaddingValues,
    outerBottomPadding: Dp? = null,
    extraBottom: Dp,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val start = innerPadding.calculateStartPadding(layoutDirection)
    val top = innerPadding.calculateTopPadding()
    val end = innerPadding.calculateEndPadding(layoutDirection)
    val bottom = (outerBottomPadding ?: innerPadding.calculateBottomPadding()) + extraBottom

    return remember(start, top, end, bottom) {
        PaddingValues(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )
    }
}

internal fun Modifier.pageVerticalOverscroll(
    scrollBehavior: ScrollBehavior,
): Modifier =
    overScrollVertical()
        .nestedScroll(scrollBehavior.nestedScrollConnection)
