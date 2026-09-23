package android.content.pm;

/**
 * Interfaz AIDL para IPackageManager de Android.
 * Permite la invocación remota a través del Binder provisto por Shizuku.
 */
interface IPackageManager {
    void grantRuntimePermission(String packageName, String permissionName, int userId);
}
