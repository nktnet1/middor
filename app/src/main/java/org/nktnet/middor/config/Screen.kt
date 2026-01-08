package org.nktnet.middor.config

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Settings : Screen("settings")
    object Info : Screen("info")
    object Help : Screen("help")
}
