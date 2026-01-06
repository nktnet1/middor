package uk.nktnet.middor.config

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Settings : Screen("settings")
    object Help : Screen("help")
}
