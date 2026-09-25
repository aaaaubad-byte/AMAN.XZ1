package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppNotification
import com.aman.app.data.repository.NotificationRepository
import com.aman.app.data.repository.NotificationRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(val notifications: List<AppNotification>) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    private val notificationRepo: NotificationRepository = NotificationRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications(customerId: String?) {
        if (customerId.isNullOrBlank()) {
            _uiState.value = NotificationsUiState.Success(emptyList())
            return
        }

        _uiState.value = NotificationsUiState.Loading
        viewModelScope.launch {
            when (val res = notificationRepo.getCustomerNotifications(customerId)) {
                is AmanResult.Success -> {
                    _uiState.value = NotificationsUiState.Success(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = NotificationsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun markAsRead(notificationId: String, customerId: String?) {
        viewModelScope.launch {
            notificationRepo.markAsRead(notificationId)
            loadNotifications(customerId)
        }
    }

    fun markAllAsRead(customerId: String?) {
        if (customerId.isNullOrBlank()) return
        viewModelScope.launch {
            notificationRepo.markAllAsRead(customerId)
            loadNotifications(customerId)
        }
    }
}
