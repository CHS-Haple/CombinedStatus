package com.chaners.combinedstatus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaners.combinedstatus.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun HotReloadAction(
    inProgress: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = !inProgress,
    ) {
        Icon(
            imageVector = MiuixIcons.Refresh,
            contentDescription = stringResource(R.string.hot_reload),
        )
    }
}
