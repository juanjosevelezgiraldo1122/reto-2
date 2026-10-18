package com.example.reto2.scanner.model

import android.graphics.Rect
import java.math.BigDecimal

/**
 * Representa una denominación o precio físico detectado mediante OCR en tiempo real.
 */
data class DetectedPrice(
    val rawText: String,
    val amount: BigDecimal,
    val currency: String,
    val formattedAmount: String,
    val isAmbiguousSymbol: Boolean = false,
    val boundingBox: Rect? = null,
    val detectedAtMillis: Long = System.currentTimeMillis()
)
