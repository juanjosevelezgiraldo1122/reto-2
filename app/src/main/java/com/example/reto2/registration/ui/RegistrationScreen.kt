package com.example.reto2.registration.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.auth.ui.AuthInput
import com.example.reto2.auth.ui.AuthPage
import com.example.reto2.auth.ui.AuthPasswordInput
import com.example.reto2.auth.ui.AuthPrimaryButton
import com.example.reto2.auth.ui.AuthStatus
import com.example.reto2.auth.ui.AuthSwitchAction
import com.example.reto2.registration.viewmodel.RegistrationViewModel
import com.example.reto2.auth.model.UserSession

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = viewModel(),
    onAuthenticated: (UserSession) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackToMarket: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    AuthPage(
        title = "Empieza aquí.",
        description = "Crea tu cuenta y haz espacio para lo que sigue.",
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
            error = state.errors.password.takeIf { state.showValidation }
        )
        AuthPasswordInput(
            value = state.form.passwordConfirmation,
            onValueChange = viewModel::onPasswordConfirmationChanged,
            label = "Confirma tu contraseña",
            error = state.errors.passwordConfirmation.takeIf { state.showValidation },
            imeAction = ImeAction.Done,
            onDone = { viewModel.onRegisterClicked(onAuthenticated) }
        )
        AuthPrimaryButton("Crear cuenta", { viewModel.onRegisterClicked(onAuthenticated) })
        state.statusMessage?.let { AuthStatus(it) }
        Spacer(Modifier.height(2.dp))
        AuthSwitchAction("¿Ya tienes cuenta?  Inicia sesión", onLoginClick)
    }
}
