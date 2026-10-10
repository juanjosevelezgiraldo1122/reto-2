package com.example.reto2.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.reto2.login.ui.LoginScreen
import com.example.reto2.registration.ui.RegistrationScreen

@Composable
fun AuthFlowScreen(
    initialRegistration: Boolean = false,
    onBackToMarket: () -> Unit = {},
    onDemoClient: () -> Unit = {},
    onDemoAdministrator: () -> Unit = {}
) {
    var showRegistration by rememberSaveable(initialRegistration) {
        mutableStateOf(initialRegistration)
    }

    if (showRegistration) {
        RegistrationScreen(
            onLoginClick = { showRegistration = false },
            onBackToMarket = onBackToMarket
        )
    } else {
        LoginScreen(
            onRegisterClick = { showRegistration = true },
            onBackToMarket = onBackToMarket,
            onDemoClient = onDemoClient,
            onDemoAdministrator = onDemoAdministrator
        )
    }
}
