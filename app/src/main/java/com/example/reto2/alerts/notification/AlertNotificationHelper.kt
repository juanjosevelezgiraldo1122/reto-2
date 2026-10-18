package com.example.reto2.alerts.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.example.reto2.MainActivity
import com.example.reto2.R
import com.example.reto2.alerts.model.PriceAlert
import com.example.reto2.alerts.model.ThresholdDirection
import java.math.BigDecimal

/**
 * Gestor de notificaciones del sistema para alertas críticas de tipo de cambio en AUREX.
 *
 * - Registra un [NotificationChannel] con [NotificationManager.IMPORTANCE_HIGH].
 * - Construye avisos enriquecidos con [NotificationCompat.Builder] y un [PendingIntent]
 *   estructurado mediante [TaskStackBuilder] para abrir directamente el convertidor o detalle de mercado.
 * - Verifica explícitamente el permiso [Manifest.permission.POST_NOTIFICATIONS] en Android 13+ (API 33).
 */
class AlertNotificationHelper(private val context: Context) {

    private val appContext = context.applicationContext

    init {
        registerCriticalAlertsChannel()
    }

    /**
     * Registra el canal de notificaciones de alta importancia (IMPORTANCE_HIGH).
     */
    fun registerCriticalAlertsChannel() {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 250L, 150L, 250L)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Valida si la aplicación cuenta con el permiso de notificaciones en tiempo de ejecución (Android 13+)
     * y si las notificaciones están habilitadas a nivel de sistema.
     */
    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    /**
     * Emite una notificación crítica cuando una cotización cruza el umbral definido en [alert].
     * Retorna `true` si la notificación fue despachada exitosamente al sistema.
     */
    @SuppressLint("MissingPermission")
    fun dispatchThresholdCrossedNotification(
        alert: PriceAlert,
        currentRate: BigDecimal
    ): Boolean {
        if (!canPostNotifications()) {
            return false
        }

        val directionAction = when (alert.direction) {
            ThresholdDirection.ABOVE -> "superó el umbral superior de"
            ThresholdDirection.BELOW -> "cayó por debajo del umbral de"
        }

        val formattedCurrent = currentRate.stripTrailingZeros().toPlainString()
        val formattedTarget = alert.targetThreshold.stripTrailingZeros().toPlainString()

        val title = "${alert.direction.symbol} Alerta ${alert.pairLabel}: $formattedCurrent ${alert.targetCurrency}"
        val shortContent = "1 ${alert.baseCurrency} $directionAction $formattedTarget ${alert.targetCurrency}."
        val expandedContent = "La cotización del par ${alert.pairLabel} alcanzó $formattedCurrent ${alert.targetCurrency} " +
            "(${alert.direction.label.lowercase()} $formattedTarget ${alert.targetCurrency}). " +
            "Toca para abrir el convertidor de AUREX con este par seleccionado."

        val deepLinkIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_DESTINATION, DESTINATION_CONVERT)
            putExtra(EXTRA_ALERT_ID, alert.id)
            putExtra(EXTRA_BASE_CURRENCY, alert.baseCurrency)
            putExtra(EXTRA_TARGET_CURRENCY, alert.targetCurrency)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(appContext).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(alert.id.hashCode(), pendingIntentFlags)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(alert.id.hashCode(), notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "aurex_critical_price_alerts"
        const val CHANNEL_NAME = "Alertas Críticas de Tipo de Cambio"
        const val CHANNEL_DESCRIPTION = "Avisos inmediatos cuando un par de divisas cruza los umbrales configurados"

        const val EXTRA_NAVIGATE_DESTINATION = "extra_navigate_destination"
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val EXTRA_BASE_CURRENCY = "extra_base_currency"
        const val EXTRA_TARGET_CURRENCY = "extra_target_currency"

        const val DESTINATION_CONVERT = "convert"
        const val DESTINATION_ALERTS = "alerts"
    }
}
