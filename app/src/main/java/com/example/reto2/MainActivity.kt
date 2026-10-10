package com.example.reto2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reto2.auth.ui.AuthFlowScreen
import com.example.reto2.forex.ui.ExchangeScreen
import com.example.reto2.ui.theme.Reto2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reto2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var authDestination by rememberSaveable { mutableStateOf<String?>(null) }
                    if (authDestination != null) {
                        AuthFlowScreen(
                            initialRegistration = authDestination == "register",
                            onBackToMarket = { authDestination = null }
                        )
                    } else {
                        ExchangeScreen(
                            onRequireLogin = { authDestination = "login" },
                            onRequireRegistration = { authDestination = "register" }
                        )
                    }
                }
            }
        }
    }
}
