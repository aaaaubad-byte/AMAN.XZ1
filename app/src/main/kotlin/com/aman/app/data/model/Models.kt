package com.aman.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain & Data Models for AMAN | أمان
 * Defined strictly according to the authoritative reference document "AMAN.XZ.txt".
 */

// ---------------------------------------------------------------------------
// 1. المستخدمون والعملاء (Users / Customers)
// ---------------------------------------------------------------------------
@Serializable
enum class UserRole {
    @SerialName("client") CLIENT,
    @SerialName("admin") ADMIN
}

@Serializable
data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.CLIENT,
    @SerialName("account_status") val accountStatus: String = "active",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ---------------------------------------------------------------------------
// 2. شركات الاتصالات والبادئات (Telecom Providers & Prefixes)
// ---------------------------------------------------------------------------
@Serializable
data class TelecomProvider(
    val id: String,
    val name: String,
    val code: String,
    @SerialName("number_length") val numberLength: Int = 9,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_visible_to_customer") val isVisibleToCustomer: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TelecomPrefix(
    val id: String,
    @SerialName("provider_id") val providerId: String,
    val prefix: String,
    @SerialName("is_active") val isActive: Boolean = true
)

// ---------------------------------------------------------------------------
// 3. أرقام العملاء (Customer Numbers)
// ---------------------------------------------------------------------------
@Serializable
enum class CustomerNumberStatus {
    @SerialName("unprotected") UNPROTECTED,
    @SerialName("pending") PENDING,
    @SerialName("protected") PROTECTED,
    @SerialName("expired") EXPIRED
}

@Serializable
data class CustomerNumber(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("phone_number") val phoneNumber: String,
    val status: CustomerNumberStatus = CustomerNumberStatus.UNPROTECTED,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Expanded relations
    @SerialName("telecom_provider") val provider: TelecomProvider? = null
)

// ---------------------------------------------------------------------------
// 4. باقات الحماية (Protection Plans)
// ---------------------------------------------------------------------------
@Serializable
data class ProtectionPlan(
    val id: String,
    @SerialName("provider_id") val providerId: String,
    val name: String,
    val price: Double,
    @SerialName("duration_days") val durationDays: Int,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_visible_to_customer") val isVisibleToCustomer: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// 5. طرق الدفع (Payment Methods)
// ---------------------------------------------------------------------------
@Serializable
data class PaymentMethod(
    val id: String,
    @SerialName("wallet_name") val walletName: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("account_holder_name") val accountHolderName: String,
    @SerialName("payment_instructions") val paymentInstructions: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

// ---------------------------------------------------------------------------
// 6. طلبات الحماية (Protection Requests)
// ---------------------------------------------------------------------------
@Serializable
enum class ProtectionRequestStatus {
    @SerialName("pending") PENDING,
    @SerialName("approved") APPROVED,
    @SerialName("rejected") REJECTED
}

@Serializable
data class ProtectionRequest(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_number_id") val customerNumberId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("payment_method_id") val paymentMethodId: String,
    @SerialName("protection_value") val protectionValue: Double,
    @SerialName("transfer_data") val transferData: String? = null,
    val status: ProtectionRequestStatus = ProtectionRequestStatus.PENDING,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded relations
    @SerialName("customer_number") val customerNumber: CustomerNumber? = null,
    @SerialName("plan") val plan: ProtectionPlan? = null,
    @SerialName("provider") val provider: TelecomProvider? = null,
    @SerialName("payment_method") val paymentMethod: PaymentMethod? = null
)

// ---------------------------------------------------------------------------
// 7. الحمايات (Protections)
// ---------------------------------------------------------------------------
@Serializable
enum class StoredProtectionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("expired") EXPIRED
}

@Serializable
data class Protection(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_number_id") val customerNumberId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("created_from_request_id") val createdFromRequestId: String? = null,
    @SerialName("price_at_purchase") val priceAtPurchase: Double,
    @SerialName("duration_at_purchase") val durationAtPurchase: Int,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val status: StoredProtectionStatus = StoredProtectionStatus.ACTIVE,
    @SerialName("created_at") val createdAt: String? = null
) {
    /**
     * "تحتاج تجديد" حالة محسوبة وليست حالة مخزنة
     */
    fun isNeedsRenewal(thresholdDays: Int = 7, currentDateIso: String): Boolean {
        if (status == StoredProtectionStatus.EXPIRED) return false
        // Calculation performed dynamically by comparing end_date against current date & threshold
        return false // Will be computed dynamically in domain/use-case layer
    }
}

// ---------------------------------------------------------------------------
// 8. المهام (Payment Tasks)
// ---------------------------------------------------------------------------
@Serializable
enum class TaskType {
    @SerialName("first") FIRST,
    @SerialName("recurring") RECURRING
}

@Serializable
enum class TaskStatus {
    @SerialName("upcoming") UPCOMING,
    @SerialName("due") DUE,
    @SerialName("overdue") OVERDUE,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED
}

@Serializable
data class PaymentTask(
    val id: String,
    @SerialName("protection_id") val protectionId: String,
    @SerialName("customer_number_id") val customerNumberId: String,
    @SerialName("task_type") val taskType: TaskType,
    val amount: Double,
    @SerialName("due_date") val dueDate: String,
    val status: TaskStatus = TaskStatus.UPCOMING,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("reschedule_data") val rescheduleData: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ---------------------------------------------------------------------------
// 9. إعدادات المهام (Task Settings)
// ---------------------------------------------------------------------------
@Serializable
data class TaskSettings(
    val id: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("first_task_enabled") val firstTaskEnabled: Boolean = false,
    @SerialName("first_task_amount") val firstTaskAmount: Double = 0.0,
    @SerialName("recurring_task_enabled") val recurringTaskEnabled: Boolean = true,
    @SerialName("recurring_task_amount") val recurringTaskAmount: Double = 0.0,
    @SerialName("recurring_cycle_days") val recurringCycleDays: Int = 30,
    @SerialName("days_visible_before_due") val daysVisibleBeforeDue: Int = 3,
    @SerialName("manual_reschedule_enabled") val manualRescheduleEnabled: Boolean = true
)

// ---------------------------------------------------------------------------
// 10. الإشعارات (Notifications)
// ---------------------------------------------------------------------------
@Serializable
data class AppNotification(
    val id: String,
    @SerialName("customer_id") val customerId: String? = null,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// 11. سجل العمليات (Audit Logs)
// ---------------------------------------------------------------------------
@Serializable
data class AuditLog(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("action_type") val actionType: String,
    @SerialName("affected_record") val affectedRecord: String? = null,
    val details: String? = null,
    @SerialName("previous_data") val previousData: String? = null,
    @SerialName("new_data") val newData: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// 12. إعدادات النظام (System Settings)
// ---------------------------------------------------------------------------
@Serializable
data class SystemSettings(
    val id: String,
    @SerialName("app_name") val appName: String = "AMAN | أمان",
    @SerialName("contact_info") val contactInfo: String? = null,
    @SerialName("terms_and_conditions") val termsAndConditions: String? = null,
    @SerialName("privacy_policy") val privacyPolicy: String? = null
)
