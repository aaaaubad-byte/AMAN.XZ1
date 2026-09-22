package com.aman.app.data.repository

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.*
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Dedicated Administration Repository Layer for AMAN | أمان (Stage 4).
 * Enforces backend-first security:
 * - Direct Supabase Auth & PostgreSQL operations
 * - Atomic approval & rejection via secure RPC functions
 * - Task completion, cancellation, and rescheduling via secure RPC functions
 * - Immutable historical snapshots & provider deactivation discipline
 */
interface AdminRepository {
    // 1. Dashboard
    suspend fun loadDashboard(): AmanResult<AdminDashboardMetrics>

    // 2. Customers
    suspend fun getCustomers(): AmanResult<List<AppUser>>
    suspend fun getCustomerDetails(customerId: String): AmanResult<CustomerDetails>

    // 3. Protection Requests
    suspend fun getProtectionRequests(status: ProtectionRequestStatus? = null): AmanResult<List<ProtectionRequest>>
    suspend fun approveProtectionRequest(requestId: String): AmanResult<String>
    suspend fun rejectProtectionRequest(requestId: String, reason: String): AmanResult<Unit>

    // 4. Protections
    suspend fun getAllProtections(): AmanResult<List<Protection>>

    // 5. Telecom Providers & Prefixes
    suspend fun getAllProviders(): AmanResult<List<TelecomProvider>>
    suspend fun createProvider(name: String, code: String, length: Int, order: Int): AmanResult<TelecomProvider>
    suspend fun updateProvider(provider: TelecomProvider): AmanResult<TelecomProvider>
    suspend fun disableProvider(providerId: String): AmanResult<Unit>

    // 6. Protection Plans
    suspend fun getAllPlans(): AmanResult<List<ProtectionPlan>>
    suspend fun createPlan(providerId: String, name: String, price: Double, durationDays: Int): AmanResult<ProtectionPlan>
    suspend fun updatePlan(plan: ProtectionPlan): AmanResult<ProtectionPlan>
    suspend fun disablePlan(planId: String): AmanResult<Unit>

    // 7. Payment Methods
    suspend fun getAllPaymentMethods(): AmanResult<List<PaymentMethod>>
    suspend fun createPaymentMethod(walletName: String, accountNumber: String, holderName: String, instructions: String?): AmanResult<PaymentMethod>
    suspend fun updatePaymentMethod(method: PaymentMethod): AmanResult<PaymentMethod>
    suspend fun disablePaymentMethod(methodId: String): AmanResult<Unit>

    // 8. Payment Tasks
    suspend fun getAllTasks(status: TaskStatus? = null): AmanResult<List<PaymentTask>>
    suspend fun completePaymentTask(taskId: String): AmanResult<String>
    suspend fun cancelPaymentTask(taskId: String, reason: String): AmanResult<Unit>
    suspend fun reschedulePaymentTask(taskId: String, newDueDate: String, reason: String): AmanResult<Unit>

    // 9. Task Settings
    suspend fun getTaskSettings(): AmanResult<List<TaskSettings>>
    suspend fun updateTaskSettings(settings: TaskSettings): AmanResult<TaskSettings>

    // 10. System Settings
    suspend fun getSystemSettings(): AmanResult<SystemSettings>
    suspend fun updateSystemSettings(settings: SystemSettings): AmanResult<SystemSettings>

    // 11. Audit Logs
    suspend fun getAuditLogs(): AmanResult<List<AuditLog>>

    // 12. Customer Numbers (Admin Management)
    suspend fun getAllCustomerNumbers(): AmanResult<List<CustomerNumber>>

    // 13. System Notifications (Admin Overview)
    suspend fun getAllNotifications(): AmanResult<List<AppNotification>>
}

class AdminRepositoryImpl : AdminRepository {

    // -----------------------------------------------------------------------
    // 1. لوحة الإدارة الرئيسية (Dashboard Metrics)
    // -----------------------------------------------------------------------
    override suspend fun loadDashboard(): AmanResult<AdminDashboardMetrics> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val users = AmanSupabase.postgrest.from("users")
                .select { filter { eq("role", "customer") } }
                .decodeList<AppUser>()

            val numbers = AmanSupabase.postgrest.from("customer_numbers")
                .select().decodeList<CustomerNumber>()

            val requests = AmanSupabase.postgrest.from("protection_requests")
                .select().decodeList<ProtectionRequest>()

            val protections = AmanSupabase.postgrest.from("protections")
                .select().decodeList<Protection>()

            val tasks = AmanSupabase.postgrest.from("payment_tasks")
                .select().decodeList<PaymentTask>()

            val metrics = AdminDashboardMetrics(
                customerCount = users.size,
                phoneNumberCount = numbers.size,
                pendingRequestsCount = requests.count { it.status == ProtectionRequestStatus.PENDING },
                activeProtectionsCount = protections.count { it.status == StoredProtectionStatus.ACTIVE },
                expiredProtectionsCount = protections.count { it.status == StoredProtectionStatus.EXPIRED },
                upcomingTasksCount = tasks.count { it.status == TaskStatus.UPCOMING },
                dueTasksCount = tasks.count { it.status == TaskStatus.DUE },
                overdueTasksCount = tasks.count { it.status == TaskStatus.OVERDUE },
                completedTasksCount = tasks.count { it.status == TaskStatus.COMPLETED }
            )
            AmanResult.Success(metrics)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحميل بيانات لوحة الإدارة: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 2. إدارة العملاء (Customers)
    // -----------------------------------------------------------------------
    override suspend fun getCustomers(): AmanResult<List<AppUser>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("users")
                .select {
                    filter { eq("role", "customer") }
                }.decodeList<AppUser>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب قائمة العملاء: ${e.message}", cause = e))
        }
    }

    override suspend fun getCustomerDetails(customerId: String): AmanResult<CustomerDetails> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val user = AmanSupabase.postgrest.from("users")
                .select { filter { eq("id", customerId) } }
                .decodeSingle<AppUser>()

            val numbers = AmanSupabase.postgrest.from("customer_numbers")
                .select { filter { eq("customer_id", customerId) } }
                .decodeList<CustomerNumber>()

            val requests = AmanSupabase.postgrest.from("protection_requests")
                .select { filter { eq("customer_id", customerId) } }
                .decodeList<ProtectionRequest>()

            val protections = AmanSupabase.postgrest.from("protections")
                .select { filter { eq("customer_id", customerId) } }
                .decodeList<Protection>()

            AmanResult.Success(
                CustomerDetails(
                    user = user,
                    numbers = numbers,
                    requests = requests,
                    protections = protections
                )
            )
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل العميل: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 3. طلبات الحماية (Protection Requests & Atomic RPC Approval/Rejection)
    // -----------------------------------------------------------------------
    override suspend fun getProtectionRequests(status: ProtectionRequestStatus?): AmanResult<List<ProtectionRequest>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_requests")
                .select {
                    if (status != null) {
                        filter { eq("status", status.name.lowercase()) }
                    }
                }.decodeList<ProtectionRequest>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب طلبات الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun approveProtectionRequest(requestId: String): AmanResult<String> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            // Atomic Backend RPC executing transactions:
            // Locks request -> Validates pending -> Sets approved -> Creates protection -> Creates first payment task -> Notifies customer -> Audits
            val params = buildJsonObject {
                put("p_request_id", requestId)
            }
            val protectionId = AmanSupabase.postgrest.rpc(
                function = "approve_protection_request",
                parameters = params
            ).decodeAs<String>()

            AmanResult.Success(protectionId)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("فشل اعتماد طلب الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun rejectProtectionRequest(requestId: String, reason: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        if (reason.isBlank()) return AmanResult.Error(AmanError.ValidationError("يجب كتابة سبب الرفض"))
        return try {
            val params = buildJsonObject {
                put("p_request_id", requestId)
                put("p_rejection_reason", reason.trim())
            }
            AmanSupabase.postgrest.rpc(
                function = "reject_protection_request",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("فشل رفض طلب الحماية: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 4. الحمايات (Protections)
    // -----------------------------------------------------------------------
    override suspend fun getAllProtections(): AmanResult<List<Protection>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protections")
                .select().decodeList<Protection>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الحمايات: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 5. شركات الاتصالات (Telecom Providers)
    // -----------------------------------------------------------------------
    override suspend fun getAllProviders(): AmanResult<List<TelecomProvider>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("telecom_providers")
                .select().decodeList<TelecomProvider>()
            AmanResult.Success(list.sortedBy { it.displayOrder })
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب شركات الاتصالات: ${e.message}", cause = e))
        }
    }

    override suspend fun createProvider(name: String, code: String, length: Int, order: Int): AmanResult<TelecomProvider> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_name", name.trim())
                put("p_code", code.trim().uppercase())
                put("p_phone_length", length)
                put("p_is_active", true)
                put("p_is_visible_to_customers", true)
                put("p_display_order", order)
            }
            val provider = AmanSupabase.postgrest.rpc(
                function = "admin_create_telecom_provider",
                parameters = params
            ).decodeAs<TelecomProvider>()
            AmanResult.Success(provider)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إنشاء شركة الاتصالات: ${e.message}", cause = e))
        }
    }

    override suspend fun updateProvider(provider: TelecomProvider): AmanResult<TelecomProvider> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val updated = AmanSupabase.postgrest.from("telecom_providers")
                .update(
                    mapOf(
                        "name" to provider.name,
                        "code" to provider.code,
                        "number_length" to provider.numberLength,
                        "display_order" to provider.displayOrder,
                        "is_active" to provider.isActive,
                        "is_visible_to_customer" to provider.isVisibleToCustomer
                    )
                ) {
                    filter { eq("id", provider.id) }
                    select()
                }.decodeSingle<TelecomProvider>()
            AmanResult.Success(updated)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث شركة الاتصالات: ${e.message}", cause = e))
        }
    }

    override suspend fun disableProvider(providerId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_provider_id", providerId)
                put("p_is_active", false)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_telecom_provider_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إلغاء تفعيل الشركة: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 6. باقات الحماية (Protection Plans)
    // -----------------------------------------------------------------------
    override suspend fun getAllPlans(): AmanResult<List<ProtectionPlan>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_plans")
                .select().decodeList<ProtectionPlan>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب باقات الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun createPlan(providerId: String, name: String, price: Double, durationDays: Int): AmanResult<ProtectionPlan> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_provider_id", providerId)
                put("p_name", name.trim())
                put("p_price", price)
                put("p_duration_days", durationDays)
                put("p_is_active", true)
                put("p_is_visible_to_customers", true)
            }
            val plan = AmanSupabase.postgrest.rpc(
                function = "admin_create_protection_plan",
                parameters = params
            ).decodeAs<ProtectionPlan>()
            AmanResult.Success(plan)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إضافة باقة الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun updatePlan(plan: ProtectionPlan): AmanResult<ProtectionPlan> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val updated = AmanSupabase.postgrest.from("protection_plans")
                .update(
                    mapOf(
                        "name" to plan.name,
                        "price" to plan.price,
                        "duration_days" to plan.durationDays,
                        "is_active" to plan.isActive,
                        "is_visible_to_customer" to plan.isVisibleToCustomer
                    )
                ) {
                    filter { eq("id", plan.id) }
                    select()
                }.decodeSingle<ProtectionPlan>()
            AmanResult.Success(updated)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث باقة الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun disablePlan(planId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_plan_id", planId)
                put("p_is_active", false)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_protection_plan_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تعطيل الباقة: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 7. طرق الدفع (Payment Methods)
    // -----------------------------------------------------------------------
    override suspend fun getAllPaymentMethods(): AmanResult<List<PaymentMethod>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("payment_methods")
                .select().decodeList<PaymentMethod>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب طرق الدفع: ${e.message}", cause = e))
        }
    }

    override suspend fun createPaymentMethod(
        walletName: String,
        accountNumber: String,
        holderName: String,
        instructions: String?
    ): AmanResult<PaymentMethod> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_name", walletName.trim())
                put("p_account_number", accountNumber.trim())
                put("p_account_owner_name", holderName.trim())
                put("p_payment_instructions", instructions?.trim() ?: "")
                put("p_is_active", true)
            }
            val method = AmanSupabase.postgrest.rpc(
                function = "admin_create_payment_method",
                parameters = params
            ).decodeAs<PaymentMethod>()
            AmanResult.Success(method)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إضافة طريقة الدفع: ${e.message}", cause = e))
        }
    }

    override suspend fun updatePaymentMethod(method: PaymentMethod): AmanResult<PaymentMethod> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val updated = AmanSupabase.postgrest.from("payment_methods")
                .update(
                    mapOf(
                        "wallet_name" to method.walletName,
                        "account_number" to method.accountNumber,
                        "account_holder_name" to method.accountHolderName,
                        "payment_instructions" to method.paymentInstructions,
                        "is_active" to method.isActive
                    )
                ) {
                    filter { eq("id", method.id) }
                    select()
                }.decodeSingle<PaymentMethod>()
            AmanResult.Success(updated)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث طريقة الدفع: ${e.message}", cause = e))
        }
    }

    override suspend fun disablePaymentMethod(methodId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_payment_method_id", methodId)
                put("p_is_active", false)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_payment_method_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تعطيل طريقة الدفع: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 8. مهام الدفع (Payment Tasks - Secure RPC Operations)
    // -----------------------------------------------------------------------
    override suspend fun getAllTasks(status: TaskStatus?): AmanResult<List<PaymentTask>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("payment_tasks")
                .select {
                    if (status != null) {
                        filter { eq("status", status.name.lowercase()) }
                    }
                }.decodeList<PaymentTask>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب مهام الدفع: ${e.message}", cause = e))
        }
    }

    override suspend fun completePaymentTask(taskId: String): AmanResult<String> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_task_id", taskId)
            }
            val nextTaskId = AmanSupabase.postgrest.rpc(
                function = "complete_payment_task",
                parameters = params
            ).decodeAsOrNull<String>() ?: ""
            AmanResult.Success(nextTaskId)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("فشل إكمال المهمة: ${e.message}", cause = e))
        }
    }

    override suspend fun cancelPaymentTask(taskId: String, reason: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_task_id", taskId)
                put("p_reason", reason.trim())
            }
            AmanSupabase.postgrest.rpc(
                function = "cancel_payment_task",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("فشل إلغاء المهمة: ${e.message}", cause = e))
        }
    }

    override suspend fun reschedulePaymentTask(taskId: String, newDueDate: String, reason: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_task_id", taskId)
                put("p_new_due_date", newDueDate)
                put("p_reason", reason.trim())
            }
            AmanSupabase.postgrest.rpc(
                function = "reschedule_payment_task",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("فشل إعادة جدولة المهمة: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 9. إعدادات المهام (Task Settings)
    // -----------------------------------------------------------------------
    override suspend fun getTaskSettings(): AmanResult<List<TaskSettings>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("task_settings")
                .select().decodeList<TaskSettings>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب إعدادات المهام: ${e.message}", cause = e))
        }
    }

    override suspend fun updateTaskSettings(settings: TaskSettings): AmanResult<TaskSettings> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_provider_id", settings.providerId)
                put("p_first_task_enabled", settings.firstTaskEnabled)
                put("p_first_task_amount", settings.firstTaskAmount ?: 0.0)
                put("p_recurring_task_enabled", settings.recurringTaskEnabled)
                put("p_recurring_task_amount", settings.recurringTaskAmount)
                put("p_repeat_interval_days", settings.recurringCycleDays)
                put("p_days_visible_before_due", settings.daysVisibleBeforeDue)
                put("p_manual_reschedule_enabled", settings.manualRescheduleEnabled)
            }
            val updated = AmanSupabase.postgrest.rpc(
                function = "admin_update_task_settings",
                parameters = params
            ).decodeAs<TaskSettings>()
            AmanResult.Success(updated)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث إعدادات المهام: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 10. إعدادات النظام (System Settings)
    // -----------------------------------------------------------------------
    override suspend fun getSystemSettings(): AmanResult<SystemSettings> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val settings = AmanSupabase.postgrest.from("system_settings")
                .select().decodeSingleOrNull<SystemSettings>() ?: SystemSettings()
            AmanResult.Success(settings)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحميل إعدادات النظام: ${e.message}", cause = e))
        }
    }

    override suspend fun updateSystemSettings(settings: SystemSettings): AmanResult<SystemSettings> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val contactJson = buildJsonObject {
                put("info", settings.contactData ?: settings.contactInfo ?: "")
            }
            val params = buildJsonObject {
                put("p_app_name", settings.appName)
                put("p_contact_data", contactJson)
                put("p_terms_and_conditions", settings.termsAndConditions)
                put("p_privacy_policy", settings.privacyPolicy)
                put("p_renewal_threshold_days", settings.renewalThresholdDays)
            }
            val updated = AmanSupabase.postgrest.rpc(
                function = "admin_update_system_settings",
                parameters = params
            ).decodeAs<SystemSettings>()
            AmanResult.Success(updated)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر حفظ إعدادات النظام: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 11. سجل العمليات والتدقيق (Audit Logs - Read Only)
    // -----------------------------------------------------------------------
    override suspend fun getAuditLogs(): AmanResult<List<AuditLog>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("audit_logs")
                .select().decodeList<AuditLog>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب سجل التدقيق: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 12. أرقام العملاء (Customer Numbers Overview)
    // -----------------------------------------------------------------------
    override suspend fun getAllCustomerNumbers(): AmanResult<List<CustomerNumber>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val numbers = AmanSupabase.postgrest.from("customer_numbers")
                .select()
                .decodeList<CustomerNumber>()
            AmanResult.Success(numbers)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب أرقام العملاء: ${e.message}", cause = e))
        }
    }

    // -----------------------------------------------------------------------
    // 13. الإشعارات (All Notifications Overview)
    // -----------------------------------------------------------------------
    override suspend fun getAllNotifications(): AmanResult<List<AppNotification>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val notifications = AmanSupabase.postgrest.from("notifications")
                .select()
                .decodeList<AppNotification>()
            AmanResult.Success(notifications)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الإشعارات: ${e.message}", cause = e))
        }
    }
}
