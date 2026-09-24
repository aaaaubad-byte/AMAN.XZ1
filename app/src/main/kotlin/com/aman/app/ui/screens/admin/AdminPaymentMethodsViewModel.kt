package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.PaymentMethod
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminPaymentMethodsUiState {
    data object Loading : AdminPaymentMethodsUiState
    data object ConfigurationPending : AdminPaymentMethodsUiState
    data class Content(val methods: List<PaymentMethod>) : AdminPaymentMethodsUiState
    data class Error(val message: String) : AdminPaymentMethodsUiState
}

class AdminPaymentMethodsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminPaymentMethodsUiState>(AdminPaymentMethodsUiState.Loading)
    val uiState: StateFlow<AdminPaymentMethodsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadMethods() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminPaymentMethodsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminPaymentMethodsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAllPaymentMethods()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminPaymentMethodsUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminPaymentMethodsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun addMethod(walletName: String, accountNumber: String, holderName: String, instructions: String?) {
        viewModelScope.launch {
            when (val res = adminRepo.createPaymentMethod(walletName, accountNumber, holderName, instructions)) {
                is AmanResult.Success -> {
                    _message.value = "تمت إضافة وسيلة الدفع بنجاح"
                    loadMethods()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إضافة وسيلة الدفع: ${res.error.message}"
                }
            }
        }
    }

    fun updateMethod(method: PaymentMethod) {
        viewModelScope.launch {
            when (val res = adminRepo.updatePaymentMethod(method)) {
                is AmanResult.Success -> {
                    _message.value = "تم تحديث وسيلة الدفع بنجاح"
                    loadMethods()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث وسيلة الدفع: ${res.error.message}"
                }
            }
        }
    }

    fun disableMethod(methodId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.disablePaymentMethod(methodId)) {
                is AmanResult.Success -> {
                    _message.value = "تم تعطيل وسيلة الدفع بنجاح"
                    loadMethods()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تعطيل وسيلة الدفع: ${res.error.message}"
                }
            }
        }
    }
}
