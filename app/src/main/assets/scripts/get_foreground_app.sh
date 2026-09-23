#!/system/bin/sh
# ==============================================================================
# get_foreground_app.sh - Game Booster
# ==============================================================================
# Obtiene el paquete de la aplicación que se encuentra actualmente en primer plano.
# Esto permite detectar cuándo el usuario sale del juego para restaurar la resolución.
# ==============================================================================

CURRENT=$(dumpsys window | grep -E 'mCurrentFocus|mFocusedApp' | head -n 1)
if [ -z "$CURRENT" ]; then
    CURRENT=$(dumpsys activity activities | grep -E 'topResumedActivity' | head -n 1)
fi

echo "$CURRENT"
exit 0
