package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AppUser
import com.aman.app.data.model.CustomerDetails
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminCustomersUiState {
    data object Loading : AdminCustomersUiState
    data object ConfigurationPending : AdminCustomersUiState
    data class Content(
        val allCustomers: List<AppUser>,
        val filteredCustomers: List<AppUser>,
        val searchQuery: String = ""
    ) : AdminCustomersUiState
    data class Error(val message: String) : AdminCustomersUiState
}

sealed interface CustomerDetailsUiState {
    data object Loading : CustomerDetailsUiState
    data class Content(val details: CustomerDetails) : CustomerDetailsUiState
    data class Error(val message: String) : CustomerDetailsUiState
}

class AdminCustomersViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminCustomersUiState>(AdminCustomersUiState.Loading)
    val uiState: StateFlow<AdminCustomersUiState> = _uiState.asStateFlow()

    private val _detailsState = MutableStateFlow<CustomerDetailsUiState>(CustomerDetailsUiState.Loading)
    val detailsState: StateFlow<CustomerDetailsUiState> = _detailsState.asStateFlow()

    fun loadCustomers() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminCustomersUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminCustomersUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getCustomers()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminCustomersUiState.Content(
                        allCustomers = res.data,
                        filteredCustomers = res.data
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminCustomersUiState.Error(res.error.message)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value as? AdminCustomersUiState.Content ?: return
        val filtered = if (query.isBlank()) {
            current.allCustomers
        } else {
            current.allCustomers.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = current.copy(
            searchQuery = query,
            filteredCustomers = filtered
        )
    }

    fun loadCustomerDetails(customerId: String) {
        _detailsState.value = CustomerDetailsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getCustomerDetails(customerId)) {
                is AmanResult.Success -> {
                    _detailsState.value = CustomerDetailsUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _detailsState.value = CustomerDetailsUiState.Error(res.error.message)
                }
            }
        }
    }
}
