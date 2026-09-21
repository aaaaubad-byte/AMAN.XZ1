package com.aman.app.data.session

import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppUser
import com.aman.app.data.remote.AmanSupabase
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized Session and Authentication State Manager for AMAN.
 * Observes real Supabase Auth state. Never invents fake users or fake sessions.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val userInfo: UserInfo) : AuthState
    data class Error(val error: AmanError) : AuthState
}

class SessionManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        if (!AmanSupabase.isConfigured()) {
            _authState.value = AuthState.Error(
                AmanError.ConfigurationError("بيانات الاتصال بـ Supabase غير مهيأة بعد")
            )
            return
        }

        scope.launch {
            try {
                AmanSupabase.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val user = AmanSupabase.auth.currentUserOrNull()
                            if (user != null) {
                                _authState.value = AuthState.Authenticated(user)
                            } else {
                                _authState.value = AuthState.Unauthenticated
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            _authState.value = AuthState.Unauthenticated
                        }
                        is SessionStatus.LoadingFromStorage -> {
                            _authState.value = AuthState.Loading
                        }
                        is SessionStatus.NetworkError -> {
                            _authState.value = AuthState.Error(
                                AmanError.NetworkError("خطأ في الاتصال بالشبكة أثناء فحص الجلسة")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    AmanError.UnexpectedError("تعذر مراقبة حالة الجلسة: ${e.message}", e)
                )
            }
        }
    }

    suspend fun signOut(): AmanResult<Unit> {
        return try {
            if (AmanSupabase.isConfigured()) {
                AmanSupabase.auth.signOut()
            }
            _authState.value = AuthState.Unauthenticated
            AmanResult.Success(Unit)
        } catch (e: Exception) {
            AmanResult.Error(AmanError.AuthenticationError("فشل تسجيل الخروج: ${e.message}", e))
        }
    }
}
