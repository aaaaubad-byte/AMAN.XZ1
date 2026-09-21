package com.aman.app.data.repository

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.*
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

/**
 * Repository contracts and Supabase implementations for AMAN | أمان.
 * Strictly adheres to AMAN.XZ.txt:
 * - Real Supabase Postgrest queries
 * - No hardcoded mock return values
 * - Proper error handling via AmanResult
 */

// ---------------------------------------------------------------------------
// 1. أرقام العملاء (Customer Numbers)
// ---------------------------------------------------------------------------
interface CustomerNumberRepository {
    suspend fun getNumbersByCustomer(customerId: String): AmanResult<List<CustomerNumber>>
    suspend fun getAllNumbers(): AmanResult<List<CustomerNumber>>
    suspend fun addCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber>
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
        val currentUser = AmanSupabase.auth.currentUserOrNull()
            ?: return AmanResult.Error(AmanError.AuthenticationError("المستخدم غير مسجل الدخول"))

        return try {
            // Automatic prefix detection handled via RPC or backend trigger as required by AMAN.XZ.txt
            val created = AmanSupabase.postgrest.from("customer_numbers")
                .insert(
                    mapOf(
                        "customer_id" to currentUser.id,
                        "phone_number" to phoneNumber.trim()
                    )
                ) {
                    select()
                }.decodeSingle<CustomerNumber>()
            AmanResult.Success(created)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر إضافة الرقم: ${e.message}", cause = e))
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
                        eq("is_visible_to_customer", true)
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
                        eq("is_visible_to_customer", true)
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
    suspend fun getAllRequests(): AmanResult<List<ProtectionRequest>>
    suspend fun submitRequest(
        customerNumberId: String,
        planId: String,
        paymentMethodId: String,
        protectionValue: Double,
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

    override suspend fun getAllRequests(): AmanResult<List<ProtectionRequest>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("protection_requests")
                .select().decodeList<ProtectionRequest>()
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
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        val currentUser = AmanSupabase.auth.currentUserOrNull()
            ?: return AmanResult.Error(AmanError.AuthenticationError("المستخدم غير مسجل الدخول"))

        return try {
            val created = AmanSupabase.postgrest.from("protection_requests")
                .insert(
                    mapOf(
                        "customer_id" to currentUser.id,
                        "customer_number_id" to customerNumberId,
                        "plan_id" to planId,
                        "payment_method_id" to paymentMethodId,
                        "protection_value" to protectionValue,
                        "transfer_data" to transferData,
                        "status" to "pending"
                    )
                ) {
                    select()
                }.decodeSingle<ProtectionRequest>()
            AmanResult.Success(created)
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

    override suspend fun markAsRead(notificationId: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            AmanSupabase.postgrest.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter { eq("id", notificationId) }
            }
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر تحديث الإشعار: ${e.message}", cause = e))
        }
    }
}
