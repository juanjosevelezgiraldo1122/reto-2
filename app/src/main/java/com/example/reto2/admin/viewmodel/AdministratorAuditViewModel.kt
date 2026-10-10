package com.example.reto2.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import com.example.reto2.forex.model.ExchangeOperation
import com.example.reto2.forex.model.ExchangeOperationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal

data class DemoAuditRecord(
    val id: String,
    val ownerLabel: String,
    val pair: String,
    val status: String,
    val referenceRate: BigDecimal,
    val appliedRate: BigDecimal
) {
    val withinLimit: Boolean
        get() = appliedRate <= referenceRate.multiply(MAX_ALLOWED_RATE_FACTOR)

    val deviationPercent: BigDecimal
        get() = appliedRate.subtract(referenceRate)
            .multiply(BigDecimal("100"))
            .divide(referenceRate, 2, java.math.RoundingMode.HALF_UP)

    private companion object {
        val MAX_ALLOWED_RATE_FACTOR = BigDecimal("1.025")
    }
}

data class AdministratorAuditUiState(
    val authorized: Boolean = false,
    val operations: List<ExchangeOperation> = emptyList(),
    val sampleRecords: List<DemoAuditRecord> = emptyList()
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
            sampleRecords = DEMO_AUDIT_RECORDS
        )
    }

    private companion object {
        val DEMO_AUDIT_RECORDS = listOf(
            DemoAuditRecord(
                id = "DEMO-1042",
                ownerLabel = "Cliente demo 01",
                pair = "USD / COP",
                status = "Exitosa · ejemplo",
                referenceRate = BigDecimal("4050.00"),
                appliedRate = BigDecimal("4108.00")
            ),
            DemoAuditRecord(
                id = "DEMO-1043",
                ownerLabel = "Cliente demo 02",
                pair = "EUR / USD",
                status = "En proceso · ejemplo",
                referenceRate = BigDecimal("1.0800"),
                appliedRate = BigDecimal("1.1110")
            ),
            DemoAuditRecord(
                id = "DEMO-1044",
                ownerLabel = "Cliente demo 03",
                pair = "GBP / EUR",
                status = "Fallida · ejemplo",
                referenceRate = BigDecimal("1.1700"),
                appliedRate = BigDecimal("1.1800")
            )
        )
    }
}
