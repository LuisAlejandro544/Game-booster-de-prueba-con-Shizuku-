package com.example.shizuku

/**
 * Estado actual de la integración con Shizuku en el dispositivo.
 *
 * Contiene información técnica sobre la disponibilidad del servicio Shizuku,
 * si el Binder del sistema está vivo, el estado de los permisos otorgados a la aplicación,
 * el UID del proceso servidor (ADB Shell 2000 o Root 0) y mensajes para el usuario.
 */
data class ShizukuState(
    val isInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val uid: Int? = null,
    val version: Int? = null,
    val isPreV11: Boolean = false,
    val message: String = "Iniciando verificación de Shizuku...",
    val isLoading: Boolean = false
) {
    /**
     * Indica si Shizuku está completamente listo para ejecutar comandos con privilegios.
     */
    val isReady: Boolean
        get() = isBinderAlive && isPermissionGranted

    /**
     * Retorna una descripción textual amigable del nivel de privilegios obtenido.
     */
    val privilegeDescription: String
        get() = when (uid) {
            0 -> "Root (UID 0 - Acceso total)"
            2000 -> "ADB Shell (UID 2000 - Privilegios elevados)"
            null -> "Sin conexión"
            else -> "UID personalizado ($uid)"
        }
}
