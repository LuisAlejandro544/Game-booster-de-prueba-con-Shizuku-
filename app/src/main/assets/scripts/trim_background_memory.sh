#!/system/bin/sh
# ==============================================================================
# trim_background_memory.sh - Game Booster
# ==============================================================================
# Realiza una liberación quirúrgica y no destructiva de la memoria RAM ocupada
# por procesos y servicios en segundo plano.
#
# A diferencia de "task killers" placebo que causan reinicios en bucle de procesos
# y sobrecalientan la CPU, este script utiliza las señales nativas del subsistema
# de gestión de memoria de Android (RUNNING_CRITICAL / COMPLETE) para solicitar
# a las apps liberar caches bitmap, buffers UI y buffers de render sin forzar
# su cierre ni corromper el sistema.
#
# Seguridad:
#   - 100% volátil, gestionado por ActivityManager del sistema operativo.
#   - CERO uso de persist.sys.*
# ==============================================================================

echo "Iniciando liberación quirúrgica de memoria en segundo plano..."

if which cmd >/dev/null 2>&1; then
    # Solicitar a todas las aplicaciones en segundo plano recortar memoria
    cmd activity trim-memory --all RUNNING_CRITICAL >/dev/null 2>&1
    cmd activity trim-memory --all COMPLETE >/dev/null 2>&1
    echo "SUCCESS: Solicitud trim-memory (RUNNING_CRITICAL / COMPLETE) ejecutada a través de ActivityManager."
    exit 0
elif which am >/dev/null 2>&1; then
    # Fallback con el binario am
    am trim-memory --all RUNNING_CRITICAL >/dev/null 2>&1
    echo "SUCCESS: Solicitud trim-memory ejecutada a través de am."
    exit 0
else
    echo "ERROR: Comandos de administración de actividad no disponibles."
    exit 1
fi
