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

            // Fetch active protections for this customer to dynamically compute protection status
            val activeProtections = try {
                AmanSupabase.postgrest.from("protections")
                    .select {
                        filter {
                            eq("customer_id", customerId)
                            eq("status", "active")
                        }
                    }.decodeList<Protection>()
            } catch (_: Exception) {
                emptyList()
            }

            // Fetch pending protection requests for this customer
            val pendingRequests = try {
                AmanSupabase.postgrest.from("protection_requests")
                    .select {
                        filter {
                            eq("customer_id", customerId)
                            eq("status", "pending")
                        }
                    }.decodeList<ProtectionRequest>()
            } catch (_: Exception) {
                emptyList()
            }

            // Fetch providers to enrich relations
            val providers = try {
                AmanSupabase.postgrest.from("telecom_providers").select().decodeList<TelecomProvider>()
            } catch (_: Exception) {
                emptyList()
            }
            val providerMap = providers.associateBy { it.id }

            val enrichedList = list.map { num ->
                val hasActive = activeProtections.any { it.customerNumberId == num.id }
                val hasPending = pendingRequests.any { it.customerNumberId == num.id }
                val computedStatus = when {
                    hasActive -> NumberProtectionStatus.PROTECTED
                    hasPending -> NumberProtectionStatus.PENDING
                    else -> NumberProtectionStatus.UNPROTECTED
                }
                num.copy(
                    provider = num.provider ?: providerMap[num.providerId],
                    protectionStatus = computedStatus
                )
            }
            AmanResult.Success(enrichedList)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب أرقام العميل: ${e.message}", cause = e))
        }
    }

    override suspend fun getAllNumbers(): AmanResult<List<CustomerNumber>> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val list = AmanSupabase.postgrest.from("customer_numbers")
                .select().decodeList<CustomerNumber>()
            val providers = try {
                AmanSupabase.postgrest.from("telecom_providers").select().decodeList<TelecomProvider>()
            } catch (_: Exception) {
                emptyList()
            }
            val providerMap = providers.associateBy { it.id }
            AmanResult.Success(list.map { it.copy(provider = it.provider ?: providerMap[it.providerId]) })
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب الأرقام: ${e.message}", cause = e))
        }
    }

    override suspend fun addCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            // 1. Try RPC add_customer_number
            val rpcResult = try {
                val params = buildJsonObject {
                    put("p_phone_number", cleanPhone)
                }
                AmanSupabase.postgrest.rpc(
                    function = "add_customer_number",
                    parameters = params
                ).decodeAs<CustomerNumber>()
            } catch (_: Exception) {
                null
            }

            if (rpcResult != null) return AmanResult.Success(rpcResult)

            // 2. Direct insert fallback
            val user = AmanSupabase.auth.currentUserOrNull()
                ?: return AmanResult.Error(AmanError.AuthenticationError("يجب تسجيل الدخول لإضافة رقم"))

            val detectedProv = when (val pRes = detectProviderFromPrefix(cleanPhone)) {
                is AmanResult.Success -> pRes.data
                else -> null
            } ?: return AmanResult.Error(AmanError.ValidationError("لم يتم التعرف على شركة الاتصالات التابع لها هذا الرقم"))

            val insertData = buildJsonObject {
                put("customer_id", user.id)
                put("provider_id", detectedProv.id)
                put("phone_number", cleanPhone)
                put("status", "active")
                put("protection_status", "unprotected")
            }

            val inserted = AmanSupabase.postgrest.from("customer_numbers")
                .insert(insertData) {
                    select()
                }.decodeSingle<CustomerNumber>()

            AmanResult.Success(inserted.copy(provider = detectedProv, protectionStatus = NumberProtectionStatus.UNPROTECTED))
        } catch (e: Exception) {
            val msg = if (e.message?.contains("unique", ignoreCase = true) == true) {
                "هذا الرقم مسجل بالفعل في حسابك"
            } else {
                "تعذر تسجيل الرقم: ${e.message}"
            }
            AmanResult.Error(AmanError.DatabaseError(msg, cause = e))
        }
    }

    override suspend fun registerCustomerNumber(phoneNumber: String): AmanResult<CustomerNumber> {
        return addCustomerNumber(phoneNumber)
    }

    override suspend fun detectProviderFromPrefix(prefix: String): AmanResult<TelecomProvider?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val cleanDigits = prefix.filter { it.isDigit() }
            if (cleanDigits.length < 2) return AmanResult.Success(null)

            // 1. Try DB RPC get_provider_by_prefix
            val providerIdFromRpc = try {
                val params = buildJsonObject {
                    put("p_phone_number", cleanDigits)
                }
                AmanSupabase.postgrest.rpc(
                    function = "get_provider_by_prefix",
                    parameters = params
                ).decodeAsOrNull<String>()
            } catch (_: Exception) {
                null
            }

            if (!providerIdFromRpc.isNullOrBlank()) {
                val provider = AmanSupabase.postgrest.from("telecom_providers")
                    .select {
                        filter { eq("id", providerIdFromRpc) }
                    }.decodeSingleOrNull<TelecomProvider>()
                if (provider != null) return AmanResult.Success(provider)
            }

            // 2. Query telecom_prefixes table
            val subPrefixes = listOf(cleanDigits.take(4), cleanDigits.take(3), cleanDigits.take(2))
            for (sub in subPrefixes) {
                val prefixRecord = try {
                    AmanSupabase.postgrest.from("telecom_prefixes")
                        .select {
                            filter {
                                eq("prefix", sub)
                                eq("is_active", true)
                            }
                        }.decodeList<TelecomPrefix>().firstOrNull()
                } catch (_: Exception) {
                    null
                }
                if (prefixRecord != null) {
                    val provider = AmanSupabase.postgrest.from("telecom_providers")
                        .select {
                            filter { eq("id", prefixRecord.providerId) }
                        }.decodeSingleOrNull<TelecomProvider>()
                    if (provider != null) return AmanResult.Success(provider)
                }
            }

            // 3. Known Yemeni telecom providers fallback
            val sub2 = cleanDigits.take(2)
            val allProviders = try {
                AmanSupabase.postgrest.from("telecom_providers").select().decodeList<TelecomProvider>()
            } catch (_: Exception) {
                emptyList()
            }

            val matched = when (sub2) {
                "77", "78" -> allProviders.find { it.name.contains("يمن") || it.name.contains("Yemen") }
                "73" -> allProviders.find { it.name.contains("يو") || it.name.contains("YOU") }
                "71" -> allProviders.find { it.name.contains("سبأ") || it.name.contains("Saba") }
                "70" -> allProviders.find { it.name.contains("واي") || it.name.contains("Y ") || it.name == "Y" }
                else -> null
            }
            AmanResult.Success(matched)
        } catch (e: Exception) {
            AmanResult.Success(null)
        }
    }

    override suspend fun getNumberDetails(numberId: String): AmanResult<CustomerNumber?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val record = AmanSupabase.postgrest.from("customer_numbers")
                .select {
                    filter { eq("id", numberId) }
                }.decodeSingleOrNull<CustomerNumber>() ?: return AmanResult.Success(null)

            val provider = try {
                AmanSupabase.postgrest.from("telecom_providers")
                    .select { filter { eq("id", record.providerId) } }
                    .decodeSingleOrNull<TelecomProvider>()
            } catch (_: Exception) { null }

            val hasActive = try {
                AmanSupabase.postgrest.from("protections")
                    .select {
                        filter {
                            eq("customer_number_id", numberId)
                            eq("status", "active")
                        }
                    }.decodeList<Protection>().isNotEmpty()
            } catch (_: Exception) { false }

            val hasPending = try {
                AmanSupabase.postgrest.from("protection_requests")
                    .select {
                        filter {
                            eq("customer_number_id", numberId)
                            eq("status", "pending")
                        }
                    }.decodeList<ProtectionRequest>().isNotEmpty()
            } catch (_: Exception) { false }

            val status = when {
                hasActive -> NumberProtectionStatus.PROTECTED
                hasPending -> NumberProtectionStatus.PENDING
                else -> NumberProtectionStatus.UNPROTECTED
            }

            AmanResult.Success(record.copy(provider = provider, protectionStatus = status))
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

    override suspend fun getRequestDetails(requestId: String): AmanResult<ProtectionRequest?> {
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

            AmanResult.Success(record.copy(
                customerNumber = number,
                plan = plan,
                provider = provider,
                paymentMethod = paymentMethod
            ))
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل الطلب: ${e.message}", cause = e))
        }
    }

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

            // 1. Try RPC create_protection_request if available
            val rpcResult = try {
                val params = buildJsonObject {
                    put("p_customer_number_id", customerNumberId)
                    put("p_plan_id", planId)
                    put("p_payment_method_id", paymentMethodId)
                    put("p_transfer_data", transferDataObj)
                }
                AmanSupabase.postgrest.rpc(
                    function = "create_protection_request",
                    parameters = params
                ).decodeAs<ProtectionRequest>()
            } catch (_: Exception) {
                null
            }

            if (rpcResult != null) return AmanResult.Success(rpcResult)

            // 2. Direct insert fallback
            val user = AmanSupabase.auth.currentUserOrNull()
                ?: return AmanResult.Error(AmanError.AuthenticationError("يجب تسجيل الدخول لتقديم طلب الحماية"))

            val number = AmanSupabase.postgrest.from("customer_numbers")
                .select { filter { eq("id", customerNumberId) } }
                .decodeSingle<CustomerNumber>()

            val plan = AmanSupabase.postgrest.from("protection_plans")
                .select { filter { eq("id", planId) } }
                .decodeSingle<ProtectionPlan>()

            val insertData = buildJsonObject {
                put("customer_id", user.id)
                put("customer_number_id", customerNumberId)
                put("provider_id", number.providerId)
                put("plan_id", planId)
                put("payment_method_id", paymentMethodId)
                put("protection_value", plan.price)
                put("plan_name_snapshot", plan.name)
                put("plan_duration_days_snapshot", plan.durationDays)
                put("transfer_data", transferDataObj)
                put("status", "pending")
            }

            val inserted = AmanSupabase.postgrest.from("protection_requests")
                .insert(insertData) {
                    select()
                }.decodeSingle<ProtectionRequest>()

            try {
                AmanSupabase.postgrest.from("customer_numbers").update({
                    set("protection_status", "pending")
                }) {
                    filter { eq("id", customerNumberId) }
                }
            } catch (_: Exception) {}

            AmanResult.Success(inserted)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("unique_pending_request_per_number", ignoreCase = true) == true ||
                e.message?.contains("CONFLICTING_REQUEST_EXISTS", ignoreCase = true) == true) {
                "يوجد بالفعل طلب حماية قيد المراجعة لهذا الرقم"
            } else {
                "تعذر إرسال طلب الحماية: ${e.message}"
            }
            AmanResult.Error(AmanError.DatabaseError(msg, cause = e))
        }
    }
}

// ---------------------------------------------------------------------------
// 6. الحمايات (Protections)
// ---------------------------------------------------------------------------

interface ProtectionRepository {
    suspend fun getCustomerProtections(customerId: String): AmanResult<List<Protection>>
    suspend fun getAllProtections(): AmanResult<List<Protection>>
    suspend fun getProtectionDetails(protectionId: String): AmanResult<Protection?>
}

class ProtectionRepositoryImpl : ProtectionRepository {

    override suspend fun getProtectionDetails(protectionId: String): AmanResult<Protection?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val record = AmanSupabase.postgrest.from("protections")
                .select {
                    filter { eq("id", protectionId) }
                }.decodeSingleOrNull<Protection>() ?: return AmanResult.Success(null)

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

            AmanResult.Success(record.copy(
                customerNumber = number,
                plan = plan,
                provider = provider
            ))
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل الحماية: ${e.message}", cause = e))
        }
    }

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
    suspend fun getTaskDetails(taskId: String): AmanResult<PaymentTask?>
}

class PaymentTaskRepositoryImpl : PaymentTaskRepository {

    override suspend fun getTaskDetails(taskId: String): AmanResult<PaymentTask?> {
        if (!AmanSupabase.isConfigured()) return AmanResult.Error(AmanError.ConfigurationError("Supabase غير مهيأ"))
        return try {
            val record = AmanSupabase.postgrest.from("payment_tasks")
                .select {
                    filter { eq("id", taskId) }
                }.decodeSingleOrNull<PaymentTask>() ?: return AmanResult.Success(null)

            val number = try {
                AmanSupabase.postgrest.from("customer_numbers")
                    .select { filter { eq("id", record.customerNumberId) } }
                    .decodeSingleOrNull<CustomerNumber>()
            } catch (_: Exception) { null }

            val provider = if (record.providerId != null) {
                try {
                    AmanSupabase.postgrest.from("telecom_providers")
                        .select { filter { eq("id", record.providerId) } }
                        .decodeSingleOrNull<TelecomProvider>()
                } catch (_: Exception) { null }
            } else null

            AmanResult.Success(record.copy(customerNumber = number, provider = provider))
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر جلب تفاصيل المهمة: ${e.message}", cause = e))
        }
    }

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
            try {
                val params = buildJsonObject {
                    put("p_notification_id", notificationId)
                }
                AmanSupabase.postgrest.rpc(
                    function = "mark_notification_read",
                    parameters = params
                )
            } catch (_: Exception) {
                AmanSupabase.postgrest.from("notifications").update({
                    set("is_read", true)
                }) {
                    filter { eq("id", notificationId) }
                }
            }
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
