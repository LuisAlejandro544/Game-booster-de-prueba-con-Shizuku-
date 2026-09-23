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
 * Estado del compilador AOT para juegos.
 */
data class AotCompilerState(
    val isCompiling: Boolean = false,
    val targetPackage: String? = null,
    val progressMessage: String = "",
    val lastResultSuccess: Boolean? = null,
    val consoleOutput: String = ""
)

/**
 * Gestor de la Compilación Previa Ahead-Of-Time (AOT) contra el Micro-Stuttering.
 *
 * Utiliza el script 'compile_game_aot.sh' que invoca 'cmd package compile -m speed-profile -f <paquete>'
 * mediante Shizuku en segundo plano (Dispatchers.IO).
 *
 * Beneficio:
 * - Evita caídas de cuadros y tirones repentinos provocados por la compilación JIT durante la partida.
 * - Reduce tiempos de carga de pantallas y assets pesados.
 */
class AotCompilationManager(
    private val context: Context,
    private val scriptManager: ScriptManager
) {
    companion object {
        private const val TAG = "AotCompiler"
    }

    private val _state = MutableStateFlow(AotCompilerState())
    val state: StateFlow<AotCompilerState> = _state.asStateFlow()

    /**
     * Ejecuta la compilación AOT de un juego antes de iniciarlo.
     *
     * @param packageName Nombre del paquete del juego a optimizar.
     * @param filter Filtro de optimización ("speed-profile" o "speed").
     */
    suspend fun compileGame(
        packageName: String,
        filter: String = "speed-profile"
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        _state.update {
            it.copy(
                isCompiling = true,
                targetPackage = packageName,
                progressMessage = "Compilando clases y métodos con filtro $filter...",
                lastResultSuccess = null
            )
        }

        Log.i(TAG, "Iniciando compilación previa AOT para $packageName ($filter)...")
        val result = scriptManager.executeScript("compile_game_aot.sh", listOf(packageName, filter))

        val isSuccess = result.isSuccess && !result.stdout.contains("Failure", ignoreCase = true)
        _state.update {
            it.copy(
                isCompiling = false,
                lastResultSuccess = isSuccess,
                progressMessage = if (isSuccess) "Compilación AOT completada exitosamente." else "La compilación finalizó con advertencias.",
                consoleOutput = result.stdout
            )
        }

        Log.i(TAG, "Resultado compilación AOT para $packageName: exitCode=${result.exitCode}")
        result
    }

    /**
     * Limpia el estado de compilación.
     */
    fun resetState() {
        _state.update { AotCompilerState() }
    }
}
