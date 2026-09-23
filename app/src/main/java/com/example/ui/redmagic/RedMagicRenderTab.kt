package com.example.ui.redmagic

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.GameRenderState
import kotlin.math.roundToInt

/**
 * Pestaña de Control de Renderizado Interno y Filtros Gráficos estilo Red Magic.
 *
 * Funcionalidades clave:
 * 1. Slider interactivo en tiempo real para deslizar la escala de renderizado (50% a 100%)
 *    mientras el usuario está jugando mediante 'cmd game set --downscale'.
 * 2. Interruptor para desactivación forzada de 4x MSAA y optimización de buffers de GPU.
 * 3. Presets tácticos directos (50% Ultra FPS, 70% Equilibrado, 85% Calidad, 100% Nativo).
 */
@Composable
fun RenderTabContent(
    renderState: GameRenderState,
    targetGamePackage: String?,
    onRenderScaleChange: (Float) -> Unit,
    onToggleMsaa: (Boolean) -> Unit,
    onResetGraphics: () -> Unit
) {
    var sliderValue by remember(renderState.renderScale) {
        mutableFloatStateOf(renderState.renderScale)
    }

    val percentage = (sliderValue * 100).roundToInt()

    // 1. Tarjeta de Estado del Renderizado
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RmSurface)
            .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTADO DEL RENDERIZADO",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (renderState.isOperating) {
                    CircularProgressIndicator(
                        color = RmAccentCyan,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Text(
                text = renderState.statusMessage,
                color = if (renderState.isDownscaleActive) RmAccentCyan else Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Juego objetivo: ${targetGamePackage ?: "No detectado"}",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    // 2. Control Deslizante de Escala de Renderizado (cmd game downscale)
    Text(
        text = "ESCALA DE RENDERIZADO (GPU)",
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calidad Interna de la Superficie",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            percentage <= 55 -> "50% - Ultra Rendimiento (+FPS masivos)"
                            percentage <= 75 -> "70% - Equilibrado Recomendado"
                            percentage <= 88 -> "85% - Nitidez Óptima"
                            else -> "100% - Nativo Original"
                        },
                        color = RmAccentCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "$percentage%",
                    color = RmAccentCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Slider que el usuario puede mover en caliente durante la partida
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    onRenderScaleChange(sliderValue)
                },
                valueRange = 0.50f..1.0f,
                steps = 4, // 50%, 60%, 70%, 80%, 90%, 100%
                colors = SliderDefaults.colors(
                    thumbColor = RmAccentCyan,
                    activeTrackColor = RmAccentCyan,
                    inactiveTrackColor = RmSurfaceBorder
                ),
                modifier = Modifier.testTag("rm_render_scale_slider")
            )

            // Explicación técnica de bajo nivel
            Text(
                text = "Reduce la carga de polígonos y sombreado 3D en la GPU sin alterar el tamaño de los textos, botones ni barra de notificaciones del sistema.",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall
            )

            // Accesos rápidos por botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0.50f to "50% Ultra",
                    0.70f to "70% Balance",
                    0.85f to "85% Óptimo",
                    1.0f to "100% Max"
                ).forEach { (scale, label) ->
                    val isSelected = (sliderValue * 100).roundToInt() == (scale * 100).roundToInt()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RmAccentCyan.copy(alpha = 0.25f) else Color(0xFF0F172A))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RmAccentCyan else RmSurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                sliderValue = scale
                                onRenderScaleChange(scale)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    // 3. Desactivación forzada de Anti-Aliasing (MSAA) y Filtros Pesados
    Text(
        text = "OPTIMIZACIÓN DE FILTROS GPU",
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anular 4x MSAA y Filtros Pesados",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desactiva pasadas de suavizado de bordes innecesarias para liberar ancho de banda en la GPU.",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = renderState.isMsaaDisabled,
                    onCheckedChange = onToggleMsaa,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RmAccentRed,
                        checkedTrackColor = RmAccentRed.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("rm_toggle_msaa_switch")
                )
            }
        }
    }

    // 4. Botón Restaurar Ajustes Gráficos
    OutlinedButton(
        onClick = onResetGraphics,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("rm_reset_graphics_button"),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = "Restablecer",
            tint = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "RESTAURAR ESCALA Y FILTROS NATIVOS",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
