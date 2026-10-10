package com.example.reto2.login.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.auth.ui.AuthInput
import com.example.reto2.auth.ui.AuthDemoButton
import com.example.reto2.auth.ui.AuthPage
import com.example.reto2.auth.ui.AuthPasswordInput
import com.example.reto2.auth.ui.AuthPrimaryButton
import com.example.reto2.auth.ui.AuthStatus
import com.example.reto2.auth.ui.AuthSwitchAction
import com.example.reto2.login.viewmodel.LoginViewModel
import com.example.reto2.auth.model.UserSession

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onAuthenticated: (UserSession) -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onBackToMarket: () -> Unit = {},
    onDemoClient: () -> Unit = {},
    onDemoAdministrator: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    AuthPage(
        title = "Qué gusto verte.",
        description = "Entra a tu espacio y continúa donde lo dejaste.",
        onBackToMarket = onBackToMarket
    ) {
        AuthInput(
            value = state.form.email,
            onValueChange = viewModel::onEmailChanged,
            label = "Correo electrónico",
            error = state.errors.email.takeIf { state.showValidation },
            keyboardType = KeyboardType.Email
        )
        AuthPasswordInput(
            value = state.form.password,
            onValueChange = viewModel::onPasswordChanged,
            label = "Contraseña",
            error = state.errors.password.takeIf { state.showValidation },
            imeAction = ImeAction.Done,
            onDone = { viewModel.onLoginClicked(onAuthenticated) }
        )
        AuthPrimaryButton("Iniciar sesión", { viewModel.onLoginClicked(onAuthenticated) })
        state.statusMessage?.let { AuthStatus(it) }
        Spacer(Modifier.height(2.dp))
        AuthSwitchAction("¿Nuevo por aquí?  Crear una cuenta", onRegisterClick)
        Text(
            "O explora los roles en modo demostración",
            color = Color(0xFF718096),
            style = MaterialTheme.typography.labelMedium
        )
        AuthDemoButton("Entrar como cliente · Demo", onDemoClient)
        AuthDemoButton("Entrar como administrador · Demo", onDemoAdministrator, emphasized = false)
    }
}
