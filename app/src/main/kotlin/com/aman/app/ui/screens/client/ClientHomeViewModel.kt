package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ClientHomeUiState {
    data object Uninitialized : ClientHomeUiState
    data object Loading : ClientHomeUiState
    data object ConfigurationPending : ClientHomeUiState
    data object Empty : ClientHomeUiState
    data class Content(
        val numbers: List<CustomerNumber>,
        val protections: List<Protection>,
        val activeProtectionsCount: Int,
        val needsRenewalCount: Int,
        val expiredCount: Int,
        val pendingRequestsCount: Int,
        val unreadNotificationsCount: Int,
        val renewalThresholdDays: Int
    ) : ClientHomeUiState
    data class Error(val message: String) : ClientHomeUiState
}

class ClientHomeViewModel(
    private val numberRepo: CustomerNumberRepository = CustomerNumberRepositoryImpl(),
    private val protectionRepo: ProtectionRepository = ProtectionRepositoryImpl(),
    private val requestRepo: ProtectionRequestRepository = ProtectionRequestRepositoryImpl(),
    private val notificationRepo: NotificationRepository = NotificationRepositoryImpl(),
    private val settingsRepo: SystemSettingsRepository = SystemSettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClientHomeUiState>(ClientHomeUiState.Uninitialized)
    val uiState: StateFlow<ClientHomeUiState> = _uiState.asStateFlow()

    fun loadData(customerId: String?) {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = ClientHomeUiState.ConfigurationPending
            return
        }

        if (customerId.isNullOrBlank()) {
            _uiState.value = ClientHomeUiState.Empty
            return
        }

        _uiState.value = ClientHomeUiState.Loading
        viewModelScope.launch {
            val settingsResult = settingsRepo.getSettings()
            if (settingsResult is AmanResult.Error) {
                _uiState.value = ClientHomeUiState.Error("تعذر تحميل إعدادات النظام: ${settingsResult.error.message}")
                return@launch
            }
            val threshold = (settingsResult as AmanResult.Success).data.renewalThresholdDays

            val numbersResult = numberRepo.getNumbersByCustomer(customerId)
            val protectionsResult = protectionRepo.getCustomerProtections(customerId)
            val requestsResult = requestRepo.getCustomerRequests(customerId)
            val unreadCountResult = notificationRepo.getUnreadCount(customerId)

            val numbers = if (numbersResult is AmanResult.Success) numbersResult.data else emptyList()
            val protections = if (protectionsResult is AmanResult.Success) protectionsResult.data else emptyList()
            val requests = if (requestsResult is AmanResult.Success) requestsResult.data else emptyList()
            val unreadCount = if (unreadCountResult is AmanResult.Success) unreadCountResult.data else 0

            if (numbers.isEmpty() && protections.isEmpty() && requests.isEmpty()) {
                _uiState.value = ClientHomeUiState.Empty
            } else {
                val activeCount = protections.count { it.calculateDisplayStatus(threshold) == ProtectionDisplayStatus.ACTIVE }
                val needsRenewalCount = protections.count { it.calculateDisplayStatus(threshold) == ProtectionDisplayStatus.NEEDS_RENEWAL }
                val expiredCount = protections.count { it.calculateDisplayStatus(threshold) == ProtectionDisplayStatus.EXPIRED }
                val pendingRequestsCount = requests.count { it.status.name.lowercase() == "pending" }

                _uiState.value = ClientHomeUiState.Content(
                    numbers = numbers,
                    protections = protections,
                    activeProtectionsCount = activeCount,
                    needsRenewalCount = needsRenewalCount,
                    expiredCount = expiredCount,
                    pendingRequestsCount = pendingRequestsCount,
                    unreadNotificationsCount = unreadCount,
                    renewalThresholdDays = threshold
                )
            }
        }
    }
}
