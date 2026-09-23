package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.shizuku.GameRenderManager
import com.example.shizuku.GraphicsDriver
import com.example.shizuku.GraphicsDriverManager
import com.example.shizuku.ResolutionManager
import com.example.shizuku.ScriptManager
import com.example.ui.redmagic.RedMagicPanelContent
import com.example.ui.redmagic.RmAccentRed
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que administra la burbuja flotante y el panel estilo Red Magic.
 *
 * Características:
 * 1. Muestra la burbuja flotante deslizable sobre cualquier juego mediante WindowManager.
 * 2. Al pulsar la burbuja se despliega el panel Red Magic para cambio de resolución y DPI.
 * 3. Supervisa en segundo plano el paquete del juego. Cuando el usuario sale del juego,
 *    restablece inmediatamente la resolución nativa original.
 * 4. Al detener el servicio o destruirlo, garantiza el restablecimiento de la resolución.
 */
class GameBoosterOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OverlayService"
        const val CHANNEL_ID = "game_booster_overlay_channel"
        const val NOTIFICATION_ID = 2001

        const val EXTRA_GAME_PACKAGE = "extra_game_package"
        const val EXTRA_GAME_NAME = "extra_game_name"

        const val ACTION_START = "action_start_overlay"
        const val ACTION_STOP = "action_stop_overlay"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _activeGamePackage = MutableStateFlow<String?>(null)
        val activeGamePackage = _activeGamePackage.asStateFlow()

        fun start(context: Context, gamePackage: String, gameName: String) {
            val intent = Intent(context, GameBoosterOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_GAME_PACKAGE, gamePackage)
                putExtra(EXTRA_GAME_NAME, gameName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameBoosterOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var scriptManager: ScriptManager

    override fun attachBaseContext(newBase: Context) {
        val config = android.content.res.Configuration(newBase.resources.configuration).apply {
            fontScale = 1.0f
        }
        val contextWithFixedFont = newBase.createConfigurationContext(config)
        super.attachBaseContext(contextWithFixedFont)
    }
    private lateinit var resolutionManager: ResolutionManager
    private lateinit var gameRenderManager: GameRenderManager
    private lateinit var graphicsDriverManager: GraphicsDriverManager
    private lateinit var memoryTrimManager: com.example.shizuku.MemoryTrimManager
    private lateinit var wifiLowLatencyManager: WifiLowLatencyManager

    private var composeView: ComposeView? = null
    private var isPanelExpanded = mutableStateOf(false)

    // Coordenadas en pantalla de la burbuja flotante
    private var bubbleX: Int = 0
    private var bubbleY: Int = 300

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var gameWatcherJob: Job? = null

    // Componentes de ciclo de vida para alojar Compose en un Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        scriptManager = ScriptManager(applicationContext)
        resolutionManager = ResolutionManager(applicationContext, scriptManager)
        gameRenderManager = GameRenderManager(applicationContext, scriptManager)
        graphicsDriverManager = GraphicsDriverManager(applicationContext, scriptManager)
        memoryTrimManager = com.example.shizuku.MemoryTrimManager(applicationContext, scriptManager)
        wifiLowLatencyManager = WifiLowLatencyManager.getInstance(applicationContext)

        val metrics = resources.displayMetrics
        bubbleX = 0
        bubbleY = (metrics.heightPixels * 0.30f).toInt()

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val gamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE) ?: "Juego"
                val gameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Juego en Curso"

                _activeGamePackage.value = gamePackage
                _isRunning.value = true

                // Activa el Modo Wi-Fi de Ultrabaja Latencia para estabilizar el ping
                WifiLowLatencyManager.getInstance(applicationContext).acquireLowLatency()

                startForeground(NOTIFICATION_ID, buildNotification(gameName))
                showFloatingOverlay()
                serviceScope.launch {
                    graphicsDriverManager.detectDriver(gamePackage)
                }
                startGameWatcher(gamePackage)
            }
        }
        return START_STICKY
    }

    private fun buildNotification(gameName: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GameBoosterOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Booster Activo")
            .setContentText("Panel Red Magic disponible para $gameName")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Game Booster Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación del servicio flotante de Game Booster"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Parámetros de la ventana cuando solo se muestra la burbuja.
     * Al usar WRAP_CONTENT y FLAG_NOT_FOCUSABLE, la ventana ocupa ÚNICAMENTE los píxeles
     * de la burbuja, permitiendo que el 100% de la superficie del juego reciba toques sin impedimentos.
     */
    private fun getBubbleLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }
    }

    /**
     * Parámetros de la ventana cuando se expande el panel Red Magic a pantalla completa.
     */
    private fun getPanelLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    /**
     * Actualiza la posición de la burbuja en tiempo real al arrastrarla por la pantalla.
     */
    private fun updateBubblePosition(dx: Float, dy: Float) {
        val view = composeView ?: return
        if (isPanelExpanded.value) return

        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - 140).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - 140).coerceAtLeast(0)

        bubbleX = (bubbleX + dx.toInt()).coerceIn(0, maxX)
        bubbleY = (bubbleY + dy.toInt()).coerceIn(40, maxY)

        val params = getBubbleLayoutParams()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando posición de burbuja: ${e.message}")
        }
    }

    /**
     * Expande la ventana a pantalla completa para mostrar el panel Red Magic.
     */
    private fun expandPanel() {
        val view = composeView ?: return
        isPanelExpanded.value = true
        val params = getPanelLayoutParams()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error expandiendo panel: ${e.message}")
        }
    }

    /**
     * Colapsa la ventana al tamaño exclusivo de la burbuja (WRAP_CONTENT)
     * devolviendo inmediatamente todos los toques de pantalla al juego.
     */
    private fun collapsePanel() {
        val view = composeView ?: return
        isPanelExpanded.value = false
        val params = getBubbleLayoutParams()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error colapsando panel a burbuja: ${e.message}")
        }
    }

    /**
     * Muestra la burbuja flotante en el WindowManager.
     */
    private fun showFloatingOverlay() {
        if (composeView != null) return

        val layoutParams = getBubbleLayoutParams()

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GameBoosterOverlayService)
            setViewTreeSavedStateRegistryOwner(this@GameBoosterOverlayService)
            setViewTreeViewModelStoreOwner(this@GameBoosterOverlayService)

            setContent {
                MyApplicationTheme {
                    val resolutionState by resolutionManager.state.collectAsState()
                    val renderState by gameRenderManager.state.collectAsState()
                    val driverState by graphicsDriverManager.state.collectAsState()
                    val isExpanded by isPanelExpanded

                    if (!isExpanded) {
                        // Muestra ÚNICAMENTE la burbuja flotante compacta.
                        // No hay vistas que cubran el resto de la pantalla, el juego responde normalmente.
                        FloatingDraggableBubble(
                            onClick = { expandPanel() },
                            onDrag = { dx, dy -> updateBubblePosition(dx, dy) }
                        )
                    } else {
                        // Al expandirse, ocupa la pantalla para mostrar el panel táctico Red Magic
                        RedMagicPanelContent(
                            resolutionState = resolutionState,
                            renderState = renderState,
                            driverState = driverState,
                            targetGamePackage = _activeGamePackage.value,
                            onApplyResolution = { w, h, dpi ->
                                serviceScope.launch {
                                    resolutionManager.applyResolution(w, h, dpi)
                                }
                            },
                            onResetResolution = {
                                serviceScope.launch {
                                    resolutionManager.resetResolution()
                                }
                            },
                            onToggleAutoDpi = { enabled ->
                                resolutionManager.toggleAutoCalculateDpi(enabled)
                            },
                            onRenderScaleChange = { scale ->
                                val pkg = _activeGamePackage.value
                                if (!pkg.isNullOrBlank()) {
                                    serviceScope.launch {
                                        gameRenderManager.applyRenderScale(pkg, scale)
                                    }
                                }
                            },
                            onToggleMsaa = { disable ->
                                serviceScope.launch {
                                    gameRenderManager.toggleDisableMsaa(disable)
                                }
                            },
                            onSelectGraphicsDriver = { driver ->
                                val pkg = _activeGamePackage.value
                                if (!pkg.isNullOrBlank()) {
                                    serviceScope.launch {
                                        graphicsDriverManager.applyDriver(pkg, driver)
                                    }
                                }
                            },
                            onResetGraphics = {
                                val pkg = _activeGamePackage.value
                                serviceScope.launch {
                                    gameRenderManager.resetAll(pkg)
                                    graphicsDriverManager.resetDriver(pkg)
                                }
                            },
                            onTrimMemory = {
                                serviceScope.launch {
                                    memoryTrimManager.trimBackgroundMemory()
                                }
                            },
                            onToggleWifiLowLatency = { enabled ->
                                wifiLowLatencyManager.setEnabledByUser(enabled)
                                if (enabled) {
                                    wifiLowLatencyManager.acquireLowLatency()
                                } else {
                                    wifiLowLatencyManager.releaseLowLatency()
                                }
                            },
                            onClosePanel = {
                                collapsePanel()
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error agregando vista flotante: ${e.message}", e)
        }
    }

    /**
     * Vigila en segundo plano si el usuario sale del juego.
     * Si el juego deja de estar en primer plano, restaura automáticamente la resolución nativa y el render scale
     * para evitar que el teléfono quede con una resolución extraña o no nativa.
     */
    private fun startGameWatcher(gamePackage: String) {
        gameWatcherJob?.cancel()
        gameWatcherJob = serviceScope.launch(Dispatchers.IO) {
            var consecutiveNonGameTicks = 0
            while (isActive) {
                delay(2500) // Verificación balanceada cada 2.5 segundos

                try {
                    val foregroundCheck = scriptManager.executeScript("get_foreground_app.sh")
                    val currentAppOutput = foregroundCheck.stdout

                    if (currentAppOutput.isNotBlank()) {
                        val isGameInForeground = currentAppOutput.contains(gamePackage, ignoreCase = true)
                        val isOurAppInForeground = currentAppOutput.contains(packageName, ignoreCase = true)

                        if (!isGameInForeground && !isOurAppInForeground) {
                            consecutiveNonGameTicks++
                            Log.d(TAG, "App fuera de juego: '$currentAppOutput' (Ticks: $consecutiveNonGameTicks)")

                            // Tras 2 comprobaciones seguidas fuera del juego (~5 segundos), restaurar por seguridad
                            if (consecutiveNonGameTicks >= 2) {
                                val isCustomRes = resolutionManager.state.value.isCustomResolutionActive
                                val isDownscale = gameRenderManager.state.value.isDownscaleActive
                                val isCustomDriver = graphicsDriverManager.state.value.isCustomDriverActive

                                if (isCustomRes || isDownscale || isCustomDriver) {
                                    Log.i(TAG, "Salida de juego $gamePackage confirmada. Restaurando resolución nativa, render scale y controlador gráfico.")
                                    resolutionManager.resetResolution()
                                    gameRenderManager.resetAll(gamePackage)
                                    graphicsDriverManager.resetDriver(gamePackage)
                                }
                            }
                        } else {
                            consecutiveNonGameTicks = 0
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error en watcher de aplicación: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        val gamePkg = _activeGamePackage.value
        _activeGamePackage.value = null

        gameWatcherJob?.cancel()

        // Liberar el bloqueo Wi-Fi de baja latencia
        WifiLowLatencyManager.getInstance(applicationContext).releaseLowLatency()

        // SEGURIDAD CRÍTICA: Restaurar resolución nativa, render scale y controlador gráfico al destruir el servicio
        // Usamos NonCancellable para que la corrutina no sea cancelada al destruir el scope
        serviceScope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            try {
                resolutionManager.resetResolution()
                gameRenderManager.resetAll(gamePkg)
                graphicsDriverManager.resetDriver(gamePkg)
            } catch (e: Exception) {
                Log.e(TAG, "Error restaurando ajustes al salir: ${e.message}")
            }
        }

        if (composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removiendo vista de ventana: ${e.message}")
            }
            composeView = null
        }

        serviceJob.cancel()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Burbuja deslizante flotante minimalista estilo Red Magic Game Space.
 * Soporta arrastre en pantalla (onDrag) y pulsación (onClick) con discriminación precisa
 * entre toque (tap/click) y arrastre (drag), garantizando apertura instantánea sin bloquear el juego.
 */
@Composable
fun FloatingDraggableBubble(
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(onClick) {
                // Umbral de movimiento para considerar un gesto como arrastre en vez de click
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    val downTime = System.currentTimeMillis()
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.isConsumed) {
                            // Si otro consumidor interceptó el gesto, cancelamos
                            break
                        }

                        if (!change.pressed) {
                            // El dedo se ha levantado (Action UP)
                            val duration = System.currentTimeMillis() - downTime
                            val distance = (change.position - downPosition).getDistance()
                            
                            if (!isDrag && distance < touchSlop && duration < 600L) {
                                change.consume()
                                onClick()
                            }
                            break
                        } else {
                            val dragDistance = (change.position - downPosition).getDistance()
                            if (!isDrag && dragDistance > touchSlop) {
                                isDrag = true
                            }

                            if (isDrag) {
                                val dragDelta = change.position - change.previousPosition
                                if (dragDelta.x != 0f || dragDelta.y != 0f) {
                                    change.consume()
                                    onDrag(dragDelta.x, dragDelta.y)
                                }
                            }
                        }
                    }
                }
            }
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(Color(0xF00B0F19))
            .border(
                width = 1.5.dp,
                color = RmAccentRed.copy(alpha = 0.85f),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "Abrir Game Space",
                tint = RmAccentRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "BOOST",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}
