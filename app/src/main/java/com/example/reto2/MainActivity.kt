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
import com.example.reto2.auth.model.SessionMode
import com.example.reto2.auth.model.UserSession
import com.example.reto2.auth.ui.AuthFlowScreen
import com.example.reto2.forex.ui.ExchangeScreen
import com.example.reto2.ui.theme.Reto2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val demoSessionPreferences = getSharedPreferences("demo_session", MODE_PRIVATE)
        setContent {
            Reto2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var authDestination by rememberSaveable { mutableStateOf<String?>(null) }
                    var activeRole by rememberSaveable {
                        mutableStateOf(demoSessionPreferences.getString("role", null))
                    }
                    var activeSessionId by rememberSaveable {
                        mutableStateOf(demoSessionPreferences.getString("session_id", null))
                    }
                    var activeMode by rememberSaveable {
                        mutableStateOf(demoSessionPreferences.getString("mode", SessionMode.DEMO.name))
                    }
                    val endDemoSession = {
                        activeRole = null
                        activeSessionId = null
                        demoSessionPreferences.edit().clear().commit()
                        Unit
                    }
                    val session = activeRole?.let { roleName ->
                        runCatching { UserRole.valueOf(roleName) }.getOrNull()?.let { role ->
                            UserSession(
                                id = activeSessionId ?: if (role == UserRole.TRADITIONAL_USER) "demo-traditional-user" else "demo-administrator",
                                role = role,
                                mode = runCatching { SessionMode.valueOf(activeMode ?: SessionMode.DEMO.name) }.getOrDefault(SessionMode.DEMO)
                            )
                        }
                    }

                    when {
                        session?.role == UserRole.ADMINISTRATOR -> {
                            AdministratorAuditScreen(session, onLogout = endDemoSession)
                        }
                        session?.role == UserRole.TRADITIONAL_USER -> {
                            ExchangeScreen(session = session, onLogout = endDemoSession)
                        }
                        authDestination != null -> {
                            AuthFlowScreen(
                                initialRegistration = authDestination == "register",
                                onBackToMarket = { authDestination = null },
                                onDemoClient = {
                                    activateSession(UserSession("demo-traditional-user", UserRole.TRADITIONAL_USER), demoSessionPreferences) { session ->
                                        activeRole = session.role.name; activeSessionId = session.id; activeMode = session.mode.name
                                    }
                                    authDestination = null
                                },
                                onDemoAdministrator = {
                                    activateSession(UserSession("demo-administrator", UserRole.ADMINISTRATOR), demoSessionPreferences) { session ->
                                        activeRole = session.role.name; activeSessionId = session.id; activeMode = session.mode.name
                                    }
                                    authDestination = null
                                },
                                onAuthenticated = { session ->
                                    activateSession(session, demoSessionPreferences) { authenticated ->
                                        activeRole = authenticated.role.name; activeSessionId = authenticated.id; activeMode = authenticated.mode.name
                                    }
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

    private fun activateSession(
        session: UserSession,
        preferences: android.content.SharedPreferences,
        onActivated: (UserSession) -> Unit
    ) {
        preferences.edit()
            .putString("role", session.role.name)
            .putString("session_id", session.id)
            .putString("mode", session.mode.name)
            .commit()
        onActivated(session)
    }
}
