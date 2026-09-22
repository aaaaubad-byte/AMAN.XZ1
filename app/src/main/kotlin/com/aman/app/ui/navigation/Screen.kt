package com.aman.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ClientHome : Screen("client_home")
    object MyNumbers : Screen("my_numbers")
    object AddNumber : Screen("add_number")
    object Protections : Screen("protections")
    object ProtectionRequests : Screen("protection_requests")
    object CreateProtectionRequest : Screen("create_protection_request")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Help : Screen("help")
    object Terms : Screen("terms")
    object Privacy : Screen("privacy")
    object About : Screen("about")
    object AdminDashboard : Screen("admin_dashboard")
}
