package com.example.reto2.forex.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.reto2.auth.model.RolePermissions
import com.example.reto2.auth.model.UserSession
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom
import java.util.Base64

data class PersistedAuditRecord(
    val id: String,
    val ownerLabel: String,
    val pair: String,
    val status: String,
    val referenceRate: String,
    val appliedRate: String,
    val withinLimit: Boolean,
    val deviationPercent: String,
    val createdAtMillis: Long
)

/** Device-local durable store. Transaction snapshots and audit examples are retained across app restarts. */
class ExchangeOperationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDataDatabase(appContext)
    private val migrationPreferences = appContext.getSharedPreferences("exchange_operations", Context.MODE_PRIVATE)

    init {
        migrateLegacyOperations()
    }

    @Synchronized
    fun getAll(): List<ExchangeOperation> = database.getAllOperations()

    fun getVisibleTo(session: UserSession): List<ExchangeOperation> =
        getAll().filter { RolePermissions.canViewOperation(session, it.ownerId) }

    fun getAuditRecords(): List<PersistedAuditRecord> = database.getAuditRecords()

    fun register(email: String, password: String): UserSession = database.register(email, password)

    fun login(email: String, password: String): UserSession? = database.login(email, password)

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
        database.insertOperation(operation)
        return operation
    }

    /** Status changes are stored with an append-only event and only accepted from the execution mechanism. */
    fun recordExecutionResult(
        operationId: String,
        status: OperationStatus,
        verifiedReason: String? = null
    ): Boolean = database.recordExecutionResult(operationId, status, verifiedReason)

    private fun migrateLegacyOperations() {
        if (migrationPreferences.getBoolean(MIGRATION_DONE, false)) return
        val legacy = migrationPreferences.getString(LEGACY_OPERATIONS, null)
        if (legacy != null) {
            runCatching {
                val records = JSONArray(legacy)
                for (index in 0 until records.length()) {
                    val item = records.getJSONObject(index)
                    val operation = ExchangeOperation(
                        id = item.getString("id"),
                        ownerId = item.optString("ownerId", "legacy-unassigned"),
                        side = TradeSide.valueOf(item.getString("side")),
                        sourceCurrency = item.getString("source"),
                        targetCurrency = item.getString("target"),
                        amount = item.getString("amount"),
                        referenceRate = item.optString("referenceRate").takeIf(String::isNotEmpty),
                        appliedRate = item.optString("appliedRate").takeIf(String::isNotEmpty),
                        status = OperationStatus.valueOf(item.getString("status")),
                        reason = item.optString("reason").takeIf(String::isNotEmpty),
                        createdAtMillis = item.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.insertOperationIfMissing(operation)
                }
            }
        }
        migrationPreferences.edit().putBoolean(MIGRATION_DONE, true).apply()
    }

    private companion object {
        const val MIGRATION_DONE = "sqlite_migration_done"
        const val LEGACY_OPERATIONS = "operations"
    }
}

private class AppDataDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE operations (
                id TEXT PRIMARY KEY NOT NULL,
                owner_id TEXT NOT NULL,
                side TEXT NOT NULL,
                source_currency TEXT NOT NULL,
                target_currency TEXT NOT NULL,
                amount TEXT NOT NULL,
                reference_rate TEXT,
                applied_rate TEXT,
                status TEXT NOT NULL,
                reason TEXT,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE operation_events (
                event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                operation_id TEXT NOT NULL,
                previous_status TEXT,
                next_status TEXT NOT NULL,
                reason TEXT,
                recorded_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE audit_records (
                id TEXT PRIMARY KEY NOT NULL,
                owner_label TEXT NOT NULL,
                pair TEXT NOT NULL,
                status TEXT NOT NULL,
                reference_rate TEXT NOT NULL,
                applied_rate TEXT NOT NULL,
                within_limit INTEGER NOT NULL,
                deviation_percent TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
        createAccountsTable(db)
        seedDemoAuditRecords(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createAccountsTable(db)
    }

    fun register(email: String, password: String): UserSession {
        val normalizedEmail = email.trim().lowercase()
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = hashPassword(password, salt)
        val userId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("id", userId)
            put("email", normalizedEmail)
            put("password_salt", Base64.getEncoder().encodeToString(salt))
            put("password_hash", Base64.getEncoder().encodeToString(hash))
            put("created_at", System.currentTimeMillis())
        }
        val inserted = writableDatabase.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        check(inserted != -1L) { "Ya existe una cuenta con ese correo." }
        return UserSession(userId, com.example.reto2.auth.model.UserRole.TRADITIONAL_USER, com.example.reto2.auth.model.SessionMode.LOCAL)
    }

    fun login(email: String, password: String): UserSession? {
        val normalizedEmail = email.trim().lowercase()
        return readableDatabase.query(
            "accounts", arrayOf("id", "password_salt", "password_hash"), "email = ?", arrayOf(normalizedEmail), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val salt = Base64.getDecoder().decode(cursor.getString(1))
            val expectedHash = Base64.getDecoder().decode(cursor.getString(2))
            val actualHash = hashPassword(password, salt)
            if (!java.security.MessageDigest.isEqual(expectedHash, actualHash)) return null
            UserSession(cursor.getString(0), com.example.reto2.auth.model.UserRole.TRADITIONAL_USER, com.example.reto2.auth.model.SessionMode.LOCAL)
        }
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun createAccountsTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS accounts (
                id TEXT PRIMARY KEY NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
    }

    fun getAllOperations(): List<ExchangeOperation> = readableDatabase.query(
        "operations", null, null, null, null, null, "created_at DESC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toExchangeOperation()) } }

    fun insertOperation(operation: ExchangeOperation) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(db.insertOrThrow("operations", null, operation.toContentValues()) != -1L)
            db.insertOrThrow(
                "operation_events", null,
                ContentValues().apply {
                    put("operation_id", operation.id)
                    put("previous_status", null as String?)
                    put("next_status", OperationStatus.PENDING.name)
                    put("reason", "Solicitud creada en modo demo")
                    put("recorded_at", operation.createdAtMillis)
                }
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertOperationIfMissing(operation: ExchangeOperation) {
        val db = writableDatabase
        if (db.query("operations", arrayOf("id"), "id = ?", arrayOf(operation.id), null, null, null).use { it.moveToFirst() }) return
        db.beginTransaction()
        try {
            db.insertOrThrow("operations", null, operation.toContentValues())
            db.insertOrThrow(
                "operation_events", null,
                ContentValues().apply {
                    put("operation_id", operation.id)
                    put("previous_status", null as String?)
                    put("next_status", operation.status.name)
                    put("reason", "Registro migrado al almacenamiento local")
                    put("recorded_at", operation.createdAtMillis)
                }
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun recordExecutionResult(operationId: String, next: OperationStatus, reason: String?): Boolean {
        require(next in setOf(
            OperationStatus.IN_PROGRESS,
            OperationStatus.SUCCESSFUL,
            OperationStatus.FAILED,
            OperationStatus.REJECTED,
            OperationStatus.AWAITING_CONFIRMATION
        ))
        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = db.query("operations", arrayOf("status"), "id = ?", arrayOf(operationId), null, null, null)
                .use { if (it.moveToFirst()) OperationStatus.valueOf(it.getString(0)) else return false }
            if (!current.canTransitionTo(next)) return false

            val now = System.currentTimeMillis()
            db.insertOrThrow(
                "operation_events", null,
                ContentValues().apply {
                    put("operation_id", operationId)
                    put("previous_status", current.name)
                    put("next_status", next.name)
                    put("reason", reason)
                    put("recorded_at", now)
                }
            )
            db.update(
                "operations",
                ContentValues().apply { put("status", next.name); put("reason", reason) },
                "id = ?", arrayOf(operationId)
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun getAuditRecords(): List<PersistedAuditRecord> = readableDatabase.query(
        "audit_records", null, null, null, null, null, "created_at DESC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toAuditRecord()) } }

    private fun seedDemoAuditRecords(db: SQLiteDatabase) {
        val samples = listOf(
            arrayOf("DEMO-1042", "Cliente demo 01", "USD / COP", "Exitosa · ejemplo", "4050.00", "4108.00"),
            arrayOf("DEMO-1043", "Cliente demo 02", "EUR / USD", "En proceso · ejemplo", "1.0800", "1.1110"),
            arrayOf("DEMO-1044", "Cliente demo 03", "GBP / EUR", "Fallida · ejemplo", "1.1700", "1.1800")
        )
        samples.forEachIndexed { index, sample ->
            val reference = sample[4].toBigDecimal()
            val applied = sample[5].toBigDecimal()
            val withinLimit = reference > BigDecimal.ZERO && applied <= reference.multiply(BigDecimal("1.025"))
            val deviation = applied.subtract(reference).multiply(BigDecimal("100"))
                .divide(reference, 2, RoundingMode.HALF_UP)
            db.insertWithOnConflict(
                "audit_records", null,
                ContentValues().apply {
                    put("id", sample[0])
                    put("owner_label", sample[1])
                    put("pair", sample[2])
                    put("status", sample[3])
                    put("reference_rate", sample[4])
                    put("applied_rate", sample[5])
                    put("within_limit", if (withinLimit) 1 else 0)
                    put("deviation_percent", deviation.toPlainString())
                    put("created_at", System.currentTimeMillis() - index * 60_000L)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    private fun Cursor.toExchangeOperation() = ExchangeOperation(
        id = getString(getColumnIndexOrThrow("id")),
        ownerId = getString(getColumnIndexOrThrow("owner_id")),
        side = TradeSide.valueOf(getString(getColumnIndexOrThrow("side"))),
        sourceCurrency = getString(getColumnIndexOrThrow("source_currency")),
        targetCurrency = getString(getColumnIndexOrThrow("target_currency")),
        amount = getString(getColumnIndexOrThrow("amount")),
        referenceRate = getStringOrNull("reference_rate"),
        appliedRate = getStringOrNull("applied_rate"),
        status = OperationStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
        reason = getStringOrNull("reason"),
        createdAtMillis = getLong(getColumnIndexOrThrow("created_at"))
    )

    private fun Cursor.toAuditRecord() = PersistedAuditRecord(
        id = getString(getColumnIndexOrThrow("id")),
        ownerLabel = getString(getColumnIndexOrThrow("owner_label")),
        pair = getString(getColumnIndexOrThrow("pair")),
        status = getString(getColumnIndexOrThrow("status")),
        referenceRate = getString(getColumnIndexOrThrow("reference_rate")),
        appliedRate = getString(getColumnIndexOrThrow("applied_rate")),
        withinLimit = getInt(getColumnIndexOrThrow("within_limit")) == 1,
        deviationPercent = getString(getColumnIndexOrThrow("deviation_percent")),
        createdAtMillis = getLong(getColumnIndexOrThrow("created_at"))
    )

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun ExchangeOperation.toContentValues() = ContentValues().apply {
        put("id", id)
        put("owner_id", ownerId)
        put("side", side.name)
        put("source_currency", sourceCurrency)
        put("target_currency", targetCurrency)
        put("amount", amount)
        put("reference_rate", referenceRate)
        put("applied_rate", appliedRate)
        put("status", status.name)
        put("reason", reason)
        put("created_at", createdAtMillis)
    }

    private fun OperationStatus.canTransitionTo(next: OperationStatus): Boolean = when (this) {
        OperationStatus.PENDING -> next in setOf(OperationStatus.IN_PROGRESS, OperationStatus.REJECTED, OperationStatus.AWAITING_CONFIRMATION)
        OperationStatus.IN_PROGRESS -> next in setOf(OperationStatus.SUCCESSFUL, OperationStatus.FAILED, OperationStatus.AWAITING_CONFIRMATION)
        OperationStatus.AWAITING_CONFIRMATION -> next in setOf(OperationStatus.SUCCESSFUL, OperationStatus.FAILED)
        OperationStatus.SUCCESSFUL, OperationStatus.FAILED, OperationStatus.REJECTED -> false
    }

    private companion object {
        const val DATABASE_NAME = "fx_application.db"
        const val DATABASE_VERSION = 2
    }
}
