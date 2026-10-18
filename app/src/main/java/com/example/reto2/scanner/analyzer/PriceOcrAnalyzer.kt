package com.example.reto2.scanner.analyzer

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.reto2.scanner.model.DetectedPrice
import com.example.reto2.scanner.model.PricePatternParser
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Analizador de frames de CameraX que ejecuta reconocimiento óptico de caracteres (OCR)
 * mediante Google ML Kit Text Recognition.
 *
 * Características clave:
 * - Diseñado para operar con [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST].
 * - Vincula estrictamente [imageProxy.imageInfo.rotationDegrees] al construir [InputImage].
 * - Garantiza la liberación del buffer de cámara invocando siempre [ImageProxy.close] en bloque `finally`.
 * - Aplica estrangulamiento (throttling) de inferencia para proteger el hilo de análisis y la batería.
 */
class PriceOcrAnalyzer(
    private val throttleIntervalMs: Long = DEFAULT_THROTTLE_INTERVAL_MS,
    private val onPriceDetected: (DetectedPrice) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val lastAnalyzedTimestamp = AtomicLong(0L)
    private val isPaused = AtomicBoolean(false)
    private val preferredDollarCurrency = AtomicReference("USD")

    fun setPaused(paused: Boolean) {
        isPaused.set(paused)
    }

    fun setPreferredDollarCurrency(currencyCode: String) {
        preferredDollarCurrency.set(currencyCode)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (isPaused.get()) {
                return
            }

            val now = SystemClock.elapsedRealtime()
            val previous = lastAnalyzedTimestamp.get()
            if (now - previous < throttleIntervalMs) {
                return
            }

            val mediaImage = imageProxy.image ?: return
            lastAnalyzedTimestamp.set(now)

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            // Se ejecuta sincrónicamente sobre el Executor dedicado de ImageAnalysis para que
            // el bloque `finally` cierre el ImageProxy únicamente cuando ML Kit haya terminado de leer el buffer.
            val visionText = Tasks.await(
                recognizer.process(inputImage),
                INFERENCE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )

            if (visionText != null && visionText.text.isNotBlank()) {
                val defaultDollar = preferredDollarCurrency.get()
                // Primero evaluamos bloque por bloque para preservar su caja delimitadora (boundingBox)
                var bestDetection: DetectedPrice? = null

                for (block in visionText.textBlocks) {
                    val parsed = PricePatternParser.parseBestMatch(
                        text = block.text,
                        defaultDollarCurrency = defaultDollar,
                        boundingBox = block.boundingBox
                    )
                    if (parsed != null) {
                        if (bestDetection == null || parsed.amount > bestDetection.amount) {
                            bestDetection = parsed
                        }
                    }
                }

                // Fallback al texto global si la expresión estaba dividida entre bloques adyacentes
                if (bestDetection == null) {
                    bestDetection = PricePatternParser.parseBestMatch(
                        text = visionText.text,
                        defaultDollarCurrency = defaultDollar
                    )
                }

                if (bestDetection != null && !isPaused.get()) {
                    onPriceDetected(bestDetection)
                }
            }
        } catch (t: Throwable) {
            onError(t)
        } finally {
            imageProxy.close()
        }
    }

    fun release() {
        runCatching { recognizer.close() }
    }

    companion object {
        const val DEFAULT_THROTTLE_INTERVAL_MS = 600L
        private const val INFERENCE_TIMEOUT_MS = 1_500L
    }
}
