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
                .select { filter { eq("role", "client") } }
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
                    filter { eq("role", "client") }
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
            val provider = AmanSupabase.postgrest.from("telecom_providers")
                .insert(
                    mapOf(
                        "name" to name.trim(),
                        "code" to code.trim().uppercase(),
                        "number_length" to length,
                        "display_order" to order,
                        "is_active" to true,
                        "is_visible_to_customer" to true
                    )
                ) { select() }.decodeSingle<TelecomProvider>()
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
            // Respect historical references: deactivate instead of deleting
            AmanSupabase.postgrest.from("telecom_providers")
                .update(mapOf("is_active" to false, "is_visible_to_customer" to false)) {
                    filter { eq("id", providerId) }
                }
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
            val plan = AmanSupabase.postgrest.from("protection_plans")
                .insert(
                    mapOf(
                        "provider_id" to providerId,
                        "name" to name.trim(),
                        "price" to price,
                        "duration_days" to durationDays,
                        "is_active" to true,
                        "is_visible_to_customer" to true
                    )
                ) { select() }.decodeSingle<ProtectionPlan>()
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
            // Snapshot integrity preserved: historical protections are untouched
            AmanSupabase.postgrest.from("protection_plans")
                .update(mapOf("is_active" to false, "is_visible_to_customer" to false)) {
                    filter { eq("id", planId) }
                }
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
            val method = AmanSupabase.postgrest.from("payment_methods")
                .insert(
                    mapOf(
                        "wallet_name" to walletName.trim(),
                        "account_number" to accountNumber.trim(),
                        "account_holder_name" to holderName.trim(),
                        "payment_instructions" to (instructions?.trim() ?: ""),
                        "is_active" to true
                    )
                ) { select() }.decodeSingle<PaymentMethod>()
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
            AmanSupabase.postgrest.from("payment_methods")
                .update(mapOf("is_active" to false)) {
                    filter { eq("id", methodId) }
                }
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
            val updated = AmanSupabase.postgrest.from("task_settings")
                .update(
                    mapOf(
                        "first_task_enabled" to settings.firstTaskEnabled,
                        "first_task_amount" to settings.firstTaskAmount,
                        "recurring_task_enabled" to settings.recurringTaskEnabled,
                        "recurring_task_amount" to settings.recurringTaskAmount,
                        "repeat_interval_days" to settings.recurringCycleDays,
                        "visibility_days_before_due" to settings.daysVisibleBeforeDue,
                        "manual_reschedule_enabled" to settings.manualRescheduleEnabled
                    )
                ) {
                    filter { eq("id", settings.id) }
                    select()
                }.decodeSingle<TaskSettings>()
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
            val updated = AmanSupabase.postgrest.from("system_settings")
                .update(
                    mapOf(
                        "app_name" to settings.appName,
                        "support_contact" to settings.contactInfo,
                        "terms_conditions" to settings.termsAndConditions,
                        "privacy_policy" to settings.privacyPolicy,
                        "renewal_warning_days" to settings.renewalWarningDays
                    )
                ) {
                    filter { eq("id", settings.id) }
                    select()
                }.decodeSingle<SystemSettings>()
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
