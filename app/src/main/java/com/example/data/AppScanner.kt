package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representa una aplicación o juego instalado en el dispositivo del usuario.
 *
 * @param packageName Nombre del paquete único (ej: "com.dts.freefireth").
 * @param appName Nombre legible de la aplicación (ej: "Free Fire").
 * @param isGame Indica si el sistema Android lo cataloga como juego (CATEGORY_GAME o FLAG_IS_GAME).
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isGame: Boolean
)

/**
 * Representa un juego añadido manualmente por el usuario a la biblioteca del Game Booster.
 *
 * @param id Identificador único interno.
 * @param name Nombre de la aplicación o juego.
 * @param packageName Nombre del paquete para invocarlo o aplicar ajustes.
 * @param targetResolutionText Etiqueta descriptiva del preset deseado.
 */
data class GameItem(
    val id: String,
    val name: String,
    val packageName: String,
    val targetResolutionText: String = "75% HD+ (320 DPI)"
)

/**
 * Escáner de aplicaciones y juegos instalados en el dispositivo.
 *
 * Funcionalidades y Principios:
 * 1. Ejecuta el escaneo estrictamente en [Dispatchers.IO] para no bloquear el hilo de interfaz.
 * 2. Utiliza PackageManager consultando actividades de tipo LAUNCHER.
 * 3. Identifica si una app es juego analizando las categorías nativas de Android (CATEGORY_GAME).
 * 4. Excluye la propia aplicación del Game Booster para no listarse a sí misma.
 */
class AppScanner(private val context: Context) {

    companion object {
        private const val TAG = "AppScanner"
    }

    /**
     * Escanea todas las aplicaciones con interfaz ejecutable instaladas en el teléfono.
     * Retorna la lista ordenada, priorizando los juegos detectados y en orden alfabético.
     */
    suspend fun scanInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val results = mutableListOf<InstalledApp>()
        val seenPackages = mutableSetOf<String>()

        try {
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val currentPkg = context.packageName

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (pkgName == currentPkg || seenPackages.contains(pkgName)) {
                    continue
                }
                seenPackages.add(pkgName)

                val label = try {
                    info.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkgName
                }

                val isGame = try {
                    val appInfo = info.activityInfo.applicationInfo
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                    } else {
                        @Suppress("DEPRECATION")
                        (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                    }
                } catch (e: Exception) {
                    false
                }

                results.add(
                    InstalledApp(
                        packageName = pkgName,
                        appName = label,
                        isGame = isGame
                    )
                )
            }

            // Ordenar: primero los juegos detectados, luego alfabéticamente por nombre
            results.sortWith(
                compareByDescending<InstalledApp> { it.isGame }
                    .thenBy { it.appName.lowercase() }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando aplicaciones instaladas: ${e.message}", e)
        }

        results
    }
}

/**
 * Almacenamiento local persistente para los juegos añadidos manualmente por el usuario.
 * Utiliza SharedPreferences con formato JSON estructurado, garantizando que los juegos
 * añadidos se conserven incluso al reiniciar la aplicación o el dispositivo móvil.
 */
class GameStorage(context: Context) {

    private val prefs = context.getSharedPreferences("game_booster_custom_games", Context.MODE_PRIVATE)
    private val keyGames = "saved_games_json"

    /**
     * Carga la lista de juegos guardados por el usuario.
     */
    fun loadGames(): List<GameItem> {
        val rawJson = prefs.getString(keyGames, null) ?: return emptyList()
        val list = mutableListOf<GameItem>()

        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    GameItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        name = obj.optString("name", "Juego"),
                        packageName = obj.optString("packageName", ""),
                        targetResolutionText = obj.optString("targetResolutionText", "75% HD+ (320 DPI)")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GameStorage", "Error parseando juegos guardados: ${e.message}")
        }

        return list
    }

    /**
     * Guarda la lista de juegos del usuario de manera persistente.
     */
    fun saveGames(games: List<GameItem>) {
        try {
            val jsonArray = JSONArray()
            for (game in games) {
                val obj = JSONObject().apply {
                    put("id", game.id)
                    put("name", game.name)
                    put("packageName", game.packageName)
                    put("targetResolutionText", game.targetResolutionText)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(keyGames, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("GameStorage", "Error guardando lista de juegos: ${e.message}")
        }
    }
}
