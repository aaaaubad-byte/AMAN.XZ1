package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.model.ProtectionRequestStatus
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminHomeUiState {
    data object Uninitialized : AdminHomeUiState
    data object Loading : AdminHomeUiState
    data object ConfigurationPending : AdminHomeUiState
    data object Empty : AdminHomeUiState
    data class Content(
        val pendingRequests: List<ProtectionRequest>
    ) : AdminHomeUiState
    data class Error(val message: String) : AdminHomeUiState
}

class AdminHomeViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminHomeUiState>(AdminHomeUiState.Uninitialized)
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminHomeUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminHomeUiState.Loading
        viewModelScope.launch {
            when (val result = adminRepo.getProtectionRequests(ProtectionRequestStatus.PENDING)) {
                is AmanResult.Success -> {
                    val pending = result.data
                    if (pending.isEmpty()) {
                        _uiState.value = AdminHomeUiState.Empty
                    } else {
                        _uiState.value = AdminHomeUiState.Content(pending)
                    }
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminHomeUiState.Error(result.error.message)
                }
            }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            when (val result = adminRepo.approveProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    _message.value = "تم اعتماد الطلب وتفعيل الحماية بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الاعتماد: ${result.error.message}"
                }
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            when (val result = adminRepo.rejectProtectionRequest(requestId, reason)) {
                is AmanResult.Success -> {
                    _message.value = "تم رفض الطلب بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الرفض: ${result.error.message}"
                }
            }
        }
    }
}
