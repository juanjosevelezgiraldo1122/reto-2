package com.example.reto2.alerts.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Dirección del umbral de disparo para una alerta cambiaria.
 */
enum class ThresholdDirection(val label: String, val symbol: String) {
    ABOVE("Suba por encima de (≥)", "↗"),
    BELOW("Baje por debajo de (≤)", "↘")
}

/**
 * Entidad de dominio para una alerta de tipo de cambio por umbral en AUREX.
 */
data class PriceAlert(
    val id: String = UUID.randomUUID().toString(),
    val baseCurrency: String,
    val targetCurrency: String,
    val targetThreshold: BigDecimal,
    val direction: ThresholdDirection,
    val isActive: Boolean = true,
    val lastNotifiedAt: Long? = null,
    val lastEvaluatedRate: BigDecimal? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val pairLabel: String
        get() = "$baseCurrency / $targetCurrency"
}

/**
 * Motor puro de evaluación en memoria para reglas de alertas cambiarias.
 * Permite evaluar en lote (batching) todas las alertas activas contra un único snapshot de tasas.
 */
object PriceAlertEvaluator {

    /**
     * Calcula la tasa cruzada entre [baseCurrency] y [targetCurrency] a partir del mapa de tasas base USD.
     */
    fun computeCrossRate(
        ratesFromUsd: Map<String, String>,
        baseCurrency: String,
        targetCurrency: String
    ): BigDecimal? {
        val baseRate = ratesFromUsd[baseCurrency]?.toBigDecimalOrNull() ?: return null
        val targetRate = ratesFromUsd[targetCurrency]?.toBigDecimalOrNull() ?: return null
        if (baseRate <= BigDecimal.ZERO || targetRate <= BigDecimal.ZERO) return null
        return targetRate.divide(baseRate, 6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .let { if (it.scale() < 0) it.setScale(0) else it }
    }

    /**
     * Determina si la cotización actual [currentRate] cumple la regla de disparo de [alert].
     */
    fun isThresholdCrossed(
        alert: PriceAlert,
        currentRate: BigDecimal
    ): Boolean {
        return when (alert.direction) {
            ThresholdDirection.ABOVE -> currentRate.compareTo(alert.targetThreshold) >= 0
            ThresholdDirection.BELOW -> currentRate.compareTo(alert.targetThreshold) <= 0
        }
    }

    /**
     * Verifica si la alerta puede emitir una nueva notificación respetando la ventana de enfriamiento
     * para evitar notificaciones duplicadas continuas en el mismo ciclo.
     */
    fun shouldNotify(
        alert: PriceAlert,
        currentRate: BigDecimal,
        nowMillis: Long = System.currentTimeMillis(),
        cooldownMillis: Long = DEFAULT_NOTIFICATION_COOLDOWN_MS
    ): Boolean {
        if (!alert.isActive) return false
        if (!isThresholdCrossed(alert, currentRate)) return false
        val lastNotified = alert.lastNotifiedAt ?: return true
        return (nowMillis - lastNotified) >= cooldownMillis
    }

    const val DEFAULT_NOTIFICATION_COOLDOWN_MS = 30 * 60 * 1_000L // 30 minutos
}
