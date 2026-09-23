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
 * Estado del gestor de liberación quirúrgica de memoria.
 */
data class MemoryTrimState(
    val isTrimming: Boolean = false,
    val lastTrimMessage: String = "Listo para optimizar",
    val lastTrimTimeMillis: Long = 0L,
    val totalTrimsCount: Int = 0
)

/**
 * Gestor de la Liberación Quirúrgica de Memoria de Aplicaciones en Segundo Plano.
 *
 * Utiliza el script 'trim_background_memory.sh' que envía la señal formal
 * 'cmd activity trim-memory --all RUNNING_CRITICAL' mediante el subsistema de Shizuku.
 *
 * Ventajas sobre task killers tradicionales:
 * - NO mata procesos bruscamente (evitando que Android vuelva a levantarlos consumiendo 100% de CPU).
 * - Notifica a las apps en segundo plano que purguen sus cachés no esenciales, liberando megabytes
 *   vitales de memoria RAM para el juego.
 */
class MemoryTrimManager(
    private val context: Context,
    private val scriptManager: ScriptManager
) {
    companion object {
        private const val TAG = "MemoryTrimManager"
    }

    private val _state = MutableStateFlow(MemoryTrimState())
    val state: StateFlow<MemoryTrimState> = _state.asStateFlow()

    /**
     * Ejecuta el comando de recorte quirúrgico de memoria en segundo plano.
     */
    suspend fun trimBackgroundMemory(): ShellCommandResult = withContext(Dispatchers.IO) {
        _state.update {
            it.copy(
                isTrimming = true,
                lastTrimMessage = "Solicitando purga de cachés y memoria en segundo plano..."
            )
        }

        Log.i(TAG, "Ejecutando liberación quirúrgica de memoria RAM...")
        val result = scriptManager.executeScript("trim_background_memory.sh")

        val success = result.isSuccess
        _state.update {
            it.copy(
                isTrimming = false,
                lastTrimMessage = if (success) "¡Memoria RAM liberada quirúrgicamente sin cierres forzados!" else "No se pudo completar el recorte de memoria",
                lastTrimTimeMillis = System.currentTimeMillis(),
                totalTrimsCount = if (success) it.totalTrimsCount + 1 else it.totalTrimsCount
            )
        }

        result
    }
}
