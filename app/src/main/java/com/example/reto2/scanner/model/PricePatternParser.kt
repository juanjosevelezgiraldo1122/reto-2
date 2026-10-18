package com.example.reto2.scanner.model

import android.graphics.Rect
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Motor de extracción de precios y denominaciones monetarias mediante expresiones regulares.
 * Soporta formatos mixtos anglosajones (1,250.50) y europeos/latinoamericanos (1.250,50),
 * así como símbolos ($, €, £, ¥) y códigos ISO 4217 (USD, EUR, COP, GBP, JPY, CAD, CHF, MXN).
 */
object PricePatternParser {

    /**
     * Expresión regular que captura:
     * 1. Prefijo opcional de moneda o símbolo (ej. "USD", "US$", "COL$", "$", "€", "£", "¥", "EUR", "COP").
     * 2. Cuerpo numérico con separadores de miles (. o ,) y decimales opcionales (. o ,).
     * 3. Sufijo opcional de moneda o símbolo (ej. "100 USD", "49,99 €", "50.000 COP").
     */
    private val PRICE_REGEX = Regex(
        pattern = """(?i)(?:(?<prefix>US\$|COL\$|COP\$|MX\$|CA\$|USD|EUR|COP|GBP|JPY|CAD|CHF|MXN|[\$€£¥])\s*)?(?<number>\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)(?:\s*(?<suffix>USD|EUR|COP|GBP|JPY|CAD|CHF|MXN|[\$€£¥]))?"""
    )

    private val SUPPORTED_ISO_CODES = setOf("USD", "EUR", "COP", "GBP", "JPY", "CAD", "CHF", "MXN")

    /**
     * Analiza un bloque de texto crudo proveniente de ML Kit y retorna la mejor coincidencia monetaria.
     *
     * @param text Texto completo o línea reconocida por OCR.
     * @param defaultDollarCurrency Moneda por defecto cuando se detecta el símbolo ambiguo '$' (por defecto "USD").
     * @param boundingBox Caja delimitadora opcional asociada al bloque de texto.
     */
    fun parseBestMatch(
        text: String,
        defaultDollarCurrency: String = "USD",
        boundingBox: Rect? = null
    ): DetectedPrice? {
        if (text.isBlank()) return null

        val candidates = parseAllMatches(text, defaultDollarCurrency, boundingBox)
        if (candidates.isEmpty()) return null

        // Prioriza coincidencias que incluyan un símbolo o código de divisa explícito en el texto original,
        // y luego aquellas con mayor valor numérico significativo (como el total de una factura o billete).
        return candidates.maxWithOrNull(
            compareBy<CandidateMatch> { it.hasExplicitCurrencyIndicator }
                .thenBy { it.hasDecimalPart }
                .thenBy { it.price.amount }
        )?.price
    }

    /**
     * Extrae todas las coincidencias válidas encontradas en el texto.
     */
    fun parseAllMatches(
        text: String,
        defaultDollarCurrency: String = "USD",
        boundingBox: Rect? = null
    ): List<CandidateMatch> {
        val cleanedText = text.replace("\n", " ").trim()
        val matches = PRICE_REGEX.findAll(cleanedText)
        val results = mutableListOf<CandidateMatch>()

        for (match in matches) {
            val rawPrefix = match.groups["prefix"]?.value?.trim().orEmpty()
            val rawNumber = match.groups["number"]?.value?.trim().orEmpty()
            val rawSuffix = match.groups["suffix"]?.value?.trim().orEmpty()

            if (rawNumber.isEmpty()) continue

            val amount = normalizeNumericAmount(rawNumber) ?: continue
            // Descartar valores nulos, negativos o años/códigos irrelevantes demasiado grandes sin formato
            if (amount <= BigDecimal.ZERO || amount > BigDecimal("999999999")) continue

            val hasExplicitMarker = rawPrefix.isNotEmpty() || rawSuffix.isNotEmpty()
            val currencyResolution = resolveCurrency(
                prefix = rawPrefix,
                suffix = rawSuffix,
                defaultDollarCurrency = defaultDollarCurrency
            )

            val hasDecimals = rawNumber.contains('.') || rawNumber.contains(',')
            val formatted = formatAmount(amount)

            val detectedPrice = DetectedPrice(
                rawText = match.value.trim(),
                amount = amount,
                currency = currencyResolution.isoCode,
                formattedAmount = formatted,
                isAmbiguousSymbol = currencyResolution.isAmbiguousDollar,
                boundingBox = boundingBox
            )

            results.add(
                CandidateMatch(
                    price = detectedPrice,
                    hasExplicitCurrencyIndicator = hasExplicitMarker,
                    hasDecimalPart = hasDecimals
                )
            )
        }

        return results
    }

    /**
     * Convierte una cadena numérica con formato mixto (puntos y comas) en un [BigDecimal] limpio.
     * Ejemplos soportados:
     * - "1,250.50" -> 1250.50
     * - "1.250,50" -> 1250.50
     * - "150.000"  -> 150000
     * - "150,000"  -> 150000
     * - "49,99"    -> 49.99
     * - "49.99"    -> 49.99
     * - "100"      -> 100
     */
    fun normalizeNumericAmount(rawNumber: String): BigDecimal? {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return null

        val lastDot = trimmed.lastIndexOf('.')
        val lastComma = trimmed.lastIndexOf(',')

        val normalized = when {
            // Contiene tanto puntos como comas (ej. "1,250.50" o "1.250,50")
            lastDot != -1 && lastComma != -1 -> {
                if (lastComma > lastDot) {
                    // Formato europeo/latino: punto para miles, coma para decimales ("1.250,50")
                    trimmed.replace(".", "").replace(',', '.')
                } else {
                    // Formato anglosajón: coma para miles, punto para decimales ("1,250.50")
                    trimmed.replace(",", "")
                }
            }
            // Solo contiene comas
            lastComma != -1 -> {
                val commaCount = trimmed.count { it == ',' }
                val digitsAfterLastComma = trimmed.length - lastComma - 1
                if (commaCount > 1) {
                    // Múltiples comas son separadores de miles ("1,000,000")
                    trimmed.replace(",", "")
                } else if (digitsAfterLastComma in 1..2) {
                    // 1 o 2 dígitos tras la única coma: separador decimal ("49,99")
                    trimmed.replace(',', '.')
                } else {
                    // 3 dígitos tras la coma: separador de miles ("1,000" o "25,000")
                    trimmed.replace(",", "")
                }
            }
            // Solo contiene puntos
            lastDot != -1 -> {
                val dotCount = trimmed.count { it == '.' }
                val digitsAfterLastDot = trimmed.length - lastDot - 1
                if (dotCount > 1) {
                    // Múltiples puntos son separadores de miles ("1.000.000")
                    trimmed.replace(".", "")
                } else if (digitsAfterLastDot in 1..2) {
                    // 1 o 2 dígitos tras el único punto: separador decimal ("49.99")
                    trimmed
                } else {
                    // 3 dígitos tras el punto: separador de miles ("50.000")
                    trimmed.replace(".", "")
                }
            }
            // Número entero puro
            else -> trimmed
        }

        return runCatching {
            BigDecimal(normalized).stripTrailingZeros().let {
                if (it.scale() < 0) it.setScale(0) else it
            }
        }.getOrNull()
    }

    private data class CurrencyResolution(
        val isoCode: String,
        val isAmbiguousDollar: Boolean
    )

    private fun resolveCurrency(
        prefix: String,
        suffix: String,
        defaultDollarCurrency: String
    ): CurrencyResolution {
        val p = prefix.uppercase(Locale.ROOT)
        val s = suffix.uppercase(Locale.ROOT)

        // Si el sufijo es un código ISO explícito (ej. "$ 50.000 COP" o "$ 20 USD"), manda el código ISO
        if (s in SUPPORTED_ISO_CODES) {
            return CurrencyResolution(isoCode = s, isAmbiguousDollar = false)
        }
        if (p in SUPPORTED_ISO_CODES) {
            return CurrencyResolution(isoCode = p, isAmbiguousDollar = false)
        }

        val token = p.ifEmpty { s }
        return when (token) {
            "US$" -> CurrencyResolution(isoCode = "USD", isAmbiguousDollar = false)
            "COL$", "COP$" -> CurrencyResolution(isoCode = "COP", isAmbiguousDollar = false)
            "MX$" -> CurrencyResolution(isoCode = "MXN", isAmbiguousDollar = false)
            "CA$" -> CurrencyResolution(isoCode = "CAD", isAmbiguousDollar = false)
            "€" -> CurrencyResolution(isoCode = "EUR", isAmbiguousDollar = false)
            "£" -> CurrencyResolution(isoCode = "GBP", isAmbiguousDollar = false)
            "¥" -> CurrencyResolution(isoCode = "JPY", isAmbiguousDollar = false)
            "$" -> CurrencyResolution(isoCode = defaultDollarCurrency, isAmbiguousDollar = true)
            else -> CurrencyResolution(isoCode = defaultDollarCurrency, isAmbiguousDollar = true)
        }
    }

    fun formatAmount(amount: BigDecimal): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val pattern = if (amount.scale() > 0) "#,##0.00" else "#,##0"
        return DecimalFormat(pattern, symbols).format(amount.setScale(amount.scale().coerceAtMost(2), RoundingMode.HALF_UP))
    }

    data class CandidateMatch(
        val price: DetectedPrice,
        val hasExplicitCurrencyIndicator: Boolean,
        val hasDecimalPart: Boolean
    )
}
