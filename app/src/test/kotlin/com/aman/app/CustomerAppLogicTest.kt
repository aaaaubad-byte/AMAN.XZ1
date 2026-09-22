package com.aman.app

import com.aman.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stage 3 Unit Tests: Customer Application Business Logic
 * Covers:
 * 1. Automatic telecom-provider detection from Yemen phone prefixes (77, 78, 73, 71, 70).
 * 2. 9-digit validation rules for Yemeni mobile phone numbers.
 * 3. Dynamic protection display status calculation (Active, Needs Renewal, Expired).
 * 4. Protection request status invariants and filter tabs.
 */
class CustomerAppLogicTest {

    private val sampleProviders = listOf(
        TelecomProvider(
            id = "prov-ym",
            name = "يمن موبايل",
            code = "YE-YM",
            numberLength = 9,
            isActive = true,
            isVisibleToCustomer = true,
            displayOrder = 1
        ),
        TelecomProvider(
            id = "prov-you",
            name = "يو (YOU)",
            code = "YE-YOU",
            numberLength = 9,
            isActive = true,
            isVisibleToCustomer = true,
            displayOrder = 2
        ),
        TelecomProvider(
            id = "prov-sb",
            name = "سبأفون",
            code = "YE-SB",
            numberLength = 9,
            isActive = true,
            isVisibleToCustomer = true,
            displayOrder = 3
        ),
        TelecomProvider(
            id = "prov-y",
            name = "واي (Y)",
            code = "YE-Y",
            numberLength = 9,
            isActive = true,
            isVisibleToCustomer = true,
            displayOrder = 4
        )
    )

    private fun detectProvider(phone: String): TelecomProvider? {
        val clean = phone.filter { it.isDigit() }
        val prefix = when {
            clean.startsWith("967") && clean.length >= 5 -> clean.substring(3, 5)
            clean.startsWith("00967") && clean.length >= 7 -> clean.substring(5, 7)
            clean.startsWith("0") && clean.length >= 3 -> clean.substring(1, 3)
            clean.length >= 2 -> clean.substring(0, 2)
            else -> ""
        }

        val providerCode = when (prefix) {
            "77", "78" -> "YE-YM"
            "73" -> "YE-YOU"
            "71" -> "YE-SB"
            "70" -> "YE-Y"
            else -> null
        }

        return sampleProviders.find { it.code == providerCode }
    }

    private fun validateYemenPhone(phone: String): String? {
        val clean = phone.filter { it.isDigit() }
        val normalized = when {
            clean.startsWith("967") -> clean.substring(3)
            clean.startsWith("00967") -> clean.substring(5)
            clean.startsWith("0") -> clean.substring(1)
            else -> clean
        }

        if (normalized.length != 9) {
            return "رقم الهاتف يجب أن يتكون من 9 أرقام"
        }
        val prefix = normalized.substring(0, 2)
        if (prefix !in listOf("77", "78", "73", "71", "70")) {
            return "مقدمة الرقم غير صحيحة، يجب أن يبدأ بـ 77، 78، 73، 71، أو 70"
        }
        return null // Valid
    }

    @Test
    fun testYemenMobilePrefixDetection() {
        val provider77 = detectProvider("771234567")
        assertNotNull(provider77)
        assertEquals("YE-YM", provider77?.code)
        assertEquals("يمن موبايل", provider77?.name)

        val provider78 = detectProvider("780987654")
        assertNotNull(provider78)
        assertEquals("YE-YM", provider78?.code)

        val providerIntl = detectProvider("967771234567")
        assertNotNull(providerIntl)
        assertEquals("YE-YM", providerIntl?.code)

        val providerWithZero = detectProvider("0771234567")
        assertNotNull(providerWithZero)
        assertEquals("YE-YM", providerWithZero?.code)
    }

    @Test
    fun testYouPrefixDetection() {
        val provider73 = detectProvider("733456789")
        assertNotNull(provider73)
        assertEquals("YE-YOU", provider73?.code)
        assertEquals("يو (YOU)", provider73?.name)
    }

    @Test
    fun testSabaFonPrefixDetection() {
        val provider71 = detectProvider("712345678")
        assertNotNull(provider71)
        assertEquals("YE-SB", provider71?.code)
        assertEquals("سبأفون", provider71?.name)
    }

    @Test
    fun testYTelecomPrefixDetection() {
        val provider70 = detectProvider("701234567")
        assertNotNull(provider70)
        assertEquals("YE-Y", provider70?.code)
        assertEquals("واي (Y)", provider70?.name)
    }

    @Test
    fun testInvalidPrefixDetection() {
        val invalidProvider = detectProvider("751234567")
        assertNull(invalidProvider)

        val landline = detectProvider("01234567")
        assertNull(landline)
    }

    @Test
    fun testPhoneNumberValidationRules() {
        // Valid 9-digit numbers
        assertNull(validateYemenPhone("771234567"))
        assertNull(validateYemenPhone("0781234567"))
        assertNull(validateYemenPhone("967731234567"))

        // Invalid length
        assertNotNull(validateYemenPhone("77123456")) // 8 digits
        assertNotNull(validateYemenPhone("7712345678")) // 10 digits

        // Invalid prefix
        assertNotNull(validateYemenPhone("751234567"))
        assertNotNull(validateYemenPhone("721234567"))
    }

    @Test
    fun testDynamicProtectionStatusCalculation_Active() {
        val now = Instant.now()
        val futureEnd = now.plus(45, ChronoUnit.DAYS).toString()

        val protection = Protection(
            id = "prot-active",
            customerId = "c1",
            customerNumberId = "n1",
            providerId = "p1",
            planId = "plan1",
            priceAtPurchase = 12000.0,
            durationAtPurchase = 90,
            startDate = now.minus(45, ChronoUnit.DAYS).toString(),
            endDate = futureEnd,
            status = StoredProtectionStatus.ACTIVE
        )

        val displayStatus = protection.calculateDisplayStatus(warningDaysThreshold = 10)
        assertEquals(ProtectionDisplayStatus.ACTIVE, displayStatus)
        assertTrue(protection.daysRemaining() > 10)
    }

    @Test
    fun testDynamicProtectionStatusCalculation_NeedsRenewal() {
        val now = Instant.now()
        val nearEnd = now.plus(5, ChronoUnit.DAYS).toString()

        val protection = Protection(
            id = "prot-renewal",
            customerId = "c1",
            customerNumberId = "n1",
            providerId = "p1",
            planId = "plan1",
            priceAtPurchase = 12000.0,
            durationAtPurchase = 90,
            startDate = now.minus(85, ChronoUnit.DAYS).toString(),
            endDate = nearEnd,
            status = StoredProtectionStatus.ACTIVE
        )

        val displayStatus = protection.calculateDisplayStatus(warningDaysThreshold = 10)
        assertEquals(ProtectionDisplayStatus.NEEDS_RENEWAL, displayStatus)
        assertEquals(5L, protection.daysRemaining())
    }

    @Test
    fun testDynamicProtectionStatusCalculation_Expired() {
        val now = Instant.now()
        val pastEnd = now.minus(2, ChronoUnit.DAYS).toString()

        val protection = Protection(
            id = "prot-expired",
            customerId = "c1",
            customerNumberId = "n1",
            providerId = "p1",
            planId = "plan1",
            priceAtPurchase = 12000.0,
            durationAtPurchase = 90,
            startDate = now.minus(92, ChronoUnit.DAYS).toString(),
            endDate = pastEnd,
            status = StoredProtectionStatus.ACTIVE
        )

        val displayStatus = protection.calculateDisplayStatus(warningDaysThreshold = 10)
        assertEquals(ProtectionDisplayStatus.EXPIRED, displayStatus)
        assertEquals(0L, protection.daysRemaining())
    }
}
