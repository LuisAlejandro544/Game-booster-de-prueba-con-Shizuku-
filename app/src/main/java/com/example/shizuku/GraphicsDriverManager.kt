package com.example.shizuku

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Controladores gráficos soportados para conmutación en caliente.
 *
 * @param id Identificador en minúsculas para los scripts shell de Shizuku.
 * @param displayName Nombre visible para el usuario en la interfaz gráfica.
 * @param technicalDesc Breve explicación técnica de su impacto en la GPU/CPU.
 */
enum class GraphicsDriver(
    val id: String,
    val displayName: String,
    val technicalDesc: String
) {
    DEFAULT(
        id = "default",
        displayName = "Por Defecto",
        technicalDesc = "Controlador nativo asignado por el motor del juego"
    ),
    OPENGL(
        id = "opengl",
        displayName = "OpenGL ES",
        technicalDesc = "Mayor compatibilidad en títulos antiguos o emuladores"
    ),
    VULKAN(
        id = "vulkan",
        displayName = "Vulkan",
        technicalDesc = "Baja sobrecarga de CPU y mejor aprovechamiento multinúcleo"
    ),
    ANGLE(
        id = "angle",
        displayName = "ANGLE",
        technicalDesc = "Capa de Google que traduce llamadas OpenGL ES a Vulkan"
    );

    companion object {
        fun fromId(id: String): GraphicsDriver {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

/**
 * Estado que describe el controlador gráfico del juego en foco.
 */
data class GraphicsDriverState(
    val detectedDefaultDriver: GraphicsDriver = GraphicsDriver.DEFAULT,
    val currentActiveDriver: GraphicsDriver = GraphicsDriver.DEFAULT,
    val gpuName: String = "Detectando GPU...",
    val glesVersion: String = "OpenGL ES 3.2",
    val vulkanVersion: String = "Vulkan 1.1+",
    val isOperating: Boolean = false,
    val isCustomDriverActive: Boolean = false,
    val statusMessage: String = "Controlador listo"
)

/**
 * GraphicsDriverManager: Gestor de conmutación y detección de controladores gráficos.
 *
 * Propósito y Lógica:
 * 1. Detecta automáticamente qué controlador gráfico utiliza un juego por defecto (OpenGL ES o Vulkan)
 *    inspeccionando mapas de memoria en tiempo de ejecución (/proc/$PID/maps), configuraciones de
 *    Android y telemetría de SurfaceFlinger.
 * 2. Permite al usuario forzar OpenGL ES, Vulkan o ANGLE (Almost Native Graphics Layer Engine)
 *    durante la sesión de juego.
 * 3. Garantiza que cuando el usuario salga del juego o se detenga el servicio flotante,
 *    se restaure inmediatamente el controlador normal del sistema.
 * 4. NUNCA toca 'persist.sys.*', operando únicamente con variables de sesión seguras.
 * 5. Ejecuta todas las llamadas Shell en [Dispatchers.IO] para evitar congelamientos en la UI.
 */
class GraphicsDriverManager(
    private val context: Context,
    private val scriptManager: ScriptManager
) {
    companion object {
        private const val TAG = "GraphicsDriverManager"
    }

    private val _state = MutableStateFlow(GraphicsDriverState())
    val state: StateFlow<GraphicsDriverState> = _state.asStateFlow()

    /**
     * Detecta el controlador gráfico del juego objetivo inspeccionando el sistema.
     *
     * @param packageName Nombre del paquete del juego (ej: "com.dts.freefireth")
     */
    suspend fun detectDriver(packageName: String): GraphicsDriverState = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext _state.value

        _state.update { it.copy(isOperating = true, statusMessage = "Detectando controlador...") }

        val result = scriptManager.executeScript("detect_graphics_driver.sh", listOf(packageName))
        var detectedDefault = GraphicsDriver.OPENGL
        var detectedActive = GraphicsDriver.DEFAULT
        var gpuName = "GPU del Dispositivo"
        var glesVersion = "OpenGL ES 3.2"
        var vulkanVersion = "Vulkan 1.1+"

        if (result.isSuccess && result.stdout.isNotBlank()) {
            result.stdout.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("DRIVER_ACTIVE=") -> {
                        val value = trimmed.removePrefix("DRIVER_ACTIVE=").trim()
                        detectedActive = when (value.uppercase()) {
                            "VULKAN" -> GraphicsDriver.VULKAN
                            "ANGLE" -> GraphicsDriver.ANGLE
                            "OPENGL" -> GraphicsDriver.OPENGL
                            else -> GraphicsDriver.DEFAULT
                        }
                    }
                    trimmed.startsWith("DRIVER_DEFAULT=") -> {
                        val value = trimmed.removePrefix("DRIVER_DEFAULT=").trim()
                        detectedDefault = when (value.uppercase()) {
                            "VULKAN" -> GraphicsDriver.VULKAN
                            "ANGLE" -> GraphicsDriver.ANGLE
                            else -> GraphicsDriver.OPENGL
                        }
                    }
                    trimmed.startsWith("GPU_NAME=") -> {
                        val name = trimmed.removePrefix("GPU_NAME=").trim()
                        if (name.isNotBlank()) gpuName = name
                    }
                    trimmed.startsWith("GLES_VERSION=") -> {
                        val ver = trimmed.removePrefix("GLES_VERSION=").trim()
                        if (ver.isNotBlank()) glesVersion = ver
                    }
                    trimmed.startsWith("VULKAN_VERSION=") -> {
                        val ver = trimmed.removePrefix("VULKAN_VERSION=").trim()
                        if (ver.isNotBlank()) vulkanVersion = ver
                    }
                }
            }
        }

        _state.update { current ->
            current.copy(
                detectedDefaultDriver = detectedDefault,
                currentActiveDriver = if (current.isCustomDriverActive) current.currentActiveDriver else detectedActive,
                gpuName = gpuName,
                glesVersion = glesVersion,
                vulkanVersion = vulkanVersion,
                isOperating = false,
                statusMessage = "Detectado: ${detectedDefault.displayName} (${gpuName.take(24)})"
            )
        }

        Log.d(TAG, "Detección completada para $packageName: Default=$detectedDefault, Active=$detectedActive")
        _state.value
    }

    /**
     * Aplica el controlador gráfico seleccionado para el juego indicado.
     *
     * @param packageName Paquete del juego en primer plano.
     * @param driver Controlador deseado (OPENGL, VULKAN, ANGLE o DEFAULT).
     */
    suspend fun applyDriver(packageName: String, driver: GraphicsDriver): Boolean = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext false

        _state.update {
            it.copy(
                isOperating = true,
                statusMessage = "Aplicando controlador ${driver.displayName}..."
            )
        }

        val isReset = driver == GraphicsDriver.DEFAULT
        val action = if (isReset) "reset" else driver.id
        val result = scriptManager.executeScript("apply_graphics_driver.sh", listOf(action, packageName))
        val success = result.isSuccess

        _state.update { current ->
            current.copy(
                currentActiveDriver = if (success) driver else current.currentActiveDriver,
                isCustomDriverActive = success && !isReset,
                isOperating = false,
                statusMessage = if (success) {
                    if (isReset) {
                        "Controlador por defecto restaurado (${current.detectedDefaultDriver.displayName})"
                    } else {
                        "Controlador activo: ${driver.displayName}"
                    }
                } else {
                    "Fallo al aplicar controlador: ${result.stderr.take(50)}"
                }
            )
        }

        Log.d(TAG, "applyDriver ($driver) en $packageName: $success | ${result.stdout}")
        success
    }

    /**
     * Restablece el controlador gráfico del juego a su configuración estándar.
     * Llamado automáticamente al salir del juego o al destruir el servicio flotante.
     *
     * @param packageName Paquete del juego, opcional.
     */
    suspend fun resetDriver(packageName: String?): Boolean = withContext(Dispatchers.IO) {
        _state.update {
            it.copy(
                isOperating = true,
                statusMessage = "Restableciendo controlador gráfico a valores del sistema..."
            )
        }

        val result = scriptManager.executeScript("apply_graphics_driver.sh", listOf("reset", packageName ?: ""))
        val success = result.isSuccess

        _state.update { current ->
            current.copy(
                currentActiveDriver = current.detectedDefaultDriver,
                isCustomDriverActive = false,
                isOperating = false,
                statusMessage = "Controlador gráfico restablecido al sistema"
            )
        }

        Log.i(TAG, "resetDriver completado: $success")
        success
    }
}
