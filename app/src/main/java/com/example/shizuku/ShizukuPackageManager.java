package com.example.shizuku;

import android.content.pm.IPackageManager;
import android.os.RemoteException;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

/**
 * Gestor del servicio de paquetes del sistema (IPackageManager) utilizando Shizuku.
 *
 * Esta clase proporciona la conexión Binder remota hacia el servicio nativo "package"
 * de Android, permitiendo ejecutar acciones con privilegios de Shell ADB o Root.
 *
 * Lógica y uso:
 * - PACKAGE_MANAGER: Instancia de IPackageManager obtenida a través de ShizukuBinderWrapper.
 * - grantRuntimePermission: Permite otorgar permisos de tiempo de ejecución a aplicaciones
 *   específicas en el dispositivo sin necesidad de usar comandos adb desde una PC.
 */
public class ShizukuPackageManager {

    private static final IPackageManager PACKAGE_MANAGER = IPackageManager.Stub.asInterface(
        new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));

    /**
     * Otorga un permiso en tiempo de ejecución a una aplicación para un usuario específico.
     *
     * @param packageName    El paquete objetivo (ej: "com.example.app").
     * @param permissionName El nombre del permiso del sistema (ej: "android.permission.WRITE_SECURE_SETTINGS").
     * @param userId         El identificador de usuario (usualmente 0 para el usuario principal).
     */
    public static void grantRuntimePermission(String packageName, String permissionName, int userId) {
        try {
            PACKAGE_MANAGER.grantRuntimePermission(packageName, permissionName, userId);
        } catch (RemoteException tr) {
            throw new RuntimeException(tr.getMessage(), tr);
        }
    }
}
