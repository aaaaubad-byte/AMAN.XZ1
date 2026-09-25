package com.aman.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.app.core.result.AmanResult
import com.aman.app.data.model.AuditLog
import com.aman.app.data.remote.AmanSupabase
import com.aman.app.data.repository.AdminRepository
import com.aman.app.data.repository.AdminRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminAuditLogsUiState {
    data object Loading : AdminAuditLogsUiState
    data object ConfigurationPending : AdminAuditLogsUiState
    data class Content(
        val allLogs: List<AuditLog>,
        val filteredLogs: List<AuditLog>,
        val searchQuery: String = "",
        val selectedEntity: String? = null,
        val selectedAction: String? = null,
        val availableEntities: List<String> = emptyList(),
        val availableActions: List<String> = emptyList()
    ) : AdminAuditLogsUiState
    data class Error(val message: String) : AdminAuditLogsUiState
}

class AdminAuditLogsViewModel(
    private val adminRepo: AdminRepository = AdminRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAuditLogsUiState>(AdminAuditLogsUiState.Loading)
    val uiState: StateFlow<AdminAuditLogsUiState> = _uiState.asStateFlow()

    private fun filterLogs(
        logs: List<AuditLog>,
        query: String,
        entity: String?,
        action: String?
    ): List<AuditLog> {
        val q = query.trim()
        return logs.filter { log ->
            val matchesEntity = entity == null || log.affectedTable?.equals(entity, ignoreCase = true) == true
            val matchesAction = action == null || log.actionType.equals(action, ignoreCase = true)
            val matchesSearch = q.isEmpty() ||
                log.actionType.contains(q, ignoreCase = true) ||
                (log.actorName?.contains(q, ignoreCase = true) == true) ||
                (log.actorId?.contains(q, ignoreCase = true) == true) ||
                (log.affectedTable?.contains(q, ignoreCase = true) == true) ||
                (log.affectedRecord?.contains(q, ignoreCase = true) == true) ||
                (log.details?.contains(q, ignoreCase = true) == true)

            matchesEntity && matchesAction && matchesSearch
        }.sortedByDescending { it.createdAt ?: "" }
    }

    fun loadLogs() {
        if (!AmanSupabase.isConfigured()) {
            _uiState.value = AdminAuditLogsUiState.ConfigurationPending
            return
        }

        _uiState.value = AdminAuditLogsUiState.Loading
        viewModelScope.launch {
            when (val res = adminRepo.getAuditLogs()) {
                is AmanResult.Success -> {
                    val logs = res.data.sortedByDescending { it.createdAt ?: "" }
                    val entities = logs.mapNotNull { it.affectedTable }.distinct().sorted()
                    val actions = logs.map { it.actionType }.distinct().sorted()

                    val currentContent = _uiState.value as? AdminAuditLogsUiState.Content
                    val currentQuery = currentContent?.searchQuery ?: ""
                    val currentEntity = currentContent?.selectedEntity
                    val currentAction = currentContent?.selectedAction

                    val filtered = filterLogs(logs, currentQuery, currentEntity, currentAction)

                    _uiState.value = AdminAuditLogsUiState.Content(
                        allLogs = logs,
                        filteredLogs = filtered,
                        searchQuery = currentQuery,
                        selectedEntity = currentEntity,
                        selectedAction = currentAction,
                        availableEntities = entities,
                        availableActions = actions
                    )
                }
                is AmanResult.Error -> {
                    _uiState.value = AdminAuditLogsUiState.Error(res.error.message)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        val current = _uiState.value as? AdminAuditLogsUiState.Content ?: return
        val filtered = filterLogs(current.allLogs, query, current.selectedEntity, current.selectedAction)
        _uiState.value = current.copy(
            searchQuery = query,
            filteredLogs = filtered
        )
    }

    fun setSelectedEntity(entity: String?) {
        val current = _uiState.value as? AdminAuditLogsUiState.Content ?: return
        val filtered = filterLogs(current.allLogs, current.searchQuery, entity, current.selectedAction)
        _uiState.value = current.copy(
            selectedEntity = entity,
            filteredLogs = filtered
        )
    }

    fun setSelectedAction(action: String?) {
        val current = _uiState.value as? AdminAuditLogsUiState.Content ?: return
        val filtered = filterLogs(current.allLogs, current.searchQuery, current.selectedEntity, action)
        _uiState.value = current.copy(
            selectedAction = action,
            filteredLogs = filtered
        )
    }
}
