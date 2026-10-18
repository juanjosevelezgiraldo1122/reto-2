package com.example.reto2.scanner.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reto2.scanner.viewmodel.ScannerUiState

private val Navy = Color(0xFF10172B)
private val Blue = Color(0xFF5046E5)
private val Teal = Color(0xFF45C7A5)
private val Amber = Color(0xFFD97706)
private val SecondaryText = Color(0xFF7B8499)
private val LightSurface = Color(0xFFF3F5FA)

/**
 * Tarjeta flotante interactiva Material 3 que presenta en tiempo real:
 * 1. Monto y moneda origen detectada (con selector rápido para resolver ambigüedad del símbolo '$').
 * 2. Monto convertido a la moneda de preferencia en tiempo real.
 * 3. Controles para pausar/congelar la lectura o transferir el valor al convertidor principal.
 */
@Composable
fun PriceResultCard(
    state: ScannerUiState,
    onSelectSourceCurrency: (String) -> Unit,
    onSelectTargetCurrency: (String) -> Unit,
    onTogglePause: () -> Unit,
    onClearDetection: () -> Unit,
    onTransferToConverter: (amountPlain: String, sourceCurrency: String, targetCurrency: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val detected = state.detectedPrice

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 16.dp,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera de estado del escáner
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
                                color = when {
                                    state.isPaused -> Amber
                                    detected != null -> Teal
                                    else -> Blue
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = when {
                            state.isPaused -> "LECTURA CONGELADA"
                            detected != null -> "PRECIO DETECTADO EN VIVO"
                            else -> "BUSCANDO BILLETE O ETIQUETA DE PRECIO…"
                        },
                        color = if (state.isPaused) Amber else Navy,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                if (detected != null) {
                    TextButton(
                        onClick = onClearDetection,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Limpiar",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (detected == null) {
                // Estado de espera mientras se apunta la cámara
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LightSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Apunta el recuadro hacia un precio o billete",
                            color = Navy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reconoce formatos mixtos como $ 1,250.50, € 49,99, 50.000 COP, USD 100.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Selector rápido de moneda por defecto para símbolo '$'
                AmbiguousDollarQuickBar(
                    selectedCurrency = state.sourceCurrency,
                    onSelectCurrency = onSelectSourceCurrency
                )
            } else {
                // 1. Monto y moneda origen detectada + conversión en tiempo real
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LightSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MONTO DETECTADO (${detected.rawText})",
                                    color = SecondaryText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${detected.formattedAmount} ${state.sourceCurrency}",
                                    color = Navy,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            InlineCurrencyBadgeDropdown(
                                label = "Origen",
                                selectedCurrency = state.sourceCurrency,
                                currencies = state.availableCurrencies,
                                onCurrencySelected = onSelectSourceCurrency
                            )
                        }

                        // Aviso y selector rápido si el símbolo '$' es ambiguo (USD vs COP vs MXN)
                        if (detected.isAmbiguousSymbol) {
                            AmbiguousDollarQuickBar(
                                selectedCurrency = state.sourceCurrency,
                                onSelectCurrency = onSelectSourceCurrency
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE1E5F0))
                        )

                        // 2. Monto convertido a la moneda de preferencia en tiempo real
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CONVERSIÓN EN TIEMPO REAL",
                                    color = Color(0xFF238B72),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (state.formattedConvertedAmount != null) {
                                        "≈ ${state.formattedConvertedAmount} ${state.targetCurrency}"
                                    } else {
                                        "Calculando…"
                                    },
                                    color = Blue,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                state.exchangeRate?.let { rate ->
                                    Text(
                                        text = "Tasa: 1 ${state.sourceCurrency} = ${rate.toPlainString()} ${state.targetCurrency}",
                                        color = SecondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            InlineCurrencyBadgeDropdown(
                                label = "Destino",
                                selectedCurrency = state.targetCurrency,
                                currencies = state.availableCurrencies,
                                onCurrencySelected = onSelectTargetCurrency
                            )
                        }
                    }
                }
            }

            // 3. Botones de acción: Pausar/Congelar lectura y Transferir al convertidor principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (state.isPaused) "▶ Reanudar" else "⏸ Congelar",
                        color = if (state.isPaused) Amber else Navy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        detected?.let {
                            onTransferToConverter(
                                it.amount.stripTrailingZeros().toPlainString(),
                                state.sourceCurrency,
                                state.targetCurrency
                            )
                        }
                    },
                    enabled = detected != null,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        disabledContainerColor = Color(0xFFD5DAE6)
                    )
                ) {
                    Text(
                        text = "Usar en convertidor →",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbiguousDollarQuickBar(
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit
) {
    val dollarOptions = listOf("USD", "COP", "MXN", "CAD")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "Símbolo '$' detectado · ¿A qué divisa corresponde el precio?",
            color = SecondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dollarOptions.forEach { code ->
                val isSelected = selectedCurrency == code
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectCurrency(code) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Blue else Color.White,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = code,
                            color = if (isSelected) Color.White else Navy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineCurrencyBadgeDropdown(
    label: String,
    selectedCurrency: String,
    currencies: List<String>,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = Navy
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = label.uppercase(),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$selectedCurrency ▾",
                        color = Teal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = code,
                                fontWeight = if (code == selectedCurrency) FontWeight.Bold else FontWeight.Normal,
                                color = if (code == selectedCurrency) Blue else Navy
                            )
                            if (code == selectedCurrency) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✓", color = Blue, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onCurrencySelected(code)
                    }
                )
            }
        }
    }
}
