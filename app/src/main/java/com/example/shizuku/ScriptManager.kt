package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Resultado estructurado de la ejecución de un comando o script Shell.
 */
data class ShellCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val isSuccess: Boolean = exitCode == 0
)

/**
 * Gestor de scripts (.sh) y comandos del sistema a través de Shizuku.
 *
 * Características y correcciones:
 * 1. Ejecuta scripts transmitiendo el contenido directamente al standard input (stdin) de la shell.
 *    Esto elimina cualquier fallo por comillas simples/dobles, regex o caracteres especiales.
 * 2. Asigna los argumentos ($1, $2, ...) mediante 'set -- arg1 arg2' antes de ejecutar el script.
 * 3. Ejecuta mediante Shizuku.newProcess para gozar de privilegios ADB (UID 2000) o Root (UID 0),
 *    con fallback automático a Runtime si Shizuku no está disponible.
 * 4. Evita el bloqueo del hilo principal ejecutando estrictamente en Dispatchers.IO.
 */
class ScriptManager(private val context: Context) {

    companion object {
        private const val TAG = "ScriptManager"
        const val SCRIPTS_DIR_NAME = "gamebooster_scripts"
    }

    /**
     * Obtiene el directorio local de almacenamiento de scripts de la app.
     */
    fun getLocalScriptsDir(): File {
        val dir = File(context.filesDir, SCRIPTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copia los scripts .sh desde assets al directorio interno y asegura permisos de ejecución.
     */
    suspend fun deployScripts(): Boolean = withContext(Dispatchers.IO) {
        try {
            val scriptsDir = getLocalScriptsDir()
            val assetManager = context.assets
            val scriptFiles = assetManager.list("scripts") ?: emptyArray()

            for (scriptName in scriptFiles) {
                val destFile = File(scriptsDir, scriptName)
                assetManager.open("scripts/$scriptName").use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.setReadable(true, false)
                destFile.setExecutable(true, false)
                Log.d(TAG, "Script desplegado: ${destFile.absolutePath}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al desplegar scripts: ${e.message}", e)
            false
        }
    }

    /**
     * Crea un proceso shell nativo mediante Shizuku (UID 2000 / UID 0) o Runtime local.
     */
    private fun createShellProcess(): Process {
        val isShizukuReady = try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }

        if (isShizukuReady) {
            try {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply {
                    isAccessible = true
                }
                return method.invoke(null, arrayOf("sh"), null, null) as Process
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku.newProcess falló, probando con Runtime exec: ${e.message}")
            }
        }

        return Runtime.getRuntime().exec(arrayOf("sh"))
    }

    /**
     * Ejecuta un comando en la shell del sistema a través del proceso de Shizuku / Runtime.
     *
     * @param command Comando completo a ejecutar (ej: "wm size 1280x720")
     */
    suspend fun executeCommand(command: String): ShellCommandResult = withContext(Dispatchers.IO) {
        try {
            val process = createShellProcess()

            // Escribir el comando directamente en el stream de la shell
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(command)
                writer.write("\nexit\n")
                writer.flush()
            }

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "Comando ejecutado: '$command' -> Code: $exitCode, out: ${stdout.take(100)}")

            ShellCommandResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando comando: ${e.message}", e)
            ShellCommandResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Excepción desconocida"
            )
        }
    }

    /**
     * Ejecuta uno de los scripts .sh pasando los argumentos indicados mediante streaming seguro a sh.
     *
     * @param scriptName Nombre del script en assets/scripts (ej: "apply_resolution.sh")
     * @param args Argumentos del script (ej: listOf("1280", "720", "320"))
     */
    suspend fun executeScript(scriptName: String, args: List<String> = emptyList()): ShellCommandResult = withContext(Dispatchers.IO) {
        val scriptFile = File(getLocalScriptsDir(), scriptName)
        if (!scriptFile.exists()) {
            deployScripts()
        }

        val scriptContent = try {
            if (scriptFile.exists()) {
                scriptFile.readText()
            } else {
                context.assets.open("scripts/$scriptName").bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            return@withContext ShellCommandResult(-1, "", "No se pudo leer el archivo de script: ${e.message}")
        }

        try {
            val process = createShellProcess()

            // Transmitir configuración de argumentos ($1, $2, ...) y contenido íntegro del script
            process.outputStream.bufferedWriter().use { writer ->
                if (args.isNotEmpty()) {
                    val safeArgs = args.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }
                    writer.write("set -- $safeArgs\n")
                }
                writer.write(scriptContent)
                writer.write("\nexit\n")
                writer.flush()
            }

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "Script $scriptName ejecutado -> Code: $exitCode, out: ${stdout.take(120)}")

            ShellCommandResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando script $scriptName: ${e.message}", e)
            ShellCommandResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Excepción en ejecución de script"
            )
        }
    }
}
