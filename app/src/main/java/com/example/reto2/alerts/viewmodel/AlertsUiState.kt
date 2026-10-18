package com.example.reto2.alerts.viewmodel

import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.alerts.model.ThresholdDirection
import java.math.BigDecimal

/**
 * Estado inmutable de UI para el módulo de alertas de tipos de cambio por umbrales en AUREX.
 */
data class AlertsUiState(
    val alerts: List<PriceAlert> = emptyList(),
    val baseCurrency: String = "USD",
    val targetCurrency: String = "COP",
    val thresholdInput: String = "",
    val direction: ThresholdDirection = ThresholdDirection.ABOVE,
    val currentPairRate: BigDecimal? = null,
    val ratesFromUsd: Map<String, String> = emptyMap(),
    val availableCurrencies: List<String> = listOf("USD", "COP", "EUR", "MXN", "GBP", "JPY", "CAD", "CHF"),
    val hasNotificationPermission: Boolean = false,
    val isPeriodicWorkScheduled: Boolean = false,
    val validationMessage: String? = null,
    val statusBannerMessage: String? = null
)
