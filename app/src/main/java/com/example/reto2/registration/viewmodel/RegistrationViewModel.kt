package com.example.reto2.registration.viewmodel

import androidx.lifecycle.ViewModel
import com.example.reto2.registration.model.RegistrationErrors
import com.example.reto2.registration.model.RegistrationForm
import com.example.reto2.registration.model.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RegistrationUiState(
    val form: RegistrationForm = RegistrationForm(),
    val errors: RegistrationErrors = RegistrationErrors(),
    val showValidation: Boolean = false,
    val statusMessage: String? = null
)

class RegistrationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) = updateForm { copy(email = value) }

    fun onPasswordChanged(value: String) = updateForm { copy(password = value) }

    fun onPasswordConfirmationChanged(value: String) =
        updateForm { copy(passwordConfirmation = value) }

    fun onRegisterClicked() {
        val current = _uiState.value
        val errors = current.form.validate()
        _uiState.update {
            it.copy(
                errors = errors,
                showValidation = true,
                statusMessage = if (errors.hasErrors) null else
                    "Formulario validado. Configura un proveedor para crear cuentas."
            )
        }
    }

    private inline fun updateForm(crossinline update: RegistrationForm.() -> RegistrationForm) {
        _uiState.update { state ->
            state.copy(
                form = state.form.update(),
                errors = RegistrationErrors(),
                showValidation = false,
                statusMessage = null
            )
        }
    }
}
