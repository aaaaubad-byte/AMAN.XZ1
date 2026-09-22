package com.aman.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.Protection
import com.aman.app.data.model.ProtectionDisplayStatus
import com.aman.app.data.repository.ProtectionRepository
import com.aman.app.data.repository.ProtectionRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProtectionsUiState {
    data object Loading : ProtectionsUiState
    data class Success(val protections: List<Protection>) : ProtectionsUiState
    data class Error(val message: String) : ProtectionsUiState
}

enum class ProtectionFilterTab(val titleAr: String) {
    ALL("الكل"),
    ACTIVE("نشطة"),
    NEEDS_RENEWAL("تحتاج تجديد"),
    EXPIRED("منتهية")
}

class ClientProtectionsViewModel(
    private val protectionRepo: ProtectionRepository = ProtectionRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProtectionsUiState>(ProtectionsUiState.Loading)
    val uiState: StateFlow<ProtectionsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(ProtectionFilterTab.ALL)
    val selectedTab: StateFlow<ProtectionFilterTab> = _selectedTab.asStateFlow()

    fun setFilterTab(tab: ProtectionFilterTab) {
        _selectedTab.value = tab
    }

    fun loadProtections(customerId: String?) {
        if (customerId.isNullOrBlank()) {
            _uiState.value = ProtectionsUiState.Success(emptyList())
            return
        }

        _uiState.value = ProtectionsUiState.Loading
        viewModelScope.launch {
            when (val res = protectionRepo.getCustomerProtections(customerId)) {
                is AmanResult.Success -> {
                    _uiState.value = ProtectionsUiState.Success(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = ProtectionsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun getFilteredProtections(all: List<Protection>, tab: ProtectionFilterTab): List<Protection> {
        return when (tab) {
            ProtectionFilterTab.ALL -> all
            ProtectionFilterTab.ACTIVE -> all.filter { it.calculateDisplayStatus() == ProtectionDisplayStatus.ACTIVE }
            ProtectionFilterTab.NEEDS_RENEWAL -> all.filter { it.calculateDisplayStatus() == ProtectionDisplayStatus.NEEDS_RENEWAL }
            ProtectionFilterTab.EXPIRED -> all.filter { it.calculateDisplayStatus() == ProtectionDisplayStatus.EXPIRED }
        }
    }
}
