#!/usr/bin/env bash
# ==============================================================================
# Script exclusivo para el GitHub Action de compilación del APK Debug
#
# Propósito:
#   Obliga al flujo de CI a generar una firma de depuración (debug.keystore)
#   completamente desde cero, eliminando cualquier rastro de firmas previas
#   y sin requerir secretos, archivos externos o interacción manual.
# ==============================================================================

set -euo pipefail

# Ruta de destino del keystore (por defecto en el directorio raíz o el parámetro proporcionado)
TARGET_KEYSTORE="${1:-debug.keystore}"

echo "=========================================================="
echo " [CI/CD] Generando firma Debug desde cero para el APK"
echo " Archivo objetivo: $TARGET_KEYSTORE"
echo "=========================================================="

# 1. Elimina cualquier archivo existente para garantizar generación pura desde cero
if [ -f "$TARGET_KEYSTORE" ]; then
    echo ">> Se detectó un archivo previo en '$TARGET_KEYSTORE'. Eliminándolo para forzar creación limpia..."
    rm -f "$TARGET_KEYSTORE"
fi

# 2. Genera el par de claves RSA estándar de Android Debug mediante keytool
echo ">> Ejecutando keytool para crear nuevo almacén de claves (RSA 2048 bits)..."
keytool -genkeypair -v \
    -keystore "$TARGET_KEYSTORE" \
    -alias "androiddebugkey" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "android" \
    -keypass "android" \
    -dname "CN=Android Debug, OU=Android Development, O=GameBooster, C=US"

# 3. Verificación de integridad del archivo generado
if [ -f "$TARGET_KEYSTORE" ]; then
    FILE_SIZE=$(wc -c < "$TARGET_KEYSTORE" | tr -d ' ')
    echo ">> Éxito: Firma debug creada correctamente en '$TARGET_KEYSTORE' ($FILE_SIZE bytes)."
    echo "=========================================================="
    exit 0
else
    echo ">> ERROR FATAL: No se pudo generar el archivo '$TARGET_KEYSTORE'." >&2
    exit 1
fi
