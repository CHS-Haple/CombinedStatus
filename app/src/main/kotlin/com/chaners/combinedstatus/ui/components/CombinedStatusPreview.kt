package com.chaners.combinedstatus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CombinedStatusPreview(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column {
            Text(
                text = "状态预览",
                style = MiuixTheme.textStyles.headline2,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "▂▄▆█   Wi‑Fi   87%",
                    style = MiuixTheme.textStyles.title2,
                )
                Text(
                    text = "预览",
                    modifier = Modifier.padding(start = 12.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
