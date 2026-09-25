package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminProtectionRequestDetailUiState {
    data object Loading : AdminProtectionRequestDetailUiState
    data object ConfigurationPending : AdminProtectionRequestDetailUiState
    data class Content(val request: ProtectionRequest) : AdminProtectionRequestDetailUiState
    data class Error(val message: String) : AdminProtectionRequestDetailUiState
}

class AdminProtectionRequestDetailViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminProtectionRequestDetailUiState>(AdminProtectionRequestDetailUiState.Loading)
    val uiState: StateFlow<AdminProtectionRequestDetailUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadDetails(requestId: String) {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminProtectionRequestDetailUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminProtectionRequestDetailUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    val req = res.data
                    if (req != null) {
                        _uiState.value = AdminProtectionRequestDetailUiState.Content(req)
                    } else {
                        _uiState.value = AdminProtectionRequestDetailUiState.Error("طلب الحماية غير موجود")
                    }
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminProtectionRequestDetailUiState.Error(res.error.message)
                }
            }
        }
    }

    fun approve(requestId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.approveProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    _message.value = "تم اعتماد الطلب وتفعيل الحماية وإصدار مهمة الدفع بنجاح"
                    loadDetails(requestId)
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الاعتماد: ${res.error.message}"
                }
            }
        }
    }

    fun reject(requestId: String, reason: String) {
        viewModelScope.launch {
            when (val res = adminRepo.rejectProtectionRequest(requestId, reason)) {
                is AmanResult.Success -> {
                    _message.value = "تم رفض الطلب بنجاح"
                    loadDetails(requestId)
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الرفض: ${res.error.message}"
                }
            }
        }
    }
}
