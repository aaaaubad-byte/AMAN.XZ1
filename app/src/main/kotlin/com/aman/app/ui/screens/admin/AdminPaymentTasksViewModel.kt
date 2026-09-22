package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.PaymentTask
import com.aman.app.data.model.TaskStatus
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminTasksUiState {
    data object Loading : AdminTasksUiState
    data object ConfigurationPending : AdminTasksUiState
    data class Content(
        val allTasks: List<PaymentTask>,
        val filteredTasks: List<PaymentTask>,
        val selectedStatus: TaskStatus? = null
    ) : AdminTasksUiState
    data class Error(val message: String) : AdminTasksUiState
}

class AdminPaymentTasksViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminTasksUiState>(AdminTasksUiState.Loading)
    val uiState: StateFlow<AdminTasksUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun loadTasks() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminTasksUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminTasksUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAllTasks()) {
                is AmanResult.Success -> {
                    val currentFilter = (_uiState.value as? AdminTasksUiState.Content)?.selectedStatus
                    val filtered = if (currentFilter == null) {
                        res.data
                    } else {
                        res.data.filter { it.status == currentFilter }
                    }
                    _uiState.value = AdminTasksUiState.Content(
                        allTasks = res.data,
                        filteredTasks = filtered,
                        selectedStatus = currentFilter
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminTasksUiState.Error(res.error.message)
                }
            }
        }
    }

    fun setFilter(status: TaskStatus?) {
        val current = _uiState.value as? AdminTasksUiState.Content ?: return
        val filtered = if (status == null) {
            current.allTasks
        } else {
            current.allTasks.filter { it.status == status }
        }
        _uiState.value = current.copy(
            selectedStatus = status,
            filteredTasks = filtered
        )
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            when (val res = adminRepo.completePaymentTask(taskId)) {
                is AmanResult.Success -> {
                    _message.value = "تم إكمال المهمة بنجاح وتوليد مهمة الدورة التالية آلياً"
                    loadTasks()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إكمال المهمة: ${res.error.message}"
                }
            }
        }
    }

    fun cancelTask(taskId: String, reason: String) {
        viewModelScope.launch {
            when (val res = adminRepo.cancelPaymentTask(taskId, reason)) {
                is AmanResult.Success -> {
                    _message.value = "تم إلغاء المهمة وتوثيق السبب في سجل العمليات"
                    loadTasks()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إلغاء المهمة: ${res.error.message}"
                }
            }
        }
    }

    fun rescheduleTask(taskId: String, newDueDate: String, reason: String) {
        viewModelScope.launch {
            when (val res = adminRepo.reschedulePaymentTask(taskId, newDueDate, reason)) {
                is AmanResult.Success -> {
                    _message.value = "تمت إعادة جدولة المهمة بنجاح"
                    loadTasks()
                }
                is AmanResult.Error -> {
                    _message.value = "تعذر إعادة الجدولة: ${res.error.message}"
                }
            }
        }
    }
}
