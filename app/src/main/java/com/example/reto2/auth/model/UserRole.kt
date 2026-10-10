package com.example.reto2.auth.model

enum class UserRole {
    TRADITIONAL_USER,
    ADMINISTRATOR
}

enum class SessionMode {
    DEMO
}

data class UserSession(
    val id: String,
    val role: UserRole,
    val mode: SessionMode = SessionMode.DEMO
)

/** Central role policy. UI visibility is only a convenience; protected actions check this policy too. */
object RolePermissions {
    fun canTrade(session: UserSession?) = session?.role == UserRole.TRADITIONAL_USER

    fun canAuditAll(session: UserSession?) = session?.role == UserRole.ADMINISTRATOR

    fun canViewOperation(session: UserSession?, ownerId: String) =
        canAuditAll(session) || (canTrade(session) && session?.id == ownerId)
}
