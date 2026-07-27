package com.ingendns.app.ui.navigation

sealed class Screen(val title: String) {

    object Dashboard : Screen("Dashboard")

    object History : Screen("History")

    object Settings : Screen("Settings")
}