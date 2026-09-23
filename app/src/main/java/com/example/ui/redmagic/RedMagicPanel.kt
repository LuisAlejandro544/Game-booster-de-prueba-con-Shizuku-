package com.example.ui.redmagic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.DisplayResolutionState
import com.example.shizuku.GameRenderState

/**
 * Pestañas y herramientas del panel táctico estilo Red Magic Game Space.
 */
enum class RedMagicTab(
    val title: String,
    val icon: ImageVector,
    val badge: String? = null
) {
    RENDER(
        title = "Render Scale",
        icon = Icons.Default.Tune,
        badge = "GPU"
    ),
    RESOLUTION(
        title = "Resolución & DPI",
        icon = Icons.Default.AspectRatio,
        badge = "WM"
    ),
    PERFORMANCE(
        title = "Modo Boost",
        icon = Icons.Default.Speed
    ),
    TOOLS(
        title = "Ajustes de Juego",
        icon = Icons.Default.AutoAwesome
    )
}

/**
 * Colores del panel estilo Red Magic (Tactical Dark Slate con acento rojo neón sutil).
 */
val RmBackground = Color(0xF00F172A)
val RmSurface = Color(0xFF1E293B)
val RmSurfaceBorder = Color(0xFF334155)
val RmAccentRed = Color(0xFFE11D48)
val RmAccentCyan = Color(0xFF0EA5E9)
val RmAccentGreen = Color(0xFF10B981)

/**
 * Panel lateral desplegable estilo Red Magic Game Space.
 *
 * Características:
 * - Slider interactivo en tiempo real para escala de renderizado (cmd game downscale).
 * - Control de resolución de pantalla y DPI proporcional.
 * - Desactivación forzada de 4x MSAA y optimizaciones de GPU.
 * - Barra lateral vertical táctica con iconos de cambio rápido.
 */
@Composable
fun RedMagicPanelContent(
    resolutionState: DisplayResolutionState,
    renderState: GameRenderState = GameRenderState(),
    onApplyResolution: (width: Int, height: Int, dpi: Int) -> Unit,
    onResetResolution: () -> Unit,
    onToggleAutoDpi: (Boolean) -> Unit,
    onRenderScaleChange: (Float) -> Unit = {},
    onToggleMsaa: (Boolean) -> Unit = {},
    onResetGraphics: () -> Unit = {},
    onClosePanel: () -> Unit,
    targetGamePackage: String? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(RedMagicTab.RENDER) }

    // Estados locales para los sliders de resolución y DPI de pantalla
    var selectedWidth by remember(resolutionState.targetWidth) { mutableStateOf(resolutionState.targetWidth) }
    var selectedHeight by remember(resolutionState.targetHeight) { mutableStateOf(resolutionState.targetHeight) }
    var selectedDpi by remember(resolutionState.targetDpi) { mutableStateOf(resolutionState.targetDpi) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClosePanel)
    ) {
        // Contenedor principal del panel lateral
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(360.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(RmBackground, Color(0xFF0A0F1D))
                    )
                )
                .border(
                    width = 1.dp,
                    color = RmAccentRed.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
                .clickable(enabled = false) {}
        ) {
            // Barra lateral de iconos de herramientas
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(68.dp)
                    .background(Color(0xFF070B14))
                    .padding(vertical = 16.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Logo cabecera Game Space
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RmAccentRed.copy(alpha = 0.15f))
                        .border(1.dp, RmAccentRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Game Space",
                        tint = RmAccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Lista de iconos de pestañas
                RedMagicTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) RmAccentRed.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) RmAccentRed else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeTab = tab }
                            .testTag("rm_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) RmAccentRed else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón cerrar panel
                IconButton(
                    onClick = onClosePanel,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RmSurface)
                        .testTag("rm_close_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar panel",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Cuerpo del contenido de la pestaña seleccionada
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera de la sección activa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = activeTab.title.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = targetGamePackage?.let { "Juego: $it" } ?: "Control en tiempo real",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (renderState.isDownscaleActive || resolutionState.isCustomResolutionActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RmAccentGreen.copy(alpha = 0.2f))
                                .border(1.dp, RmAccentGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "BOOST ACTIVO",
                                color = RmAccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                when (activeTab) {
                    RedMagicTab.RENDER -> {
                        RenderTabContent(
                            renderState = renderState,
                            targetGamePackage = targetGamePackage,
                            onRenderScaleChange = onRenderScaleChange,
                            onToggleMsaa = onToggleMsaa,
                            onResetGraphics = onResetGraphics
                        )
                    }

                    RedMagicTab.RESOLUTION -> {
                        ResolutionTabContent(
                            state = resolutionState,
                            selectedWidth = selectedWidth,
                            selectedHeight = selectedHeight,
                            selectedDpi = selectedDpi,
                            onWidthChange = { selectedWidth = it },
                            onHeightChange = { selectedHeight = it },
                            onDpiChange = { selectedDpi = it },
                            onPresetSelect = { preset ->
                                val nativeW = resolutionState.nativeSettings.width
                                val nativeH = resolutionState.nativeSettings.height
                                selectedWidth = (nativeW * preset.widthScale).toInt()
                                selectedHeight = (nativeH * preset.heightScale).toInt()
                                selectedDpi = preset.suggestedDpi
                            },
                            onToggleAutoDpi = onToggleAutoDpi,
                            onApply = {
                                onApplyResolution(selectedWidth, selectedHeight, selectedDpi)
                            },
                            onReset = onResetResolution
                        )
                    }

                    RedMagicTab.PERFORMANCE -> {
                        PerformanceTabContent()
                    }

                    RedMagicTab.TOOLS -> {
                        ToolsTabContent(
                            onResetAll = {
                                onResetResolution()
                                onResetGraphics()
                            }
                        )
                    }
                }
            }
        }
    }
}
