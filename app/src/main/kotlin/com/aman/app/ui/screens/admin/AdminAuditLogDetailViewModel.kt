package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.data.model.AuditLog
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminAuditLogDetailUiState {
    data object Loading : AdminAuditLogDetailUiState
    data object ConfigurationPending : AdminAuditLogDetailUiState
    data class Content(val log: AuditLog) : AdminAuditLogDetailUiState
    data class Error(val message: String) : AdminAuditLogDetailUiState
}

class AdminAuditLogDetailViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAuditLogDetailUiState>(AdminAuditLogDetailUiState.Loading)
    val uiState: StateFlow<AdminAuditLogDetailUiState> = _uiState.asStateFlow()

    fun loadDetails(logId: String) {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminAuditLogDetailUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminAuditLogDetailUiState.Loading
        viewModelScope.launch {
            try {
                val log = AmanSupabase.postgrest.from("audit_logs")
                    .select { filter { eq("id", logId) } }
                    .decodeSingleOrNull<AuditLog>()

                if (log != null) {
                    _uiState.value = AdminAuditLogDetailUiState.Content(log)
                } else {
                    _uiState.value = AdminAuditLogDetailUiState.Error("سجل العملية غير موجود")
                }
            } catch (e: Exception) {
                _uiState.value = AdminAuditLogDetailUiState.Error("تعذر جلب تفاصيل السجل: ${e.message}")
            }
        }
    }
}
