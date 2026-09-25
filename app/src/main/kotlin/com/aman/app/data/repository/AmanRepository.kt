package com.aman.app.data.repository

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.*
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository contracts and Supabase implementations for AMAN | أمان.
 * Strictly adheres to V7 database schema:
 * - Real Supabase Postgrest queries & RPCs
 * - RPC add_customer_number
 * - RPC create_protection_request
 * - RPC mark_notification_read
 * - Notification update consistency (is_read + read_at)
 * - Error handling via AmanResult
 */

// ---------------------------------------------------------------------------
// 1. أرقام العملاء (Customer Numbers)
// ---------------------------------------------------------------------------

interface CustomerNumberRepository {
    suspend fun getNumbersByCustomer(customerId: String): AmanResult<List<CustomerNumber>>
    suspend fun getAllNumbers(): AmanResult<List<CustomerNumber>>
    suspend fun addCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber>
    suspend fun registerCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber>
    suspend fun detectProviderFromPrefix(prefix: String): AmanResult<TelecomProvider?>
    suspend fun getNumberDetails(numberId: String): AmanResult<CustomerNumber?>
}

class CustomerNumberRepositoryImpl : CustomerNumberRepository {

    override suspend fun getNumbersByCustomer(customerId: String): AmanResult<List<CustomerNumber>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("customer_numbers")
                .select {
                    filter { eq("customer_id", customerId) }
                }.decodeList<CustomerNumber>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب أرقام العميل: ${e.message}", cause = e))
        }
    }

    override suspend fun getAllNumbers(): AmanResult<List<CustomerNumber>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("customer_numbers")
                .select().decodeList<CustomerNumber>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الأرقام: ${e.message}", cause = e))
        }
    }

    override suspend fun addCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_phone_number", phoneNumber.trim())
            }
            val record = AmanSupabase.postgrest.rpc(
                function = "add_customer_number",
                parameters = params
            ).decodeAs<CustomerNumber>()
            AmanResult.Success(record)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تسجيل الرقم: ${e.message}", cause = e))
        }
    }

    override suspend fun registerCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber> {
        return addCustomerNumber(phoneNumber)
    }

    override suspend fun detectProviderFromPrefix(prefix: String): AmanResult<TelecomProvider?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_phone_number", prefix.trim())
            }
            val providerId = AmanSupabase.postgrest.rpc(
                function = "detect_provider_for_number",
                parameters = params
            ).decodeAsOrNull<String>()

            if (providerId.isNullOrBlank()) {
                AmanResult.Success(null)
            } else {
                val provider = AmanSupabase.postgrest.from("telecom_providers")
                    .select {
                        filter { eq("id", providerId) }
                    }.decodeSingleOrNull<TelecomProvider>()
                AmanResult.Success(provider)
            }
        } catch (e: Exception) {
            // If prefix is incomplete, treat gracefully
            AmanResult.Success(null)
        }
    }

    override suspend fun getNumberDetails(numberId: String): AmanResult<CustomerNumber?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val record = AmanSupabase.postgrest.from("customer_numbers")
                .select {
                    filter { eq("id", numberId) }
                }.decodeSingleOrNull<CustomerNumber>()
            AmanResult.Success(record)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل الرقم: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 2. شركات الاتصالات والبادئات (Telecom Providers & Prefixes)
// ---------------------------------------------------------------------------

interface TelecomProviderRepository {
    suspend fun getActiveProviders(): AmanResult<List<TelecomProvider>>
    suspend fun getAllProviders(): AmanResult<List<TelecomProvider>>
    suspend fun getProviderPrefixes(providerId: String): AmanResult<List<TelecomPrefix>>
}

class TelecomProviderRepositoryImpl : TelecomProviderRepository {

    override suspend fun getActiveProviders(): AmanResult<List<TelecomProvider>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("telecom_providers")
                .select {
                    filter {
                        eq("is_active", true)
                        eq("is_visible_to_customers", true)
                    }
                }.decodeList<TelecomProvider>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب شركات الاتصالات: ${e.message}", cause = e))
        }
    }

    override suspend fun getAllProviders(): AmanResult<List<TelecomProvider>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("telecom_providers")
                .select().decodeList<TelecomProvider>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب شركات الاتصالات: ${e.message}", cause = e))
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
}

// ---------------------------------------------------------------------------
// 3. باقات الحماية (Protection Plans)
// ---------------------------------------------------------------------------

interface ProtectionPlanRepository {
    suspend fun getPlansByProvider(providerId: String): AmanResult<List<ProtectionPlan>>
    suspend fun getAllPlans(): AmanResult<List<ProtectionPlan>>
}

class ProtectionPlanRepositoryImpl : ProtectionPlanRepository {

    override suspend fun getPlansByProvider(providerId: String): AmanResult<List<ProtectionPlan>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_plans")
                .select {
                    filter {
                        eq("provider_id", providerId)
                        eq("is_active", true)
                        eq("is_visible_to_customers", true)
                    }
                }.decodeList<ProtectionPlan>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب باقات الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun getAllPlans(): AmanResult<List<ProtectionPlan>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_plans")
                .select().decodeList<ProtectionPlan>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الباقات: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 4. طرق الدفع (Payment Methods)
// ---------------------------------------------------------------------------

interface PaymentMethodRepository {
    suspend fun getActivePaymentMethods(): AmanResult<List<PaymentMethod>>
}

class PaymentMethodRepositoryImpl : PaymentMethodRepository {

    override suspend fun getActivePaymentMethods(): AmanResult<List<PaymentMethod>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("payment_methods")
                .select {
                    filter { eq("is_active", true) }
                }.decodeList<PaymentMethod>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب طرق الدفع: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 5. طلبات الحماية (Protection Requests)
// ---------------------------------------------------------------------------

interface ProtectionRequestRepository {
    suspend fun getCustomerRequests(customerId: String): AmanResult<List<ProtectionRequest>>
    suspend fun submitRequest(
        customerNumberId: String,
        planId: String,
        paymentMethodId: String,
        protectionValue: Double,
        transferData: String
    ): AmanResult<ProtectionRequest>

    suspend fun submitProtectionRequest(
        customerNumberId: String,
        planId: String,
        paymentMethodId: String,
        transferData: String
    ): AmanResult<ProtectionRequest>
}

class ProtectionRequestRepositoryImpl : ProtectionRequestRepository {

    override suspend fun getCustomerRequests(customerId: String): AmanResult<List<ProtectionRequest>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_requests")
                .select {
                    filter { eq("customer_id", customerId) }
                }.decodeList<ProtectionRequest>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب طلبات الحماية: ${e.message}", cause = e))
        }
    }

    override suspend fun submitRequest(
        customerNumberId: String,
        planId: String,
        paymentMethodId: String,
        protectionValue: Double,
        transferData: String
    ): AmanResult<ProtectionRequest> {
        return submitProtectionRequest(customerNumberId, planId, paymentMethodId, transferData)
    }

    override suspend fun submitProtectionRequest(
        customerNumberId: String,
        planId: String,
        paymentMethodId: String,
        transferData: String
    ): AmanResult<ProtectionRequest> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val transferDataObj = buildJsonObject {
                put("reference", transferData.trim())
                put("note", transferData.trim())
            }
            val params = buildJsonObject {
                put("p_customer_number_id", customerNumberId)
                put("p_plan_id", planId)
                put("p_payment_method_id", paymentMethodId)
                put("p_transfer_data", transferDataObj)
            }
            val record = AmanSupabase.postgrest.rpc(
                function = "create_protection_request",
                parameters = params
            ).decodeAs<ProtectionRequest>()
            AmanResult.Success(record)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إرسال طلب الحماية: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 6. الحمايات (Protections)
// ---------------------------------------------------------------------------

interface ProtectionRepository {
    suspend fun getCustomerProtections(customerId: String): AmanResult<List<Protection>>
    suspend fun getAllProtections(): AmanResult<List<Protection>>
}

class ProtectionRepositoryImpl : ProtectionRepository {

    override suspend fun getCustomerProtections(customerId: String): AmanResult<List<Protection>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protections")
                .select {
                    filter { eq("customer_id", customerId) }
                }.decodeList<Protection>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الحمايات: ${e.message}", cause = e))
        }
    }

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
}

// ---------------------------------------------------------------------------
// 7. المهام (Payment Tasks - للإدارة فقط)
// ---------------------------------------------------------------------------

interface PaymentTaskRepository {
    suspend fun getAllTasks(): AmanResult<List<PaymentTask>>
    suspend fun getTasksByStatus(status: TaskStatus): AmanResult<List<PaymentTask>>
}

class PaymentTaskRepositoryImpl : PaymentTaskRepository {

    override suspend fun getAllTasks(): AmanResult<List<PaymentTask>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("payment_tasks")
                .select().decodeList<PaymentTask>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب المهام: ${e.message}", cause = e))
        }
    }

    override suspend fun getTasksByStatus(status: TaskStatus): AmanResult<List<PaymentTask>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("payment_tasks")
                .select {
                    filter { eq("status", status.name.lowercase()) }
                }.decodeList<PaymentTask>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب المهام: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 8. الإشعارات (Notifications)
// ---------------------------------------------------------------------------

interface NotificationRepository {
    suspend fun getCustomerNotifications(customerId: String): AmanResult<List<AppNotification>>
    suspend fun getUnreadCount(customerId: String): AmanResult<Int>
    suspend fun markAsRead(notificationId: String): AmanResult<Unit>
}

class NotificationRepositoryImpl : NotificationRepository {

    override suspend fun getCustomerNotifications(customerId: String): AmanResult<List<AppNotification>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("notifications")
                .select {
                    filter { eq("customer_id", customerId) }
                }.decodeList<AppNotification>()
            AmanResult.Success(list)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الإشعارات: ${e.message}", cause = e))
        }
    }

    override suspend fun getUnreadCount(customerId: String): AmanResult<Int> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Success(0)
        return try {
            val list = AmanSupabase.postgrest.from("notifications")
                .select {
                    filter {
                        eq("customer_id", customerId)
                        eq("is_read", false)
                    }
                }.decodeList<AppNotification>()
            AmanResult.Success(list.size)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب عدد الإشعارات: ${e.message}", cause = e))
        }
    }

    override suspend fun markAsRead(notificationId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val params = buildJsonObject {
                put("p_notification_id", notificationId)
            }
            AmanSupabase.postgrest.rpc(
                function = "mark_notification_read",
                parameters = params
            )
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث الإشعار: ${e.message}", cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 9. إعدادات النظام ومعلومات التطبيق (System Settings)
// ---------------------------------------------------------------------------

interface SystemSettingsRepository {
    suspend fun getSettings(): AmanResult<SystemSettings>
}

class SystemSettingsRepositoryImpl : SystemSettingsRepository {

    override suspend fun getSettings(): AmanResult<SystemSettings> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Error(AmanError.ConfigurationError("بيانات اتصال Supabase غير مهيأة بعد"))
        }
        return try {
            val settings = AmanSupabase.postgrest.from("system_settings")
                .select()
                .decodeSingleOrNull<SystemSettings>() ?: SystemSettings()
            AmanResult.Success(settings)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب إعدادات النظام: ${e.message}", cause = e))
        }
    }
}
