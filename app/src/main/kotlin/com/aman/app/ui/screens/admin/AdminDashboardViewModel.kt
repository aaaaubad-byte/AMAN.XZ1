package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AdminDashboardMetrics
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.model.ProtectionRequestStatus
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminDashboardUiState {
    data object Loading : AdminDashboardUiState
    data object ConfigurationPending : AdminDashboardUiState
    data class Content(
        val metrics: AdminDashboardMetrics,
        val pendingRequests: List<ProtectionRequest>
    ) : AdminDashboardUiState
    data class Error(val message: String) : AdminDashboardUiState
}

class AdminDashboardViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminDashboardUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminDashboardUiState.Loading
        viewModelScope.launch {
            when (val metricsRes = adminRepo.loadDashboard()) {
                is AmanResult.Success -> {
                    val requestsRes = adminRepo.getProtectionRequests(ProtectionRequestStatus.PENDING)
                    val pending = if (requestsRes is AmanResult.Success) requestsRes.data else emptyList()
                    _uiState.value = AdminDashboardUiState.Content(
                        metrics = metricsRes.data,
                        pendingRequests = pending
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminDashboardUiState.Error(metricsRes.error.message)
                }
            }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.approveProtectionRequest(requestId)) {
                is AmanResult.Success -> {
                    _actionMessage.value = "تم اعتماد وتفعيل الحماية بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _actionMessage.value = "فشل الاعتماد: ${res.error.message}"
                }
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            when (val res = adminRepo.rejectProtectionRequest(requestId, reason)) {
                is AmanResult.Success -> {
                    _actionMessage.value = "تم رفض الطلب بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _actionMessage.value = "فشل الرفض: ${res.error.message}"
                }
            }
        }
    }
}
