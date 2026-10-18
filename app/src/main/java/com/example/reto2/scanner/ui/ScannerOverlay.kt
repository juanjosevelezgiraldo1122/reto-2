package com.example.reto2.scanner.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

private val MintAccent = Color(0xFF45C7A5)
private val PausedAmber = Color(0xFFF5A623)
private val ScrimColor = Color(0x990A0F1D)

/**
 * Superposición gráfica en [Canvas] para el visor de cámara de AUREX.
 * Dibuja una máscara oscurecida exterior, una caja de enfoque central con esquinas marcadas
 * y un haz de escaneo animado cuando la lectura está activa.
 */
@Composable
fun ScannerOverlay(
    isPaused: Boolean,
    hasDetection: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_beam_transition")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_progress"
    )

    val activeColor = when {
        isPaused -> PausedAmber
        hasDetection -> MintAccent
        else -> Color.White
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val boxWidth = size.width * 0.82f
        val boxHeight = (size.height * 0.25f).coerceIn(160.dp.toPx(), 260.dp.toPx())
        val left = (size.width - boxWidth) / 2f
        // Ubicamos la caja ligeramente por encima del centro para dejar espacio a la tarjeta flotante inferior
        val top = (size.height * 0.30f) - (boxHeight / 2f)
        val right = left + boxWidth
        val bottom = top + boxHeight
        val cornerRadiusPx = 22.dp.toPx()

        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        // 1. Fondo semitransparente exterior con recorte central limpio
        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = ScrimColor)
        }

        // 2. Borde sutil alrededor de la caja de enfoque
        drawRoundRect(
            color = activeColor.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 3. Esquinas marcadas (brackets en L de alta visibilidad)
        val cornerLength = 32.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        val cornersPath = Path().apply {
            // Esquina superior izquierda
            moveTo(left, top + cornerLength)
            lineTo(left, top + cornerRadiusPx / 2f)
            quadraticTo(left, top, left + cornerRadiusPx / 2f, top)
            lineTo(left + cornerLength, top)

            // Esquina superior derecha
            moveTo(right - cornerLength, top)
            lineTo(right - cornerRadiusPx / 2f, top)
            quadraticTo(right, top, right, top + cornerRadiusPx / 2f)
            lineTo(right, top + cornerLength)

            // Esquina inferior izquierda
            moveTo(left, bottom - cornerLength)
            lineTo(left, bottom - cornerRadiusPx / 2f)
            quadraticTo(left, bottom, left + cornerRadiusPx / 2f, bottom)
            lineTo(left + cornerLength, bottom)

            // Esquina inferior derecha
            moveTo(right - cornerLength, bottom)
            lineTo(right - cornerRadiusPx / 2f, bottom)
            quadraticTo(right, bottom, right, bottom - cornerRadiusPx / 2f)
            lineTo(right, bottom - cornerLength)
        }

        drawPath(
            path = cornersPath,
            color = activeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 4. Retícula central discreta
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val crosshairSize = 8.dp.toPx()
        drawLine(
            color = activeColor.copy(alpha = 0.4f),
            start = Offset(centerX - crosshairSize, centerY),
            end = Offset(centerX + crosshairSize, centerY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = activeColor.copy(alpha = 0.4f),
            start = Offset(centerX, centerY - crosshairSize),
            end = Offset(centerX, centerY + crosshairSize),
            strokeWidth = 1.dp.toPx()
        )

        // 5. Línea láser animada cuando el escáner está activo
        if (!isPaused) {
            val scanY = top + 16.dp.toPx() + (boxHeight - 32.dp.toPx()) * scanProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MintAccent.copy(alpha = 0.9f),
                        Color(0xFF8DE8D0),
                        MintAccent.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    startX = left + 12.dp.toPx(),
                    endX = right - 12.dp.toPx()
                ),
                start = Offset(left + 12.dp.toPx(), scanY),
                end = Offset(right - 12.dp.toPx(), scanY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
