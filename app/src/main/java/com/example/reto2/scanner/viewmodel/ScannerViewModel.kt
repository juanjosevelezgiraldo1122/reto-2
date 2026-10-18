package com.example.reto2.scanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reto2.forex.model.FinnhubRatesRepository
import com.example.reto2.scanner.model.DetectedPrice
import com.example.reto2.scanner.model.PricePatternParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ViewModel para el módulo de Visión & OCR de AUREX.
 * Gestiona el estado de permisos, lecturas del analizador ML Kit, resolución de ambigüedad
 * del símbolo '$' y conversión instantánea contra las tasas vigentes de mercado.
 */
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val ratesRepository = FinnhubRatesRepository(application)

    private val _uiState = MutableStateFlow(
        ScannerUiState(
            ratesFromUsd = ratesRepository.cached()?.ratesFromUsd?.takeIf { it.isNotEmpty() }
                ?: FALLBACK_RATES_FROM_USD
        )
    )
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        refreshRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { ratesRepository.fetchLatest() }
            }.onSuccess { snapshot ->
                _uiState.update { current ->
                    recalculateConversion(current.copy(ratesFromUsd = snapshot.ratesFromUsd, errorMessage = null))
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onPriceDetected(detected: DetectedPrice) {
        _uiState.update { current ->
            if (current.isPaused) return@update current

            // Si el usuario fijó manualmente la moneda origen ante un símbolo '$' ambiguo, se respeta su elección.
            val effectiveSourceCurrency = if (current.isSourceOverriddenByUser && detected.isAmbiguousSymbol) {
                current.sourceCurrency
            } else {
                detected.currency
            }

            // Evitar que moneda origen y destino sean idénticas tras la detección
            val effectiveTargetCurrency = if (effectiveSourceCurrency == current.targetCurrency) {
                if (effectiveSourceCurrency == "COP") "USD" else "COP"
            } else {
                current.targetCurrency
            }

            val updatedPrice = detected.copy(currency = effectiveSourceCurrency)
            recalculateConversion(
                current.copy(
                    detectedPrice = updatedPrice,
                    sourceCurrency = effectiveSourceCurrency,
                    targetCurrency = effectiveTargetCurrency,
                    errorMessage = null
                )
            )
        }
    }

    /**
     * Permite al usuario cambiar rápidamente la moneda origen detectada (ej. cuando el OCR
     * lee '$' y deduce USD pero el precio físico está en COP o MXN).
     */
    fun selectSourceCurrency(currency: String) {
        _uiState.update { current ->
            val adjustedTarget = if (currency == current.targetCurrency) {
                if (currency == "COP") "USD" else "COP"
            } else {
                current.targetCurrency
            }
            val updatedDetected = current.detectedPrice?.copy(currency = currency)
            recalculateConversion(
                current.copy(
                    sourceCurrency = currency,
                    targetCurrency = adjustedTarget,
                    detectedPrice = updatedDetected,
                    isSourceOverriddenByUser = true
                )
            )
        }
    }

    fun selectTargetCurrency(currency: String) {
        _uiState.update { current ->
            recalculateConversion(current.copy(targetCurrency = currency))
        }
    }

    fun togglePause() {
        _uiState.update { current ->
            current.copy(isPaused = !current.isPaused)
        }
    }

    fun setPaused(paused: Boolean) {
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun clearDetection() {
        _uiState.update { current ->
            current.copy(
                detectedPrice = null,
                convertedAmount = null,
                formattedConvertedAmount = null,
                exchangeRate = null,
                isPaused = false
            )
        }
    }

    fun onAnalyzerError(throwable: Throwable) {
        _uiState.update { current ->
            current.copy(errorMessage = throwable.localizedMessage ?: "Error en lectura óptica")
        }
    }

    private fun recalculateConversion(state: ScannerUiState): ScannerUiState {
        val detected = state.detectedPrice ?: return state.copy(
            convertedAmount = null,
            formattedConvertedAmount = null,
            exchangeRate = null
        )

        val rates = state.ratesFromUsd.ifEmpty { FALLBACK_RATES_FROM_USD }
        val sourceRateFromUsd = rates[state.sourceCurrency]?.toBigDecimalOrNull()
            ?: FALLBACK_RATES_FROM_USD[state.sourceCurrency]?.toBigDecimalOrNull()
        val targetRateFromUsd = rates[state.targetCurrency]?.toBigDecimalOrNull()
            ?: FALLBACK_RATES_FROM_USD[state.targetCurrency]?.toBigDecimalOrNull()

        if (sourceRateFromUsd == null || targetRateFromUsd == null || sourceRateFromUsd <= BigDecimal.ZERO) {
            return state.copy(
                convertedAmount = null,
                formattedConvertedAmount = null,
                exchangeRate = null
            )
        }

        val crossRate = targetRateFromUsd.divide(sourceRateFromUsd, 6, RoundingMode.HALF_UP)
        val converted = detected.amount.multiply(crossRate).setScale(2, RoundingMode.HALF_UP)
        val formattedConverted = PricePatternParser.formatAmount(converted)

        return state.copy(
            convertedAmount = converted,
            formattedConvertedAmount = formattedConverted,
            exchangeRate = crossRate.stripTrailingZeros()
        )
    }

    companion object {
        val FALLBACK_RATES_FROM_USD = mapOf(
            "USD" to "1.0",
            "COP" to "4120.00",
            "EUR" to "0.92",
            "MXN" to "19.85",
            "GBP" to "0.79",
            "JPY" to "153.20",
            "CAD" to "1.38",
            "CHF" to "0.88"
        )
    }
}
