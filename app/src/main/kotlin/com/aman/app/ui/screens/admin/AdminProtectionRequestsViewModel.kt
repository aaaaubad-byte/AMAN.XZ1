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

sealed interface AdminProtectionRequestsUiState {
    data object Loading : AdminProtectionRequestsUiState
    data object ConfigurationPending : AdminProtectionRequestsUiState
    data class Content(
        val allRequests: List<ProtectionRequest>,
        val filteredRequests: List<ProtectionRequest>,
        val selectedStatus: ProtectionRequestStatus? = null
    ) : AdminProtectionRequestsUiState
    data class Error(val message: String) : AdminProtectionRequestsUiState
}

class AdminProtectionRequestsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminProtectionRequestsUiState>(AdminProtectionRequestsUiState.Loading)
    val uiState: StateFlow<AdminProtectionRequestsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadRequests() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminProtectionRequestsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminProtectionRequestsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getProtectionRequests()) {
                is AmanResult.Success -> {
                    val currentFilter = (_uiState.value as? AdminProtectionRequestsUiState.Content)?.selectedStatus
                    val filtered = if (currentFilter == null) {
                        res.data
                    } else {
                        res.data.filter { it.status == currentFilter }
                    }
                    _uiState.value = AdminProtectionRequestsUiState.Content(
                        allRequests = res.data,
                        filteredRequests = filtered,
                        selectedStatus = currentFilter
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminProtectionRequestsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun setFilter(status: ProtectionRequestStatus?) {
        val current = _uiState.value as? AdminProtectionRequestsUiState.Content ?: return
        val filtered = if (status == null) {
            current.allRequests
        } else {
            current.allRequests.filter { it.status == status }
        }
        _uiState.value = current.copy(
            selectedStatus = status,
            filteredRequests = filtered
        )
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.approveProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    _message.value = "تم اعتماد الطلب وتفعيل الحماية وإصدار مهمة الدفع بنجاح"
                    loadRequests()
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الاعتماد: ${res.error.message}"
                }
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            when (val res = adminRepo.rejectProtectionRequest(requestId, reason)) {
                is AmanResult.Success -> {
                    _message.value = "تم رفض الطلب بنجاح"
                    loadRequests()
                }
                is AmanResult.Error -> {
                    _message.value = "فشل الرفض: ${res.error.message}"
                }
            }
        }
    }
}
