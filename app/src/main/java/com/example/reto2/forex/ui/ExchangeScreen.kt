package com.example.reto2.forex.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.forex.model.ExchangeOperation
import com.example.reto2.forex.model.OperationStatus
import com.example.reto2.forex.model.TradeSide
import com.example.reto2.forex.viewmodel.ExchangeUiState
import com.example.reto2.forex.viewmodel.ExchangeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF10172B)
private val Blue = Color(0xFF5046E5)
private val Teal = Color(0xFF45C7A5)
private val Background = Color(0xFFF3F5FA)
private val SecondaryText = Color(0xFF7B8499)
private val CurrencyCodes = listOf("USD", "EUR", "COP", "GBP", "JPY", "CAD", "CHF", "MXN")

@Composable
fun ExchangeScreen(viewModel: ExchangeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ExchangeHeader()
        MarketIntro()
        TradeCard(state, viewModel)
        RateNotice()
        OperationHistory(state.operations)
    }
}

@Composable
private fun ExchangeHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(15.dp), color = Navy) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text("FX", color = Teal, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("CURRENCY EXCHANGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Navy)
            Text("Global markets", fontSize = 12.sp, color = SecondaryText)
        }
        Surface(shape = CircleShape, color = Color.White) {
            Text("●  Global", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = Color(0xFF238B72), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MarketIntro() {
    Column {
        Text("Tu dinero,\nsin fronteras.", color = Navy, fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
        Spacer(Modifier.height(7.dp))
        Text("Compra y vende divisas con claridad en cada paso.", color = SecondaryText, fontSize = 14.sp)
    }
}

@Composable
private fun TradeCard(state: ExchangeUiState, viewModel: ExchangeViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Background, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TradeSideButton("Comprar", state.side == TradeSide.BUY, Modifier.weight(1f)) { viewModel.selectSide(TradeSide.BUY) }
                TradeSideButton("Vender", state.side == TradeSide.SELL, Modifier.weight(1f)) { viewModel.selectSide(TradeSide.SELL) }
            }

            Text(if (state.side == TradeSide.BUY) "Quiero comprar" else "Quiero vender", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CurrencyDropdown(
                    label = "Entregas",
                    selected = state.sourceCurrency,
                    modifier = Modifier.weight(1f),
                    onSelected = viewModel::selectSource
                )
                Text("→", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                CurrencyDropdown(
                    label = "Recibes",
                    selected = state.targetCurrency,
                    modifier = Modifier.weight(1f),
                    onSelected = viewModel::selectTarget
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Monto en ${state.sourceCurrency}", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0,00", color = Color(0xFFB1B8C7)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = Navy),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(15.dp)
                )
            }

            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFF6F7FC)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tipo de cambio", fontSize = 12.sp, color = SecondaryText)
                        Text("Disponible al conectar proveedor", fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Medium)
                    }
                    Text("—", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                }
            }

            if (state.validationMessage != null) {
                Text(state.validationMessage, color = Color(0xFFB34848), fontSize = 13.sp)
            }
            if (state.latestRequestId != null) {
                Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFEAF7F3)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Text("Solicitud guardada · Pendiente", color = Color(0xFF16745E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Referencia ${state.latestRequestId.take(8).uppercase(Locale.ROOT)}. No se ha ejecutado ninguna operación.", color = Color(0xFF47796E), fontSize = 11.sp)
                    }
                }
            }
            Button(
                onClick = viewModel::confirmRequest,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text(if (state.side == TradeSide.BUY) "Continuar compra" else "Continuar venta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Text("Al continuar, crearás una solicitud pendiente de procesamiento.", modifier = Modifier.fillMaxWidth(), color = SecondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TradeSideButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) Color.White else Color.Transparent,
            contentColor = if (selected) Blue else SecondaryText
        )
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun CurrencyDropdown(label: String, selected: String, modifier: Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(15.dp),
            color = Color(0xFFF6F7FC)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(label.uppercase(Locale.ROOT), color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected, color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("⌄", color = Blue, fontSize = 18.sp)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CurrencyCodes.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        expanded = false
                        onSelected(currency)
                    }
                )
            }
        }
    }
}

@Composable
private fun RateNotice() {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFEDEFFE)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
            Text("✦", color = Blue, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Column {
                Text("Transparencia desde el inicio", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("El precio final y cualquier cargo se mostrarán antes de confirmar cuando se conecte el servicio de divisas.", color = SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun OperationHistory(operations: List<ExchangeOperation>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Actividad", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${operations.size} solicitudes", color = SecondaryText, fontSize = 12.sp)
        }
        if (operations.isEmpty()) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(19.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tu actividad aparecerá aquí", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Las solicitudes y sus estados se conservarán en este dispositivo.", color = SecondaryText, fontSize = 12.sp)
                }
            }
        } else {
            operations.take(10).forEach { operation -> OperationRow(operation) }
        }
    }
}

@Composable
private fun OperationRow(operation: ExchangeOperation) {
    val statusColor = when (operation.status) {
        OperationStatus.SUCCESSFUL -> Color(0xFF18866D)
        OperationStatus.FAILED, OperationStatus.REJECTED -> Color(0xFFB34848)
        OperationStatus.AWAITING_CONFIRMATION -> Color(0xFF986E20)
        OperationStatus.IN_PROGRESS -> Blue
        OperationStatus.PENDING -> Color(0xFF6C7391)
    }
    Surface(shape = RoundedCornerShape(17.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFF0F1FC)) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(if (operation.side == TradeSide.BUY) "↗" else "↙", color = Blue, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("${operation.sourceCurrency} → ${operation.targetCurrency}", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${operation.amount} ${operation.sourceCurrency} · ${formatDate(operation.createdAtMillis)}", color = SecondaryText, fontSize = 11.sp)
                operation.reason?.let {
                    Text(it, color = statusColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(operation.status.label(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${operation.id.take(8).uppercase(Locale.ROOT)}", color = SecondaryText, fontSize = 9.sp)
            }
        }
    }
}

private fun OperationStatus.label() = when (this) {
    OperationStatus.PENDING -> "Pendiente"
    OperationStatus.IN_PROGRESS -> "En proceso"
    OperationStatus.SUCCESSFUL -> "Exitosa"
    OperationStatus.FAILED -> "Fallida"
    OperationStatus.REJECTED -> "Rechazada"
    OperationStatus.AWAITING_CONFIRMATION -> "Pendiente de confirmación"
}

private fun formatDate(timeMillis: Long): String =
    SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es-CO")).format(Date(timeMillis))
