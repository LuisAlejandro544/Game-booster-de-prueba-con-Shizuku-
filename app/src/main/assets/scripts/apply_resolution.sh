#!/system/bin/sh
# ==============================================================================
# apply_resolution.sh - Game Booster
# ==============================================================================
# Modifica la resolución (ancho x alto) y la densidad de píxeles (DPI) en el
# dispositivo objetivo mediante comandos oficiales de Android (wm).
#
# Argumentos:
#   $1 = Ancho deseado en píxeles (ej: 1280 o 720)
#   $2 = Alto deseado en píxeles  (ej: 720 o 1600)
#   $3 = DPI deseado (opcional, calculado automáticamente o manual)
#
# Seguridad:
#   - Guarda la resolución nativa original antes de modificar nada.
#   - NO utiliza ninguna propiedad persistente del tipo persist.sys.*
# ==============================================================================

WIDTH=$1
HEIGHT=$2
DPI=$3

BACKUP_FILE="/data/local/tmp/gamebooster_display_backup.txt"

# Validar argumentos de entrada mínimos
if [ -z "$WIDTH" ] || [ -z "$HEIGHT" ]; then
    echo "ERROR: Se requiere ancho y alto. Uso: sh apply_resolution.sh <ancho> <alto> [dpi]"
    exit 1
fi

# Guardar la resolución nativa actual si no existe backup previo (usando sed y grep para compatibilidad universal)
if [ ! -f "$BACKUP_FILE" ]; then
    ORIG_SIZE=$(wm size 2>/dev/null | grep -i "Physical size" | head -n 1 | sed 's/.*: //')
    ORIG_DENSITY=$(wm density 2>/dev/null | grep -i "Physical density" | head -n 1 | sed 's/.*: //')
    if [ -n "$ORIG_SIZE" ]; then
        echo "ORIG_SIZE=$ORIG_SIZE" > "$BACKUP_FILE"
        echo "ORIG_DENSITY=$ORIG_DENSITY" >> "$BACKUP_FILE"
        echo "INFO: Backup de pantalla creado: Tamaño=$ORIG_SIZE, Densidad=$ORIG_DENSITY"
    fi
fi

# Aplicar el cambio de resolución de pantalla
wm size "${WIDTH}x${HEIGHT}"
APPLY_SIZE_STATUS=$?

if [ $APPLY_SIZE_STATUS -ne 0 ]; then
    echo "ERROR: Falló al aplicar la resolución con 'wm size ${WIDTH}x${HEIGHT}'"
    exit 2
fi

# Si se especificó DPI, aplicarlo
if [ -n "$DPI" ] && [ "$DPI" -gt 0 ]; then
    wm density "$DPI"
    APPLY_DPI_STATUS=$?
    if [ $APPLY_DPI_STATUS -ne 0 ]; then
        echo "WARN: Falló al aplicar 'wm density $DPI'"
    else
        echo "SUCCESS: Resolución aplicada: ${WIDTH}x${HEIGHT} @ ${DPI} DPI"
    fi
else
    echo "SUCCESS: Resolución aplicada: ${WIDTH}x${HEIGHT}"
fi

exit 0
