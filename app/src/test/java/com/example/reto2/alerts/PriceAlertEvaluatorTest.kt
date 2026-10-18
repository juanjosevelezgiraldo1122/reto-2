package com.example.reto2.alerts

import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.alerts.model.PriceAlertEvaluator
import com.example.reto2.alerts.model.ThresholdDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PriceAlertEvaluatorTest {

    private val sampleFinnhubRatesFromUsd = mapOf(
        "USD" to "1",
        "COP" to "4150.00",
        "EUR" to "0.92",
        "GBP" to "0.79"
    )

    @Test
    fun `computes direct and cross rates accurately from single USD snapshot`() {
        val usdCop = PriceAlertEvaluator.computeCrossRate(sampleFinnhubRatesFromUsd, "USD", "COP")
        assertNotNull(usdCop)
        assertEquals(BigDecimal("4150"), usdCop)

        // 1 EUR = 1 / 0.92 USD = 1.086957 USD
        val eurUsd = PriceAlertEvaluator.computeCrossRate(sampleFinnhubRatesFromUsd, "EUR", "USD")
        assertNotNull(eurUsd)
        assertEquals(BigDecimal("1.086957"), eurUsd)
    }

    @Test
    fun `evaluates ABOVE and BELOW threshold rules accurately`() {
        val aboveAlert = PriceAlert(
            baseCurrency = "USD",
            targetCurrency = "COP",
            targetThreshold = BigDecimal("4100"),
            direction = ThresholdDirection.ABOVE
        )

        assertTrue(PriceAlertEvaluator.isThresholdCrossed(aboveAlert, BigDecimal("4150")))
        assertTrue(PriceAlertEvaluator.isThresholdCrossed(aboveAlert, BigDecimal("4100")))
        assertFalse(PriceAlertEvaluator.isThresholdCrossed(aboveAlert, BigDecimal("4050")))

        val belowAlert = PriceAlert(
            baseCurrency = "USD",
            targetCurrency = "COP",
            targetThreshold = BigDecimal("4000"),
            direction = ThresholdDirection.BELOW
        )

        assertTrue(PriceAlertEvaluator.isThresholdCrossed(belowAlert, BigDecimal("3950")))
        assertTrue(PriceAlertEvaluator.isThresholdCrossed(belowAlert, BigDecimal("4000")))
        assertFalse(PriceAlertEvaluator.isThresholdCrossed(belowAlert, BigDecimal("4050")))
    }

    @Test
    fun `respects notification cooldown window to prevent duplicate alert spam`() {
        val now = 1_700_000_000_000L
        val recentlyNotifiedAlert = PriceAlert(
            baseCurrency = "USD",
            targetCurrency = "COP",
            targetThreshold = BigDecimal("4100"),
            direction = ThresholdDirection.ABOVE,
            isActive = true,
            lastNotifiedAt = now - 5 * 60 * 1_000L // hace 5 minutos
        )

        assertFalse(
            PriceAlertEvaluator.shouldNotify(
                alert = recentlyNotifiedAlert,
                currentRate = BigDecimal("4150"),
                nowMillis = now
            )
        )

        val cooldownExpiredAlert = recentlyNotifiedAlert.copy(
            lastNotifiedAt = now - 45 * 60 * 1_000L // hace 45 minutos
        )

        assertTrue(
            PriceAlertEvaluator.shouldNotify(
                alert = cooldownExpiredAlert,
                currentRate = BigDecimal("4150"),
                nowMillis = now
            )
        )
    }
}
