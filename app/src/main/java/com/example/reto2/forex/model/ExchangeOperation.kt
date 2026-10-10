package com.example.reto2.forex.model

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
