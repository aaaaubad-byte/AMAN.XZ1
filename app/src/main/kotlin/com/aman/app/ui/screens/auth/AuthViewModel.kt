package com.aman.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanError
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppUser
import com.aman.app.data.repository.AuthRepository
import com.aman.app.data.repository.AuthRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: AppUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class PasswordResetSent(val email: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepositoryContract = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, pass: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني")
            return
        }
        if (pass.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال كلمة المرور")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, pass)) {
                is AmanResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.error.message)
                }
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, confirmPass: String) {
        if (name.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال الاسم")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني")
            return
        }
        if (pass.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال كلمة المرور")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = AuthUiState.Error("كلمتا المرور غير متطابقتين")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signUp(name, email, pass)) {
                is AmanResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.error.message)
                }
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال البريد الإلكتروني لاستعادة كلمة المرور")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.resetPassword(email)) {
                is AmanResult.Success -> {
                    _uiState.value = AuthUiState.PasswordResetSent(email)
                }
                is AmanResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.error.message)
                }
            }
        }
    }

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }
}
