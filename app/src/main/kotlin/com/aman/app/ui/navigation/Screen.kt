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
    object CreateProtectionRequest : Screen("create_protection_request") {
        fun createRoute(numberId: String? = null) = if (!numberId.isNullOrBlank()) "client/create_request?numberId=$numberId" else "create_protection_request"
    }
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Help : Screen("help")
    object Terms : Screen("terms")
    object Privacy : Screen("privacy")
    object About : Screen("about")

    // Detail Screens
    object ClientNumberDetail : Screen("client_numbers/{numberId}") {
        fun createRoute(numberId: String) = "client_numbers/$numberId"
    }
    object ProtectionDetail : Screen("protections/{protectionId}") {
        fun createRoute(protectionId: String) = "protections/$protectionId"
    }
    object ProtectionRequestDetail : Screen("protection_requests/{requestId}") {
        fun createRoute(requestId: String) = "protection_requests/$requestId"
    }
    object AdminPaymentTaskDetail : Screen("admin_payment_task_detail/{taskId}") {
        fun createRoute(taskId: String) = "admin_payment_task_detail/$taskId"
    }

    // Administration Screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminCustomers : Screen("admin_customers")
    object AdminCustomerNumbers : Screen("admin_customer_numbers")
    object AdminCustomerDetails : Screen("admin_customers/{customerId}") {
        fun createRoute(customerId: String) = "admin_customers/$customerId"
    }
    object AdminProtectionRequests : Screen("admin_protection_requests")
    object AdminProtections : Screen("admin_protections")
    object AdminTelecomProviders : Screen("admin_telecom_providers")
    object AdminProtectionPlans : Screen("admin_protection_plans")
    object AdminPaymentMethods : Screen("admin_payment_methods")
    object AdminPaymentTasks : Screen("admin_payment_tasks")
    object AdminNotifications : Screen("admin_notifications")
    object AdminTaskSettings : Screen("admin_task_settings")
    object AdminSystemSettings : Screen("admin_system_settings")
    object AdminAuditLogs : Screen("admin_audit_logs")
}

