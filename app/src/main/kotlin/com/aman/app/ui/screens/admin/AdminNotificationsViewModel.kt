package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppNotification
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminNotificationsUiState {
    data object Loading : AdminNotificationsUiState
    data object ConfigurationPending : AdminNotificationsUiState
    data class Content(
        val notifications: List<AppNotification>
    ) : AdminNotificationsUiState
    data class Error(val message: String) : AdminNotificationsUiState
}

class AdminNotificationsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminNotificationsUiState>(AdminNotificationsUiState.Loading)
    val uiState: StateFlow<AdminNotificationsUiState> = _uiState.asStateFlow()

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminNotificationsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminNotificationsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAllNotifications()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminNotificationsUiState.Content(notifications = res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminNotificationsUiState.Error(res.error.message)
                }
            }
        }
    }
}
