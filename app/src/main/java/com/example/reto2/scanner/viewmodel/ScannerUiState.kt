package com.example.reto2.scanner.viewmodel

import com.example.reto2.scanner.model.DetectedPrice
import java.math.BigDecimal

/**
 * Estado inmutable de UI para el escáner y conversor de precios físicos en tiempo real.
 */
data class ScannerUiState(
    val hasCameraPermission: Boolean = false,
    val isPaused: Boolean = false,
    val detectedPrice: DetectedPrice? = null,
    val sourceCurrency: String = "USD",
    val isSourceOverriddenByUser: Boolean = false,
    val targetCurrency: String = "COP",
    val convertedAmount: BigDecimal? = null,
    val formattedConvertedAmount: String? = null,
    val exchangeRate: BigDecimal? = null,
    val ratesFromUsd: Map<String, String> = emptyMap(),
    val availableCurrencies: List<String> = listOf("USD", "COP", "EUR", "MXN", "GBP", "JPY", "CAD", "CHF"),
    val errorMessage: String? = null
)
