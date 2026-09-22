package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AuditLog
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminAuditLogsUiState {
    data object Loading : AdminAuditLogsUiState
    data object ConfigurationPending : AdminAuditLogsUiState
    data class Content(val logs: List<AuditLog>) : AdminAuditLogsUiState
    data class Error(val message: String) : AdminAuditLogsUiState
}

class AdminAuditLogsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAuditLogsUiState>(AdminAuditLogsUiState.Loading)
    val uiState: StateFlow<AdminAuditLogsUiState> = _uiState.asStateFlow()

    fun loadLogs() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminAuditLogsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminAuditLogsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAuditLogs()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminAuditLogsUiState.Content(res.data.sortedByDescending { it.createdAt })
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminAuditLogsUiState.Error(res.error.message)
                }
            }
        }
    }
}
