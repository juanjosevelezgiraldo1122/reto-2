package com.example.reto2.forex.model

import android.content.Context
import com.example.reto2.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FxRatesSnapshot(
    val ratesFromUsd: Map<String, String>,
    val fetchedAtMillis: Long
)

class FinnhubRatesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("fx_rates_cache", Context.MODE_PRIVATE)

    fun cached(): FxRatesSnapshot? {
        val json = preferences.getString("snapshot", null) ?: return null
        return runCatching {
            val root = JSONObject(json)
            val rates = root.getJSONObject("rates")
            buildMap { rates.keys().forEach { key -> put(key, rates.getString(key)) } }
                .let { FxRatesSnapshot(it, root.getLong("fetchedAt")) }
        }.getOrNull()
    }

    fun fetchLatest(): FxRatesSnapshot {
        check(BuildConfig.FINNHUB_TOKEN.isNotBlank()) { "Falta configurar la clave de cotizaciones." }
        val connection = (URL("https://finnhub.io/api/v1/forex/rates?base=USD").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("X-Finnhub-Token", BuildConfig.FINNHUB_TOKEN)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) {
                when (code) {
                    401 -> "No se pudo validar el acceso a las cotizaciones."
                    403 -> "Tu plan de datos no tiene permiso para consultar cotizaciones."
                    429 -> "Se alcanzó el límite de consultas. Inténtalo más tarde."
                    else -> "El servicio de cotizaciones respondió con error ($code)."
                }
            }
            val payload = JSONObject(body)
            val quotes = payload.optJSONObject("quote") ?: error("Respuesta de tasas incompleta")
            val rates = mutableMapOf("USD" to "1")
            val keys = quotes.keys()
            while (keys.hasNext()) {
                val pair = keys.next()
                if (pair.startsWith("USD") && pair.length == 6) {
                    val currency = pair.takeLast(3)
                    val rate = quotes.optDouble(pair, Double.NaN)
                    if (rate.isFinite() && rate > 0.0) rates[currency] = rate.toString()
                }
            }
            check(rates.size > 1) { "El servicio no devolvió tasas de cambio." }
            val snapshot = FxRatesSnapshot(rates, System.currentTimeMillis())
            preferences.edit().putString("snapshot", JSONObject().apply {
                put("fetchedAt", snapshot.fetchedAtMillis)
                put("rates", JSONObject(snapshot.ratesFromUsd))
            }.toString()).apply()
            return snapshot
        } finally {
            connection.disconnect()
        }
    }
}
