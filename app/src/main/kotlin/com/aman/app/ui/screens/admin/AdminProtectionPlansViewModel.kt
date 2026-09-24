package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.ProtectionPlan
import com.aman.app.data.model.TelecomProvider
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminPlansUiState {
    data object Loading : AdminPlansUiState
    data object ConfigurationPending : AdminPlansUiState
    data class Content(
        val plans: List<ProtectionPlan>,
        val providers: List<TelecomProvider>
    ) : AdminPlansUiState
    data class Error(val message: String) : AdminPlansUiState
}

class AdminProtectionPlansViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminPlansUiState>(AdminPlansUiState.Loading)
    val uiState: StateFlow<AdminPlansUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadData() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminPlansUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminPlansUiState.Loading
        viewModelScope.launch {
            val plansRes = adminRepo.getAllPlans()
            val provsRes = adminRepo.getAllProviders()

            if (plansRes is AmanResult.Success && provsRes is AmanResult.Success) {
                _uiState.value = AdminPlansUiState.Content(
                    plans = plansRes.data,
                    providers = provsRes.data
                )
            } else {
                val errorMsg = (plansRes as? AmanResult.Error)?.error?.message
                    ?: (provsRes as? AmanResult.Error)?.error?.message
                    ?: "تعذر تحميل البيانات"
                _uiState.value = AdminPlansUiState.Error(errorMsg)
            }
        }
    }

    fun addPlan(providerId: String, name: String, price: Double, durationDays: Int) {
        viewModelScope.launch {
            when (val res = adminRepo.createPlan(providerId, name, price, durationDays)) {
                is AmanResult.Success -> {
                    _message.value = "تمت إضافة باقة الحماية بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إضافة الباقة: ${res.error.message}"
                }
            }
        }
    }

    fun updatePlan(plan: ProtectionPlan) {
        viewModelScope.launch {
            when (val res = adminRepo.updatePlan(plan)) {
                is AmanResult.Success -> {
                    _message.value = "تم تحديث بيانات الباقة بنجاح"
                    loadData()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تحديث الباقة: ${res.error.message}"
                }
            }
        }
    }

    fun disablePlan(planId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.disablePlan(planId)) {
                is AmanResult.Success -> {
                    _message.value = "تم تعطيل الباقة بنجاح مع صون سلامة الحمايات القديمة"
                    loadData()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر تعطيل الباقة: ${res.error.message}"
                }
            }
        }
    }
}
