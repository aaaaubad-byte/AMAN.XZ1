package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.TelecomPrefix
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
    data class Content(
        val providers: List<TelecomProvider>,
        val prefixesByProvider: Map<String, List<TelecomPrefix>> = emptyMap()
    ) : AdminTelecomUiState
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
                    val providers = res.data
                    val prefixMap = mutableMapOf<String, List<TelecomPrefix>>()
                    for (prov in providers) {
                        when (val prefixRes = adminRepo.getProviderPrefixes(prov.id)) {
                            is AmanResult.Success -> prefixMap[prov.id] = prefixRes.data
                            is AmanResult.Error -> prefixMap[prov.id] = emptyList()
                        }
                    }
                    _uiState.value = AdminTelecomUiState.Content(
                        providers = providers,
                        prefixesByProvider = prefixMap
                    )
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
                    _message.value = "تم تعطيل الشركة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تعطيل الشركة: ${res.error.message}"
                }
            }
        }
    }

    fun activateProvider(providerId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.activateProvider(providerId)) {
                is AmanResult.Success -> {
                    _message.value = "تم تفعيل الشركة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تفعيل الشركة: ${res.error.message}"
                }
            }
        }
    }

    fun toggleProviderVisibility(provider: TelecomProvider) {
        val newVisibility = !provider.isVisibleToCustomer
        viewModelScope.launch {
            when (val res = adminRepo.setProviderVisibility(provider.id, newVisibility)) {
                is AmanResult.Success -> {
                    _message.value = if (newVisibility) "تم إظهار الشركة للعملاء بنجاح" else "تم إخفاء الشركة عن العملاء بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تغيير حالة ظهور الشركة: ${res.error.message}"
                }
            }
        }
    }

    fun addPrefix(providerId: String, prefix: String) {
        viewModelScope.launch {
            when (val res = adminRepo.addProviderPrefix(providerId, prefix)) {
                is AmanResult.Success -> {
                    _message.value = "تمت إضافة البادئة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إضافة البادئة: ${res.error.message}"
                }
            }
        }
    }

    fun updatePrefix(prefixId: String, prefix: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val res = adminRepo.updateProviderPrefix(prefixId, prefix, isActive)) {
                is AmanResult.Success -> {
                    _message.value = "تم تحديث البادئة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث البادئة: ${res.error.message}"
                }
            }
        }
    }

    fun togglePrefixStatus(prefixId: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val res = adminRepo.setPrefixStatus(prefixId, isActive)) {
                is AmanResult.Success -> {
                    _message.value = if (isActive) "تم تفعيل البادئة بنجاح" else "تم تعطيل البادئة بنجاح"
                    loadProviders()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث حالة البادئة: ${res.error.message}"
                }
            }
        }
    }
}
