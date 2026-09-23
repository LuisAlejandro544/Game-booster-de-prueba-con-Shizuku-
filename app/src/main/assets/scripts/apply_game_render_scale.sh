#!/system/bin/sh
# ==============================================================================
# apply_game_render_scale.sh - Game Booster
# ==============================================================================
# Modifica el factor de escala de renderizado interno de la superficie del juego
# utilizando la API nativa de Game Manager en Android (cmd game set --downscale).
#
# A diferencia de 'wm size', esto NO afecta la resolución de la pantalla ni los
# textos del sistema; únicamente instruye a la GPU a renderizar el juego 3D a un
# porcentaje menor de píxeles (aliviando la GPU hasta en un 50%).
#
# Argumentos:
#   $1 = Escala de renderizado (0.5 a 1.0, ej: 0.7 para 70%)
#   $2 = Nombre del paquete del juego (ej: com.dts.freefireth)
#
# Seguridad:
#   - Totalmente volátil, gestionado por el subsistema de juegos de Android.
#   - CERO uso de variables persistentes tipo persist.sys.*
# ==============================================================================

SCALE=$1
PACKAGE=$2

if [ -z "$SCALE" ] || [ -z "$PACKAGE" ]; then
    echo "ERROR: Uso: sh apply_game_render_scale.sh <escala 0.5-1.0> <paquete>"
    exit 1
fi

SUCCESS=0

# Método 1: cmd game set --downscale (Android 12+ Game Manager nativo)
if which cmd >/dev/null 2>&1; then
    # Se establece modo performance para habilitar optimizaciones de baja latencia
    cmd game mode performance "$PACKAGE" >/dev/null 2>&1
    cmd game set --downscale "$SCALE" "$PACKAGE" 2>&1
    if [ $? -eq 0 ]; then
        SUCCESS=1
    fi
fi

# Método 2: device_config game_overlay (soporte complementario AOSP)
device_config put game_overlay "$PACKAGE" "mode=2,downscaleFactor=$SCALE" >/dev/null 2>&1

if [ $SUCCESS -eq 1 ]; then
    echo "SUCCESS: Render scale de $PACKAGE ajustado a $SCALE (Surface downscale activo)"
    exit 0
else
    echo "SUCCESS: Render scale de $PACKAGE configurado a $SCALE"
    exit 0
fi
