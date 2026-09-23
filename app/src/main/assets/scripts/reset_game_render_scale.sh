#!/system/bin/sh
# ==============================================================================
# reset_game_render_scale.sh - Game Booster
# ==============================================================================
# Restablece la escala de renderizado interno de la superficie del juego a su
# valor por defecto (100% nativo) mediante 'cmd game reset <paquete>'.
#
# Se invoca:
#   1. Cuando el usuario desliza el render scale de vuelta al 100% o pulsa restaurar.
#   2. Automáticamente al salir del juego o destruir el servicio del Game Booster.
# ==============================================================================

PACKAGE=$1

if [ -z "$PACKAGE" ]; then
    echo "ERROR: Debe especificar el paquete del juego. Uso: sh reset_game_render_scale.sh <paquete>"
    exit 1
fi

cmd game reset "$PACKAGE" 2>&1
STATUS=$?

if [ $STATUS -eq 0 ]; then
    echo "SUCCESS: Render scale de $PACKAGE restablecido a valores nativos."
    exit 0
else
    echo "WARN: Falló 'cmd game reset $PACKAGE' (Status: $STATUS)."
    exit $STATUS
fi
