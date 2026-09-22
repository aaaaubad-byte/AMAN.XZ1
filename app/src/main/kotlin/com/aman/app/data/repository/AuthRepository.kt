package com.aman.app.data.repository

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppUser
import com.aman.app.data.model.UserRole
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Authentication Repository for AMAN | أمان.
 * Aligned with V7 database schema:
 * - Direct insert/upsert into users table is FORBIDDEN.
 * - Trigger public.handle_new_auth_user() creates the public.users record on auth.users creation.
 * - User metadata (name) is passed in raw_user_meta_data.
 */
interface AuthRepositoryContract {
    val sessionStatus: Flow<SessionStatus>
    suspend fun signIn(email: String, pass: String): AmanResult<AppUser>
    suspend fun signUp(name: String, email: String, pass: String): AmanResult<AppUser>
    suspend fun resetPassword(email: String): AmanResult<Unit>
    suspend fun signOut(): AmanResult<Unit>
    suspend fun getCurrentUser(): AmanResult<AppUser?>
}

class AuthRepository : AuthRepositoryContract {

    override val sessionStatus: Flow<SessionStatus>
        get() {
            return if (AmanSupabase.isConfigured()) {
                AmanSupabase.auth.sessionStatus
            } else {
                emptyFlow()
            }
        }

    override suspend fun signIn(email: String, pass: String): AmanResult<AppUser> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Error(AmanError.ConfigurationError("بيانات اتصال Supabase غير مهيأة بعد"))
        }

        return try {
            AmanSupabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = pass
            }

            val currentUser = AmanSupabase.auth.currentUserOrNull()
                ?: return AmanResult.Error(AmanError.AuthenticationError("لم يتم العثور على بيانات المستخدم بعد الدخول"))

            // Fetch app user profile from users table
            val userRecord = AmanSupabase.postgrest.from("users")
                .select {
                    filter { eq("id", currentUser.id) }
                }.decodeSingleOrNull<AppUser>()

            if (userRecord != null) {
                AmanResult.Success(userRecord)
            } else {
                AmanResult.Error(AmanError.AuthenticationError("ملف المستخدم غير موجود في النظام. يرجى التواصل مع الإدارة"))
            }
        } catch (e: Exception) {
            AmanResult.Error(AmanError.AuthenticationError("فشل تسجيل الدخول: ${e.message}", e))
        }
    }

    override suspend fun signUp(name: String, email: String, pass: String): AmanResult<AppUser> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Error(AmanError.ConfigurationError("بيانات اتصال Supabase غير مهيأة بعد"))
        }

        return try {
            // Sign up passing name in user metadata so trigger public.handle_new_auth_user() can use it
            AmanSupabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = pass
                this.data = buildJsonObject {
                    put("name", name.trim())
                    put("full_name", name.trim())
                }
            }

            val currentUser = AmanSupabase.auth.currentUserOrNull()
                ?: return AmanResult.Error(AmanError.AuthenticationError("فشل إنشاء حساب المصادقة"))

            // Allow the trigger handle_new_auth_user() to execute and populate public.users
            var userRecord: AppUser? = null
            for (attempt in 1..4) {
                delay(300L * attempt)
                userRecord = try {
                    AmanSupabase.postgrest.from("users")
                        .select {
                            filter { eq("id", currentUser.id) }
                        }.decodeSingleOrNull<AppUser>()
                } catch (_: Exception) {
                    null
                }
                if (userRecord != null) break
            }

            val finalUser = userRecord ?: AppUser(
                id = currentUser.id,
                name = name.trim(),
                email = email.trim(),
                role = UserRole.CUSTOMER,
                accountStatus = "active"
            )

            AmanResult.Success(finalUser)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.AuthenticationError("فشل إنشاء الحساب: ${e.message}", e))
        }
    }

    override suspend fun resetPassword(email: String): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Error(AmanError.ConfigurationError("بيانات اتصال Supabase غير مهيأة بعد"))
        }

        return try {
            AmanSupabase.auth.resetPasswordForEmail(email.trim())
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.AuthenticationError("فشل طلب إعادة تعيين كلمة المرور: ${e.message}", e))
        }
    }

    override suspend fun signOut(): AmanResult<Unit> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Success(Unit)
        }

        return try {
            AmanSupabase.auth.signOut()
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.AuthenticationError("فشل تسجيل الخروج: ${e.message}", e))
        }
    }

    override suspend fun getCurrentUser(): AmanResult<AppUser?> {
        if (!AmanSupabase.isConfigured()) {
            return AmanResult.Success(null)
        }

        return try {
            val user = AmanSupabase.auth.currentUserOrNull() ?: return AmanResult.Success(null)
            val appUser = AmanSupabase.postgrest.from("users")
                .select {
                    filter { eq("id", user.id) }
                }.decodeSingleOrNull<AppUser>()

            AmanResult.Success(appUser)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.DatabaseError("تعذر استرجاع بيانات المستخدم: ${e.message}", cause = e))
        }
    }
}
