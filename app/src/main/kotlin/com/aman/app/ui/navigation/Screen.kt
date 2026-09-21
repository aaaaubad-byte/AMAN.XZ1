package com.aman.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ClientHome : Screen("client_home")
    object MyNumbers : Screen("my_numbers")
    object ProtectionRequests : Screen("protection_requests")
    object AdminDashboard : Screen("admin_dashboard")
}
