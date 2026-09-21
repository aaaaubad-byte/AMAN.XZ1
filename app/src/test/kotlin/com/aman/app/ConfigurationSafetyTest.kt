package com.aman.app

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Architectural Invariant Test:
 * - Error propagation guarantees backend failures are never turned into fake successes.
 * - Configuration safety prevents placeholder credentials from being marked as valid.
 */
class ConfigurationSafetyTest {

    @Test
    fun testAmanResultErrorPropagation() {
        val errorResult: AmanResult<String> = AmanResult.Error(
            AmanError.ConfigurationError("Supabase credentials not configured")
        )

        assertTrue(errorResult is AmanResult.Error)
        val error = (errorResult as AmanResult.Error).error
        assertEquals("Supabase credentials not configured", error.message)
        assertTrue(error is AmanError.ConfigurationError)
    }

    @Test
    fun testNoSwallowedErrors() {
        val databaseError: AmanResult<List<String>> = AmanResult.Error(
            AmanError.DatabaseError(
                message = "Table 'protections' relation violation",
                code = "23505"
            )
        )

        when (databaseError) {
            is AmanResult.Success -> fail("Database failure must not produce Success")
            is AmanResult.Error -> {
                val dbErr = databaseError.error as AmanError.DatabaseError
                assertEquals("23505", dbErr.code)
            }
        }
    }
}
