#!/system/bin/sh
# ==============================================================================
# compile_game_aot.sh - Game Booster
# ==============================================================================
# Ejecuta la compilación previa Ahead-Of-Time (AOT) del código DEX del juego
# utilizando el compilador dex2oat nativo de Android (cmd package compile).
#
# Elimina los micro-tirones (micro-stuttering) y congelamientos de frames provocados
# por el compilador JIT mientras se juegan partidas en tiempo real.
#
# Argumentos:
#   $1 = Nombre del paquete del juego (ej: com.dts.freefireth)
#   $2 = Filtro de compilación ("speed-profile" o "speed", por defecto "speed-profile")
#
# Seguridad:
#   - Operación estándar del Administrador de Paquetes de Android (pm / cmd package).
#   - CERO uso de persist.sys.* ni binarios invasivos.
# ==============================================================================

PACKAGE=$1
FILTER=$2

if [ -z "$PACKAGE" ]; then
    echo "ERROR: Debe especificar el nombre del paquete a compilar."
    exit 1
fi

if [ -z "$FILTER" ]; then
    FILTER="speed-profile"
fi

echo "Iniciando compilación AOT con filtro '$FILTER' para $PACKAGE..."

# Ejecutar compilación forzada AOT mediante el comando del gestor de paquetes de Android
if which cmd >/dev/null 2>&1; then
    COMPILE_OUTPUT=$(cmd package compile -m "$FILTER" -f "$PACKAGE" 2>&1)
    EXIT_CODE=$?
else
    COMPILE_OUTPUT=$(pm compile -m "$FILTER" -f "$PACKAGE" 2>&1)
    EXIT_CODE=$?
fi

echo "$COMPILE_OUTPUT"

if [ $EXIT_CODE -eq 0 ]; then
    echo "SUCCESS: Compilación AOT finalizada exitosamente para $PACKAGE."
    exit 0
else
    echo "WARNING: Compilación terminó con código $EXIT_CODE."
    exit $EXIT_CODE
fi
