package com.example.reto2.scanner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.scanner.analyzer.PriceOcrAnalyzer
import com.example.reto2.scanner.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

private val Navy = Color(0xFF10172B)
private val Blue = Color(0xFF5046E5)
private val Mint = Color(0xFF8DE8D0)

/**
 * Pantalla completa del Escáner y Conversor de Billetes/Precios Físicos en Tiempo Real.
 * Integra CameraX ([PreviewView] + [ImageAnalysis] con [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]),
 * Google ML Kit Text Recognition, superposición gráfica en [ScannerOverlay] y tarjeta interactiva [PriceResultCard].
 */
@Composable
fun ScannerScreen(
    initialTargetCurrency: String = "COP",
    onClose: () -> Unit,
    onTransferToConverter: (amount: String, sourceCurrency: String, targetCurrency: String) -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.selectTargetCurrency(initialTargetCurrency)
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
    ) {
        if (state.hasCameraPermission) {
            CameraPreviewWithOcr(
                isPaused = state.isPaused,
                preferredDollarCurrency = state.sourceCurrency,
                onPriceDetected = viewModel::onPriceDetected,
                onError = viewModel::onAnalyzerError
            )

            ScannerOverlay(
                isPaused = state.isPaused,
                hasDetection = state.detectedPrice != null
            )
        } else {
            CameraPermissionDeniedContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
        }

        // Barra superior flotante
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Navy.copy(alpha = 0.82f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Mint, CircleShape)
                    )
                    Column {
                        Text(
                            text = "AUREX VISION OCR",
                            color = Mint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Escáner de precios y billetes",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = Navy.copy(alpha = 0.82f)
            ) {
                TextButton(onClick = onClose) {
                    Text(
                        text = "✕ Cerrar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tarjeta flotante inferior interactiva Material 3
        PriceResultCard(
            state = state,
            onSelectSourceCurrency = viewModel::selectSourceCurrency,
            onSelectTargetCurrency = viewModel::selectTargetCurrency,
            onTogglePause = viewModel::togglePause,
            onClearDetection = viewModel::clearDetection,
            onTransferToConverter = onTransferToConverter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )
    }
}

@Composable
private fun CameraPreviewWithOcr(
    isPaused: Boolean,
    preferredDollarCurrency: String,
    onPriceDetected: (com.example.reto2.scanner.model.DetectedPrice) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val ocrAnalyzer = remember {
        PriceOcrAnalyzer(
            onPriceDetected = onPriceDetected,
            onError = onError
        )
    }

    LaunchedEffect(isPaused) {
        ocrAnalyzer.setPaused(isPaused)
    }

    LaunchedEffect(preferredDollarCurrency) {
        ocrAnalyzer.setPreferredDollarCurrency(preferredDollarCurrency)
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        onDispose {
            runCatching {
                cameraProviderFuture.get()?.unbindAll()
            }
            ocrAnalyzer.release()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener(
                {
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(analysisExecutor, ocrAnalyzer)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }.onFailure(onError)
                },
                ContextCompat.getMainExecutor(ctx)
            )

            previewView
        }
    )
}

@Composable
private fun CameraPermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Blue.copy(alpha = 0.18f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "📷",
                    fontSize = 32.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Permiso de cámara requerido",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Para escanear billetes y etiquetas de precios físicos en tiempo real con AUREX Vision, concede acceso a la cámara de tu dispositivo.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Conceder permiso de cámara",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClose) {
            Text(
                text = "Volver al convertidor",
                color = Mint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
