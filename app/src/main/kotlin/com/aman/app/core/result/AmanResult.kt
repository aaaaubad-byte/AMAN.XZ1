package com.aman.app.core.result

/**
 * Standard Result and Error model for AMAN.
 * Ensures strict error handling without swallowing exceptions or converting backend failures
 * into fake successes.
 */
sealed interface AmanResult<out T> {
    data class Success<out T>(val data: T) : AmanResult<T>
    data class Error(val error: AmanError) : AmanResult<Nothing>
}

sealed class AmanError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class AuthenticationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)

    data class AuthorizationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)

    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)

    data class DatabaseError(
        override val message: String,
        val code: String? = null,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)

    data class BusinessRuleError(
        override val message: String,
        val rule: String? = null,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)

    data class ValidationError(
        override val message: String,
        val field: String? = null
    ) : AmanError(message)

    data class SessionExpiredError(
        override val message: String = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجددًا"
    ) : AmanError(message)

    data class ConfigurationError(
        override val message: String
    ) : AmanError(message)

    data class UnexpectedError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AmanError(message, cause)
}
