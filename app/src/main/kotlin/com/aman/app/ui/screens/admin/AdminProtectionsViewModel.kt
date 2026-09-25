package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminProtectionsUiState {
    data object Loading : AdminProtectionsUiState
    data object ConfigurationPending : AdminProtectionsUiState
    data class Content(
        val allProtections: List<Protection>,
        val filteredProtections: List<Protection>,
        val selectedFilter: ProtectionDisplayStatus? = null,
        val searchQuery: String = "",
        val renewalThresholdDays: Int
    ) : AdminProtectionsUiState
    data class Error(val message: String) : AdminProtectionsUiState
}

class AdminProtectionsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminProtectionsUiState>(AdminProtectionsUiState.Loading)
    val uiState: StateFlow<AdminProtectionsUiState> = _uiState.asStateFlow()

    fun loadProtections() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminProtectionsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminProtectionsUiState.Loading
        viewModelScope.launch {
            val settingsRes = adminRepo.getSystemSettings()
            if (settingsRes is AmanResult.Error) {
                _uiState.value = AdminProtectionsUiState.Error("تعذر تحميل إعدادات النظام: ${settingsRes.error.message}")
                return@launch
            }
            val threshold = (settingsRes as AmanResult.Success).data.renewalThresholdDays

            when (val res = adminRepo.getAllProtections()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminProtectionsUiState.Content(
                        allProtections = res.data,
                        filteredProtections = res.data,
                        renewalThresholdDays = threshold
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminProtectionsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun setFilter(status: ProtectionDisplayStatus?) {
        val current = _uiState.value as? AdminProtectionsUiState.Content ?: return
        val filtered = filterList(current.allProtections, status, current.searchQuery, current.renewalThresholdDays)
        _uiState.value = current.copy(
            selectedFilter = status,
            filteredProtections = filtered
        )
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value as? AdminProtectionsUiState.Content ?: return
        val filtered = filterList(current.allProtections, current.selectedFilter, query, current.renewalThresholdDays)
        _uiState.value = current.copy(
            searchQuery = query,
            filteredProtections = filtered
        )
    }

    private fun filterList(
        list: List<Protection>,
        status: ProtectionDisplayStatus?,
        query: String,
        threshold: Int
    ): List<Protection> {
        return list.filter { prot ->
            val matchesStatus = if (status == null) true else prot.calculateDisplayStatus(threshold) == status
            val matchesQuery = if (query.isBlank()) true else {
                prot.customerNumber?.phoneNumber?.contains(query, ignoreCase = true) == true ||
                prot.customerId.contains(query, ignoreCase = true)
            }
            matchesStatus && matchesQuery
        }
    }
}
