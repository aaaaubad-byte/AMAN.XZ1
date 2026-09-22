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

    // Administration Screens (Stage 4)
    object AdminDashboard : Screen("admin_dashboard")
    object AdminCustomers : Screen("admin_customers")
    object AdminCustomerDetails : Screen("admin_customers/{customerId}") {
        fun createRoute(customerId: String) = "admin_customers/$customerId"
    }
    object AdminProtectionRequests : Screen("admin_protection_requests")
    object AdminProtections : Screen("admin_protections")
    object AdminTelecomProviders : Screen("admin_telecom_providers")
    object AdminProtectionPlans : Screen("admin_protection_plans")
    object AdminPaymentMethods : Screen("admin_payment_methods")
    object AdminPaymentTasks : Screen("admin_payment_tasks")
    object AdminTaskSettings : Screen("admin_task_settings")
    object AdminSystemSettings : Screen("admin_system_settings")
    object AdminAuditLogs : Screen("admin_audit_logs")
}

