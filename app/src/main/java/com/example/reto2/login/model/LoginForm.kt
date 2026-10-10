package com.example.reto2.login.model

data class LoginForm(
    val email: String = "",
    val password: String = ""
)

data class LoginErrors(
    val email: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean
        get() = email != null || password != null
}

fun LoginForm.validate(): LoginErrors {
    val normalizedEmail = email.trim()
    return LoginErrors(
        email = when {
            normalizedEmail.isBlank() -> "Ingresa tu correo electrónico"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                "Ingresa un correo electrónico válido"
            else -> null
        },
        password = if (password.isBlank()) "Ingresa tu contraseña" else null
    )
}

/** Boundary for a future authentication provider; no provider is selected here. */
interface LoginRepository {
    suspend fun login(form: LoginForm)
}
