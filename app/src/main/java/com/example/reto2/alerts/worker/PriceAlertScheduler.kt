package com.example.reto2.alerts.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Orquestador de trabajos en segundo plano con [WorkManager] para el monitoreo periódico
 * de tipos de cambio por umbrales en AUREX.
 *
 * Cumple las restricciones de plataforma:
 * - Intervalo mínimo del sistema de 15 minutos ([PeriodicWorkRequestBuilder]).
 * - Política [ExistingPeriodicWorkPolicy.UPDATE].
 * - Restricciones estrictas: [NetworkType.CONNECTED] y `setRequiresBatteryNotLow(true)`.
 * - Política de reintento exponencial ([BackoffPolicy.EXPONENTIAL]).
 */
object PriceAlertScheduler {

    const val UNIQUE_PERIODIC_WORK_NAME = "aurex_periodic_price_alert_work"
    private const val UNIQUE_IMMEDIATE_WORK_NAME = "aurex_immediate_price_alert_work"
    private const val PERIODIC_INTERVAL_MINUTES = 15L

    /**
     * Programa o actualiza el trabajo periódico de 15 minutos con [ExistingPeriodicWorkPolicy.UPDATE].
     */
    fun schedulePeriodicAlerts(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<PriceAlertWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(UNIQUE_PERIODIC_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * Dispara una verificación inmediata en segundo plano cuando el usuario crea una alerta
     * o solicita comprobar las cotizaciones vigentes al instante.
     */
    fun triggerImmediateEvaluation(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<PriceAlertWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    /**
     * Cancela el monitoreo periódico si el usuario elimina o desactiva todas sus alertas.
     */
    fun cancelPeriodicAlerts(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
    }
}
