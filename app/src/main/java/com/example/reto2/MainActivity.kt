package com.example.reto2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.reto2.registration.ui.RegistrationScreen
import com.example.reto2.ui.theme.Reto2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reto2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RegistrationScreen()
                }
            }
        }
    }
}
