package com.aman.app.data.repository

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.*
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonPrimitive
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
    suspend fun getProtectionRequest(requestId: String): AmanResult<ProtectionRequest?>
    suspend fun approveProtectionRequest(requestId: String): AmanResult<Protection>
    suspend fun rejectProtectionRequest(requestId: String, reason: String): AmanResult<Unit>

    // 4. Protections
    suspend fun getAllProtections(): AmanResult<List<Protection>>

    // 5. Telecom Providers & Prefixes
    suspend fun getAllProviders(): AmanResult<List<TelecomProvider>>
    suspend fun createProvider(name: String, code: String, length: Int, order: Int): AmanResult<TelecomProvider>
    suspend fun updateProvider(provider: TelecomProvider): AmanResult<TelecomProvider>
    suspend fun disableProvider(providerId: String): AmanResult<Unit>
    suspend fun activateProvider(providerId: String): AmanResult<Unit>
    suspend fun setProviderVisibility(providerId: String, isVisible: Boolean): AmanResult<Unit>
    suspend fun getProviderPrefixes(providerId: String): AmanResult<List<TelecomPrefix>>
    suspend fun addProviderPrefix(providerId: String, prefix: String): AmanResult<TelecomPrefix>
    suspend fun updateProviderPrefix(prefixId: String, prefix: String, isActive: Boolean): AmanResult<TelecomPrefix>
    suspend fun setPrefixStatus(prefixId: String, isActive: Boolean): AmanResult<Unit>

    // 6. Protection Plans
    suspend fun getAllPlans(): AmanResult<List<ProtectionPlan>>
    suspend fun createPlan(providerId: String, name: String, price: Double, durationDays: Int): AmanResult<ProtectionPlan>
    suspend fun updatePlan(plan: ProtectionPlan): AmanResult<ProtectionPlan>
    suspend fun disablePlan(planId: String): AmanResult<Unit>
    suspend fun activatePlan(planId: String): AmanResult<Unit>
    suspend fun setPlanVisibility(planId: String, isVisible: Boolean): AmanResult<Unit>

    // 7. Payment Methods
    suspend fun getAllPaymentMethods(): AmanResult<List<PaymentMethod>>
    suspend fun createPaymentMethod(walletName: String, accountNumber: String, holderName: String, instructions: String?): AmanResult<PaymentMethod>
    suspend fun updatePaymentMethod(method: PaymentMethod): AmanResult<PaymentMethod>
    suspend fun disablePaymentMethod(methodId: String): AmanResult<Unit>
    suspend fun activatePaymentMethod(methodId: String): AmanResult<Unit>

    // 8. Payment Tasks
    suspend fun getAllTasks(status: TaskStatus? = null): AmanResult<List<PaymentTask>>
    suspend fun completePaymentTask(taskId: String): AmanResult<PaymentTask>
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
    suspend fun getAuditLog(logId: String): AmanResult<AuditLog?>

    // 12. Customer Numbers (Admin Management)
    suspend fun getAllCustomerNumbers(): AmanResult<List<CustomerNumber>>
    suspend fun getCustomerNumberDetails(numberId: String): AmanResult<CustomerNumberDetails>

    // 13. System Notifications (Admin Overview)
    suspend fun getAllNotifications(): AmanResult<List<AppNotification>>
    suspend fun getNotification(notificationId: String): AmanResult<AppNotification?>
    suspend fun markNotificationAsRead(notificationId: String): AmanResult<Unit>
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
                upcomingTasksCount = tasks.count { it.status == TaskStatus.UPCOMING || it.status == TaskStatus.PENDING || it.status == TaskStatus.DUE_SOON },
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

            val tasks = try {
                AmanSupabase.postgrest.from("payment_tasks")
                    .select { filter { eq("customer_id", customerId) } }
                    .decodeList<PaymentTask>()
            } catch (e: Exception) {
                emptyList()
            }

            AmanResult.Success(
                CustomerDetails(
                    user = user,
                    numbers = numbers,
                    requests = requests,
                    protections = protections,
                    tasks = tasks
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

    override suspend fun getProtectionRequest(requestId: String): AmanResult<ProtectionRequest?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val record = AmanSupabase.postgrest.from("protection_requests")
                .select {
                    filter { eq("id", requestId) }
                }.decodeSingleOrNull<ProtectionRequest>() ?: return AmanResult.Success(null)

            val number = try {
                AmanSupabase.postgrest.from("customer_numbers")
                    .select { filter { eq("id", record.customerNumberId) } }
                    .decodeSingleOrNull<CustomerNumber>()
            } catch (_: Exception) { null }

            val plan = try {
                AmanSupabase.postgrest.from("protection_plans")
                    .select { filter { eq("id", record.planId) } }
                    .decodeSingleOrNull<ProtectionPlan>()
            } catch (_: Exception) { null }

            val provider = try {
                AmanSupabase.postgrest.from("telecom_providers")
                    .select { filter { eq("id", record.providerId) } }
                    .decodeSingleOrNull<TelecomProvider>()
            } catch (_: Exception) { null }

            val paymentMethod = try {
                AmanSupabase.postgrest.from("payment_methods")
                    .select { filter { eq("id", record.paymentMethodId) } }
                    .decodeSingleOrNull<PaymentMethod>()
            } catch (_: Exception) { null }

            val customer = try {
                AmanSupabase.postgrest.from("users")
                    .select { filter { eq("id", record.customerId) } }
                    .decodeSingleOrNull<AppUser>()
            } catch (_: Exception) { null }

            AmanResult.Success(
                record.copy(
                    customerNumber = number,
                    plan = plan,
                    provider = provider,
                    paymentMethod = paymentMethod,
                    actorName = customer?.name ?: customer?.email ?: record.actorName
                )
            )
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل طلب الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun approveProtectionRequest(requestId: String): AmanResult<Protection> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            // Atomic Backend RPC executing transactions:
            // Locks request -> Validates pending -> Sets approved -> Creates protection -> Creates first payment task -> Notifies customer -> Audits
            val params = buildJsonObject {
                put("p_request_id", requestId)
            }
            val protection = AmanSupabase.postgrest.rpc(
                function = "approve_protection_request",
                parameters = params
            ).decodeAs<Protection>()

            AmanResult.Success(protection)
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
                put("p_reason", reason.trim())
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
            val prefixes = try {
                AmanSupabase.postgrest.from("telecom_prefixes")
                    .select { filter { eq("provider_id", provider.id) } }
                    .decodeList<TelecomPrefix>()
                    .map { it.prefix }
            } catch (_: Exception) {
                emptyList()
            }
            val params = buildJsonObject {
                put("p_provider_id", provider.id)
                put("p_name", provider.name.trim())
                put("p_code", provider.code.trim().uppercase())
                put("p_phone_length", provider.phoneLength)
                put("p_prefixes", buildJsonArray { prefixes.forEach { add(JsonPrimitive(it)) } })
                put("p_is_active", provider.isActive)
                put("p_is_visible_to_customers", provider.isVisibleToCustomer)
                put("p_display_order", provider.displayOrder)
            }
            val updated = AmanSupabase.postgrest.rpc(
                function = "admin_update_telecom_provider",
                parameters = params
            ).decodeAs<TelecomProvider>()
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

    override suspend fun activateProvider(providerId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_provider_id", providerId)
                put("p_is_active", true)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_telecom_provider_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تفعيل الشركة: ${e.message}", cause = e))
        }
    }

    override suspend fun setProviderVisibility(providerId: String, isVisible: Boolean): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            AmanSupabase.postgrest.from("telecom_providers").update(
                buildJsonObject {
                    put("is_visible_to_customers", isVisible)
                }
            ) {
                filter { eq("id", providerId) }
            }
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث ظهور الشركة: ${e.message}", cause = e))
        }
    }

    override suspend fun getProviderPrefixes(providerId: String): AmanResult<List<TelecomPrefix>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("telecom_prefixes")
                .select {
                    filter { eq("provider_id", providerId) }
                }.decodeList<TelecomPrefix>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب البادئات: ${e.message}", cause = e))
        }
    }

    override suspend fun addProviderPrefix(providerId: String, prefix: String): AmanResult<TelecomPrefix> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        if (prefix.isBlank()) return AmanResult.Error(AmanError.ValidationError("يجب إدخال البادئة"))
        return try {
            val res = AmanSupabase.postgrest.from("telecom_prefixes").insert(
                buildJsonObject {
                    put("provider_id", providerId)
                    put("prefix", prefix.trim())
                    put("is_active", true)
                }
            ) { select() }.decodeSingle<TelecomPrefix>()
            AmanResult.Success(res)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إضافة البادئة: ${e.message}", cause = e))
        }
    }

    override suspend fun updateProviderPrefix(prefixId: String, prefix: String, isActive: Boolean): AmanResult<TelecomPrefix> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val res = AmanSupabase.postgrest.from("telecom_prefixes").update(
                buildJsonObject {
                    put("prefix", prefix.trim())
                    put("is_active", isActive)
                }
            ) {
                filter { eq("id", prefixId) }
                select()
            }.decodeSingle<TelecomPrefix>()
            AmanResult.Success(res)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تعديل البادئة: ${e.message}", cause = e))
        }
    }

    override suspend fun setPrefixStatus(prefixId: String, isActive: Boolean): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            AmanSupabase.postgrest.from("telecom_prefixes").update(
                buildJsonObject {
                    put("is_active", isActive)
                }
            ) {
                filter { eq("id", prefixId) }
            }
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث حالة البادئة: ${e.message}", cause = e))
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
            val params = buildJsonObject {
                put("p_plan_id", plan.id)
                put("p_provider_id", plan.providerId)
                put("p_name", plan.name.trim())
                put("p_price", plan.price)
                put("p_duration_days", plan.durationDays)
                put("p_is_active", plan.isActive)
                put("p_is_visible_to_customers", plan.isVisibleToCustomer)
            }
            val updated = AmanSupabase.postgrest.rpc(
                function = "admin_update_protection_plan",
                parameters = params
            ).decodeAs<ProtectionPlan>()
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

    override suspend fun activatePlan(planId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_plan_id", planId)
                put("p_is_active", true)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_protection_plan_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تفعيل باقة الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun setPlanVisibility(planId: String, isVisible: Boolean): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            AmanSupabase.postgrest.from("protection_plans").update(
                buildJsonObject {
                    put("is_visible_to_customers", isVisible)
                }
            ) {
                filter { eq("id", planId) }
            }
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث ظهور الباقة: ${e.message}", cause = e))
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
            val params = buildJsonObject {
                put("p_payment_method_id", method.id)
                put("p_name", method.name.trim())
                put("p_account_number", method.accountNumber.trim())
                put("p_account_owner_name", method.accountOwnerName.trim())
                put("p_payment_instructions", method.paymentInstructions?.trim() ?: "")
                put("p_is_active", method.isActive)
            }
            val updated = AmanSupabase.postgrest.rpc(
                function = "admin_update_payment_method",
                parameters = params
            ).decodeAs<PaymentMethod>()
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

    override suspend fun activatePaymentMethod(methodId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_payment_method_id", methodId)
                put("p_is_active", true)
            }
            AmanSupabase.postgrest.rpc(
                function = "admin_set_payment_method_status",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تفعيل طريقة الدفع: ${e.message}", cause = e))
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

    override suspend fun completePaymentTask(taskId: String): AmanResult<PaymentTask> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_task_id", taskId)
            }
            val res = AmanSupabase.postgrest.rpc(
                function = "complete_payment_task",
                parameters = params
            ).decodeAs<PaymentTask>()

            AmanResult.Success(res)
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
            val rpcResult = try {
                val params = buildJsonObject {
                    put("p_provider_id", settings.providerId)
                    put("p_first_task_enabled", settings.firstTaskEnabled)
                    put("p_first_task_amount", settings.firstTaskAmount ?: 0.0)
                    put("p_recurring_task_enabled", settings.recurringTaskEnabled)
                    put("p_recurring_task_amount", settings.recurringTaskAmount)
                    put("p_repeat_interval_days", settings.repeatIntervalDays)
                    put("p_days_visible_before_due", settings.daysVisibleBeforeDue)
                    put("p_manual_reschedule_enabled", settings.manualRescheduleEnabled)
                }
                AmanSupabase.postgrest.rpc(
                    function = "admin_update_task_settings",
                    parameters = params
                ).decodeAs<TaskSettings>()
            } catch (_: Exception) {
                null
            }

            if (rpcResult != null) return AmanResult.Success(rpcResult)

            // Direct upsert into task_settings
            val updatePayload = buildJsonObject {
                if (settings.id.isNotBlank()) put("id", settings.id)
                put("provider_id", settings.providerId)
                put("first_task_enabled", settings.firstTaskEnabled)
                put("first_task_amount", settings.firstTaskAmount ?: 0.0)
                put("recurring_task_enabled", settings.recurringTaskEnabled)
                put("recurring_task_amount", settings.recurringTaskAmount)
                put("repeat_interval_days", settings.repeatIntervalDays)
                put("days_visible_before_due", settings.daysVisibleBeforeDue)
                put("manual_reschedule_enabled", settings.manualRescheduleEnabled)
            }

            val updated = AmanSupabase.postgrest.from("task_settings")
                .upsert(updatePayload) {
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
                .select().decodeSingleOrNull<SystemSettings>()
            if (settings != null) {
                AmanResult.Success(settings)
            } else {
                AmanResult.Error(AmanError.DatabaseError("لم يتم العثور على إعدادات النظام في قاعدة البيانات"))
            }
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
                .sortedByDescending { it.createdAt ?: "" }

            // Enrich with user name/email if actorId present
            val users = try {
                AmanSupabase.postgrest.from("users").select().decodeList<AppUser>()
            } catch (_: Exception) { emptyList() }
            val userMap = users.associateBy { it.id }

            val enriched = list.map { log ->
                val actorUser = log.actorId?.let { userMap[it] }
                log.copy(actorName = actorUser?.name ?: actorUser?.email)
            }

            AmanResult.Success(enriched)
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

    override suspend fun getCustomerNumberDetails(numberId: String): AmanResult<CustomerNumberDetails> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val number = AmanSupabase.postgrest.from("customer_numbers")
                .select { filter { eq("id", numberId) } }
                .decodeSingle<CustomerNumber>()

            val customer = try {
                AmanSupabase.postgrest.from("users")
                    .select { filter { eq("id", number.customerId) } }
                    .decodeSingle<AppUser>()
            } catch (_: Exception) {
                null
            }

            val provider = try {
                AmanSupabase.postgrest.from("telecom_providers")
                    .select { filter { eq("id", number.providerId) } }
                    .decodeSingle<TelecomProvider>()
            } catch (_: Exception) {
                null
            }

            val currentProtection = try {
                val protections = AmanSupabase.postgrest.from("protections")
                    .select { filter { eq("customer_number_id", numberId) } }
                    .decodeList<Protection>()
                protections.firstOrNull { it.status == StoredProtectionStatus.ACTIVE } ?: protections.firstOrNull()
            } catch (_: Exception) {
                null
            }

            val requests = try {
                AmanSupabase.postgrest.from("protection_requests")
                    .select { filter { eq("customer_number_id", numberId) } }
                    .decodeList<ProtectionRequest>()
                    .sortedByDescending { it.createdAt ?: "" }
            } catch (_: Exception) {
                emptyList()
            }

            val tasks = try {
                AmanSupabase.postgrest.from("payment_tasks")
                    .select { filter { eq("customer_number_id", numberId) } }
                    .decodeList<PaymentTask>()
                    .sortedByDescending { it.dueDate }
            } catch (_: Exception) {
                emptyList()
            }

            val enrichedNumber = number.copy(provider = provider)

            AmanResult.Success(
                CustomerNumberDetails(
                    number = enrichedNumber,
                    customer = customer,
                    provider = provider,
                    currentProtection = currentProtection,
                    requests = requests,
                    tasks = tasks
                )
            )
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل رقم العميل: ${e.message}", cause = e))
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
                .sortedByDescending { it.createdAt ?: "" }
            AmanResult.Success(notifications)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الإشعارات: ${e.message}", cause = e))
        }
    }

    override suspend fun getAuditLog(logId: String): AmanResult<AuditLog?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val log = AmanSupabase.postgrest.from("audit_logs")
                .select { filter { eq("id", logId) } }
                .decodeSingleOrNull<AuditLog>() ?: return AmanResult.Success(null)

            val actorUser = log.actorId?.let { id ->
                try {
                    AmanSupabase.postgrest.from("users")
                        .select { filter { eq("id", id) } }
                        .decodeSingleOrNull<AppUser>()
                } catch (_: Exception) { null }
            }

            AmanResult.Success(log.copy(actorName = actorUser?.name ?: actorUser?.email ?: log.actorName))
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل سجل التدقيق: ${e.message}", cause = e))
        }
    }

    override suspend fun getNotification(notificationId: String): AmanResult<AppNotification?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val n = AmanSupabase.postgrest.from("notifications")
                .select { filter { eq("id", notificationId) } }
                .decodeSingleOrNull<AppNotification>()
            AmanResult.Success(n)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل الإشعار: ${e.message}", cause = e))
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            try {
                val params = buildJsonObject {
                    put("p_notification_id", notificationId)
                }
                AmanSupabase.postgrest.rpc(
                    function = "mark_notification_read",
                    parameters = params
                )
            } catch (_: Exception) {
                AmanSupabase.postgrest.from("notifications").update(
                    buildJsonObject {
                        put("is_read", true)
                    }
                ) {
                    filter { eq("id", notificationId) }
                }
            }
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث الإشعار: ${e.message}", cause = e))
        }
    }
}
