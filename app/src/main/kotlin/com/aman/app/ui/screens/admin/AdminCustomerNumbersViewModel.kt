package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.CustomerNumber
import com.aman.app.data.model.TelecomProvider
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminCustomerNumbersUiState {
    data object Loading : AdminCustomerNumbersUiState
    data object ConfigurationPending : AdminCustomerNumbersUiState
    data class Content(
        val numbers: List<CustomerNumber>,
        val providers: Map<String, TelecomProvider>
    ) : AdminCustomerNumbersUiState
    data class Error(val message: String) : AdminCustomerNumbersUiState
}

class AdminCustomerNumbersViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminCustomerNumbersUiState>(AdminCustomerNumbersUiState.Loading)
    val uiState: StateFlow<AdminCustomerNumbersUiState> = _uiState.asStateFlow()

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminCustomerNumbersUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminCustomerNumbersUiState.Loading
        viewModelScope.launch {
            val numbersRes = adminRepo.getAllCustomerNumbers()
            val providersRes = adminRepo.getAllProviders()

            if (numbersRes is AmanResult.Success && providersRes is AmanResult.Success) {
                val providerMap = providersRes.data.associateBy { it.id }
                _uiState.value = AdminCustomerNumbersUiState.Content(
                    numbers = numbersRes.data,
                    providers = providerMap
                )
            } else {
                val errorMsg = (numbersRes as? AmanResult.Error)?.error?.message
                    ?: (providersRes as? AmanResult.Error)?.error?.message
                    ?: "تعذر تحميل أرقام العملاء"
                _uiState.value = AdminCustomerNumbersUiState.Error(errorMsg)
            }
        }
    }
}
