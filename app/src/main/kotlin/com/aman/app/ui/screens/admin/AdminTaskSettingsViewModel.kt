package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.TaskSettings
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminTaskSettingsUiState {
    data object Loading : AdminTaskSettingsUiState
    data object ConfigurationPending : AdminTaskSettingsUiState
    data class Content(val settings: List<TaskSettings>) : AdminTaskSettingsUiState
    data class Error(val message: String) : AdminTaskSettingsUiState
}

class AdminTaskSettingsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminTaskSettingsUiState>(AdminTaskSettingsUiState.Loading)
    val uiState: StateFlow<AdminTaskSettingsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadSettings() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminTaskSettingsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminTaskSettingsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getTaskSettings()) {
                is AmanResult.Success -> {
                    _uiState.value = AdminTaskSettingsUiState.Content(res.data)
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminTaskSettingsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun updateSettings(settings: TaskSettings) {
        viewModelScope.launch {
            when (val res = adminRepo.updateTaskSettings(settings)) {
                is AmanResult.Success -> {
                    _message.value = "تم حفظ إعدادات المهام بنجاح"
                    loadSettings()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث إعدادات المهام: ${res.error.message}"
                }
            }
        }
    }
}
