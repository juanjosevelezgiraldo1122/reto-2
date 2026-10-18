package com.example.reto2.forex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import com.example.reto2.forex.model.ExchangeOperation
import com.example.reto2.forex.model.ExchangeOperationRepository
import com.example.reto2.forex.model.FinnhubRatesRepository
import com.example.reto2.forex.model.TradeSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

data class ExchangeUiState(
    val side: TradeSide = TradeSide.BUY,
    val sourceCurrency: String = "USD",
    val targetCurrency: String = "COP",
    val amount: String = "",
    val operations: List<ExchangeOperation> = emptyList(),
    val validationMessage: String? = null,
    val latestRequestId: String? = null,
    val ratesFromUsd: Map<String, String> = emptyMap(),
    val ratesUpdatedAt: Long? = null,
    val indicativeRate: String? = null,
    val rateError: String? = null
)

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExchangeOperationRepository(application)
    private val ratesRepository = FinnhubRatesRepository(application)
    private val cachedRates = ratesRepository.cached()
    private val _uiState = MutableStateFlow(
        withIndicativeRate(ExchangeUiState(
            operations = repository.getAll(),
            ratesFromUsd = cachedRates?.ratesFromUsd.orEmpty(),
            ratesUpdatedAt = cachedRates?.fetchedAtMillis
        ))
    )
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    init { refreshRates() }

    fun selectSide(side: TradeSide) = _uiState.update { it.copy(side = side, validationMessage = null) }

    fun selectSource(currency: String) = _uiState.update { withIndicativeRate(it.copy(sourceCurrency = currency, validationMessage = null)) }

    fun selectTarget(currency: String) = _uiState.update { withIndicativeRate(it.copy(targetCurrency = currency, validationMessage = null)) }

    fun updateAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d{0,12}([.,]\\d{0,2})?$"))) {
            _uiState.update { withIndicativeRate(it.copy(amount = amount, validationMessage = null)) }
        }
    }

    fun applyScannedValue(amount: String, sourceCurrency: String, targetCurrency: String) {
        _uiState.update { current ->
            withIndicativeRate(
                current.copy(
                    amount = amount,
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                    validationMessage = null
                )
            )
        }
    }

    fun selectPair(sourceCurrency: String, targetCurrency: String) {
        _uiState.update { current ->
            withIndicativeRate(
                current.copy(
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                    validationMessage = null
                )
            )
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { ratesRepository.fetchLatest() } }
                .onSuccess { snapshot ->
                    _uiState.update {
                        withIndicativeRate(it.copy(ratesFromUsd = snapshot.ratesFromUsd, ratesUpdatedAt = snapshot.fetchedAtMillis, rateError = null))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(rateError = error.message ?: "No se pudo actualizar la cotización.") }
                }
        }
    }

    fun refreshFor(session: UserSession?) {
        _uiState.update { state ->
            state.copy(operations = session?.let(repository::getVisibleTo).orEmpty())
        }
    }

    fun confirmRequest(session: UserSession?) {
        if (!RolePermissions.canTrade(session) || session == null) {
            _uiState.update { it.copy(validationMessage = "Este rol no tiene permiso para comprar o vender.") }
            return
        }
        val current = _uiState.value
        val amount = current.amount.replace(',', '.').toBigDecimalOrNull()
        val message = when {
            amount == null || amount <= java.math.BigDecimal.ZERO -> "Ingresa un monto válido."
            current.sourceCurrency == current.targetCurrency -> "Elige dos monedas diferentes."
            else -> null
        }
        if (message != null) {
            _uiState.update { it.copy(validationMessage = message) }
            return
        }

        val saved = runCatching {
            repository.createPending(
                session = session,
                side = current.side,
                sourceCurrency = current.sourceCurrency,
                targetCurrency = current.targetCurrency,
                amount = amount!!.stripTrailingZeros().toPlainString(),
                referenceRate = current.indicativeRate
            )
        }.getOrElse {
            _uiState.update { it.copy(validationMessage = "No se pudo guardar. Inténtalo de nuevo.") }
            return
        }
        _uiState.update {
            it.copy(
                amount = "",
                operations = repository.getVisibleTo(session),
                latestRequestId = saved.id,
                validationMessage = null
            )
        }
    }

    private fun withIndicativeRate(state: ExchangeUiState): ExchangeUiState {
        val sourceRate = state.ratesFromUsd[state.sourceCurrency]?.toBigDecimalOrNull() ?: return state.copy(indicativeRate = null)
        val targetRate = state.ratesFromUsd[state.targetCurrency]?.toBigDecimalOrNull() ?: return state.copy(indicativeRate = null)
        val rate = targetRate.divide(sourceRate, 6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return state.copy(indicativeRate = rate)
    }
}
