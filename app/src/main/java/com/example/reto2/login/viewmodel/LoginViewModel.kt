package com.example.reto2.login.viewmodel

import androidx.lifecycle.ViewModel
import com.example.reto2.login.model.LoginErrors
import com.example.reto2.login.model.LoginForm
import com.example.reto2.login.model.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val form: LoginForm = LoginForm(),
    val errors: LoginErrors = LoginErrors(),
    val showValidation: Boolean = false,
    val statusMessage: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) = updateForm { copy(email = value) }

    fun onPasswordChanged(value: String) = updateForm { copy(password = value) }

    fun onLoginClicked() {
        val errors = _uiState.value.form.validate()
        _uiState.update {
            it.copy(
                errors = errors,
                showValidation = true,
                statusMessage = if (errors.hasErrors) null else
                    "Formulario validado. Configura un proveedor para iniciar sesión."
            )
        }
    }

    private inline fun updateForm(crossinline update: LoginForm.() -> LoginForm) {
        _uiState.update { state ->
            state.copy(
                form = state.form.update(),
                errors = LoginErrors(),
                showValidation = false,
                statusMessage = null
            )
        }
    }
}
