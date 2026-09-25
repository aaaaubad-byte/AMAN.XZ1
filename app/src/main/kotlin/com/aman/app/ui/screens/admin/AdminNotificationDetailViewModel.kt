package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppNotification
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminNotificationDetailUiState {
    data object Loading : AdminNotificationDetailUiState
    data object ConfigurationPending : AdminNotificationDetailUiState
    data class Content(val notification: AppNotification) : AdminNotificationDetailUiState
    data class Error(val message: String) : AdminNotificationDetailUiState
}

class AdminNotificationDetailViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminNotificationDetailUiState>(AdminNotificationDetailUiState.Loading)
    val uiState: StateFlow<AdminNotificationDetailUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadDetails(notificationId: String) {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminNotificationDetailUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminNotificationDetailUiState.Loading
        viewModelScope.launch {
            try {
                val notif = AmanSupabase.postgrest.from("notifications")
                    .select { filter { eq("id", notificationId) } }
                    .decodeSingleOrNull<AppNotification>()

                if (notif != null) {
                    _uiState.value = AdminNotificationDetailUiState.Content(notif)
                } else {
                    _uiState.value = AdminNotificationDetailUiState.Error("الإشعار غير موجود")
                }
            } catch (e: Exception) {
                _uiState.value = AdminNotificationDetailUiState.Error("تعذر جلب تفاصيل الإشعار: ${e.message}")
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.markNotificationAsRead(notificationId)) {
                is AmanResult.Success -> {
                    val current = (_uiState.value as? AdminNotificationDetailUiState.Content)?.notification
                    if (current != null) {
                        _uiState.value = AdminNotificationDetailUiState.Content(current.copy(isRead = true))
                    }
                    _message.value = "تم تحديد الإشعار كمقروء"
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث الإشعار: ${res.error.message}"
                }
            }
        }
    }
}
