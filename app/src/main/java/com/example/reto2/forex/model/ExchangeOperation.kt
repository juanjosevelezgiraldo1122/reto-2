package com.example.reto2.forex.model

import android.content.Context
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TradeSide { BUY, SELL }

enum class OperationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESSFUL,
    FAILED,
    REJECTED,
    AWAITING_CONFIRMATION
}

data class ExchangeOperation(
    val id: String,
    val ownerId: String,
    val side: TradeSide,
    val sourceCurrency: String,
    val targetCurrency: String,
    val amount: String,
    val referenceRate: String? = null,
    val appliedRate: String? = null,
    val status: OperationStatus = OperationStatus.PENDING,
    val reason: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Persists confirmed requests locally. Execution is intentionally provided by a separate service. */
class ExchangeOperationRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("exchange_operations", Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(): List<ExchangeOperation> {
        val stored = preferences.getString(KEY_OPERATIONS, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(stored)
            List(json.length()) { index -> json.getJSONObject(index).toOperation() }
                .sortedByDescending(ExchangeOperation::createdAtMillis)
        }.getOrDefault(emptyList())
    }

    fun getVisibleTo(session: UserSession): List<ExchangeOperation> =
        getAll().filter { RolePermissions.canViewOperation(session, it.ownerId) }

    @Synchronized
    fun createPending(
        session: UserSession,
        side: TradeSide,
        sourceCurrency: String,
        targetCurrency: String,
        amount: String
    ): ExchangeOperation {
        check(RolePermissions.canTrade(session)) { "Este rol no puede operar divisas" }
        val operation = ExchangeOperation(
            id = UUID.randomUUID().toString(),
            ownerId = session.id,
            side = side,
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            amount = amount
        )
        save(listOf(operation) + getAll())
        return operation
    }

    /** A status change requires an explicit result from the execution mechanism. */
    @Synchronized
    fun recordExecutionResult(
        operationId: String,
        status: OperationStatus,
        verifiedReason: String? = null
    ): Boolean {
        require(status in setOf(
            OperationStatus.IN_PROGRESS,
            OperationStatus.SUCCESSFUL,
            OperationStatus.FAILED,
            OperationStatus.REJECTED,
            OperationStatus.AWAITING_CONFIRMATION
        ))
        val operations = getAll()
        val current = operations.firstOrNull { it.id == operationId } ?: return false
        if (!current.canTransitionTo(status)) return false
        save(operations.map {
            if (it.id == operationId) it.copy(status = status, reason = verifiedReason) else it
        })
        return true
    }

    private fun ExchangeOperation.canTransitionTo(next: OperationStatus): Boolean = when (status) {
        OperationStatus.PENDING -> next in setOf(
            OperationStatus.IN_PROGRESS,
            OperationStatus.REJECTED,
            OperationStatus.AWAITING_CONFIRMATION
        )
        OperationStatus.IN_PROGRESS -> next in setOf(
            OperationStatus.SUCCESSFUL,
            OperationStatus.FAILED,
            OperationStatus.AWAITING_CONFIRMATION
        )
        OperationStatus.AWAITING_CONFIRMATION -> next in setOf(
            OperationStatus.SUCCESSFUL,
            OperationStatus.FAILED
        )
        OperationStatus.SUCCESSFUL, OperationStatus.FAILED, OperationStatus.REJECTED -> false
    }

    private fun save(operations: List<ExchangeOperation>) {
        val json = JSONArray()
        operations.forEach { operation ->
            json.put(
                JSONObject()
                    .put("id", operation.id)
                    .put("ownerId", operation.ownerId)
                    .put("side", operation.side.name)
                    .put("source", operation.sourceCurrency)
                    .put("target", operation.targetCurrency)
                    .put("amount", operation.amount)
                    .apply { operation.referenceRate?.let { put("referenceRate", it) } }
                    .apply { operation.appliedRate?.let { put("appliedRate", it) } }
                    .put("status", operation.status.name)
                    .put("createdAt", operation.createdAtMillis)
                    .apply { operation.reason?.let { put("reason", it) } }
            )
        }
        check(preferences.edit().putString(KEY_OPERATIONS, json.toString()).commit()) {
            "No se pudo guardar la solicitud de cambio"
        }
    }

    private fun JSONObject.toOperation() = ExchangeOperation(
        id = getString("id"),
        ownerId = optString("ownerId", "legacy-unassigned"),
        side = TradeSide.valueOf(getString("side")),
        sourceCurrency = getString("source"),
        targetCurrency = getString("target"),
        amount = getString("amount"),
        referenceRate = optString("referenceRate").takeIf(String::isNotEmpty),
        appliedRate = optString("appliedRate").takeIf(String::isNotEmpty),
        status = OperationStatus.valueOf(getString("status")),
        reason = optString("reason").takeIf(String::isNotEmpty),
        createdAtMillis = getLong("createdAt")
    )

    private companion object {
        const val KEY_OPERATIONS = "operations"
    }
}
