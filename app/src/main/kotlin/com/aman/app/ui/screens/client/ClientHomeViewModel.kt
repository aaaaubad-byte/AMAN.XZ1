package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.Protection
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.CustomerNumberRepository
import com.aman.app.data.repository.CustomerNumberRepositoryImpl
import com.aman.app.data.repository.ProtectionRepository
import com.aman.app.data.repository.ProtectionRepositoryImpl
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
        val activeProtectionsCount: Int,
        val needsRenewalCount: Int,
        val expiredCount: Int
    ) : ClientHomeUiState
    data class Error(val message: String) : ClientHomeUiState
}

class ClientHomeViewModel(
    private val numberRepo: CustomerNumberRepository = CustomerNumberRepositoryImpl(),
    private val protectionRepo: ProtectionRepository = ProtectionRepositoryImpl()
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
            when (val numbersResult = numberRepo.getNumbersByCustomer(customerId)) {
                is AmanResult.Success -> {
                    val numbers = numbersResult.data
                    if (numbers.isEmpty()) {
                        _uiState.value = ClientHomeUiState.Empty
                    } else {
                        when (val protectionsResult = protectionRepo.getCustomerProtections(customerId)) {
                            is AmanResult.Success -> {
                                val protections = protectionsResult.data
                                _uiState.value = ClientHomeUiState.Content(
                                    numbers = numbers,
                                    activeProtectionsCount = protections.count { it.status.name.lowercase() == "active" },
                                    needsRenewalCount = 0,
                                    expiredCount = protections.count { it.status.name.lowercase() == "expired" }
                                )
                            }
                            is AmanResult.Error -> {
                                _uiState.value = ClientHomeUiState.Content(
                                    numbers = numbers,
                                    activeProtectionsCount = 0,
                                    needsRenewalCount = 0,
                                    expiredCount = 0
                                )
                            }
                        }
                    }
                }
                is AmanResult.Error -> {
                    _uiState.value = ClientHomeUiState.Error(numbersResult.error.message)
                }
            }
        }
    }
}
