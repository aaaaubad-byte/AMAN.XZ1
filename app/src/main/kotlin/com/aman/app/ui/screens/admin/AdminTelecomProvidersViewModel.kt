package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.TelecomProvider
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminTelecomUiState {
    data object Loading : AdminTelecomUiState
    data object ConfigurationPending : AdminTelecomUiState
    data class Content(val providers: List<TelecomProvider>) : AdminTelecomUiState
    data class Error(val message: String) : AdminTelecomUiState
}

class AdminTelecomProvidersViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminTelecomUiState>(AdminTelecomUiState.Loading)
    val uiState: StateFlow<AdminTelecomUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadProviders() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminTelecomUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminTelecomUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAllProviders()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminTelecomUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminTelecomUiState.Error(res.error.message)
                }
            }
        }
    }

    fun addProvider(name: String, code: String, length: Int, order: Int) {
        viewModelScope.launch {
            when (val res = adminRepo.createProvider(name, code, length, order)) {
                is AmanResult.Success -> {
                    _message.value = "تمت إضافة شركة الاتصالات بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إضافة الشركة: ${res.error.message}"
                }
            }
        }
    }

    fun updateProvider(provider: TelecomProvider) {
        viewModelScope.launch {
            when (val res = adminRepo.updateProvider(provider)) {
                is AmanResult.Success -> {
                    _message.value = "تم تحديث بيانات الشركة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث الشركة: ${res.error.message}"
                }
            }
        }
    }

    fun disableProvider(providerId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.disableProvider(providerId)) {
                is AmanResult.Success -> {
                    _message.value = "تم تعطيل الشركة بنجاح مع الحفاظ على سلامة الحمايات المرتبطة بها"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تعطيل الشركة: ${res.error.message}"
                }
            }
        }
    }
}
