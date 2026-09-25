package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumberDetails
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminCustomerNumberDetailsUiState {
    data object Loading : AdminCustomerNumberDetailsUiState
    data object ConfigurationPending : AdminCustomerNumberDetailsUiState
    data class Content(val details: CustomerNumberDetails) : AdminCustomerNumberDetailsUiState
    data class Error(val message: String) : AdminCustomerNumberDetailsUiState
}

class AdminCustomerNumberDetailViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminCustomerNumberDetailsUiState>(AdminCustomerNumberDetailsUiState.Loading)
    val uiState: StateFlow<AdminCustomerNumberDetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(numberId: String) {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminCustomerNumberDetailsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminCustomerNumberDetailsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getCustomerNumberDetails(numberId)) {
                is AmanResult.Success -> {
                    _uiState.value = AdminCustomerNumberDetailsUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminCustomerNumberDetailsUiState.Error(res.error.message)
                }
            }
        }
    }
}
