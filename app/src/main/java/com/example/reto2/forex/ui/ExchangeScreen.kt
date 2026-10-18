package com.example.reto2.forex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.alerts.notification.AlertNotificationHelper
import com.example.reto2.alerts.ui.AlertsSectionView
import com.example.reto2.auth.ui.AuthSwitchAction
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import com.example.reto2.forex.model.ExchangeOperation
import com.example.reto2.forex.model.OperationStatus
import com.example.reto2.forex.model.TradeSide
import com.example.reto2.forex.viewmodel.ExchangeUiState
import com.example.reto2.forex.viewmodel.ExchangeViewModel
import com.example.reto2.scanner.ui.ScannerScreen
import java.text.SimpleDateFormat
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF10172B)
private val Blue = Color(0xFF5046E5)
private val Teal = Color(0xFF45C7A5)
private val Background = Color(0xFFF3F5FA)
private val SecondaryText = Color(0xFF7B8499)
private val CurrencyCodes = listOf("USD", "EUR", "COP", "GBP", "JPY", "CAD", "CHF", "MXN")
private val Mint = Color(0xFF8DE8D0)

private enum class MarketTab(val title: String, val glyph: String) {
    HOME("Inicio", "⌂"),
    MARKETS("Mercados", "↗"),
    CONVERT("Convertir", "⇄"),
    ALERTS("Alertas", "🔔"),
    ACTIVITY("Actividad", "◷"),
    PROFILE("Perfil", "○")
}

@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel = viewModel(),
    session: UserSession? = null,
    initialDestination: String? = null,
    initialBaseCurrency: String? = null,
    initialTargetCurrency: String? = null,
    onRequireLogin: () -> Unit = {},
    onRequireRegistration: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(MarketTab.HOME.name) }
    var isScannerOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(session) { viewModel.refreshFor(session) }

    LaunchedEffect(initialDestination, initialBaseCurrency, initialTargetCurrency) {
        if (initialBaseCurrency != null && initialTargetCurrency != null) {
            viewModel.selectPair(initialBaseCurrency, initialTargetCurrency)
        }
        when (initialDestination) {
            AlertNotificationHelper.DESTINATION_CONVERT -> selectedTab = MarketTab.CONVERT.name
            AlertNotificationHelper.DESTINATION_ALERTS -> selectedTab = MarketTab.ALERTS.name
        }
    }

    if (isScannerOpen) {
        ScannerScreen(
            initialTargetCurrency = state.targetCurrency,
            onClose = { isScannerOpen = false },
            onTransferToConverter = { scannedAmount, scannedSource, scannedTarget ->
                viewModel.applyScannedValue(
                    amount = scannedAmount,
                    sourceCurrency = scannedSource,
                    targetCurrency = scannedTarget
                )
                isScannerOpen = false
                selectedTab = MarketTab.CONVERT.name
            }
        )
        return
    }

    val activeTab = runCatching { MarketTab.valueOf(selectedTab) }.getOrDefault(MarketTab.HOME)
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
                MarketTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == activeTab,
                        onClick = { selectedTab = tab.name },
                        icon = { Text(tab.glyph, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                        label = { Text(tab.title, fontSize = 8.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            indicatorColor = Blue.copy(alpha = .09f),
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp)) {
                ExchangeHeader(
                    session = session,
                    onRequireLogin = onRequireLogin,
                    onLogout = onLogout,
                    onOpenScanner = { isScannerOpen = true },
                    onOpenAlerts = { selectedTab = MarketTab.ALERTS.name }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (activeTab) {
                    MarketTab.HOME -> HomeView(
                        state = state,
                        onOpenMarkets = { selectedTab = MarketTab.MARKETS.name },
                        onOpenConverter = { selectedTab = MarketTab.CONVERT.name },
                        onOpenScanner = { isScannerOpen = true },
                        onOpenAlerts = { selectedTab = MarketTab.ALERTS.name }
                    )
                    MarketTab.MARKETS -> MarketsView(
                        state = state,
                        onRefresh = viewModel::refreshRates,
                        onOpenAlerts = { selectedTab = MarketTab.ALERTS.name }
                    )
                    MarketTab.CONVERT -> {
                        PageHeading("Convertir", "Configura tu solicitud de compra o venta.")
                        TradeCard(
                            state = state,
                            viewModel = viewModel,
                            session = session,
                            onRequireLogin = onRequireLogin,
                            onOpenScanner = { isScannerOpen = true }
                        )
                        RateNotice()
                    }
                    MarketTab.ALERTS -> {
                        PageHeading("Alertas de mercado", "Recibe notificaciones críticas cuando un par cruce tu umbral.")
                        AlertsSectionView(
                            onOpenPairInConverter = { base, target ->
                                viewModel.selectPair(base, target)
                                selectedTab = MarketTab.CONVERT.name
                            }
                        )
                    }
                    MarketTab.ACTIVITY -> {
                        PageHeading("Tu actividad", "Consulta el estado y el historial de tus solicitudes.")
                        if (session == null) LoginRequiredCard("Inicia sesión para consultar tu actividad", onRequireLogin)
                        else OperationHistory(state.operations)
                    }
                    MarketTab.PROFILE -> ProfileView(session, onRequireLogin, onRequireRegistration, onLogout)
                }
            }
        }
    }
}

@Composable
private fun HomeView(
    state: ExchangeUiState,
    onOpenMarkets: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenAlerts: () -> Unit
) {
    MarketHero()
    MarketHighlights()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Vista rápida", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Tasas de referencia disponibles", color = SecondaryText, fontSize = 11.sp)
        }
        TextButton(onClick = onOpenMarkets) { Text("Ver mercado", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FeaturedPair("USD", "COP", state.ratesFromUsd["COP"], Modifier.weight(1f))
        FeaturedPair("EUR", "USD", state.ratesFromUsd["EUR"]?.toBigDecimalOrNull()?.let {
            runCatching { java.math.BigDecimal.ONE.divide(it, 5, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }.getOrNull()
        }, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onOpenScanner,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) { Text("📷 Escanear precio", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }

        Button(
            onClick = onOpenAlerts,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) { Text("🔔 Alertas umbral", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
    }
    Button(
        onClick = onOpenConverter,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Navy)
    ) { Text("Abrir el convertidor  →", fontWeight = FontWeight.SemiBold) }
    RateNotice()
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Navy, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
        Text(subtitle, color = SecondaryText, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun MarketsView(
    state: ExchangeUiState,
    onRefresh: () -> Unit,
    onOpenAlerts: () -> Unit
) {
    PageHeading("Mercados", "Monedas principales frente al dólar estadounidense.")
    Surface(shape = RoundedCornerShape(22.dp), color = Navy) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("COTIZACIONES", color = Color.White.copy(alpha = .66f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Referencia de mercado", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenAlerts) { Text("🔔 Alertas", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onRefresh) { Text("Actualizar", color = Mint, fontSize = 11.sp) }
            }
            state.ratesUpdatedAt?.let {
                Text("Actualizado ${SimpleDateFormat("HH:mm · dd MMM", Locale.forLanguageTag("es-CO")).format(Date(it))}", color = Color.White.copy(alpha = .62f), fontSize = 10.sp)
            }
        }
    }
    CurrencyCodes.forEach { currency ->
        MarketCurrencyRow(currency, state.ratesFromUsd[currency])
    }
    RateNotice()
}

@Composable
private fun FeaturedPair(base: String, quote: String, rate: String?, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(19.dp), color = Color.White) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("$base / $quote", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
            Text(rate ?: "—", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (rate == null) "Cotización no disponible" else "Tasa indicativa", color = if (rate == null) SecondaryText else Color(0xFF247862), fontSize = 9.sp)
        }
    }
}

@Composable
private fun MarketCurrencyRow(currency: String, rate: String?) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFF0F1FC)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(currency.take(1), color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(currency, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("1 USD en $currency", color = SecondaryText, fontSize = 10.sp)
            }
            Text(rate ?: "No disponible", color = if (rate == null) SecondaryText else Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoginRequiredCard(message: String, onRequireLogin: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = Blue.copy(alpha = .08f)) {
                Text("↗", Modifier.padding(horizontal = 16.dp, vertical = 11.dp), color = Blue, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Text(message, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Button(onClick = onRequireLogin, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                Text("Iniciar sesión")
            }
        }
    }
}

@Composable
private fun ProfileView(
    session: UserSession?,
    onRequireLogin: () -> Unit,
    onRequireRegistration: () -> Unit,
    onLogout: () -> Unit
) {
    PageHeading("Perfil", "Tu espacio personal y preferencias de acceso.")
    Surface(shape = RoundedCornerShape(24.dp), color = Navy) {
        Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Text(if (session == null) "A" else "U", color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(if (session == null) "Visitante" else "Usuario tradicional", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (session == null) "Explora el mercado libremente" else if (session.mode.name == "DEMO") "Sesión de demostración" else "Sesión personal", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
            }
            Text(if (session == null) "INVITADO" else "ACTIVO", color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
        }
    }
    ProfilePreference("Seguridad", "Tu contraseña se guarda protegida en este dispositivo", "✓")
    ProfilePreference("Privacidad", "Tus solicitudes solo se muestran en tu cuenta", "◈")
    ProfilePreference("Moneda de referencia", "USD · utilizada para mostrar las tasas del mercado", "◎")
    if (session == null) {
        Button(onClick = onRequireLogin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
            Text("Iniciar sesión", fontWeight = FontWeight.SemiBold)
        }
        AuthSwitchAction("Crear una cuenta", onRequireRegistration)
    } else {
        Text("Las operaciones de esta versión son solicitudes locales de demostración y no realizan transferencias.", color = SecondaryText, fontSize = 11.sp, lineHeight = 16.sp)
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión", color = Color(0xFFB04444), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ProfilePreference(title: String, description: String, glyph: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF0F1FC)) {
                Text(glyph, Modifier.padding(horizontal = 11.dp, vertical = 9.dp), color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun ExchangeHeader(
    session: UserSession?,
    onRequireLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenScanner: () -> Unit = {},
    onOpenAlerts: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(15.dp), color = Navy, shadowElevation = 5.dp) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text("FX", color = Teal, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("AUREX", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp, color = Navy)
            Text("Mercado global de divisas", fontSize = 11.sp, color = SecondaryText)
        }
        TextButton(onClick = onOpenScanner) {
            Text("📷", fontSize = 16.sp)
        }
        TextButton(onClick = onOpenAlerts) {
            Text("🔔", fontSize = 16.sp)
        }
        if (session == null) {
            TextButton(onClick = onRequireLogin) {
                Text("Iniciar sesión", color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(if (session.mode.name == "DEMO") "CLIENTE · DEMO" else "CUENTA PERSONAL", color = Color(0xFF238B72), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onLogout) { Text("Cerrar sesión", color = Blue, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun MarketHero() {
    Surface(shape = RoundedCornerShape(30.dp), color = Navy, shadowElevation = 12.dp) {
        Box(Modifier.fillMaxWidth().height(232.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(listOf(Color(0xFF111A31), Color(0xFF272A70), Color(0xFF153B54))))
                drawCircle(Color(0xFF7774FF).copy(alpha = .12f), size.minDimension * .57f, Offset(size.width * .94f, size.height * .06f))
                drawCircle(Mint.copy(alpha = .08f), size.minDimension * .32f, Offset(size.width * .99f, size.height * .08f))
                val chart = Path().apply {
                    moveTo(size.width * .58f, size.height * .61f)
                    cubicTo(size.width * .67f, size.height * .46f, size.width * .70f, size.height * .68f, size.width * .78f, size.height * .51f)
                    cubicTo(size.width * .85f, size.height * .36f, size.width * .89f, size.height * .52f, size.width, size.height * .25f)
                }
                drawPath(chart, Mint.copy(alpha = .52f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 22.dp, vertical = 20.dp).fillMaxWidth(.82f)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(6.dp).background(Mint, CircleShape))
                        Text("CAMBIO DE DIVISAS · SIMPLE", color = Color.White.copy(alpha = .86f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Tu próximo destino\nno tiene fronteras.", color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
                Spacer(Modifier.height(9.dp))
                Text("Explora el mercado y prepara tu cambio con total claridad.", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, lineHeight = 17.sp)
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 17.dp), shape = CircleShape, color = Mint.copy(alpha = .13f)) {
                Text("✦", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Mint, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MarketHighlights() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf("08" to "DIVISAS", "02" to "OPERACIONES", "01" to "HISTORIAL").forEach { (value, label) ->
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(17.dp), color = Color.White) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {
                    Text(value, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
                    Spacer(Modifier.height(3.dp))
                    Text(label, color = SecondaryText, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TradeCard(
    state: ExchangeUiState,
    viewModel: ExchangeViewModel,
    session: UserSession?,
    onRequireLogin: () -> Unit,
    onOpenScanner: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Background, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TradeSideButton("Comprar", state.side == TradeSide.BUY, Modifier.weight(1f)) { viewModel.selectSide(TradeSide.BUY) }
                TradeSideButton("Vender", state.side == TradeSide.SELL, Modifier.weight(1f)) { viewModel.selectSide(TradeSide.SELL) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (state.side == TradeSide.BUY) "Prepara tu compra" else "Prepara tu venta", color = Navy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("Elige las monedas y el importe de tu solicitud.", color = SecondaryText, fontSize = 12.sp)
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Monto en ${state.sourceCurrency}", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    TextButton(onClick = onOpenScanner) {
                        Text("📷 Escanear billete o precio", color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
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

            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF5F6FA)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("COTIZACIÓN INDICATIVA", fontSize = 10.sp, color = SecondaryText, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                        if (state.indicativeRate == null) {
                            Text(state.rateError ?: "Cargando cotización…", fontSize = 12.sp, color = if (state.rateError == null) Navy else Color(0xFFB04444), fontWeight = FontWeight.Medium)
                        } else {
                            Text("1 ${state.sourceCurrency} = ${state.indicativeRate} ${state.targetCurrency}", fontSize = 14.sp, color = Navy, fontWeight = FontWeight.Bold)
                            val amount = state.amount.replace(',', '.').toBigDecimalOrNull()
                            val rate = state.indicativeRate.toBigDecimalOrNull()
                            if (amount != null && rate != null) {
                                val estimate = DecimalFormat("#,##0.00").format(amount.multiply(rate))
                                Text("Recibirías aprox. $estimate ${state.targetCurrency}", fontSize = 11.sp, color = Color(0xFF247862))
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MERCADO", fontSize = 9.sp, color = Blue, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                        TextButton(onClick = viewModel::refreshRates) {
                            Text("Actualizar", fontSize = 10.sp, color = Blue)
                        }
                    }
                }
            }
            state.ratesUpdatedAt?.let { updatedAt ->
                Text("Actualizada ${SimpleDateFormat("HH:mm · dd MMM", Locale.forLanguageTag("es-CO")).format(Date(updatedAt))} · tasa de referencia, no es una oferta de ejecución", color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
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
                onClick = {
                    if (RolePermissions.canTrade(session)) viewModel.confirmRequest(session)
                    else onRequireLogin()
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text(
                    when {
                        !RolePermissions.canTrade(session) -> "Inicia sesión para operar"
                        state.side == TradeSide.BUY -> "Continuar con la compra"
                        else -> "Continuar con la venta"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Text(
                if (RolePermissions.canTrade(session))
                    if (session?.mode?.name == "DEMO") "Modo demostración: la solicitud se guardará como pendiente, sin ejecutar un cambio real."
                    else "Tu solicitud se guardará y podrás consultar su estado en Actividad."
                else "Inicia sesión para continuar. No se enviará ninguna operación antes de autenticarte.",
                modifier = Modifier.fillMaxWidth(),
                color = SecondaryText,
                fontSize = 11.sp
            )
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
    Surface(shape = RoundedCornerShape(19.dp), color = Color(0xFFEDEFFE)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = Blue.copy(alpha = .1f), modifier = Modifier.padding(end = 11.dp)) {
                Text("✓", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
            }
            Column {
                Text("Transparencia desde el inicio", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("La cotización es indicativa. No es una oferta de ejecución; las solicitudes de esta versión quedan pendientes y no mueven dinero.", color = SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
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
