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

if which cmd >/dev/null 2>&1; then
    cmd game reset "$PACKAGE" >/dev/null 2>&1
    cmd game mode standard "$PACKAGE" >/dev/null 2>&1
fi

device_config delete game_overlay "$PACKAGE" >/dev/null 2>&1

echo "SUCCESS: Render scale de $PACKAGE restablecido a valores nativos."
exit 0
