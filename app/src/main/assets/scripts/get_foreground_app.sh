#!/system/bin/sh
# ==============================================================================
# get_foreground_app.sh - Game Booster
# ==============================================================================
# Obtiene el paquete de la aplicación que se encuentra actualmente en primer plano.
# Esto permite detectar cuándo el usuario sale del juego para restaurar la resolución.
# Compatible con arquitecturas de 32 y 64 bits en Android 10, 11, 12, 13, 14 y 15.
# ==============================================================================

# Método 1: dumpsys activity activities (Método principal y más preciso en Android 10+)
TOP_APP=$(dumpsys activity activities 2>/dev/null | grep -m 1 "topResumedActivity=" | sed -E 's/.*([a-zA-Z0-9._]+)\/[a-zA-Z0-9._]+.*/\1/')

# Método 2: mCurrentFocus en WindowManager (Fallback cuando el foco cambia rápidamente)
if [ -z "$TOP_APP" ]; then
    TOP_APP=$(dumpsys window 2>/dev/null | grep -m 1 "mCurrentFocus" | sed -E 's/.*([a-zA-Z0-9._]+)\/[a-zA-Z0-9._]+.*/\1/')
fi

# Método 3: mFocusedApp en WindowManager
if [ -z "$TOP_APP" ]; then
    TOP_APP=$(dumpsys window 2>/dev/null | grep -m 1 "mFocusedApp" | sed -E 's/.*([a-zA-Z0-9._]+)\/[a-zA-Z0-9._]+.*/\1/')
fi

# Método 4: Fallback a línea de texto completa si no coincidió el regex
if [ -z "$TOP_APP" ]; then
    TOP_APP=$(dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | head -n 1)
fi

echo "$TOP_APP"
exit 0
