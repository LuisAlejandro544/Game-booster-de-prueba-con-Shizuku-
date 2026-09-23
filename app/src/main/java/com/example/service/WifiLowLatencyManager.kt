package com.example.service

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor del Modo Wi-Fi de Ultrabaja Latencia para juegos en línea.
 *
 * Utiliza la API nativa de Android:
 * WifiManager.WIFI_MODE_FULL_LOW_LATENCY (introducido en Android 10 / API 29).
 *
 * Beneficios técnicos:
 * - Deshabilita los ciclos de ahorro de energía y suspensiones del chip Wi-Fi mientras el juego está activo.
 * - Reduce el jitter y estabiliza el ping (latencia) en juegos multijugador competitivos.
 * - Adquisición y liberación segura y controlada.
 */
class WifiLowLatencyManager private constructor(context: Context) {

    companion object {
        private const val TAG = "WifiLowLatencyManager"

        @Volatile
        private var instance: WifiLowLatencyManager? = null

        fun getInstance(context: Context): WifiLowLatencyManager {
            return instance ?: synchronized(this) {
                instance ?: WifiLowLatencyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val wifiManager: WifiManager? = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var wifiLock: WifiManager.WifiLock? = null

    private val _isLowLatencyActive = MutableStateFlow(false)
    val isLowLatencyActive: StateFlow<Boolean> = _isLowLatencyActive.asStateFlow()

    private val _isEnabledByUser = MutableStateFlow(true)
    val isEnabledByUser: StateFlow<Boolean> = _isEnabledByUser.asStateFlow()

    /**
     * Habilita o deshabilita la preferencia del usuario para el modo de baja latencia.
     */
    fun setEnabledByUser(enabled: Boolean) {
        _isEnabledByUser.value = enabled
        if (!enabled && _isLowLatencyActive.value) {
            releaseLowLatency()
        }
    }

    /**
     * Adquiere el bloqueo Wi-Fi de baja latencia al iniciar la sesión de juego.
     */
    @Synchronized
    fun acquireLowLatency(): Boolean {
        if (!_isEnabledByUser.value) {
            Log.d(TAG, "Modo baja latencia deshabilitado por el usuario.")
            return false
        }

        if (wifiLock?.isHeld == true) {
            Log.d(TAG, "WifiLock de ultrabaja latencia ya está retenido.")
            _isLowLatencyActive.value = true
            return true
        }

        return try {
            if (wifiManager == null) {
                Log.w(TAG, "WifiManager no disponible en este dispositivo.")
                return false
            }

            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }

            wifiLock = wifiManager.createWifiLock(mode, "GameBooster:LowLatencyLock").apply {
                setReferenceCounted(false)
                acquire()
            }

            _isLowLatencyActive.value = true
            Log.i(TAG, "WifiLock de ultrabaja latencia adquirido con éxito (Mode: $mode).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adquiriendo WifiLock: ${e.message}", e)
            _isLowLatencyActive.value = false
            false
        }
    }

    /**
     * Libera el bloqueo de Wi-Fi restaurando el modo normal de ahorro de energía del módem.
     */
    @Synchronized
    fun releaseLowLatency() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.i(TAG, "WifiLock de ultrabaja latencia liberado.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando WifiLock: ${e.message}", e)
        } finally {
            wifiLock = null
            _isLowLatencyActive.value = false
        }
    }
}
