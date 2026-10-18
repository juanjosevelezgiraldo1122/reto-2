package com.example.reto2.scanner

import com.example.reto2.scanner.model.PricePatternParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PricePatternParserTest {

    @Test
    fun `parses US format with dollar symbol and marks ambiguous symbol`() {
        val result = PricePatternParser.parseBestMatch("Total: $ 1,250.50", defaultDollarCurrency = "USD")
        assertNotNull(result)
        assertEquals(BigDecimal("1250.5"), result!!.amount)
        assertEquals("USD", result.currency)
        assertTrue(result.isAmbiguousSymbol)
    }

    @Test
    fun `parses European format with euro symbol as unambiguous EUR`() {
        val result = PricePatternParser.parseBestMatch("Precio: € 1.250,50", defaultDollarCurrency = "USD")
        assertNotNull(result)
        assertEquals(BigDecimal("1250.5"), result!!.amount)
        assertEquals("EUR", result.currency)
        assertFalse(result.isAmbiguousSymbol)
    }

    @Test
    fun `parses Colombian Peso format with dot thousands separator and explicit COP suffix`() {
        val result = PricePatternParser.parseBestMatch("$ 150.000 COP", defaultDollarCurrency = "USD")
        assertNotNull(result)
        assertEquals(BigDecimal("150000"), result!!.amount)
        assertEquals("COP", result.currency)
        assertFalse(result.isAmbiguousSymbol)
    }

    @Test
    fun `respects user preferred dollar currency when bare dollar sign is scanned`() {
        val result = PricePatternParser.parseBestMatch("$ 45.000", defaultDollarCurrency = "COP")
        assertNotNull(result)
        assertEquals(BigDecimal("45000"), result!!.amount)
        assertEquals("COP", result.currency)
        assertTrue(result.isAmbiguousSymbol)
    }

    @Test
    fun `parses ISO prefix and suffix formats`() {
        val usdResult = PricePatternParser.parseBestMatch("USD 99.95")
        assertNotNull(usdResult)
        assertEquals(BigDecimal("99.95"), usdResult!!.amount)
        assertEquals("USD", usdResult.currency)
        assertFalse(usdResult.isAmbiguousSymbol)

        val eurResult = PricePatternParser.parseBestMatch("49,99 EUR")
        assertNotNull(eurResult)
        assertEquals(BigDecimal("49.99"), eurResult!!.amount)
        assertEquals("EUR", eurResult.currency)
        assertFalse(eurResult.isAmbiguousSymbol)
    }
}
