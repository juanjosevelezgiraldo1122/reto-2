package com.example.reto2.registration.model

/** Form data used by the registration flow. Authentication is supplied separately. */
data class RegistrationForm(
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = ""
)

data class RegistrationErrors(
    val email: String? = null,
    val password: String? = null,
    val passwordConfirmation: String? = null
) {
    val hasErrors: Boolean
        get() = email != null || password != null || passwordConfirmation != null
}

fun RegistrationForm.validate(): RegistrationErrors {
    val normalizedEmail = email.trim()
    return RegistrationErrors(
        email = when {
            normalizedEmail.isBlank() -> "Ingresa tu correo electrónico"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                "Ingresa un correo electrónico válido"
            else -> null
        },
        password = when {
            password.isBlank() -> "Ingresa una contraseña"
            password.length < 8 -> "Usa al menos 8 caracteres"
            else -> null
        },
        passwordConfirmation = when {
            passwordConfirmation.isBlank() -> "Confirma tu contraseña"
            passwordConfirmation != password -> "Las contraseñas no coinciden"
            else -> null
        }
    )
}

/** Boundary for a future authentication provider; no provider is selected here. */
interface RegistrationRepository {
    suspend fun register(form: RegistrationForm)
}
