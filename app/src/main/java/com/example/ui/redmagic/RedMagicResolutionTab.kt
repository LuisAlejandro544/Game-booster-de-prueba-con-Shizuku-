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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.DisplayResolutionState

/**
 * Presets rápidos de resolución pensados para gaming competitivo.
 */
data class ResolutionPreset(
    val name: String,
    val widthScale: Float,
    val heightScale: Float,
    val suggestedDpi: Int,
    val description: String
)

val DEFAULT_PRESETS = listOf(
    ResolutionPreset("100% Nativo", 1.0f, 1.0f, 420, "Resolución original del panel"),
    ResolutionPreset("85% Óptimo", 0.85f, 0.85f, 360, "Equilibrio nitidez y FPS"),
    ResolutionPreset("75% HD+ (720p)", 0.72f, 0.72f, 320, "Fluidez alta, menor temperatura"),
    ResolutionPreset("60% Max FPS (540p)", 0.60f, 0.60f, 260, "Máximo rendimiento competitivo")
)

/**
 * Contenido de la pestaña de Resolución de Pantalla y DPI proporcional estilo Red Magic.
 */
@Composable
fun ResolutionTabContent(
    state: DisplayResolutionState,
    selectedWidth: Int,
    selectedHeight: Int,
    selectedDpi: Int,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onDpiChange: (Int) -> Unit,
    onPresetSelect: (ResolutionPreset) -> Unit,
    onToggleAutoDpi: (Boolean) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    // Tarjeta de estado actual
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RmSurface)
            .border(1.dp, RmSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Pantalla Nativa: ${state.nativeSettings.description}",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Estado Actual: ${state.currentSettings.description}",
                color = if (state.isCustomResolutionActive) RmAccentCyan else Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.statusMessage,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    // Presets Rápidos
    Text(
        text = "PRESETS RÁPIDOS",
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DEFAULT_PRESETS.forEach { preset ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RmSurface)
                    .border(1.dp, RmSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { onPresetSelect(preset) }
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = preset.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.description,
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "${preset.suggestedDpi} DPI",
                        color = RmAccentCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Ajuste Manual y DPI
    Text(
        text = "AJUSTES PRECISOS DE PANTALLA",
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ancho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Ancho (Width):", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(text = "${selectedWidth} px", color = RmAccentCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = selectedWidth.toFloat(),
                onValueChange = {
                    val w = it.toInt()
                    onWidthChange(w)
                    if (state.autoCalculateDpi) {
                        val ratio = w.toFloat() / state.nativeSettings.width.toFloat()
                        val newDpi = (state.nativeSettings.dpi * ratio).toInt().coerceIn(120, 640)
                        onDpiChange(newDpi)
                    }
                },
                valueRange = 540f..state.nativeSettings.width.toFloat().coerceAtLeast(1080f),
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = RmAccentCyan,
                    activeTrackColor = RmAccentCyan,
                    inactiveTrackColor = RmSurfaceBorder
                )
            )

            // Alto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Alto (Height):", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(text = "${selectedHeight} px", color = RmAccentCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = selectedHeight.toFloat(),
                onValueChange = { onHeightChange(it.toInt()) },
                valueRange = 960f..state.nativeSettings.height.toFloat().coerceAtLeast(2400f),
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = RmAccentCyan,
                    activeTrackColor = RmAccentCyan,
                    inactiveTrackColor = RmSurfaceBorder
                )
            )

            // Switch Cálculo Automático de DPI
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Auto-calcular DPI",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Escala proporcional según resolución",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(
                    checked = state.autoCalculateDpi,
                    onCheckedChange = onToggleAutoDpi,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RmAccentRed,
                        checkedTrackColor = RmAccentRed.copy(alpha = 0.4f)
                    )
                )
            }

            // Slider DPI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Densidad DPI:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(text = "$selectedDpi DPI", color = RmAccentRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = selectedDpi.toFloat(),
                onValueChange = { onDpiChange(it.toInt()) },
                valueRange = 160f..560f,
                enabled = !state.autoCalculateDpi,
                colors = SliderDefaults.colors(
                    thumbColor = RmAccentRed,
                    activeTrackColor = RmAccentRed,
                    inactiveTrackColor = RmSurfaceBorder
                )
            )
        }
    }

    // Botones de acción: Aplicar y Restaurar
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("rm_apply_resolution_button"),
            colors = ButtonDefaults.buttonColors(containerColor = RmAccentRed),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (state.isOperating) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "APLICAR AL JUEGO",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // Botón explícito para restaurar resolución original
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("rm_reset_resolution_button"),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Restaurar",
                tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RESTAURAR RESOLUCIÓN ORIGINAL",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
