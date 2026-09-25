package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.PaymentTask
import com.aman.app.data.model.TaskSettings
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

    private var loadedSettingsByProvider: Map<String, TaskSettings> = emptyMap()

    fun clearMessage() {
        _message.value = null
    }

    private fun resolveVisibilityWindow(task: PaymentTask, settingsByProvider: Map<String, TaskSettings>): Int {
        val configuredWindow = settingsByProvider[task.providerId]?.daysVisibleBeforeDue
        return configuredWindow ?: 7
    }

    private fun classifyTask(task: PaymentTask, settingsByProvider: Map<String, TaskSettings>): TaskStatus {
        return task.displayStatus(
            visibilityWindowDays = resolveVisibilityWindow(task, settingsByProvider),
        )
    }

    private fun filterTasks(
        tasks: List<PaymentTask>,
        status: TaskStatus?,
        settingsByProvider: Map<String, TaskSettings>
    ): List<PaymentTask> {
        if (status == null) return tasks
        return tasks.filter { task ->
            when (status) {
                TaskStatus.UPCOMING -> classifyTask(task, settingsByProvider) == TaskStatus.UPCOMING
                TaskStatus.DUE -> classifyTask(task, settingsByProvider) == TaskStatus.DUE || classifyTask(task, settingsByProvider) == TaskStatus.DUE_SOON
                TaskStatus.OVERDUE -> classifyTask(task, settingsByProvider) == TaskStatus.OVERDUE
                TaskStatus.COMPLETED -> classifyTask(task, settingsByProvider) == TaskStatus.COMPLETED
                TaskStatus.CANCELLED -> classifyTask(task, settingsByProvider) == TaskStatus.CANCELLED
                TaskStatus.PENDING -> classifyTask(task, settingsByProvider) == TaskStatus.UPCOMING || classifyTask(task, settingsByProvider) == TaskStatus.DUE_SOON
                TaskStatus.DUE_SOON -> classifyTask(task, settingsByProvider) == TaskStatus.DUE_SOON
            }
        }
    }

    fun loadTasks() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminTasksUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminTasksUiState.Loading
        viewModelScope.launch {
            when (val tasksResult = adminRepo.getAllTasks()) {
                is AmanResult.Success -> {
                    val settingsMap = when (val settingsResult = adminRepo.getTaskSettings()) {
                        is AmanResult.Success -> settingsResult.data.associateBy { it.providerId }
                        is AmanResult.Error -> loadedSettingsByProvider
                    }
                    loadedSettingsByProvider = settingsMap

                    val currentFilter = (_uiState.value as? AdminTasksUiState.Content)?.selectedStatus
                    val filtered = if (currentFilter == null) {
                        tasksResult.data
                    } else {
                        filterTasks(tasksResult.data, currentFilter, loadedSettingsByProvider)
                    }
                    _uiState.value = AdminTasksUiState.Content(
                        allTasks = tasksResult.data,
                        filteredTasks = filtered,
                        selectedStatus = currentFilter
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminTasksUiState.Error(tasksResult.error.message)
                }
            }
        }
    }

    fun setFilter(status: TaskStatus?) {
        val current = _uiState.value as? AdminTasksUiState.Content ?: return
        val filtered = filterTasks(current.allTasks, status, loadedSettingsByProvider)
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
