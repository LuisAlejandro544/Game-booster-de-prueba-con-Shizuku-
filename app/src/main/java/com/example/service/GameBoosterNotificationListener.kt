package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.NotificationBlockerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio de escucha de notificaciones para Game Booster.
 *
 * Se ejecuta en segundo plano como parte del subsistema de Android NotificationListenerService.
 * Intercepta notificaciones emergentes (heads-up) y alertas durante partidas de juegos,
 * silenciándolas inmediatamente excepto aquellas aplicaciones que el usuario haya agregado
 * expresamente a su lista de "Excepciones".
 */
class GameBoosterNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationBlocker"

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected = _isListenerConnected.asStateFlow()
    }

    private lateinit var blockerManager: NotificationBlockerManager

    override fun onCreate() {
        super.onCreate()
        blockerManager = NotificationBlockerManager.getInstance(applicationContext)
        Log.i(TAG, "GameBoosterNotificationListener inicializado.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        Log.i(TAG, "NotificationListener conectado con éxito al subsistema de Android.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
        Log.w(TAG, "NotificationListener desconectado por el sistema operativo.")
    }

    /**
     * Intercepta cada notificación entrante emitida por cualquier aplicación del dispositivo.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val targetPackage = sbn.packageName ?: return

        // 1. Nunca bloquear notificaciones del propio Game Booster (necesarias para el overlay)
        if (targetPackage == applicationContext.packageName) {
            return
        }

        // 2. Verificar si el bloqueo está activo en este momento (según configuración y estado de juego)
        if (!blockerManager.isBlockingCurrentlyActive()) {
            return
        }

        // 3. Verificar si la app está en la lista de excepciones (lista blanca configurada por el usuario)
        if (blockerManager.isPackageAllowed(targetPackage)) {
            Log.d(TAG, "Notificación de '$targetPackage' permitida por estar en la lista de excepciones.")
            return
        }

        // 4. Protección para llamadas telefónicas entrantes urgentes (categoría CALL continua)
        val category = sbn.notification?.category
        val isCall = category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_ALARM
        if (isCall && sbn.isOngoing) {
            Log.d(TAG, "Llamada o alarma prioritaria detectada: permitiendo paso para seguridad del usuario.")
            return
        }

        // 5. Bloquear y descartar la notificación para evitar interrupción en la pantalla del juego
        try {
            cancelNotification(sbn.key)

            val extras = sbn.notification?.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Notificación"
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(targetPackage, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                targetPackage
            }

            blockerManager.recordBlockedNotification(
                packageName = targetPackage,
                appName = appName,
                title = title,
                text = text
            )

            Log.i(TAG, "Notificación bloqueada de: $appName ($targetPackage) - '$title'")
        } catch (e: Exception) {
            Log.e(TAG, "Error al silenciar notificación de $targetPackage: ${e.message}")
        }
    }
}
