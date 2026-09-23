package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.service.GameBoosterNotificationListener
import com.example.service.GameBoosterOverlayService
import com.example.shizuku.ScriptManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Modelo de datos con la configuración del bloqueador de notificaciones.
 *
 * @param isEnabled Indica si el sistema de bloqueo de notificaciones está activo globalmente.
 * @param blockOnlyDuringGaming Si es verdadero, solo silencia las notificaciones mientras la burbuja
 *                             u overlay del Game Booster esté en ejecución (durante la partida).
 * @param exceptionPackages Conjunto de nombres de paquete autorizados a notificar (lista blanca).
 * @param totalBlockedCount Cantidad total acumulada de notificaciones bloqueadas para estadísticas.
 */
data class NotificationBlockerConfig(
    val isEnabled: Boolean = true,
    val blockOnlyDuringGaming: Boolean = true,
    val exceptionPackages: Set<String> = emptySet(),
    val totalBlockedCount: Int = 0
)

/**
 * Representa una notificación interceptada y silenciada durante el juego.
 */
data class BlockedNotificationEvent(
    val id: String = System.currentTimeMillis().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Administrador central del bloqueo de notificaciones y lista de excepciones.
 *
 * Funcionalidades clave:
 * 1. Persistencia ligera en SharedPreferences para acceso síncrono instantáneo desde
 *    el NotificationListenerService sin demoras de inicialización.
 * 2. Soporte para activación automática con Shizuku sin necesidad de PC mediante:
 *    `cmd notification allow_listener <paquete>/<servicio>`.
 * 3. Gestión de lista blanca de aplicaciones autorizadas (excepciones).
 * 4. Registro y monitoreo de notificaciones bloqueadas en tiempo real.
 */
class NotificationBlockerManager private constructor(private val appContext: Context) {

    companion object {
        private const val PREFS_NAME = "game_booster_notifications_prefs"
        private const val KEY_ENABLED = "key_blocker_enabled"
        private const val KEY_ONLY_GAMING = "key_block_only_gaming"
        private const val KEY_EXCEPTIONS = "key_exception_packages"
        private const val KEY_BLOCKED_COUNT = "key_blocked_count"

        @Volatile
        private var INSTANCE: NotificationBlockerManager? = null

        fun getInstance(context: Context): NotificationBlockerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationBlockerManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<NotificationBlockerConfig> = _config.asStateFlow()

    private val _blockedHistory = MutableStateFlow<List<BlockedNotificationEvent>>(emptyList())
    val blockedHistory: StateFlow<List<BlockedNotificationEvent>> = _blockedHistory.asStateFlow()

    private fun loadConfig(): NotificationBlockerConfig {
        val isEnabled = prefs.getBoolean(KEY_ENABLED, true)
        val onlyGaming = prefs.getBoolean(KEY_ONLY_GAMING, true)
        val exceptions = prefs.getStringSet(KEY_EXCEPTIONS, emptySet()) ?: emptySet()
        val count = prefs.getInt(KEY_BLOCKED_COUNT, 0)
        return NotificationBlockerConfig(
            isEnabled = isEnabled,
            blockOnlyDuringGaming = onlyGaming,
            exceptionPackages = exceptions.toSet(),
            totalBlockedCount = count
        )
    }

    /**
     * Determina si el bloqueo de notificaciones debe operar en este instante exacto.
     * Retorna verdadero si la función está encendida Y (no depende de estar en juego O el overlay está activo).
     */
    fun isBlockingCurrentlyActive(): Boolean {
        val current = _config.value
        if (!current.isEnabled) return false
        return if (current.blockOnlyDuringGaming) {
            GameBoosterOverlayService.isRunning.value
        } else {
            true
        }
    }

    /**
     * Valida si un paquete tiene autorización para emitir notificaciones (está en lista de excepciones).
     */
    fun isPackageAllowed(packageName: String): Boolean {
        if (packageName == appContext.packageName) return true
        return _config.value.exceptionPackages.contains(packageName)
    }

    /**
     * Registra una notificación silenciada por el sistema.
     */
    fun recordBlockedNotification(packageName: String, appName: String, title: String, text: String) {
        val newCount = _config.value.totalBlockedCount + 1
        prefs.edit().putInt(KEY_BLOCKED_COUNT, newCount).apply()

        _config.update { it.copy(totalBlockedCount = newCount) }

        val event = BlockedNotificationEvent(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text
        )
        _blockedHistory.update { current ->
            // Mantiene como máximo las últimas 50 notificaciones bloqueadas en memoria
            (listOf(event) + current).take(50)
        }
    }

    /**
     * Agrega una aplicación a la lista de excepciones (permitida).
     */
    fun addException(packageName: String) {
        val updated = _config.value.exceptionPackages + packageName
        prefs.edit().putStringSet(KEY_EXCEPTIONS, updated).apply()
        _config.update { it.copy(exceptionPackages = updated) }
    }

    /**
     * Elimina una aplicación de la lista de excepciones.
     */
    fun removeException(packageName: String) {
        val updated = _config.value.exceptionPackages - packageName
        prefs.edit().putStringSet(KEY_EXCEPTIONS, updated).apply()
        _config.update { it.copy(exceptionPackages = updated) }
    }

    /**
     * Activa o desactiva el bloqueador globalmente.
     */
    fun setBlockerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _config.update { it.copy(isEnabled = enabled) }
    }

    /**
     * Configura si el bloqueo opera únicamente cuando hay un juego en sesión.
     */
    fun setBlockOnlyDuringGaming(onlyGaming: Boolean) {
        prefs.edit().putBoolean(KEY_ONLY_GAMING, onlyGaming).apply()
        _config.update { it.copy(blockOnlyDuringGaming = onlyGaming) }
    }

    /**
     * Reinicia el contador de notificaciones silenciadas y vacía el registro temporal.
     */
    fun clearBlockedHistory() {
        prefs.edit().putInt(KEY_BLOCKED_COUNT, 0).apply()
        _config.update { it.copy(totalBlockedCount = 0) }
        _blockedHistory.value = emptyList()
    }

    /**
     * Comprueba si el usuario ya otorgó el permiso de acceso a notificaciones en el sistema Android.
     */
    fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(appContext)
        return enabledPackages.contains(appContext.packageName)
    }

    /**
     * Otorga automáticamente el permiso del NotificationListenerService usando Shizuku
     * sin necesidad de conectar el teléfono a una PC.
     */
    suspend fun grantAccessViaShizuku(scriptManager: ScriptManager): Boolean {
        return try {
            val componentName = "${appContext.packageName}/${GameBoosterNotificationListener::class.java.name}"
            val command = "cmd notification allow_listener $componentName"
            val result = scriptManager.executeCommand(command)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Crea un Intent para que el usuario abra la pantalla oficial de Ajustes de Notificaciones de Android.
     */
    fun createSettingsIntent(): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        } else {
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
