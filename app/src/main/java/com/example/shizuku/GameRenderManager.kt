package com.example.shizuku

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Estado que describe la configuración del motor de renderizado del juego.
 *
 * @param renderScale Factor de escala de la superficie 3D (0.50f = 50%, 1.0f = 100%).
 * @param isDownscaleActive Indica si hay una reducción activa en el juego actual.
 * @param isMsaaDisabled Indica si el 4x MSAA y filtros pesados fueron desactivados forzadamente.
 * @param isOperating Indica si se está ejecutando una llamada de shell en segundo plano.
 * @param statusMessage Mensaje de diagnóstico descriptivo para la interfaz de usuario.
 */
data class GameRenderState(
    val renderScale: Float = 1.0f,
    val isDownscaleActive: Boolean = false,
    val isMsaaDisabled: Boolean = false,
    val isOperating: Boolean = false,
    val statusMessage: String = "Renderizado nativo (100%)"
)

/**
 * GameRenderManager: Gestor del motor de renderizado y filtros gráficos.
 *
 * Propósito y Lógica:
 * 1. Controla la reducción de la superficie 3D mediante 'cmd game set --downscale' (Android 12+).
 *    A diferencia de 'wm size', este comando no modifica los menús ni los textos del sistema Android,
 *    sino que ordena a la GPU renderizar los gráficos del juego a una resolución interna menor.
 * 2. Controla la desactivación forzada de 4x MSAA (Multisample Anti-Aliasing) y la optimización
 *    de buffers de GPU para eliminar sobrecarga de procesamiento en chips gráficos de gama media.
 * 3. Ejecuta estrictamente en [Dispatchers.IO] para evitar cualquier retardo en el hilo principal.
 * 4. NUNCA toca variables persistentes 'persist.sys.*' para evitar riesgo de inestabilidad.
 */
class GameRenderManager(
    private val context: Context,
    private val scriptManager: ScriptManager
) {
    companion object {
        private const val TAG = "GameRenderManager"
    }

    private val _state = MutableStateFlow(GameRenderState())
    val state: StateFlow<GameRenderState> = _state.asStateFlow()

    /**
     * Aplica la escala de renderizado interno de la superficie del juego especificado.
     *
     * @param packageName Paquete del juego (ej: "com.dts.freefireth")
     * @param scale Escala entre 0.50f (50% resolución interna) y 1.0f (100% nativo)
     */
    suspend fun applyRenderScale(packageName: String, scale: Float): Boolean = withContext(Dispatchers.IO) {
        val clampedScale = scale.coerceIn(0.50f, 1.0f)
        val formattedScale = String.format(Locale.US, "%.2f", clampedScale)

        _state.update {
            it.copy(
                isOperating = true,
                statusMessage = "Aplicando render scale: ${(clampedScale * 100).toInt()}%..."
            )
        }

        var result = if (clampedScale >= 0.98f) {
            // Si la escala vuelve al 100%, restauramos mediante cmd game reset
            scriptManager.executeScript("reset_game_render_scale.sh", listOf(packageName))
        } else {
            scriptManager.executeScript("apply_game_render_scale.sh", listOf(formattedScale, packageName))
        }

        // Fallback directo con cmd game o device_config
        if (!result.isSuccess) {
            val fallbackCmd = if (clampedScale >= 0.98f) {
                "cmd game reset $packageName || device_config delete game_overlay $packageName"
            } else {
                "cmd game mode performance $packageName ; cmd game set --downscale $formattedScale $packageName ; device_config put game_overlay $packageName \"mode=2,downscaleFactor=$formattedScale\""
            }
            val fallbackResult = scriptManager.executeCommand(fallbackCmd)
            if (fallbackResult.isSuccess) {
                result = fallbackResult
            }
        }

        val success = result.isSuccess
        val isReduced = clampedScale < 0.98f && success

        _state.update {
            it.copy(
                renderScale = clampedScale,
                isDownscaleActive = isReduced,
                isOperating = false,
                statusMessage = if (success) {
                    if (isReduced) {
                        "Render scale activo: ${(clampedScale * 100).toInt()}% (Ahorro GPU)"
                    } else {
                        "Render scale nativo: 100%"
                    }
                } else {
                    "Error al aplicar render scale: ${result.stderr}"
                }
            )
        }

        Log.d(TAG, "Resultado applyRenderScale ($formattedScale): $success | ${result.stdout}")
        success
    }

    /**
     * Restablece la escala de renderizado a su valor por defecto (100% nativo).
     */
    suspend fun resetRenderScale(packageName: String): Boolean = withContext(Dispatchers.IO) {
        _state.update { it.copy(isOperating = true, statusMessage = "Restableciendo render scale...") }

        val result = scriptManager.executeScript("reset_game_render_scale.sh", listOf(packageName))
        val success = result.isSuccess

        _state.update {
            it.copy(
                renderScale = 1.0f,
                isDownscaleActive = false,
                isOperating = false,
                statusMessage = if (success) "Render scale nativo restaurado (100%)" else "Error: ${result.stderr}"
            )
        }

        success
    }

    /**
     * Activa o desactiva la anulación forzada de 4x MSAA y filtros pesados de la GPU.
     */
    suspend fun toggleDisableMsaa(disable: Boolean): Boolean = withContext(Dispatchers.IO) {
        val action = if (disable) "disable_msaa" else "reset"
        val result = scriptManager.executeScript("apply_graphic_filters.sh", listOf(action))
        val success = result.isSuccess

        _state.update {
            it.copy(
                isMsaaDisabled = if (success) disable else it.isMsaaDisabled,
                statusMessage = if (success) {
                    if (disable) "4x MSAA desactivado (Menos carga en GPU)" else "Filtros de GPU restablecidos"
                } else {
                    "Error al cambiar filtros: ${result.stderr}"
                }
            )
        }

        success
    }

    /**
     * Restaura completamente todos los ajustes gráficos (Render Scale y Filtros MSAA).
     */
    suspend fun resetAll(packageName: String?): Boolean = withContext(Dispatchers.IO) {
        if (!packageName.isNullOrBlank()) {
            scriptManager.executeScript("reset_game_render_scale.sh", listOf(packageName))
        }
        scriptManager.executeScript("apply_graphic_filters.sh", listOf("reset"))

        _state.update {
            it.copy(
                renderScale = 1.0f,
                isDownscaleActive = false,
                isMsaaDisabled = false,
                isOperating = false,
                statusMessage = "Ajustes gráficos restaurados a valores nativos"
            )
        }

        true
    }
}
