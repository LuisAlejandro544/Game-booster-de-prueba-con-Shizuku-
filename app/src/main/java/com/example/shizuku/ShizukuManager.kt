package com.example.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Gestor principal para la comunicación con Shizuku / Sui.
 *
 * Esta clase encapsula la verificación del ciclo de vida del Binder de Shizuku,
 * la gestión de escuchadores (Listeners) y las solicitudes de permisos en tiempo de ejecución.
 *
 * Utiliza Coroutines para realizar comprobaciones en Dispatchers.IO evitando congelar el hilo principal.
 */
class ShizukuManager(private val context: Context) {

    companion object {
        const val TAG = "ShizukuManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_PERMISSION = 7001
    }

    private val _state = MutableStateFlow(ShizukuState(isLoading = true))
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku Binder recibido.")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku Binder ha muerto o el servicio se detuvo.")
        _state.update {
            it.copy(
                isBinderAlive = false,
                isPermissionGranted = false,
                uid = null,
                version = null,
                message = "El servicio Shizuku se ha detenido."
            )
        }
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "Resultado de permiso Shizuku: $granted")
                refreshStatus()
            }
        }

    private var isListenersRegistered = false

    /**
     * Registra los escuchadores de Shizuku y verifica el estado inicial.
     */
    fun registerListeners() {
        if (!isListenersRegistered) {
            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
                isListenersRegistered = true
                Log.d(TAG, "Listeners de Shizuku registrados exitosamente.")
            } catch (e: Throwable) {
                Log.e(TAG, "Error registrando listeners de Shizuku: ${e.message}", e)
            }
        }
        refreshStatus()
    }

    /**
     * Libera los escuchadores para prevenir fugas de memoria cuando el ciclo de vida finalice.
     */
    fun unregisterListeners() {
        if (isListenersRegistered) {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
                isListenersRegistered = false
                Log.d(TAG, "Listeners de Shizuku desregistrados.")
            } catch (e: Throwable) {
                Log.e(TAG, "Error al desregistrar listeners de Shizuku: ${e.message}", e)
            }
        }
    }

    /**
     * Comprueba si la aplicación Shizuku está instalada en el sistema.
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Consulta y actualiza el estado general de Shizuku en segundo plano.
     */
    fun refreshStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            _state.update { it.copy(isLoading = true) }
            val installed = isShizukuInstalled()

            var binderAlive = false
            var permissionGranted = false
            var uid: Int? = null
            var version: Int? = null
            var isPreV11 = false
            var message = ""

            try {
                binderAlive = Shizuku.pingBinder()
            } catch (e: Throwable) {
                Log.w(TAG, "pingBinder falló: ${e.message}")
            }

            if (binderAlive) {
                try {
                    isPreV11 = Shizuku.isPreV11()
                    version = Shizuku.getVersion()
                    uid = Shizuku.getUid()

                    if (isPreV11) {
                        message = "Versión de Shizuku no soportada (pre-v11)."
                    } else {
                        val permCheck = Shizuku.checkSelfPermission()
                        permissionGranted = permCheck == PackageManager.PERMISSION_GRANTED
                        message = if (permissionGranted) {
                            "Shizuku activo y autorizado con éxito."
                        } else {
                            "Servicio Shizuku activo. Requiere autorización."
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error consultando detalles de Shizuku: ${e.message}", e)
                    message = "Error consultando Shizuku: ${e.message}"
                }
            } else {
                message = if (installed) {
                    "La aplicación Shizuku está instalada pero el servicio no se está ejecutando."
                } else {
                    "Shizuku no está instalado en este dispositivo."
                }
            }

            _state.update {
                it.copy(
                    isInstalled = installed,
                    isBinderAlive = binderAlive,
                    isPermissionGranted = permissionGranted,
                    uid = uid,
                    version = version,
                    isPreV11 = isPreV11,
                    message = message,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Acción ejecutada por el botón principal para activar Shizuku:
     * 1. Si no está instalado: Ofrece abrir el enlace de descarga o APK.
     * 2. Si no está en ejecución: Abre la aplicación Shizuku para que el usuario inicie el servicio.
     * 3. Si está en ejecución pero sin permiso: Solicita el permiso oficial de Shizuku.
     * 4. Si ya está concedido: Notifica que ya está listo.
     */
    fun activateShizuku(activityContext: Context) {
        val currentState = _state.value

        when {
            !currentState.isInstalled && !currentState.isBinderAlive -> {
                // Caso: no instalado
                openDownloadPage(activityContext)
            }
            !currentState.isBinderAlive -> {
                // Caso: instalado pero servicio inactivo
                openShizukuApp(activityContext)
            }
            currentState.isBinderAlive && !currentState.isPermissionGranted -> {
                // Caso: servicio activo pero falta permiso
                try {
                    if (Shizuku.isPreV11()) {
                        _state.update { it.copy(message = "Versión antigua de Shizuku incompatible.") }
                    } else {
                        Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error al solicitar permiso a Shizuku: ${e.message}", e)
                    _state.update { it.copy(message = "Fallo al solicitar permiso: ${e.message}") }
                }
            }
            else -> {
                _state.update { it.copy(message = "Shizuku ya se encuentra activo y vinculado.") }
            }
        }
    }

    /**
     * Abre la aplicación de Shizuku instalada en el dispositivo.
     */
    fun openShizukuApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                openDownloadPage(context)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "No se pudo abrir Shizuku: ${e.message}")
        }
    }

    /**
     * Abre la página web oficial o enlace seguro para descargar Shizuku o Sui.
     */
    fun openDownloadPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Error abriendo navegador para descargar Shizuku: ${e.message}")
        }
    }

    /**
     * Ejecuta una prueba segura de otorgar permisos mediante IPackageManager a través de Shizuku.
     */
    suspend fun grantPermissionSafely(packageName: String, permissionName: String, userId: Int = 0): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_state.value.isReady) {
                    return@withContext Result.failure(IllegalStateException("Shizuku no está listo o autorizado"))
                }
                ShizukuPackageManager.grantRuntimePermission(packageName, permissionName, userId)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
    }
}
