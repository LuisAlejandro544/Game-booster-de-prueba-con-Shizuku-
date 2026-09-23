package com.example.ui.redmagic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationBlockerManager

/**
 * Pestaña de rendimiento estilo Red Magic con Liberación Quirúrgica de RAM y Wi-Fi de Ultrabaja Latencia.
 */
@Composable
fun PerformanceTabContent(
    onTrimMemory: () -> Unit = {},
    onToggleWifiLowLatency: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val wifiManager = remember { com.example.service.WifiLowLatencyManager.getInstance(context) }
    val isWifiActive by wifiManager.isLowLatencyActive.collectAsState()
    val isWifiEnabled by wifiManager.isEnabledByUser.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Bloque 1: Perfil de CPU y Prioridad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RmSurface)
                .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PERFIL DE EJECUCIÓN",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "El Game Space asigna máxima prioridad de CPU y reduce tareas secundarias para mantener los FPS estables.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = RmAccentRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Prioridad de Hilos en Primer Plano: ALTA",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bloque 2: Modo Wi-Fi de Ultrabaja Latencia
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RmSurface)
                .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(RmAccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Wi-Fi",
                                color = RmAccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Column {
                            Text(
                                text = "WI-FI ULTRABAJA LATENCIA",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isWifiActive) "WIFI_MODE_FULL_LOW_LATENCY activo" else "En espera",
                                color = if (isWifiActive) RmAccentGreen else Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Switch(
                        checked = isWifiEnabled,
                        onCheckedChange = { onToggleWifiLowLatency(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RmAccentCyan
                        ),
                        modifier = Modifier.testTag("rm_switch_wifi_low_latency")
                    )
                }

                Text(
                    text = "Evita micro-cortes y reduce la variabilidad del ping (jitter) desactivando el ahorro de energía del chip Wi-Fi durante partidas competitivas.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }

        // Bloque 3: Liberación Quirúrgica de Memoria RAM
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RmSurface)
                .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LIBERACIÓN QUIRÚRGICA DE RAM",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Envía 'cmd activity trim-memory --all RUNNING_CRITICAL' a apps secundarias. Purga cachés y buffers pesados sin forzar cierres ni reiniciar procesos.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = onTrimMemory,
                    colors = ButtonDefaults.buttonColors(containerColor = RmAccentRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rm_btn_trim_memory"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Purgar Memoria en 2° Plano")
                }
            }
        }
    }
}

/**
 * Pestaña de herramientas de partida estilo Red Magic con Bloqueador de Notificaciones táctico.
 */
@Composable
fun ToolsTabContent(onResetAll: () -> Unit) {
    val context = LocalContext.current
    val blockerManager = NotificationBlockerManager.getInstance(context)
    val config by blockerManager.config.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Bloque 1: Modo No Molestar y Bloqueo de Alertas Táctico
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RmSurface)
                .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (config.isEnabled) RmAccentRed.copy(alpha = 0.2f)
                                    else Color.Gray.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (config.isEnabled) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (config.isEnabled) RmAccentRed else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "BLOQUEO DE NOTIFICACIONES",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (config.isEnabled) "Silenciando banners y alertas" else "Permitiendo alertas",
                                color = if (config.isEnabled) RmAccentGreen else Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { blockerManager.setBlockerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RmAccentRed
                        ),
                        modifier = Modifier.testTag("rm_switch_notifications_toggle")
                    )
                }

                Text(
                    text = "Oculta notificaciones que no estén en tu lista de excepciones mientras estás dentro del juego.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )

                // Resumen de estado de excepciones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apps en Excepción:",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${config.exceptionPackages.size} autorizadas",
                        color = RmAccentCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Silenciadas acumuladas:",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${config.totalBlockedCount} alertas",
                        color = RmAccentRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bloque 2: Restablecer todos los ajustes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RmSurface)
                .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RESTABLECER EN CALIENTE",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restaura la resolución nativa de Android y reestablece los filtros gráficos.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = onResetAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rm_reset_all_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Restablecer resolución y gráficos")
                }
            }
        }
    }
}
