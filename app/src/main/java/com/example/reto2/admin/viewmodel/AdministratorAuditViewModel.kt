package com.example.reto2.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import com.example.reto2.forex.model.ExchangeOperation
import com.example.reto2.forex.model.ExchangeOperationRepository
import com.example.reto2.forex.model.PersistedAuditRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdministratorAuditUiState(
    val authorized: Boolean = false,
    val operations: List<ExchangeOperation> = emptyList(),
    val sampleRecords: List<PersistedAuditRecord> = emptyList()
)

class AdministratorAuditViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExchangeOperationRepository(application)
    private val _uiState = MutableStateFlow(AdministratorAuditUiState())
    val uiState: StateFlow<AdministratorAuditUiState> = _uiState.asStateFlow()

    fun load(session: UserSession?) {
        if (!RolePermissions.canAuditAll(session)) {
            _uiState.value = AdministratorAuditUiState(authorized = false)
            return
        }
        _uiState.value = AdministratorAuditUiState(
            authorized = true,
            operations = repository.getAll(),
            sampleRecords = repository.getAuditRecords()
        )
    }
}
