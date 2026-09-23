package com.example.shizuku

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Representa la configuración de resolución y densidad de pantalla.
 */
data class DisplaySettings(
    val width: Int,
    val height: Int,
    val dpi: Int
) {
    val description: String
        get() = "${width}x${height} @ ${dpi} DPI"
}

/**
 * Estado general de la resolución de pantalla.
 */
data class DisplayResolutionState(
    val nativeSettings: DisplaySettings = DisplaySettings(1080, 2400, 420),
    val currentSettings: DisplaySettings = DisplaySettings(1080, 2400, 420),
    val isCustomResolutionActive: Boolean = false,
    val autoCalculateDpi: Boolean = true,
    val targetWidth: Int = 1080,
    val targetHeight: Int = 2400,
    val targetDpi: Int = 420,
    val statusMessage: String = "Resolución nativa activa",
    val isOperating: Boolean = false
)

/**
 * Administrador de resolución de pantalla y DPI.
 *
 * Utiliza los scripts `apply_resolution.sh` y `reset_resolution.sh` mediante ScriptManager.
 * Ofrece cálculo automático de DPI para conservar la escala proporcional en pantalla.
 */
class ResolutionManager(
    private val context: Context,
    private val scriptManager: ScriptManager
) {
    companion object {
        private const val TAG = "ResolutionManager"
    }

    private val _state = MutableStateFlow(DisplayResolutionState())
    val state: StateFlow<DisplayResolutionState> = _state.asStateFlow()

    init {
        detectInitialDisplayMetrics()
    }

    /**
     * Detecta las métricas nativas de la pantalla mediante WindowManager de Android.
     */
    fun detectInitialDisplayMetrics() {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            val nativeWidth = metrics.widthPixels
            val nativeHeight = metrics.heightPixels
            val nativeDpi = metrics.densityDpi

            val detected = DisplaySettings(nativeWidth, nativeHeight, nativeDpi)
            Log.d(TAG, "Métricas detectadas: ${detected.description}")

            _state.update {
                it.copy(
                    nativeSettings = detected,
                    currentSettings = detected,
                    targetWidth = nativeWidth,
                    targetHeight = nativeHeight,
                    targetDpi = nativeDpi
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando métricas de pantalla: ${e.message}", e)
        }
    }

    /**
     * Calcula automáticamente el DPI proporcional a una resolución objetivo.
     * Ejemplo: si se reduce el ancho de 1080 a 720 (escala ~0.66), el DPI de 420 se reduce a ~280.
     */
    fun calculateProportionalDpi(targetWidth: Int, targetHeight: Int): Int {
        val native = _state.value.nativeSettings
        if (native.width <= 0) return 320

        val ratio = targetWidth.toFloat() / native.width.toFloat()
        val calculated = (native.dpi * ratio).toInt()
        return calculated.coerceIn(120, 640)
    }

    /**
     * Actualiza la resolución objetivo deseada.
     */
    fun setTargetResolution(width: Int, height: Int, customDpi: Int? = null) {
        val autoDpi = _state.value.autoCalculateDpi
        val dpiToUse = if (autoDpi || customDpi == null) {
            calculateProportionalDpi(width, height)
        } else {
            customDpi
        }

        _state.update {
            it.copy(
                targetWidth = width,
                targetHeight = height,
                targetDpi = dpiToUse
            )
        }
    }

    /**
     * Alterna la opción de cálculo automático de DPI.
     */
    fun toggleAutoCalculateDpi(enabled: Boolean) {
        _state.update { current ->
            val newDpi = if (enabled) {
                calculateProportionalDpi(current.targetWidth, current.targetHeight)
            } else {
                current.targetDpi
            }
            current.copy(
                autoCalculateDpi = enabled,
                targetDpi = newDpi
            )
        }
    }

    /**
     * Aplica la resolución y DPI objetivo utilizando el script apply_resolution.sh
     */
    suspend fun applyResolution(width: Int, height: Int, dpi: Int): Boolean = withContext(Dispatchers.IO) {
        _state.update { it.copy(isOperating = true, statusMessage = "Aplicando resolución...") }

        var result = scriptManager.executeScript(
            scriptName = "apply_resolution.sh",
            args = listOf(width.toString(), height.toString(), dpi.toString())
        )

        // Fallback directo con comando nativo wm si el script devolvió código no exitoso
        if (!result.isSuccess) {
            Log.w(TAG, "Script apply_resolution.sh retornó ${result.exitCode}, aplicando comandos wm directos...")
            val directCmd = if (dpi > 0) {
                "wm size ${width}x${height} && wm density $dpi"
            } else {
                "wm size ${width}x${height}"
            }
            val fallbackResult = scriptManager.executeCommand(directCmd)
            if (fallbackResult.isSuccess) {
                result = fallbackResult
            }
        }

        val success = result.isSuccess
        val newSettings = if (success) DisplaySettings(width, height, dpi) else _state.value.currentSettings

        _state.update {
            it.copy(
                isOperating = false,
                isCustomResolutionActive = success,
                currentSettings = newSettings,
                targetWidth = width,
                targetHeight = height,
                targetDpi = dpi,
                statusMessage = if (success) {
                    "Resolución aplicada: ${width}x${height} @ ${dpi} DPI"
                } else {
                    "Error al aplicar resolución: ${result.stderr}"
                }
            )
        }

        success
    }

    /**
     * Restaura la resolución y DPI nativos del dispositivo mediante reset_resolution.sh
     */
    suspend fun resetResolution(): Boolean = withContext(Dispatchers.IO) {
        _state.update { it.copy(isOperating = true, statusMessage = "Restaurando resolución nativa...") }

        var result = scriptManager.executeScript(
            scriptName = "reset_resolution.sh",
            args = emptyList()
        )

        // Fallback directo a reset de wm
        if (!result.isSuccess) {
            Log.w(TAG, "Script reset_resolution.sh retornó ${result.exitCode}, ejecutando reset wm directo...")
            val fallbackResult = scriptManager.executeCommand("wm size reset && wm density reset")
            if (fallbackResult.isSuccess) {
                result = fallbackResult
            }
        }

        val success = result.isSuccess
        val native = _state.value.nativeSettings

        _state.update {
            it.copy(
                isOperating = false,
                isCustomResolutionActive = false,
                currentSettings = native,
                targetWidth = native.width,
                targetHeight = native.height,
                targetDpi = native.dpi,
                statusMessage = if (success) {
                    "Resolución nativa restaurada (${native.description})"
                } else {
                    "Error al restaurar: ${result.stderr}"
                }
            )
        }

        success
    }
}
