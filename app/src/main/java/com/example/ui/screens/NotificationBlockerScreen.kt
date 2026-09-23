package com.example.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedNotificationEvent
import com.example.data.NotificationBlockerConfig
import com.example.shizuku.ShizukuState
import com.example.ui.components.AppPickerDialog
import com.example.ui.redmagic.RmAccentCyan
import com.example.ui.redmagic.RmAccentGreen
import com.example.ui.redmagic.RmAccentRed
import com.example.ui.redmagic.RmBackground
import com.example.ui.redmagic.RmSurface
import com.example.ui.redmagic.RmSurfaceBorder
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusPending
import com.example.viewmodel.BoosterUiState
import com.example.viewmodel.GameBoosterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de configuración del Bloqueador de Notificaciones y Modo No Molestar Gamer.
 *
 * Permite al usuario:
 * 1. Activar o pausar el bloqueo automático de notificaciones emergentes durante juegos.
 * 2. Gestionar la lista de excepciones (aplicaciones autorizadas a mostrar notificaciones).
 * 3. Conceder el permiso de NotificationListenerService con un solo toque vía Shizuku o mediante Ajustes del sistema.
 * 4. Visualizar en vivo las estadísticas y el historial de notificaciones silenciadas.
 */
@Composable
fun NotificationBlockerScreen(
    viewModel: GameBoosterViewModel,
    shizukuState: ShizukuState,
    uiState: BoosterUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.notificationConfig.collectAsState()
    val blockedHistory by viewModel.blockedNotifications.collectAsState()
    val isListenerConnected by viewModel.isNotificationListenerConnected.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabecera Hero con identidad visual gamer
        HeaderHeroSection(
            isBlockerActive = config.isEnabled,
            totalBlocked = config.totalBlockedCount,
            exceptionsCount = config.exceptionPackages.size
        )

        // 2. Tarjeta de Estado del Permiso y Activación rápida
        PermissionStatusSection(
            hasPermission = uiState.hasNotificationPermission,
            isListenerConnected = isListenerConnected,
            isShizukuAuthorized = shizukuState.isReady,
            onGrantViaShizuku = { viewModel.grantNotificationAccessViaShizuku() },
            onOpenSettings = { viewModel.requestNotificationAccessSettings(context) }
        )

        // 3. Controles Principales (Switches Maestros)
        MainControlsSection(
            config = config,
            onToggleEnabled = { viewModel.toggleNotificationBlocker(it) },
            onToggleOnlyGaming = { viewModel.toggleBlockOnlyDuringGaming(it) }
        )

        // 4. Gestión de Excepciones (Lista Blanca)
        ExceptionsSection(
            exceptionPackages = config.exceptionPackages,
            packageManager = context.packageManager,
            onAddExceptionClick = { viewModel.openExceptionAppPicker() },
            onRemoveException = { viewModel.removeNotificationException(it) }
        )

        // 5. Historial de Notificaciones Silenciadas
        BlockedHistorySection(
            history = blockedHistory,
            onClearHistory = { viewModel.clearBlockedNotificationHistory() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Diálogo selector de aplicaciones para añadir a excepciones
    if (uiState.isAddingExceptionDialogVisible) {
        AppPickerDialog(
            isOpen = true,
            isScanning = uiState.isScanningApps,
            installedApps = uiState.scannedApps,
            existingPackageNames = config.exceptionPackages,
            onScanRequested = { viewModel.scanInstalledApps() },
            onAppSelected = { app ->
                viewModel.addNotificationException(app.packageName, app.appName)
            },
            onDismiss = { viewModel.closeExceptionAppPicker() }
        )
    }
}

/**
 * Cabecera hero con gradiente y resumen en tiempo real.
 */
@Composable
private fun HeaderHeroSection(
    isBlockerActive: Boolean,
    totalBlocked: Int,
    exceptionsCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_notification_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RmSurfaceBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isBlockerActive) RmAccentRed.copy(alpha = 0.15f) else Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isBlockerActive) RmAccentRed.copy(alpha = 0.2f)
                                    else Color.Gray.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBlockerActive) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = "Icono bloqueador",
                                tint = if (isBlockerActive) RmAccentRed else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "MODO NO MOLESTAR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isBlockerActive) "Bloqueo de alertas activo" else "Bloqueo pausado",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isBlockerActive) RmAccentGreen else Color.Gray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isBlockerActive) RmAccentGreen.copy(alpha = 0.15f)
                                else Color.Gray.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isBlockerActive) RmAccentGreen else Color.Gray,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBlockerActive) "ACTIVO" else "PAUSADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlockerActive) RmAccentGreen else Color.Gray
                        )
                    }
                }

                Text(
                    text = "Silencia banners flotantes, mensajes y avisos de apps secundarias para que nada interrumpa tu visión táctica ni reduzca tus FPS mientras juegas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )

                // Métricas rápidas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBadge(
                        title = "Silenciadas",
                        value = totalBlocked.toString(),
                        color = RmAccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        title = "Excepciones",
                        value = exceptionsCount.toString(),
                        color = RmAccentCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, RmSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Estado y activación del permiso especial de NotificationListener.
 */
@Composable
private fun PermissionStatusSection(
    hasPermission: Boolean,
    isListenerConnected: Boolean,
    isShizukuAuthorized: Boolean,
    onGrantViaShizuku: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_permission_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RmSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Estado de permiso",
                        tint = if (hasPermission) RmAccentGreen else RmAccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Permiso de Acceso a Notificaciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (hasPermission) StatusConnected.copy(alpha = 0.2f)
                            else StatusDisconnected.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (hasPermission) "CONCEDIDO" else "PENDIENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasPermission) StatusConnected else StatusDisconnected,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasPermission) {
                Text(
                    text = "El servicio de escucha está activo y listo para filtrar notificaciones según tus excepciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Android requiere acceso especial para interceptar y ocultar las alertas emergentes de otras aplicaciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isShizukuAuthorized) {
                        Button(
                            onClick = onGrantViaShizuku,
                            colors = ButtonDefaults.buttonColors(containerColor = RmAccentRed),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_grant_notification_shizuku")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Shizuku",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Activar con Shizuku", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_notification_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajustes del Sistema", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Controles de configuración del bloqueador.
 */
@Composable
private fun MainControlsSection(
    config: NotificationBlockerConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleOnlyGaming: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_controls_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RmSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "CONFIGURACIÓN DEL BLOQUEO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = RmAccentCyan,
                letterSpacing = 0.5.sp
            )

            // Switch 1: Bloqueador general
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bloquear Notificaciones",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Oculta automáticamente cualquier notificación que no esté en la lista de excepciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RmAccentRed
                    ),
                    modifier = Modifier.testTag("switch_notification_blocker_master")
                )
            }

            HorizontalDivider(color = RmSurfaceBorder)

            // Switch 2: Solo durante el juego
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Solo mientras juegas",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Se activa únicamente cuando abres un juego desde el Game Booster con el panel activo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Switch(
                    checked = config.blockOnlyDuringGaming,
                    onCheckedChange = onToggleOnlyGaming,
                    enabled = config.isEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RmAccentCyan
                    ),
                    modifier = Modifier.testTag("switch_notification_only_gaming")
                )
            }
        }
    }
}

/**
 * Gestión de la lista de excepciones.
 */
@Composable
private fun ExceptionsSection(
    exceptionPackages: Set<String>,
    packageManager: PackageManager,
    onAddExceptionClick: () -> Unit,
    onRemoveException: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_exceptions_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RmSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LISTA DE EXCEPCIONES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = RmAccentCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Apps autorizadas a notificar durante el juego",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = onAddExceptionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RmAccentRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_notification_exception")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (exceptionPackages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, RmSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sin excepciones",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Bloqueo total: ninguna app secundaria interrumpirá tus juegos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Pulsa '+ Añadir App' si necesitas recibir alertas de WhatsApp, llamadas o Discord.",
                            style = MaterialTheme.typography.labelSmall,
                            color = RmAccentCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exceptionPackages.forEach { pkg ->
                        val appName = remember(pkg) {
                            try {
                                val info = packageManager.getApplicationInfo(pkg, 0)
                                packageManager.getApplicationLabel(info).toString()
                            } catch (e: Exception) {
                                pkg
                            }
                        }

                        ExceptionAppRow(
                            appName = appName,
                            packageName = pkg,
                            onRemove = { onRemoveException(pkg) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExceptionAppRow(
    appName: String,
    packageName: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, RmSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RmAccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Permitida",
                    tint = RmAccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag("btn_remove_exception_$packageName")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Quitar excepción",
                tint = RmAccentRed.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Historial de notificaciones silenciadas durante partidas.
 */
@Composable
private fun BlockedHistorySection(
    history: List<BlockedNotificationEvent>,
    onClearHistory: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blocked_history_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RmSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RmSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HISTORIAL DE SILENCIADAS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = RmAccentCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Notificaciones bloqueadas en tu última sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_clear_blocked_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Limpiar",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar", fontSize = 11.sp)
                    }
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = "Aún no se han interceptado notificaciones. Cuando juegues con el Game Booster, las notificaciones bloqueadas aparecerán registradas aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(10).forEach { item ->
                        BlockedNotificationItemRow(item = item, dateFormat = dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedNotificationItemRow(
    item: BlockedNotificationEvent,
    dateFormat: SimpleDateFormat
) {
    val formattedTime = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RmAccentRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = "Silenciada",
                tint = RmAccentRed,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.appName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }

            if (item.title.isNotEmpty()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.text.isNotEmpty()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}
