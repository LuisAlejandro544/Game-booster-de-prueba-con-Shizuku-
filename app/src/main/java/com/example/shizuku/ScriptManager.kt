package com.example.shizuku

import android.content.Context
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
 * Características:
 * 1. Extrae los scripts desde assets a un directorio seguro ejecutable (/data/local/tmp/gamebooster/).
 * 2. Ejecuta mediante Shizuku.newProcess para gozar de privilegios ADB (UID 2000) o Root (UID 0).
 * 3. Permite edición rápida de scripts .sh sin necesidad de recompilar la aplicación.
 * 4. Evita el bloqueo del hilo principal ejecutando todo en Dispatchers.IO.
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
     * Ejecuta un comando en la shell del sistema a través del proceso de Shizuku.
     *
     * @param command Comando completo a ejecutar (ej: "wm size 1280x720")
     */
    suspend fun executeCommand(command: String): ShellCommandResult = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext ShellCommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "El Binder de Shizuku no está disponible."
                )
            }

            val process = try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply {
                    isAccessible = true
                }
                newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
            } catch (refEx: Exception) {
                Log.w(TAG, "Invocación por reflexión de Shizuku.newProcess falló, intentando Runtime: ${refEx.message}")
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            ShellCommandResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando comando via Shizuku: ${e.message}", e)
            ShellCommandResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Excepción desconocida"
            )
        }
    }

    /**
     * Ejecuta uno de los scripts .sh desplegados pasando los argumentos indicados.
     *
     * @param scriptName Nombre del script en assets/scripts (ej: "apply_resolution.sh")
     * @param args Argumentos del script (ej: listOf("1280", "720", "320"))
     */
    suspend fun executeScript(scriptName: String, args: List<String> = emptyList()): ShellCommandResult {
        val scriptFile = File(getLocalScriptsDir(), scriptName)
        if (!scriptFile.exists()) {
            deployScripts()
        }

        val scriptContent = try {
            scriptFile.readText()
        } catch (e: Exception) {
            return ShellCommandResult(-1, "", "No se pudo leer el archivo de script: ${e.message}")
        }

        // Para evitar problemas de permisos de lectura de la shell en directorios privados de la app,
        // pasamos el contenido del script directamente a sh con sus argumentos o lo invocamos vía sh -s
        val argsString = args.joinToString(" ")
        val command = "sh -c '$scriptContent' sh $argsString"
        return executeCommand(command)
    }
}
