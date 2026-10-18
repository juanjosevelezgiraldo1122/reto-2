package com.example.reto2.alerts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reto2.alerts.data.PriceAlertRepository
import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.alerts.model.PriceAlertEvaluator
import com.example.reto2.alerts.model.ThresholdDirection
import com.example.reto2.alerts.notification.AlertNotificationHelper
import com.example.reto2.alerts.worker.PriceAlertScheduler
import com.example.reto2.forex.model.FinnhubRatesRepository
import com.example.reto2.scanner.viewmodel.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * ViewModel para administrar las alertas cambiarias por umbrales y su programación en [WorkManager].
 */
class AlertsViewModel(application: Application) : AndroidViewModel(application) {

    private val alertRepository = PriceAlertRepository(application)
    private val ratesRepository = FinnhubRatesRepository(application)
    private val notificationHelper = AlertNotificationHelper(application)

    private val initialRates = ratesRepository.cached()?.ratesFromUsd?.takeIf { it.isNotEmpty() }
        ?: ScannerViewModel.FALLBACK_RATES_FROM_USD

    private val _uiState = MutableStateFlow(
        withCalculatedPairRate(
            AlertsUiState(
                alerts = alertRepository.getAllAlerts(),
                ratesFromUsd = initialRates,
                hasNotificationPermission = notificationHelper.canPostNotifications()
            )
        )
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        syncSchedulerWithActiveAlerts()
        refreshRates()
    }

    fun refreshPermissionStatus() {
        _uiState.update {
            it.copy(hasNotificationPermission = notificationHelper.canPostNotifications())
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasNotificationPermission = granted && notificationHelper.canPostNotifications(),
                validationMessage = if (!granted) {
                    "Concede el permiso de notificaciones para recibir avisos cuando se cruce un umbral."
                } else {
                    null
                }
            )
        }
    }

    fun selectBaseCurrency(currency: String) {
        _uiState.update { current ->
            val adjustedTarget = if (currency == current.targetCurrency) {
                if (currency == "COP") "USD" else "COP"
            } else {
                current.targetCurrency
            }
            withCalculatedPairRate(
                current.copy(
                    baseCurrency = currency,
                    targetCurrency = adjustedTarget,
                    validationMessage = null,
                    statusBannerMessage = null
                )
            )
        }
    }

    fun selectTargetCurrency(currency: String) {
        _uiState.update { current ->
            val adjustedBase = if (currency == current.baseCurrency) {
                if (currency == "USD") "COP" else "USD"
            } else {
                current.baseCurrency
            }
            withCalculatedPairRate(
                current.copy(
                    baseCurrency = adjustedBase,
                    targetCurrency = currency,
                    validationMessage = null,
                    statusBannerMessage = null
                )
            )
        }
    }

    fun selectDirection(direction: ThresholdDirection) {
        _uiState.update {
            it.copy(direction = direction, validationMessage = null, statusBannerMessage = null)
        }
    }

    fun updateThresholdInput(input: String) {
        if (input.isEmpty() || input.matches(Regex("""^\d{0,10}([.,]\d{0,6})?$"""))) {
            _uiState.update {
                it.copy(thresholdInput = input, validationMessage = null, statusBannerMessage = null)
            }
        }
    }

    fun useCurrentRateAsThreshold() {
        _uiState.update { current ->
            val rateStr = current.currentPairRate?.stripTrailingZeros()?.toPlainString() ?: return@update current
            current.copy(thresholdInput = rateStr, validationMessage = null)
        }
    }

    fun createAlert() {
        val current = _uiState.value
        val threshold = current.thresholdInput.replace(',', '.').toBigDecimalOrNull()

        if (threshold == null || threshold <= BigDecimal.ZERO) {
            _uiState.update { it.copy(validationMessage = "Ingresa un valor de umbral numérico mayor a cero.") }
            return
        }
        if (current.baseCurrency == current.targetCurrency) {
            _uiState.update { it.copy(validationMessage = "Selecciona dos divisas distintas para el par.") }
            return
        }

        val newAlert = PriceAlert(
            baseCurrency = current.baseCurrency,
            targetCurrency = current.targetCurrency,
            targetThreshold = threshold.stripTrailingZeros(),
            direction = current.direction,
            isActive = true,
            lastEvaluatedRate = current.currentPairRate
        )

        alertRepository.saveAlert(newAlert)
        val updatedAlerts = alertRepository.getAllAlerts()

        // Programar PeriodicWorkRequest (15 min, UPDATE) y disparar evaluación inmediata en segundo plano
        PriceAlertScheduler.schedulePeriodicAlerts(getApplication())
        PriceAlertScheduler.triggerImmediateEvaluation(getApplication())

        // Si la tasa actual ya cruza el umbral y tenemos permiso, emitir aviso de inmediato
        val rateNow = current.currentPairRate
        var immediateNotificationSent = false
        if (rateNow != null && PriceAlertEvaluator.isThresholdCrossed(newAlert, rateNow)) {
            immediateNotificationSent = notificationHelper.dispatchThresholdCrossedNotification(newAlert, rateNow)
            if (immediateNotificationSent) {
                alertRepository.recordEvaluation(newAlert.id, rateNow, System.currentTimeMillis())
            }
        }

        _uiState.update {
            it.copy(
                alerts = alertRepository.getAllAlerts(),
                thresholdInput = "",
                isPeriodicWorkScheduled = updatedAlerts.any { a -> a.isActive },
                validationMessage = null,
                statusBannerMessage = if (immediateNotificationSent) {
                    "Alerta creada y disparada: la cotización actual ya cumple tu umbral."
                } else {
                    "Alerta registrada. WorkManager monitoreará el par cada 15 min."
                }
            )
        }
    }

    fun toggleAlertActive(alertId: String, active: Boolean) {
        alertRepository.setAlertActive(alertId, active)
        syncSchedulerWithActiveAlerts()
        _uiState.update {
            val all = alertRepository.getAllAlerts()
            it.copy(
                alerts = all,
                isPeriodicWorkScheduled = all.any { a -> a.isActive }
            )
        }
    }

    fun deleteAlert(alertId: String) {
        alertRepository.deleteAlert(alertId)
        syncSchedulerWithActiveAlerts()
        _uiState.update {
            val all = alertRepository.getAllAlerts()
            it.copy(
                alerts = all,
                isPeriodicWorkScheduled = all.any { a -> a.isActive },
                statusBannerMessage = "Alerta eliminada."
            )
        }
    }

    fun evaluateNow() {
        PriceAlertScheduler.triggerImmediateEvaluation(getApplication())
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { ratesRepository.fetchLatest() }
            }.onSuccess { snapshot ->
                val now = System.currentTimeMillis()
                var triggeredCount = 0
                for (alert in alertRepository.getActiveAlerts()) {
                    val rate = PriceAlertEvaluator.computeCrossRate(
                        snapshot.ratesFromUsd,
                        alert.baseCurrency,
                        alert.targetCurrency
                    ) ?: continue
                    if (PriceAlertEvaluator.isThresholdCrossed(alert, rate)) {
                        if (notificationHelper.dispatchThresholdCrossedNotification(alert, rate)) {
                            triggeredCount++
                            alertRepository.recordEvaluation(alert.id, rate, now)
                        } else {
                            alertRepository.recordEvaluation(alert.id, rate, null)
                        }
                    } else {
                        alertRepository.recordEvaluation(alert.id, rate, null)
                    }
                }
                _uiState.update { current ->
                    withCalculatedPairRate(
                        current.copy(
                            ratesFromUsd = snapshot.ratesFromUsd,
                            alerts = alertRepository.getAllAlerts(),
                            statusBannerMessage = if (triggeredCount > 0) {
                                "Evaluación completada: $triggeredCount alerta(s) notificada(s)."
                            } else {
                                "Cotizaciones actualizadas en lote (1 petición Finnhub). Ningún umbral cruzado."
                            }
                        )
                    )
                }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(
                        alerts = alertRepository.getAllAlerts(),
                        statusBannerMessage = "Evaluación encolada en WorkManager (esperando red estable)."
                    )
                }
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { ratesRepository.fetchLatest() }
            }.onSuccess { snapshot ->
                _uiState.update { current ->
                    withCalculatedPairRate(current.copy(ratesFromUsd = snapshot.ratesFromUsd))
                }
            }
        }
    }

    private fun syncSchedulerWithActiveAlerts() {
        val activeAlerts = alertRepository.getActiveAlerts()
        if (activeAlerts.isNotEmpty()) {
            PriceAlertScheduler.schedulePeriodicAlerts(getApplication())
        } else {
            PriceAlertScheduler.cancelPeriodicAlerts(getApplication())
        }
        _uiState.update {
            it.copy(isPeriodicWorkScheduled = activeAlerts.isNotEmpty())
        }
    }

    private fun withCalculatedPairRate(state: AlertsUiState): AlertsUiState {
        val rates = state.ratesFromUsd.ifEmpty { ScannerViewModel.FALLBACK_RATES_FROM_USD }
        val crossRate = PriceAlertEvaluator.computeCrossRate(
            ratesFromUsd = rates,
            baseCurrency = state.baseCurrency,
            targetCurrency = state.targetCurrency
        )
        return state.copy(currentPairRate = crossRate)
    }
}
