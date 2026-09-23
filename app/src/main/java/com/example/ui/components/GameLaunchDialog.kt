package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameItem
import com.example.shizuku.AotCompilerState
import com.example.ui.redmagic.RmAccentCyan
import com.example.ui.redmagic.RmAccentGreen
import com.example.ui.redmagic.RmAccentRed
import com.example.ui.redmagic.RmBackground
import com.example.ui.redmagic.RmSurface
import com.example.ui.redmagic.RmSurfaceBorder

/**
 * Diálogo interactivo para elegir el tipo de ejecución de un juego:
 * 1. Ejecución Normal (Inicio directo con burbuja Game Space y Wi-Fi de Ultrabaja Latencia).
 * 2. Ejecución con Compilación Previa AOT contra el Micro-Stuttering (dex2oat speed-profile).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLaunchDialog(
    isOpen: Boolean,
    game: GameItem?,
    aotState: AotCompilerState,
    shizukuReady: Boolean,
    isWifiLowLatencyActive: Boolean,
    onLaunchNormal: (GameItem) -> Unit,
    onLaunchAot: (GameItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen || game == null) return

    BasicAlertDialog(
        onDismissRequest = {
            if (!aotState.isCompiling) onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, RmAccentRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            color = RmBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera del diálogo
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RmAccentRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = game.name.firstOrNull()?.uppercase() ?: "J",
                                color = RmAccentRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Iniciar ${game.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Selecciona el modo de lanzamiento",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (!aotState.isCompiling) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Indicador de estado de Wi-Fi de Ultrabaja Latencia integrado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RmSurface)
                        .border(1.dp, RmSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = RmAccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Modo Wi-Fi de Ultrabaja Latencia: ACTIVO (reduce el jitter del ping)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RmAccentCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Si se está compilando AOT actualmente
                if (aotState.isCompiling) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070B14))
                            .border(1.dp, RmAccentRed.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = RmAccentRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "COMPILANDO AOT (DEX2OAT)...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = RmAccentRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = aotState.progressMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Esto pre-compila el código de máquina para evitar micro-tirones durante la partida. El juego iniciará automáticamente al terminar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    // Opción 1: Ejecución Normal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RmSurface)
                            .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onLaunchNormal(game) }
                            .padding(14.dp)
                            .testTag("btn_launch_normal")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ejecución Normal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Inicio directo e instantáneo con burbuja flotante y Wi-Fi Low Latency.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Opción 2: Compilación Previa AOT contra el Micro-Stuttering
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RmSurface)
                            .border(
                                1.5.dp,
                                if (shizukuReady) RmAccentRed else RmSurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onLaunchAot(game) }
                            .padding(14.dp)
                            .testTag("btn_launch_aot")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(RmAccentRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = RmAccentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Compilación Previa AOT",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RmAccentGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ANTI-STUTTER",
                                            color = RmAccentGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Text(
                                    text = "Compila dex a código nativo vía Shizuku ('cmd package compile'). Elimina los tirones provocados por el compilador JIT en tiempo real.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                if (!shizukuReady) {
                                    Text(
                                        text = "(Requiere Shizuku activo)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RmAccentRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !aotState.isCompiling
                    ) {
                        Text(text = "Cancelar", color = Color.White)
                    }
                }
            }
        }
    }
}
