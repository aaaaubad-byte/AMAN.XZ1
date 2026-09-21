package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.ProtectionRequest
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.ProtectionRequestRepository
import com.aman.app.data.repository.ProtectionRequestRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminHomeUiState {
    data object Uninitialized : AdminHomeUiState
    data object Loading : AdminHomeUiState
    data object ConfigurationPending : AdminHomeUiState
    data object Empty : AdminHomeUiState
    data class Content(
        val pendingRequests: List<ProtectionRequest>
    ) : AdminHomeUiState
    data class Error(val message: String) : AdminHomeUiState
}

class AdminHomeViewModel(
    private val requestRepo: ProtectionRequestRepository = ProtectionRequestRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminHomeUiState>(AdminHomeUiState.Uninitialized)
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminHomeUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminHomeUiState.Loading
        viewModelScope.launch {
            when (val result = requestRepo.getAllRequests()) {
                is AmanResult.Success -> {
                    val pending = result.data.filter { it.status.name.lowercase() == "pending" }
                    if (pending.isEmpty()) {
                        _uiState.value = AdminHomeUiState.Empty
                    } else {
                        _uiState.value = AdminHomeUiState.Content(pending)
                    }
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminHomeUiState.Error(result.error.message)
                }
            }
        }
    }
}
