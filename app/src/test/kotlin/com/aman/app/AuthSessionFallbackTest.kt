package com.aman.app

import com.aman.app.data.model.AppUser
import com.aman.app.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthSessionFallbackTest {
    @Test
    fun testUserRoleNormalizationHandlesCustomerAliases() {
        assertEquals(UserRole.CUSTOMER, UserRole.fromValue("customer"))
        assertEquals(UserRole.CLIENT, UserRole.fromValue("client"))
        assertEquals(UserRole.MANAGER, UserRole.fromValue("manager"))
        assertEquals(UserRole.ADMIN, UserRole.fromValue("admin"))
        assertEquals(UserRole.CUSTOMER, UserRole.fromValue("unknown"))
    }

    @Test
    fun testFallbackUserUsesSafeDefaultsWhenProfileIsMissing() {
        val fallbackUser = AppUser.fallbackFromAuth(
            id = "user-123",
            email = "ali@example.com",
            displayName = "Ali",
            rawRole = "client"
        )

        assertEquals("user-123", fallbackUser.id)
        assertEquals("Ali", fallbackUser.name)
        assertEquals("ali@example.com", fallbackUser.email)
        assertEquals(UserRole.CLIENT, fallbackUser.role)
        assertEquals("active", fallbackUser.accountStatus)
    }
}
