package com.chaners.combinedstatus.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Appearance : AppRoute

    @Serializable
    data object StatusBar : AppRoute

    @Serializable
    data object Keyguard : AppRoute

    @Serializable
    data object Charging : AppRoute

    @Serializable
    data object Diagnostics : AppRoute
}
