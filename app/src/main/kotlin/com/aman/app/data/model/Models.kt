package com.aman.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Domain & Data Models for AMAN | أمان
 * Defined strictly according to the authoritative V7 database schema.
 */

// ---------------------------------------------------------------------------
// 1. المستخدمون والعملاء (Users / Customers)
// ---------------------------------------------------------------------------

@Serializable
enum class UserRole {
    @SerialName("customer") CUSTOMER,
    @SerialName("client") CLIENT,
    @SerialName("manager") MANAGER,
    @SerialName("admin") ADMIN;

    companion object {
        fun fromValue(rawRole: String?): UserRole {
            return when (rawRole?.trim()?.lowercase()) {
                "manager" -> MANAGER
                "admin" -> ADMIN
                "client" -> CLIENT
                "customer" -> CUSTOMER
                else -> CUSTOMER
            }
        }
    }

    val isManagerOrAdmin: Boolean
        get() = this == MANAGER || this == ADMIN
}

@Serializable
data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.CUSTOMER,
    @SerialName("status") val accountStatus: String = "active",
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val status: String get() = accountStatus
    val username: String get() = name

    constructor(
        id: String,
        email: String,
        username: String,
        role: UserRole = UserRole.CUSTOMER
    ) : this(id = id, name = username, email = email, role = role, accountStatus = "active")

    companion object {
        fun fallbackFromAuth(
            id: String,
            email: String,
            displayName: String? = null,
            rawRole: String? = null,
            accountStatus: String = "active"
        ): AppUser {
            val safeName = displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore('@').ifBlank { "User" }
            return AppUser(
                id = id,
                name = safeName,
                email = email,
                role = UserRole.fromValue(rawRole),
                accountStatus = accountStatus
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. شركات الاتصالات والبادئات (Telecom Providers & Prefixes)
// ---------------------------------------------------------------------------

@Serializable
data class TelecomProvider(
    val id: String,
    val name: String,
    val code: String,
    @SerialName("phone_length") val numberLength: Int = 9,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_visible_to_customers") val isVisibleToCustomer: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val phoneLength: Int get() = numberLength
}

@Serializable
data class TelecomPrefix(
    val id: String,
    @SerialName("provider_id") val providerId: String,
    val prefix: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ---------------------------------------------------------------------------
// 3. أرقام العملاء (Customer Numbers)
// ---------------------------------------------------------------------------

@Serializable
enum class CustomerNumberStatus {
    @SerialName("active") ACTIVE,
    @SerialName("suspended") SUSPENDED,
    @SerialName("inactive") INACTIVE
}

@Serializable
enum class NumberProtectionStatus {
    @SerialName("unprotected") UNPROTECTED,
    @SerialName("pending") PENDING,
    @SerialName("protected") PROTECTED
}

@Serializable
data class CustomerNumber(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("phone_number") val phoneNumber: String,
    val status: CustomerNumberStatus = CustomerNumberStatus.ACTIVE,
    @SerialName("protection_status") val protectionStatus: NumberProtectionStatus = NumberProtectionStatus.UNPROTECTED,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
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
    @SerialName("currency") val currency: String = "SAR",
    @SerialName("protection_duration_days") val durationDays: Int = 30,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_visible_to_customers") val isVisibleToCustomer: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ---------------------------------------------------------------------------
// 5. طرق الدفع (Payment Methods)
// ---------------------------------------------------------------------------

@Serializable
data class PaymentMethod(
    val id: String,
    val name: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("account_owner_name") val accountOwnerName: String = "",
    @SerialName("payment_instructions") val paymentInstructions: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val walletName: String get() = name
    val accountHolderName: String get() = accountOwnerName
    val instructions: String? get() = paymentInstructions
}

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
    @SerialName("protection_value") val protectionValue: Double = 0.0,
    @SerialName("transfer_data") val rawTransferData: JsonElement? = null,
    val status: ProtectionRequestStatus = ProtectionRequestStatus.PENDING,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("plan_name_snapshot") val planNameSnapshot: String? = null,
    @SerialName("plan_price_snapshot") val planPriceSnapshot: Double? = null,
    @SerialName("plan_duration_days_snapshot") val planDurationDaysSnapshot: Int? = null,
    @SerialName("payment_method_name_snapshot") val paymentMethodNameSnapshot: String? = null,
    @SerialName("payment_account_number_snapshot") val paymentAccountNumberSnapshot: String? = null,
    @SerialName("payment_account_owner_snapshot") val paymentAccountOwnerSnapshot: String? = null,
    @SerialName("payment_instructions_snapshot") val paymentInstructionsSnapshot: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Expanded relations
    @SerialName("customer_number") val customerNumber: CustomerNumber? = null,
    @SerialName("plan") val plan: ProtectionPlan? = null,
    @SerialName("provider") val provider: TelecomProvider? = null,
    @SerialName("payment_method") val paymentMethod: PaymentMethod? = null
) {
    val transferData: String?
        get() = when (val el = rawTransferData) {
            null -> null
            is JsonPrimitive -> el.contentOrNull
            is JsonObject -> el["reference"]?.jsonPrimitive?.contentOrNull
                ?: el["note"]?.jsonPrimitive?.contentOrNull
                ?: el.toString()
            else -> el.toString()
        }
}

// ---------------------------------------------------------------------------
// 7. الحمايات (Protections)
// ---------------------------------------------------------------------------

@Serializable
enum class StoredProtectionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("expired") EXPIRED
}

@Serializable
enum class ProtectionDisplayStatus {
    @SerialName("active") ACTIVE,
    @SerialName("renewal_needed") NEEDS_RENEWAL,
    @SerialName("expired") EXPIRED;

    val isRenewalNeeded: Boolean get() = this == NEEDS_RENEWAL
}

@Serializable
data class Protection(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_number_id") val customerNumberId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("created_from_request_id") val createdFromRequestId: String? = null,
    @SerialName("protection_value") val priceAtPurchase: Double = 0.0,
    @SerialName("protection_duration_days") val durationAtPurchase: Int = 30,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val status: StoredProtectionStatus = StoredProtectionStatus.ACTIVE,
    @SerialName("plan_name_snapshot") val planNameSnapshot: String? = null,
    @SerialName("provider_name_snapshot") val providerNameSnapshot: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Expanded relations
    @SerialName("customer_number") val customerNumber: CustomerNumber? = null,
    @SerialName("plan") val plan: ProtectionPlan? = null,
    @SerialName("provider") val provider: TelecomProvider? = null
) {
    val protectionValue: Double get() = priceAtPurchase
    val protectionDurationDays: Int get() = durationAtPurchase

    /**
     * حساب الأيام المتبقية حتى تاريخ الانتهاء
     */
    fun daysRemaining(): Long {
        return try {
            val endEpoch = java.time.Instant.parse(
                if (endDate.endsWith("Z") || endDate.contains("+")) endDate else "Z"
            ).epochSecond
            val nowEpoch = java.time.Instant.now().epochSecond
            val diffSec = endEpoch - nowEpoch
            if (diffSec <= 0) 0L else diffSec / 86400L
        } catch (_: Exception) {
            try {
                val localEnd = java.time.LocalDate.parse(endDate.take(10))
                val today = java.time.LocalDate.now()
                val diff = java.time.temporal.ChronoUnit.DAYS.between(today, localEnd)
                if (diff < 0) 0L else diff
            } catch (_: Exception) {
                0L
            }
        }
    }

    /**
     * "تحتاج تجديد" حالة محسوبة ديناميكياً وليست حالة مخزنة
     */
    fun calculateDisplayStatus(warningDaysThreshold: Int = 30): ProtectionDisplayStatus {
        if (status == StoredProtectionStatus.EXPIRED) return ProtectionDisplayStatus.EXPIRED
        val remaining = daysRemaining()
        return when {
            remaining <= 0 -> ProtectionDisplayStatus.EXPIRED
            remaining <= warningDaysThreshold -> ProtectionDisplayStatus.NEEDS_RENEWAL
            else -> ProtectionDisplayStatus.ACTIVE
        }
    }
}

// ---------------------------------------------------------------------------
// 8. المهام (Payment Tasks)
// ---------------------------------------------------------------------------

@Serializable
enum class TaskType {
    @SerialName("first") FIRST,
    @SerialName("recurring") RECURRING,
    @SerialName("manual") MANUAL
}

@Serializable
enum class TaskStatus {
    @SerialName("upcoming") UPCOMING,
    @SerialName("pending") PENDING,
    @SerialName("due_soon") DUE_SOON,
    @SerialName("due") DUE,
    @SerialName("overdue") OVERDUE,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED;

    val isActionable: Boolean
        get() = this == UPCOMING || this == PENDING || this == DUE_SOON || this == DUE || this == OVERDUE

    val label: String
        get() = when (this) {
            UPCOMING -> "قادمة"
            PENDING -> "قيد التنفيذ"
            DUE_SOON -> "قريبة الاستحقاق"
            DUE -> "مستحقة"
            OVERDUE -> "متأخرة"
            COMPLETED -> "مكتملة"
            CANCELLED -> "ملغاة"
        }
}

@Serializable
data class PaymentTask(
    val id: String,
    @SerialName("protection_id") val protectionId: String,
    @SerialName("customer_number_id") val customerNumberId: String,
    @SerialName("provider_id") val providerId: String? = null,
    @SerialName("task_type") val taskType: TaskType = TaskType.RECURRING,
    val amount: Double,
    @SerialName("due_date") val dueDate: String,
    val status: TaskStatus = TaskStatus.UPCOMING,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("previous_due_date") val previousDueDate: String? = null,
    @SerialName("rescheduled_at") val rescheduledAt: String? = null,
    @SerialName("rescheduled_by") val rescheduledBy: String? = null,
    @SerialName("reschedule_reason") val rescheduleReason: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancelled_by") val cancelledBy: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("customer_number") val customerNumber: CustomerNumber? = null,
    @SerialName("provider") val provider: TelecomProvider? = null
) {
    fun displayStatus(visibilityWindowDays: Int = 7, now: LocalDate = LocalDate.now()): TaskStatus {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            return status
        }

        val dueDateValue = runCatching {
            LocalDate.parse(dueDate.substringBefore("T"))
        }.getOrElse {
            runCatching { LocalDate.parse(dueDate.take(10)) }.getOrDefault(now)
        }

        val daysDifference = ChronoUnit.DAYS.between(now, dueDateValue)
        return when {
            daysDifference < 0 -> TaskStatus.OVERDUE
            daysDifference == 0L -> TaskStatus.DUE
            daysDifference in 1..maxOf(0, visibilityWindowDays) -> TaskStatus.DUE_SOON
            else -> TaskStatus.UPCOMING
        }
    }

    val actionableStatus: TaskStatus
        get() = displayStatus(visibilityWindowDays = 7)

    val isActionable: Boolean
        get() = actionableStatus == TaskStatus.UPCOMING || actionableStatus == TaskStatus.DUE || actionableStatus == TaskStatus.OVERDUE
}

// ---------------------------------------------------------------------------
// 9. إعدادات المهام (Task Settings)
// ---------------------------------------------------------------------------

@Serializable
data class TaskSettings(
    val id: String = "",
    @SerialName("provider_id") val providerId: String,
    @SerialName("first_task_enabled") val firstTaskEnabled: Boolean = false,
    @SerialName("first_task_amount") val firstTaskAmount: Double? = 0.0,
    @SerialName("recurring_task_enabled") val recurringTaskEnabled: Boolean = true,
    @SerialName("recurring_task_amount") val recurringTaskAmount: Double = 0.0,
    @SerialName("repeat_interval_days") val repeatIntervalDays: Int = 30,
    @SerialName("days_visible_before_due") val daysVisibleBeforeDue: Int = 7,
    @SerialName("manual_reschedule_enabled") val manualRescheduleEnabled: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val recurringCycleDays: Int get() = repeatIntervalDays
}

// ---------------------------------------------------------------------------
// 10. الإشعارات (Notifications)
// ---------------------------------------------------------------------------

@Serializable
data class AppNotification(
    val id: String,
    @SerialName("customer_id") val customerId: String? = null,
    val title: String,
    val message: String,
    @SerialName("notification_type") val type: String = "general",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null
)

// ---------------------------------------------------------------------------
// 11. سجل العمليات (Audit Logs)
// ---------------------------------------------------------------------------

@Serializable
data class AuditLog(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("action_type") val actionType: String,
    @SerialName("affected_record_id") val affectedRecord: String? = null,
    @SerialName("affected_table") val affectedTable: String? = null,
    @SerialName("details") val rawDetails: JsonElement? = null,
    @SerialName("old_data") val rawOldData: JsonElement? = null,
    @SerialName("new_data") val rawNewData: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val actorName: String? = null
) {
    val details: String? get() = when (val el = rawDetails) {
        null -> null
        is JsonPrimitive -> el.contentOrNull
        else -> el.toString()
    }
    val oldData: String? get() = when (val el = rawOldData) {
        null -> null
        is JsonPrimitive -> el.contentOrNull
        else -> el.toString()
    }
    val newData: String? get() = when (val el = rawNewData) {
        null -> null
        is JsonPrimitive -> el.contentOrNull
        else -> el.toString()
    }
    val recordId: String? get() = affectedRecord
    val previousData: String? get() = oldData
}

// ---------------------------------------------------------------------------
// 12. إعدادات النظام (System Settings)
// ---------------------------------------------------------------------------

@Serializable
data class SystemSettings(
    val id: Boolean = true,
    @SerialName("app_name") val appName: String = "AMAN",
    @SerialName("contact_data") val rawContactData: JsonElement? = null,
    @SerialName("terms_and_conditions") val termsAndConditions: String? = null,
    @SerialName("privacy_policy") val privacyPolicy: String? = null,
    @SerialName("renewal_threshold_days") val renewalThresholdDays: Int = 30,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val contactData: String?
        get() = when (val el = rawContactData) {
            null -> null
            is JsonPrimitive -> el.contentOrNull
            is JsonObject -> el["phone"]?.jsonPrimitive?.contentOrNull
                ?: el["email"]?.jsonPrimitive?.contentOrNull
                ?: el["info"]?.jsonPrimitive?.contentOrNull
                ?: el.toString()
            else -> el.toString()
        }
    val contactInfo: String? get() = contactData
    val renewalWarningDays: Int get() = renewalThresholdDays
}

// ---------------------------------------------------------------------------
// 13. نماذج الإدارة (Administration Models)
// ---------------------------------------------------------------------------

@Serializable
data class AdminDashboardMetrics(
    val customerCount: Int = 0,
    val phoneNumberCount: Int = 0,
    val pendingRequestsCount: Int = 0,
    val activeProtectionsCount: Int = 0,
    val expiredProtectionsCount: Int = 0,
    val upcomingTasksCount: Int = 0,
    val dueTasksCount: Int = 0,
    val overdueTasksCount: Int = 0,
    val completedTasksCount: Int = 0
)

@Serializable
data class CustomerDetails(
    val user: AppUser,
    val numbers: List<CustomerNumber> = emptyList(),
    val requests: List<ProtectionRequest> = emptyList(),
    val protections: List<Protection> = emptyList(),
    val tasks: List<PaymentTask> = emptyList()
)
