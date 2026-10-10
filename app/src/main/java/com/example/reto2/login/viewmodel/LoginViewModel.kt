package com.example.reto2.login.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reto2.auth.model.LocalAuthRepository
import com.example.reto2.auth.model.UserSession
import com.example.reto2.login.model.LoginErrors
import com.example.reto2.login.model.LoginForm
import com.example.reto2.login.model.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoginUiState(
    val form: LoginForm = LoginForm(),
    val errors: LoginErrors = LoginErrors(),
    val showValidation: Boolean = false,
    val statusMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAuthRepository(application)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) = updateForm { copy(email = value) }

    fun onPasswordChanged(value: String) = updateForm { copy(password = value) }

    fun onLoginClicked(onAuthenticated: (UserSession) -> Unit) {
        val form = _uiState.value.form
        val errors = form.validate()
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors, showValidation = true, statusMessage = null) }
            return
        }
        _uiState.update { it.copy(showValidation = true, statusMessage = null) }
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { repository.login(form.email, form.password) }
            if (session != null) onAuthenticated(session)
            else _uiState.update { it.copy(statusMessage = "Correo o contraseña incorrectos.") }
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
