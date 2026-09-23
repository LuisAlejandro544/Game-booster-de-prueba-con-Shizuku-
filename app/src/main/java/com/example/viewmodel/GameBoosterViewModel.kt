package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppScanner
import com.example.data.GameItem
import com.example.data.GameStorage
import com.example.data.InstalledApp
import com.example.service.GameBoosterOverlayService
import com.example.shizuku.DisplayResolutionState
import com.example.shizuku.GameRenderManager
import com.example.shizuku.GameRenderState
import com.example.shizuku.ResolutionManager
import com.example.shizuku.ScriptManager
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Perfiles clásicos de optimización para el Game Booster.
 *
 * Cada perfil define una configuración teórica que interactuará mediante el servicio Shizuku
 * sin recurrir a modificaciones peligrosas de propiedades del sistema (como persist.sys.*).
 */
enum class BoosterProfile(
    val title: String,
    val description: String,
    val cpuGovernor: String
) {
    BALANCED(
        title = "Modo Equilibrado",
        description = "Estabilidad térmica equilibrada y respuesta táctil óptima sin consumo excesivo.",
        cpuGovernor = "schedutil"
    ),
    PERFORMANCE(
        title = "Modo Alto Rendimiento",
        description = "Priorización máxima de cuadros por segundo (FPS) y retención del proceso en primer plano.",
        cpuGovernor = "performance"
    ),
    BATTERY_SAVER(
        title = "Modo Batería Prolongada",
        description = "Optimización energética para sesiones prolongadas reduciendo la sobrecarga de tareas en segundo plano.",
        cpuGovernor = "powersave"
    )
}

/**
 * Estado general de la UI del Game Booster.
 */
data class BoosterUiState(
    val selectedProfile: BoosterProfile = BoosterProfile.BALANCED,
    val isOptimizationActive: Boolean = false,
    val notificationMessage: String? = null,
    val isOverlayServiceRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isRedMagicSimulatorOpen: Boolean = false,
    val isAppPickerOpen: Boolean = false,
    val isAddingExceptionDialogVisible: Boolean = false,
    val isScanningApps: Boolean = false,
    val scannedApps: List<InstalledApp> = emptyList(),
    val gamesList: List<GameItem> = emptyList()
)

/**
 * ViewModel principal del Game Booster.
 *
 * Centraliza la lógica de negocio, coordina Shizuku, gestiona scripts .sh,
 * controla el cálculo y aplicación de resolución/DPI, administra el servicio
 * de la burbuja y panel flotante estilo Red Magic, y gestiona el bloqueo de
 * notificaciones con excepciones para evitar interrupciones durante el juego.
 */
class GameBoosterViewModel(application: Application) : AndroidViewModel(application) {

    val shizukuManager = ShizukuManager(application.applicationContext)
    val scriptManager = ScriptManager(application.applicationContext)
    val resolutionManager = ResolutionManager(application.applicationContext, scriptManager)
    val gameRenderManager = GameRenderManager(application.applicationContext, scriptManager)
    val appScanner = AppScanner(application.applicationContext)
    val gameStorage = GameStorage(application.applicationContext)
    val notificationBlockerManager = com.example.data.NotificationBlockerManager.getInstance(application.applicationContext)

    val shizukuState: StateFlow<ShizukuState> = shizukuManager.state
    val resolutionState: StateFlow<DisplayResolutionState> = resolutionManager.state
    val gameRenderState: StateFlow<GameRenderState> = gameRenderManager.state
    val notificationConfig = notificationBlockerManager.config
    val blockedNotifications = notificationBlockerManager.blockedHistory
    val isNotificationListenerConnected = com.example.service.GameBoosterNotificationListener.isListenerConnected

    private val _uiState = MutableStateFlow(BoosterUiState())
    val uiState: StateFlow<BoosterUiState> = _uiState.asStateFlow()

    init {
        // Carga los juegos personalizados guardados previamente por el usuario
        val savedGames = gameStorage.loadGames()
        _uiState.update { it.copy(gamesList = savedGames) }

        // Registra listeners de Shizuku
        shizukuManager.registerListeners()

        // Despliega los scripts .sh en background
        viewModelScope.launch {
            scriptManager.deployScripts()
        }

        // Monitorea el estado del servicio de Overlay
        viewModelScope.launch {
            GameBoosterOverlayService.isRunning.collect { running ->
                _uiState.update { it.copy(isOverlayServiceRunning = running) }
            }
        }

        checkOverlayPermission()
        checkNotificationAccessPermission()
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.unregisterListeners()
    }

    /**
     * Comprueba si la aplicación tiene concedido el permiso de superposición (overlay).
     */
    fun checkOverlayPermission() {
        val hasPermission = Settings.canDrawOverlays(getApplication())
        _uiState.update { it.copy(hasOverlayPermission = hasPermission) }
    }

    /**
     * Comprueba si la aplicación tiene concedido el permiso de escucha de notificaciones.
     */
    fun checkNotificationAccessPermission() {
        val hasPermission = notificationBlockerManager.isNotificationAccessGranted()
        _uiState.update { it.copy(hasNotificationPermission = hasPermission) }
    }

    /**
     * Solicita al usuario conceder el permiso de superposición de pantalla.
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Inicia un juego con la burbuja flotante y el panel Red Magic activos.
     */
    fun launchGameWithOverlay(context: Context, game: GameItem) {
        checkOverlayPermission()
        if (!_uiState.value.hasOverlayPermission) {
            _uiState.update {
                it.copy(notificationMessage = "Por favor concede el permiso para mostrar sobre otras aplicaciones.")
            }
            requestOverlayPermission(context)
            return
        }

        // Inicia el servicio en primer plano con la burbuja flotante
        GameBoosterOverlayService.start(context, game.packageName, game.name)

        // Intenta abrir el juego si está instalado
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(game.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                _uiState.update {
                    it.copy(notificationMessage = "Burbuja de Red Magic activada. (El juego '${game.name}' no está instalado en este dispositivo)")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(notificationMessage = "Burbuja activada. Abriendo overlay de control.")
            }
        }
    }

    /**
     * Detiene el servicio de superposición y restaura la resolución nativa.
     */
    fun stopOverlay(context: Context) {
        GameBoosterOverlayService.stop(context)
        resetResolution()
    }

    /**
     * Abre o cierra el simulador interactivo del panel Red Magic en pantalla completa dentro de la app.
     */
    fun toggleRedMagicSimulator(open: Boolean) {
        _uiState.update { it.copy(isRedMagicSimulatorOpen = open) }
    }

    /**
     * Aplica la resolución y DPI deseados usando scripts .sh vía Shizuku.
     */
    fun applyResolution(width: Int, height: Int, dpi: Int) {
        viewModelScope.launch {
            resolutionManager.applyResolution(width, height, dpi)
        }
    }

    /**
     * Restaura la resolución y densidad originales del dispositivo (función requerida explícitamente).
     */
    fun resetResolution() {
        viewModelScope.launch {
            resolutionManager.resetResolution()
        }
    }

    /**
     * Alterna la opción de cálculo automático proporcional de DPI.
     */
    fun toggleAutoDpi(enabled: Boolean) {
        resolutionManager.toggleAutoCalculateDpi(enabled)
    }

    /**
     * Modifica la escala de renderizado interno de la superficie del juego (cmd game downscale).
     */
    fun applyRenderScale(packageName: String, scale: Float) {
        viewModelScope.launch {
            gameRenderManager.applyRenderScale(packageName, scale)
        }
    }

    /**
     * Activa o desactiva la anulación forzada de 4x MSAA y filtros pesados de la GPU.
     */
    fun toggleDisableMsaa(disable: Boolean) {
        viewModelScope.launch {
            gameRenderManager.toggleDisableMsaa(disable)
        }
    }

    /**
     * Restablece los ajustes de render scale y filtros gráficos.
     */
    fun resetGraphics(packageName: String? = null) {
        viewModelScope.launch {
            gameRenderManager.resetAll(packageName)
        }
    }

    /**
     * Dispara la activación de Shizuku.
     */
    fun activateShizuku(context: Context) {
        shizukuManager.activateShizuku(context)
    }

    /**
     * Refresca el estado de Shizuku.
     */
    fun refreshShizukuStatus() {
        shizukuManager.refreshStatus()
    }

    /**
     * Abre la app de Shizuku.
     */
    fun openShizukuApp(context: Context) {
        shizukuManager.openShizukuApp(context)
    }

    /**
     * Abre enlace oficial de descarga de Shizuku.
     */
    fun openDownloadShizuku(context: Context) {
        shizukuManager.openDownloadPage(context)
    }

    /**
     * Abre el diálogo de selección de aplicaciones y dispara el escaneo en segundo plano.
     */
    fun openAppPicker() {
        _uiState.update { it.copy(isAppPickerOpen = true) }
        scanInstalledApps()
    }

    /**
     * Cierra el diálogo de selección de aplicaciones.
     */
    fun closeAppPicker() {
        _uiState.update { it.copy(isAppPickerOpen = false) }
    }

    /**
     * Escanea las aplicaciones y juegos instalados en el dispositivo en un hilo secundario (Dispatchers.IO).
     */
    fun scanInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanningApps = true) }
            val apps = appScanner.scanInstalledApps()
            _uiState.update {
                it.copy(
                    scannedApps = apps,
                    isScanningApps = false
                )
            }
        }
    }

    /**
     * Añade manualmente una aplicación o juego a la biblioteca del usuario y persiste el cambio.
     */
    fun addGameToLibrary(app: InstalledApp, defaultPreset: String = "75% HD+ (320 DPI)") {
        val currentList = _uiState.value.gamesList
        if (currentList.any { it.packageName == app.packageName }) {
            return
        }

        val newGame = GameItem(
            id = System.currentTimeMillis().toString(),
            name = app.appName,
            packageName = app.packageName,
            targetResolutionText = defaultPreset
        )
        val updated = currentList + newGame
        _uiState.update {
            it.copy(
                gamesList = updated,
                notificationMessage = "'${app.appName}' añadido a tu biblioteca de juegos"
            )
        }
        gameStorage.saveGames(updated)
    }

    /**
     * Elimina un juego de la biblioteca del usuario y actualiza el almacenamiento persistente.
     */
    fun removeGameFromLibrary(gameId: String) {
        val currentList = _uiState.value.gamesList
        val target = currentList.find { it.id == gameId }
        val updated = currentList.filter { it.id != gameId }
        _uiState.update {
            it.copy(
                gamesList = updated,
                notificationMessage = target?.let { "'${it.name}' eliminado de tu biblioteca" }
            )
        }
        gameStorage.saveGames(updated)
    }

    /**
     * Selecciona perfil de optimización.
     */
    fun selectProfile(profile: BoosterProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    /**
     * Limpia mensajes o avisos temporales.
     */
    fun clearNotificationMessage() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    /**
     * Alterna el estado del bloqueo de notificaciones de aplicaciones.
     */
    fun toggleNotificationBlocker(enabled: Boolean) {
        notificationBlockerManager.setBlockerEnabled(enabled)
        _uiState.update {
            it.copy(
                notificationMessage = if (enabled) "Bloqueador de notificaciones ACTIVADO" else "Bloqueador de notificaciones PAUSADO"
            )
        }
    }

    /**
     * Configura si el bloqueo de notificaciones solo opera durante partidas activas.
     */
    fun toggleBlockOnlyDuringGaming(onlyDuringGaming: Boolean) {
        notificationBlockerManager.setBlockOnlyDuringGaming(onlyDuringGaming)
    }

    /**
     * Agrega una aplicación a la lista de excepciones (permitidas).
     */
    fun addNotificationException(packageName: String, appName: String) {
        notificationBlockerManager.addException(packageName)
        _uiState.update {
            it.copy(
                notificationMessage = "'$appName' añadida a excepciones (se permitirán sus notificaciones)",
                isAddingExceptionDialogVisible = false
            )
        }
    }

    /**
     * Elimina una aplicación de la lista de excepciones.
     */
    fun removeNotificationException(packageName: String) {
        notificationBlockerManager.removeException(packageName)
        _uiState.update {
            it.copy(notificationMessage = "Excepción eliminada: la app será silenciada")
        }
    }

    /**
     * Reinicia el historial y el conteo de notificaciones silenciadas.
     */
    fun clearBlockedNotificationHistory() {
        notificationBlockerManager.clearBlockedHistory()
        _uiState.update {
            it.copy(notificationMessage = "Historial de notificaciones silenciadas reiniciado")
        }
    }

    /**
     * Abre los ajustes oficiales de Android para otorgar manualmente el permiso de acceso a notificaciones.
     */
    fun requestNotificationAccessSettings(context: Context) {
        try {
            val intent = notificationBlockerManager.createSettingsIntent()
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(notificationMessage = "Abre Ajustes > Apps > Acceso especial > Acceso a notificaciones")
            }
        }
    }

    /**
     * Otorga automáticamente el permiso de acceso a notificaciones usando Shizuku sin necesidad de PC.
     */
    fun grantNotificationAccessViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = notificationBlockerManager.grantAccessViaShizuku(scriptManager)
            checkNotificationAccessPermission()
            _uiState.update {
                it.copy(
                    notificationMessage = if (success) {
                        "¡Permiso de notificaciones otorgado con éxito vía Shizuku!"
                    } else {
                        "No se pudo otorgar vía Shizuku. Concede el permiso en los Ajustes del sistema."
                    }
                )
            }
        }
    }

    /**
     * Abre el diálogo selector para añadir una app a la lista de excepciones.
     */
    fun openExceptionAppPicker() {
        _uiState.update { it.copy(isAddingExceptionDialogVisible = true) }
        scanInstalledApps()
    }

    /**
     * Cierra el diálogo selector de excepciones.
     */
    fun closeExceptionAppPicker() {
        _uiState.update { it.copy(isAddingExceptionDialogVisible = false) }
    }
}
