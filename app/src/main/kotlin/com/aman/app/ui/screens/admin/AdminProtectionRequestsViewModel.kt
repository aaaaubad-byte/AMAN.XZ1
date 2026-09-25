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
        val selectedStatus: ProtectionRequestStatus? = null,
        val searchQuery: String = ""
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

    private fun applyFilterAndSearch(
        requests: List<ProtectionRequest>,
        status: ProtectionRequestStatus?,
        query: String
    ): List<ProtectionRequest> {
        val q = query.trim()
        return requests
            .filter { req ->
                val matchesStatus = (status == null || req.status == status)
                val matchesQuery = q.isEmpty() ||
                    req.id.contains(q, ignoreCase = true) ||
                    (req.customerNumber?.phoneNumber?.contains(q) == true) ||
                    (req.provider?.name?.contains(q, ignoreCase = true) == true) ||
                    (req.actorName?.contains(q, ignoreCase = true) == true) ||
                    (req.plan?.name?.contains(q, ignoreCase = true) == true) ||
                    (req.planNameSnapshot?.contains(q, ignoreCase = true) == true) ||
                    (req.paymentMethodNameSnapshot?.contains(q, ignoreCase = true) == true)
                matchesStatus && matchesQuery
            }
            .sortedByDescending { it.createdAt ?: "" }
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
                    val sorted = res.data.sortedByDescending { it.createdAt ?: "" }
                    val currentContent = _uiState.value as? AdminProtectionRequestsUiState.Content
                    val currentFilter = currentContent?.selectedStatus
                    val currentQuery = currentContent?.searchQuery ?: ""
                    val filtered = applyFilterAndSearch(sorted, currentFilter, currentQuery)
                    _uiState.value = AdminProtectionRequestsUiState.Content(
                        allRequests = sorted,
                        filteredRequests = filtered,
                        selectedStatus = currentFilter,
                        searchQuery = currentQuery
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
        val filtered = applyFilterAndSearch(current.allRequests, status, current.searchQuery)
        _uiState.value = current.copy(
            selectedStatus = status,
            filteredRequests = filtered
        )
    }

    fun setSearchQuery(query: String) {
        val current = _uiState.value as? AdminProtectionRequestsUiState.Content ?: return
        val filtered = applyFilterAndSearch(current.allRequests, current.selectedStatus, query)
        _uiState.value = current.copy(
            searchQuery = query,
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
