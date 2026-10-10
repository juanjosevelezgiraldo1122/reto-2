package com.example.reto2.registration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reto2.registration.viewmodel.RegistrationUiState
import com.example.reto2.registration.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    RegistrationContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onPasswordConfirmationChanged = viewModel::onPasswordConfirmationChanged,
        onRegisterClicked = viewModel::onRegisterClicked
    )
}

@Composable
private fun RegistrationContent(
    state: RegistrationUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordConfirmationChanged: (String) -> Unit,
    onRegisterClicked: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Completa tus datos para iniciar el registro.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = state.form.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.showValidation && state.errors.email != null,
                supportingText = state.errors.email.takeIf { state.showValidation }?.let { error ->
                    { Text(error) }
                }
            )
            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = state.form.password,
                label = "Contraseña",
                error = state.errors.password.takeIf { state.showValidation },
                onValueChange = onPasswordChanged
            )
            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = state.form.passwordConfirmation,
                label = "Confirmar contraseña",
                error = state.errors.passwordConfirmation.takeIf { state.showValidation },
                onValueChange = onPasswordConfirmationChanged
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onRegisterClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarme")
            }

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } }
    )
}
