package com.example.reto2.alerts.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.alerts.model.PriceAlertEvaluator
import com.example.reto2.alerts.model.ThresholdDirection
import com.example.reto2.alerts.viewmodel.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF10172B)
private val Blue = Color(0xFF5046E5)
private val Teal = Color(0xFF45C7A5)
private val Mint = Color(0xFF8DE8D0)
private val SecondaryText = Color(0xFF7B8499)
private val LightBg = Color(0xFFF3F5FA)
private val DangerRed = Color(0xFFB04444)

/**
 * Vista de gestión del Sistema en Segundo Plano de Alertas de Tipos de Cambio por Umbrales
 * (WorkManager + NotificationManager + Finnhub API).
 */
@Composable
fun AlertsSectionView(
    viewModel: AlertsViewModel = viewModel(),
    onOpenPairInConverter: (baseCurrency: String, targetCurrency: String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onNotificationPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera de estado del motor WorkManager en segundo plano
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Navy,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    color = if (state.isPeriodicWorkScheduled) Mint else SecondaryText,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (state.isPeriodicWorkScheduled) {
                                "WORKMANAGER ACTIVO · CADA 15 MIN"
                            } else {
                                "MONITOREO EN ESPERA"
                            },
                            color = Mint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.9.sp
                        )
                    }

                    TextButton(onClick = viewModel::evaluateNow) {
                        Text(
                            text = "Evaluar ahora ↻",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Alertas de umbrales con Finnhub Batching",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restricciones activas: Red conectada + Batería no baja + Backoff exponencial.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Banner declarativo para permiso POST_NOTIFICATIONS en Android 13+ (API 33)
        if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF4E5)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Permiso de notificaciones requerido (Android 13+)",
                        color = Color(0xFF92400E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Para que AUREX pueda emitir alertas críticas de alta importancia cuando una divisa cruce tu umbral en segundo plano, autoriza el permiso POST_NOTIFICATIONS.",
                        color = Color(0xFF78350F),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text(
                            text = "Habilitar notificaciones críticas",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Formulario para crear una nueva regla de alerta por umbral
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configurar nueva alerta de umbral",
                    color = Navy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                // Selección del par de divisas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlertCurrencySelector(
                        label = "Divisa base",
                        selected = state.baseCurrency,
                        currencies = state.availableCurrencies,
                        onSelected = viewModel::selectBaseCurrency,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = Blue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AlertCurrencySelector(
                        label = "Divisa cotizada",
                        selected = state.targetCurrency,
                        currencies = state.availableCurrencies,
                        onSelected = viewModel::selectTargetCurrency,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tasa actual de referencia y botón para autocompletar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LightBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TASA ACTUAL DE MERCADO",
                                color = SecondaryText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = state.currentPairRate?.let {
                                    "1 ${state.baseCurrency} = ${it.toPlainString()} ${state.targetCurrency}"
                                } ?: "Cargando cotización…",
                                color = Navy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (state.currentPairRate != null) {
                            TextButton(onClick = viewModel::useCurrentRateAsThreshold) {
                                Text(
                                    text = "Usar valor",
                                    color = Blue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Selector de condición de cruce: ABOVE vs BELOW
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Regla de disparo",
                        color = Navy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBg, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThresholdDirection.entries.forEach { dir ->
                            val selected = state.direction == dir
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectDirection(dir) },
                                shape = RoundedCornerShape(11.dp),
                                color = if (selected) Navy else Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dir.symbol} ${if (dir == ThresholdDirection.ABOVE) "Sube (ABOVE)" else "Baja (BELOW)"}",
                                        color = if (selected) Color.White else SecondaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Campo de umbral objetivo
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Umbral objetivo en ${state.targetCurrency}",
                        color = Navy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = state.thresholdInput,
                        onValueChange = viewModel::updateThresholdInput,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = state.currentPairRate?.toPlainString() ?: "Ej. 4150.00",
                                color = Color(0xFFB1B8C7)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = Navy,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                state.validationMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                state.statusBannerMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Teal.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = Color(0xFF196953),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.createAlert()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text(
                        text = "Activar alerta por umbral",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Lista de alertas configuradas
        Text(
            text = "Tus alertas configuradas (${state.alerts.size})",
            color = Navy,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        if (state.alerts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔔 Sin alertas activas",
                        color = Navy,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Crea una regla arriba (ej. USD / COP ≥ 4100) y WorkManager la evaluará periódicamente en segundo plano.",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            state.alerts.forEach { alert ->
                val liveRate = PriceAlertEvaluator.computeCrossRate(
                    state.ratesFromUsd,
                    alert.baseCurrency,
                    alert.targetCurrency
                ) ?: alert.lastEvaluatedRate

                val isCrossedNow = liveRate != null && PriceAlertEvaluator.isThresholdCrossed(alert, liveRate)

                AlertItemCard(
                    alert = alert,
                    liveRate = liveRate?.toPlainString(),
                    isCrossedNow = isCrossedNow,
                    onToggleActive = { active -> viewModel.toggleAlertActive(alert.id, active) },
                    onDelete = { viewModel.deleteAlert(alert.id) },
                    onOpenInConverter = { onOpenPairInConverter(alert.baseCurrency, alert.targetCurrency) }
                )
            }
        }

        // Nota técnica de arquitectura sobre ventanas de mantenimiento de WorkManager y Doze
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFEAEFF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Arquitectura de Segundo Plano y Eficiencia Energética",
                    color = Navy,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "WorkManager ejecuta las verificaciones en ventanas de mantenimiento del sistema (cada 15–30 min según el modo Doze de Android) realizando una única consulta agrupada (batch) a Finnhub API para evaluar todas tus reglas en memoria sin drenar la batería.",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: PriceAlert,
    liveRate: String?,
    isCrossedNow: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenInConverter: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd MMM", Locale.forLanguageTag("es-CO")) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (alert.direction == ThresholdDirection.ABOVE) {
                            Teal.copy(alpha = 0.16f)
                        } else {
                            Blue.copy(alpha = 0.14f)
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = alert.direction.symbol,
                                color = if (alert.direction == ThresholdDirection.ABOVE) Color(0xFF196953) else Blue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Column {
                        Text(
                            text = alert.pairLabel,
                            color = Navy,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${alert.direction.label} ${alert.targetThreshold.toPlainString()} ${alert.targetCurrency}",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = alert.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Teal
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Cotización vigente: ${liveRate ?: "—"} ${alert.targetCurrency}",
                        color = Navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (alert.lastNotifiedAt != null) {
                        Text(
                            text = "Último aviso: ${dateFormat.format(Date(alert.lastNotifiedAt))}",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        !alert.isActive -> LightBg
                        isCrossedNow -> Teal.copy(alpha = 0.18f)
                        else -> Blue.copy(alpha = 0.10f)
                    }
                ) {
                    Text(
                        text = when {
                            !alert.isActive -> "PAUSADA"
                            isCrossedNow -> "UMBRAL ALCANZADO"
                            else -> "MONITOREANDO"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = when {
                            !alert.isActive -> SecondaryText
                            isCrossedNow -> Color(0xFF196953)
                            else -> Blue
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenInConverter,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Abrir par en convertidor →",
                        color = Navy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onDelete) {
                    Text(
                        text = "Eliminar",
                        color = DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(15.dp),
            color = LightBg
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(
                    text = label,
                    color = SecondaryText,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected,
                        color = Navy,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▾",
                        color = Blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        expanded = false
                        onSelected(currency)
                    }
                )
            }
        }
    }
}
