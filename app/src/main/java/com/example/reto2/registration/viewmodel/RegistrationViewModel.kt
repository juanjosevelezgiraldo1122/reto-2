package com.example.reto2.registration.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reto2.auth.model.LocalAuthRepository
import com.example.reto2.auth.model.UserSession
import com.example.reto2.registration.model.RegistrationErrors
import com.example.reto2.registration.model.RegistrationForm
import com.example.reto2.registration.model.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegistrationUiState(
    val form: RegistrationForm = RegistrationForm(),
    val errors: RegistrationErrors = RegistrationErrors(),
    val showValidation: Boolean = false,
    val statusMessage: String? = null
)

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAuthRepository(application)
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) = updateForm { copy(email = value) }

    fun onPasswordChanged(value: String) = updateForm { copy(password = value) }

    fun onPasswordConfirmationChanged(value: String) =
        updateForm { copy(passwordConfirmation = value) }

    fun onRegisterClicked(onAuthenticated: (UserSession) -> Unit) {
        val current = _uiState.value
        val errors = current.form.validate()
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors, showValidation = true, statusMessage = null) }
            return
        }
        _uiState.update { it.copy(errors = RegistrationErrors(), showValidation = true, statusMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { repository.register(current.form.email, current.form.password) }
            }
            result.onSuccess(onAuthenticated).onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = error.message ?: "No se pudo crear la cuenta.")
                }
            }
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
