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
import com.example.reto2.admin.ui.AdministratorAuditScreen
import com.example.reto2.auth.model.UserRole
import com.example.reto2.auth.model.UserSession
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
                    var activeRole by rememberSaveable { mutableStateOf<String?>(null) }
                    val session = activeRole?.let { roleName ->
                        runCatching { UserRole.valueOf(roleName) }.getOrNull()?.let { role ->
                            UserSession(
                                id = if (role == UserRole.TRADITIONAL_USER) "demo-traditional-user" else "demo-administrator",
                                role = role
                            )
                        }
                    }

                    when {
                        session?.role == UserRole.ADMINISTRATOR -> {
                            AdministratorAuditScreen(session, onLogout = { activeRole = null })
                        }
                        session?.role == UserRole.TRADITIONAL_USER -> {
                            ExchangeScreen(session = session, onLogout = { activeRole = null })
                        }
                        authDestination != null -> {
                        AuthFlowScreen(
                            initialRegistration = authDestination == "register",
                            onBackToMarket = { authDestination = null },
                            onDemoClient = {
                                activeRole = UserRole.TRADITIONAL_USER.name
                                authDestination = null
                            },
                            onDemoAdministrator = {
                                activeRole = UserRole.ADMINISTRATOR.name
                                authDestination = null
                            }
                        )
                        }
                        else -> {
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
}
