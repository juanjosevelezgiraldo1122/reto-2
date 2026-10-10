package com.example.reto2.admin.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.admin.viewmodel.AdministratorAuditViewModel
import com.example.reto2.admin.viewmodel.DemoAuditRecord
import com.example.reto2.auth.model.UserRole
import com.example.reto2.auth.model.UserSession
import com.example.reto2.forex.model.ExchangeOperation
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Midnight = Color(0xFF10172B)
private val Violet = Color(0xFF5046E5)
private val Pale = Color(0xFFF3F5FA)
private val Quiet = Color(0xFF788196)

@Composable
fun AdministratorAuditScreen(
    session: UserSession,
    onLogout: () -> Unit,
    viewModel: AdministratorAuditViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(session) { viewModel.load(session) }

    Column(
        modifier = Modifier.fillMaxSize().background(Pale).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FX  /  CONTROL", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("Centro de auditoría", color = Midnight, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onLogout) { Text("Salir", color = Violet, fontWeight = FontWeight.SemiBold) }
        }

        Surface(shape = RoundedCornerShape(22.dp), color = Midnight) {
            Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Text("A", color = Color(0xFF8DE8D0), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text("Administrador · Modo demo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Solo lectura · sin permisos de compraventa", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
                }
                Text("DEMO", color = Color(0xFF8DE8D0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!state.authorized || session.role != UserRole.ADMINISTRATOR) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFEEEE)) {
                Text("Acceso denegado. Esta vista requiere el rol administrador.", Modifier.padding(18.dp), color = Color(0xFF9C3535))
            }
        } else {
            AuditSummary(state.sampleRecords.count(DemoAuditRecord::withinLimit), state.sampleRecords.size)

            SectionHeading("Transacciones de ejemplo", "Datos ficticios para explorar auditoría")
            state.sampleRecords.forEach { record -> DemoAuditCard(record) }

            SectionHeading("Operaciones locales", "Registros de este dispositivo · solo lectura")
            if (state.operations.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                    Text(
                        "Todavía no hay solicitudes locales. Las operaciones reales se cargarán desde el servicio cuando se conecte.",
                        Modifier.fillMaxWidth().padding(18.dp), color = Quiet, fontSize = 13.sp
                    )
                }
            } else {
                state.operations.forEach { operation -> LocalOperationCard(operation) }
            }
            Text(
                "Los registros originales son de solo lectura. Los ejemplos no representan operaciones reales.",
                color = Quiet,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AuditSummary(withinLimit: Int, total: Int) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Revisión de tasa", color = Quiet, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("Límite de referencia: +2,5 %", color = Midnight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("$withinLimit / $total", color = Violet, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    Column {
        Text(title, color = Midnight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Quiet, fontSize = 12.sp)
    }
}

@Composable
private fun DemoAuditCard(record: DemoAuditRecord) {
    val deviation = record.deviationPercent
    val compliant = record.withinLimit
    val verdictColor = if (compliant) Color(0xFF187B64) else Color(0xFFB04444)
    Surface(shape = RoundedCornerShape(19.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.pair, color = Midnight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${record.id} · ${record.ownerLabel}", color = Quiet, fontSize = 11.sp)
                }
                Text(record.status, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RateValue("Referencia", record.referenceRate.toPlainString())
                RateValue("Aplicada", record.appliedRate.toPlainString())
                RateValue("Variación", "${deviation.toPlainString()}%")
            }
            Surface(shape = RoundedCornerShape(12.dp), color = verdictColor.copy(alpha = 0.09f)) {
                Text(
                    if (compliant) "Dentro del límite de +2,5 %" else "Supera el límite de +2,5 % · revisar",
                    Modifier.fillMaxWidth().padding(12.dp), color = verdictColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RateValue(label: String, value: String) {
    Column {
        Text(label, color = Quiet, fontSize = 10.sp)
        Text(value, color = Midnight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LocalOperationCard(operation: ExchangeOperation) {
    val hasRates = operation.referenceRate != null && operation.appliedRate != null
    val isWithinLimit = if (hasRates) {
        val reference = operation.referenceRate!!.toBigDecimalOrNull()
        val applied = operation.appliedRate!!.toBigDecimalOrNull()
        reference != null && reference > java.math.BigDecimal.ZERO && applied != null &&
            applied <= reference.multiply(java.math.BigDecimal("1.025"))
    } else null
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${operation.sourceCurrency} → ${operation.targetCurrency} · ${operation.amount}", color = Midnight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Ref. ${operation.id.take(8).uppercase(Locale.ROOT)} · ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("es-CO")).format(Date(operation.createdAtMillis))}", color = Quiet, fontSize = 10.sp)
                if (operation.reason != null) Text(operation.reason, color = Quiet, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(operation.status.name.replace('_', ' '), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (isWithinLimit) {
                        true -> "Tasa conforme"
                        false -> "Revisar tasa"
                        null -> "Tasa no disponible"
                    },
                    color = when (isWithinLimit) {
                        true -> Color(0xFF187B64)
                        false -> Color(0xFFB04444)
                        null -> Quiet
                    },
                    fontSize = 9.sp
                )
            }
        }
    }
}
