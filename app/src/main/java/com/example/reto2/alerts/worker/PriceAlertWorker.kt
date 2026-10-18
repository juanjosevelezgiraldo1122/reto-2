package com.example.reto2.alerts.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reto2.alerts.data.PriceAlertRepository
import com.example.reto2.alerts.model.PriceAlertEvaluator
import com.example.reto2.alerts.notification.AlertNotificationHelper
import com.example.reto2.forex.model.FinnhubRatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Worker en segundo plano basado en corrutinas ([CoroutineWorker]) encargado de monitorear
 * las cotizaciones cambiarias frente a los umbrales configurados por el usuario.
 *
 * Estrategia de eficiencia (Batching):
 * - Si no existen alertas activas, finaliza de inmediato sin realizar peticiones de red.
 * - Si hay N alertas activas (ej. USD/COP, EUR/USD, GBP/EUR), ejecuta UNA ÚNICA consulta
 *   HTTP a Finnhub API (`fetchLatest()`) y evalúa todas las reglas locales en memoria.
 * - Ante fallos transitorios de red o límite temporal de tasa (HTTP 429/5xx), retorna [Result.retry]
 *   respetando la política de backoff exponencial.
 */
class PriceAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val alertRepository = PriceAlertRepository(applicationContext)
        val activeAlerts = alertRepository.getActiveAlerts()

        if (activeAlerts.isEmpty()) {
            return@withContext Result.success()
        }

        val ratesRepository = FinnhubRatesRepository(applicationContext)
        val notificationHelper = AlertNotificationHelper(applicationContext)

        try {
            // Consulta única en lote (batch) de todas las divisas frente a USD usando FINNHUB_API_KEY
            val snapshot = ratesRepository.fetchLatest()
            val ratesFromUsd = snapshot.ratesFromUsd
            val nowMillis = System.currentTimeMillis()

            for (alert in activeAlerts) {
                val currentRate = PriceAlertEvaluator.computeCrossRate(
                    ratesFromUsd = ratesFromUsd,
                    baseCurrency = alert.baseCurrency,
                    targetCurrency = alert.targetCurrency
                ) ?: continue

                val shouldTriggerNotification = PriceAlertEvaluator.shouldNotify(
                    alert = alert,
                    currentRate = currentRate,
                    nowMillis = nowMillis
                )

                if (shouldTriggerNotification) {
                    val posted = notificationHelper.dispatchThresholdCrossedNotification(
                        alert = alert,
                        currentRate = currentRate
                    )
                    alertRepository.recordEvaluation(
                        alertId = alert.id,
                        currentRate = currentRate,
                        notifiedAtMillis = if (posted) nowMillis else alert.lastNotifiedAt
                    )
                } else {
                    alertRepository.recordEvaluation(
                        alertId = alert.id,
                        currentRate = currentRate,
                        notifiedAtMillis = null
                    )
                }
            }

            Result.success()
        } catch (ioException: IOException) {
            // Fallo de conectividad transitorio: reintentar con backoff exponencial
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (stateException: IllegalStateException) {
            val message = stateException.message.orEmpty()
            // Si falta el token o no hay permisos (401/403), no tiene sentido reintentar indefinidamente
            val isPermanentAuthError = message.contains("Falta configurar") ||
                message.contains("validar el acceso") ||
                message.contains("permiso")
            if (!isPermanentAuthError && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
    }
}
