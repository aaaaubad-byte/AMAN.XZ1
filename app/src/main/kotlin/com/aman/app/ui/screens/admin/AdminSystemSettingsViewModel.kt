package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.SystemSettings
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminSystemSettingsUiState {
    data object Loading : AdminSystemSettingsUiState
    data object ConfigurationPending : AdminSystemSettingsUiState
    data class Content(val settings: SystemSettings) : AdminSystemSettingsUiState
    data class Error(val message: String) : AdminSystemSettingsUiState
}

class AdminSystemSettingsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminSystemSettingsUiState>(AdminSystemSettingsUiState.Loading)
    val uiState: StateFlow<AdminSystemSettingsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadSettings() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminSystemSettingsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminSystemSettingsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getSystemSettings()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminSystemSettingsUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminSystemSettingsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun saveSettings(appName: String, contactInfo: String?, terms: String?, privacy: String?) {
        val current = (_uiState.value as? AdminSystemSettingsUiState.Content)?.settings ?: SystemSettings()
        val updated = current.copy(
            appName = appName.trim(),
            contactData = contactInfo?.trim(),
            termsAndConditions = terms?.trim(),
            privacyPolicy = privacy?.trim()
        )

        viewModelScope.launch {
            when (val res = adminRepo.updateSystemSettings(updated)) {
                is AmanResult.Success -> {
                    _message.value = "تم حفظ إعدادات النظام بنجاح"
                    loadSettings()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر حفظ إعدادات النظام: ${res.error.message}"
                }
            }
        }
    }
}
