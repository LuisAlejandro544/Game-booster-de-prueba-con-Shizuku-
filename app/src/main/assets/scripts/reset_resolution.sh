#!/system/bin/sh
# ==============================================================================
# reset_resolution.sh - Game Booster
# ==============================================================================
# Restablece la resolución de pantalla y la densidad (DPI) a los valores
# originales y nativos del dispositivo.
#
# Se invoca:
#   1. Cuando el usuario pulsa el botón "Restaurar Resolución" en el panel.
#   2. Automáticamente al salir del juego o cerrar la burbuja del Game Booster.
# ==============================================================================

BACKUP_FILE="/data/local/tmp/gamebooster_display_backup.txt"

# Restaurar valores por defecto del sistema
wm size reset
wm density reset

# Limpiar archivo temporal de respaldo
if [ -f "$BACKUP_FILE" ]; then
    rm -f "$BACKUP_FILE"
fi

echo "SUCCESS: Resolución y densidad restablecidas a valores nativos."
exit 0
