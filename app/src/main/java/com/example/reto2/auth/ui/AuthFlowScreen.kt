package com.example.reto2.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.reto2.login.ui.LoginScreen
import com.example.reto2.registration.ui.RegistrationScreen

@Composable
fun AuthFlowScreen() {
    var showRegistration by rememberSaveable { mutableStateOf(false) }

    if (showRegistration) {
        RegistrationScreen(onLoginClick = { showRegistration = false })
    } else {
        LoginScreen(onRegisterClick = { showRegistration = true })
    }
}
