package com.example.reto2.alerts.data

import android.content.Context
import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.forex.model.AppDataDatabase
import java.math.BigDecimal

/**
 * Repositorio para gestionar la persistencia local de alertas de tipos de cambio por umbrales.
 */
class PriceAlertRepository(context: Context) {

    private val database = AppDataDatabase(context.applicationContext)

    @Synchronized
    fun getAllAlerts(): List<PriceAlert> = database.getAllPriceAlerts()

    @Synchronized
    fun getActiveAlerts(): List<PriceAlert> = database.getActivePriceAlerts()

    @Synchronized
    fun saveAlert(alert: PriceAlert) {
        database.insertPriceAlert(alert)
    }

    @Synchronized
    fun setAlertActive(alertId: String, isActive: Boolean) {
        database.setPriceAlertActive(alertId, isActive)
    }

    @Synchronized
    fun recordEvaluation(alertId: String, currentRate: BigDecimal, notifiedAtMillis: Long?) {
        database.updatePriceAlertEvaluation(alertId, currentRate, notifiedAtMillis)
    }

    @Synchronized
    fun deleteAlert(alertId: String) {
        database.deletePriceAlert(alertId)
    }
}
