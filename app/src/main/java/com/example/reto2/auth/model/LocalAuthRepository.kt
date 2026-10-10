package com.example.reto2.auth.model

import android.content.Context
import com.example.reto2.forex.model.ExchangeOperationRepository

/** Local-only account storage for the current prototype. Passwords are never stored in plain text. */
class LocalAuthRepository(context: Context) {
    private val operations = ExchangeOperationRepository(context.applicationContext)

    fun register(email: String, password: String): UserSession = operations.register(email, password)

    fun login(email: String, password: String): UserSession? = operations.login(email, password)
}
