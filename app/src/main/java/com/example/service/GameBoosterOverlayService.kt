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
import com.example.shizuku.ResolutionManager
import com.example.shizuku.ScriptManager
import com.example.ui.redmagic.RedMagicPanelContent
import com.example.ui.redmagic.RmAccentRed
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
    private lateinit var resolutionManager: ResolutionManager
    private lateinit var gameRenderManager: GameRenderManager

    private var composeView: ComposeView? = null
    private var isPanelExpanded = mutableStateOf(false)

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

                startForeground(NOTIFICATION_ID, buildNotification(gameName))
                showFloatingOverlay()
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
     * Muestra la burbuja flotante y el contenedor Compose en el WindowManager.
     */
    private fun showFloatingOverlay() {
        if (composeView != null) return

        val layoutParams = WindowManager.LayoutParams(
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
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GameBoosterOverlayService)
            setViewTreeSavedStateRegistryOwner(this@GameBoosterOverlayService)
            setViewTreeViewModelStoreOwner(this@GameBoosterOverlayService)

            setContent {
                val resolutionState by resolutionManager.state.collectAsState()
                val renderState by gameRenderManager.state.collectAsState()
                var isExpanded by remember { isPanelExpanded }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Burbuja deslizante (visible cuando el panel está cerrado)
                    if (!isExpanded) {
                        FloatingDraggableBubble(
                            onClick = {
                                isExpanded = true
                                updateWindowFocusable(true)
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp)
                        )
                    }

                    // Panel lateral estilo Red Magic (visible cuando está expandido)
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                    ) {
                        RedMagicPanelContent(
                            resolutionState = resolutionState,
                            renderState = renderState,
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
                            onResetGraphics = {
                                serviceScope.launch {
                                    gameRenderManager.resetAll(_activeGamePackage.value)
                                }
                            },
                            onClosePanel = {
                                isExpanded = false
                                updateWindowFocusable(false)
                            }
                        )
                    }
                }
            }
        }

        windowManager.addView(composeView, layoutParams)
    }

    /**
     * Actualiza las banderas de foco del WindowManager según si el panel está desplegado.
     */
    private fun updateWindowFocusable(focusable: Boolean) {
        val view = composeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(view, params)
    }

    /**
     * Vigila en segundo plano si el usuario sale del juego.
     * Si el juego deja de estar en primer plano, restaura automáticamente la resolución nativa.
     */
    private fun startGameWatcher(gamePackage: String) {
        gameWatcherJob?.cancel()
        gameWatcherJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3500) // Comprueba cada 3.5 segundos de manera eficiente

                // Si la resolución o el render scale fueron modificados, verificamos si seguimos en el juego
                if (resolutionManager.state.value.isCustomResolutionActive || gameRenderManager.state.value.isDownscaleActive) {
                    val foregroundCheck = scriptManager.executeScript("get_foreground_app.sh")
                    val currentAppOutput = foregroundCheck.stdout

                    // Si la aplicación en primer plano ya no contiene el paquete del juego
                    if (currentAppOutput.isNotBlank() && !currentAppOutput.contains(gamePackage, ignoreCase = true)) {
                        Log.i(TAG, "El usuario salió del juego $gamePackage. Restaurando resolución nativa y render scale automáticamente.")
                        resolutionManager.resetResolution()
                        gameRenderManager.resetAll(gamePackage)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        _activeGamePackage.value = null

        // SEGURIDAD CRÍTICA: Restaurar resolución nativa y render scale al destruir el servicio
        serviceScope.launch(Dispatchers.IO) {
            try {
                resolutionManager.resetResolution()
                gameRenderManager.resetAll(_activeGamePackage.value)
            } catch (e: Exception) {
                Log.e(TAG, "Error restaurando ajustes al salir: ${e.message}")
            }
        }

        gameWatcherJob?.cancel()
        serviceJob.cancel()

        if (composeView != null) {
            windowManager.removeView(composeView)
            composeView = null
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Burbuja deslizante flotante minimalista estilo Game Space.
 */
@Composable
fun FloatingDraggableBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(Color(0xE60F172A))
            .border(
                width = 1.5.dp,
                color = RmAccentRed.copy(alpha = 0.8f),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
