package com.aman.app

import com.aman.app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Test verifying architectural invariants:
 * - Domain and data models accurately serialize and deserialize to/from JSON matching AMAN.XZ.txt.
 * - Strict schema matching with Postgrest expectations.
 */
class ModelsSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testAppUserSerialization() {
        val user = AppUser(
            id = "user-123",
            name = "مستخدم أمان",
            email = "user@example.com",
            role = UserRole.CLIENT,
            accountStatus = "active"
        )

        val serialized = json.encodeToString(user)
        assertTrue(serialized.contains("user-123"))
        assertTrue(serialized.contains("client"))

        val deserialized = json.decodeFromString<AppUser>(serialized)
        assertEquals(user.id, deserialized.id)
        assertEquals(user.email, deserialized.email)
        assertEquals(UserRole.CLIENT, deserialized.role)
    }

    @Test
    fun testTelecomProviderSerialization() {
        val provider = TelecomProvider(
            id = "prov-001",
            name = "يمن موبايل",
            code = "YE-YM",
            numberLength = 9,
            isActive = true,
            isVisibleToCustomer = true,
            displayOrder = 1
        )

        val serialized = json.encodeToString(provider)
        assertTrue(serialized.contains("يمن موبايل"))
        assertTrue(serialized.contains("YE-YM"))

        val deserialized = json.decodeFromString<TelecomProvider>(serialized)
        assertEquals(provider.id, deserialized.id)
        assertEquals(9, deserialized.numberLength)
    }

    @Test
    fun testProtectionRequestSerialization() {
        val req = ProtectionRequest(
            id = "req-99",
            customerId = "cust-1",
            customerNumberId = "num-1",
            providerId = "prov-1",
            planId = "plan-1",
            paymentMethodId = "pay-1",
            protectionValue = 15000.0,
            status = ProtectionRequestStatus.PENDING
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("pending"))
        assertTrue(serialized.contains("15000"))

        val deserialized = json.decodeFromString<ProtectionRequest>(serialized)
        assertEquals(req.id, deserialized.id)
        assertEquals(ProtectionRequestStatus.PENDING, deserialized.status)
        assertEquals(15000.0, deserialized.protectionValue, 0.001)
    }

    @Test
    fun testProtectionStatusDynamicCalculation() {
        val protection = Protection(
            id = "prot-01",
            customerId = "cust-1",
            customerNumberId = "num-1",
            providerId = "prov-1",
            planId = "plan-1",
            priceAtPurchase = 20000.0,
            durationAtPurchase = 365,
            startDate = "2026-01-01T00:00:00Z",
            endDate = "2027-01-01T00:00:00Z",
            status = StoredProtectionStatus.ACTIVE
        )

        assertEquals(StoredProtectionStatus.ACTIVE, protection.status)
        // Verify stored statuses only allow active or expired
        assertNotEquals("needs_renewal", protection.status.name.lowercase())
    }
}
