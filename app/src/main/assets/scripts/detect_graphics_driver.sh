#!/system/bin/sh
# ==============================================================================
# detect_graphics_driver.sh - Game Booster
# ==============================================================================
# Detecta el controlador gráfico (OpenGL ES, Vulkan o ANGLE) activo y por defecto
# para un paquete de juego determinado.
#
# Argumentos:
#   $1 = Nombre de paquete del juego (ej: "com.dts.freefireth")
#
# Salida:
#   Imprime variables estructuradas clave-valor para ser parseadas por Kotlin:
#   DRIVER_ACTIVE=...
#   DRIVER_DEFAULT=...
#   GPU_NAME=...
#   GLES_VERSION=...
#   VULKAN_VERSION=...
#   DETAILS=...
#
# Compatibilidad:
#   - Android 10 a 15 (ARMv7 32-bit, ARM64 64-bit, x86/x86_64)
#   - Sin uso de persist.sys.*
# ==============================================================================

PKG=$1

if [ -z "$PKG" ]; then
    echo "ERROR: Debe especificar el paquete del juego como argumento \$1"
    exit 1
fi

# 1. Obtener información de hardware de la GPU mediante SurfaceFlinger
SF_INFO=$(dumpsys SurfaceFlinger 2>/dev/null)
GLES_INFO=$(echo "$SF_INFO" | grep -m 1 "GLES:" | sed -E 's/^[[:space:]]*GLES:[[:space:]]*//')
VK_INFO=$(echo "$SF_INFO" | grep -m 1 -i "Vulkan" | sed -E 's/^[[:space:]]*//')

# Si no hay salida de SurfaceFlinger, consultar propiedades de sistema
if [ -z "$GLES_INFO" ]; then
    GLES_PROP=$(getprop ro.opengles.version 2>/dev/null)
    GPU_PROP=$(getprop ro.hardware.egl 2>/dev/null)
    GLES_INFO="${GPU_PROP:-GPU Estándar} (GLES $GLES_PROP)"
fi

# 2. Comprobar configuraciones globales actuales de Android para ANGLE y Game Driver
ANGLE_PKGS=$(settings get global angle_gl_driver_selection_pkgs 2>/dev/null)
ANGLE_VALS=$(settings get global angle_gl_driver_selection_values 2>/dev/null)
GAME_DRIVER_PKGS=$(settings get global updatable_driver_production_opt_in_apps 2>/dev/null)

CONFIGURED_DRIVER="NONE"
case "$ANGLE_PKGS" in
    *"$PKG"*)
        if [ "$ANGLE_VALS" = "angle" ]; then
            CONFIGURED_DRIVER="ANGLE"
        elif [ "$ANGLE_VALS" = "native" ]; then
            CONFIGURED_DRIVER="OPENGL"
        fi
        ;;
esac

if [ "$CONFIGURED_DRIVER" = "NONE" ]; then
    case "$GAME_DRIVER_PKGS" in
        *"$PKG"*)
            CONFIGURED_DRIVER="VULKAN"
            ;;
    esac
fi

# 3. Inspeccionar el proceso en ejecución del juego a través de /proc/$PID/maps
PID=$(pidof "$PKG" 2>/dev/null | awk '{print $1}')
if [ -z "$PID" ]; then
    PID=$(pgrep -f "$PKG" 2>/dev/null | head -n 1)
fi

RUNNING_DRIVER="UNKNOWN"
HAS_VULKAN_MAP=0
HAS_GLES_MAP=0
HAS_ANGLE_MAP=0

if [ -n "$PID" ] && [ -d "/proc/$PID" ]; then
    MAPS=$(cat /proc/$PID/maps 2>/dev/null)
    
    # Comprobar si tiene mapeadas librerías de ANGLE
    if echo "$MAPS" | grep -q -i -E "libGLESv2_angle|libEGL_angle|angle_"; then
        HAS_ANGLE_MAP=1
    fi
    
    # Comprobar si tiene mapeadas librerías de Vulkan
    if echo "$MAPS" | grep -q -i -E "vulkan|libvulkan|vulkan\..*\.so"; then
        HAS_VULKAN_MAP=1
    fi
    
    # Comprobar librerías nativas de OpenGL ES
    if echo "$MAPS" | grep -q -i -E "libGLESv2|libGLESv3|libEGL|libGLES_mali|adreno|libmali"; then
        HAS_GLES_MAP=1
    fi
fi

# Determinar el controlador activo según el mapa de memoria del proceso
if [ "$HAS_ANGLE_MAP" -eq 1 ]; then
    RUNNING_DRIVER="ANGLE"
elif [ "$HAS_VULKAN_MAP" -eq 1 ]; then
    RUNNING_DRIVER="VULKAN"
elif [ "$HAS_GLES_MAP" -eq 1 ]; then
    RUNNING_DRIVER="OPENGL"
else
    # Si el juego aún no arranca o no se puede inspeccionar maps, usar la configuración actual
    if [ "$CONFIGURED_DRIVER" != "NONE" ]; then
        RUNNING_DRIVER="$CONFIGURED_DRIVER"
    fi
fi

# 4. Determinar el controlador por defecto del juego
# La mayoría de juegos modernos 3D en Android tienen soporte Vulkan por defecto si el hardware lo soporta,
# de lo contrario recurren a OpenGL ES.
DEFAULT_DRIVER="OPENGL"
if [ "$HAS_VULKAN_MAP" -eq 1 ] && [ "$CONFIGURED_DRIVER" != "VULKAN" ]; then
    DEFAULT_DRIVER="VULKAN"
elif [ -n "$VK_INFO" ] || [ -n "$(getprop ro.hardware.vulkan 2>/dev/null)" ]; then
    # El dispositivo soporta Vulkan. Si en maps se vio Vulkan o es un juego 64-bit moderno:
    if [ "$HAS_VULKAN_MAP" -eq 1 ]; then
        DEFAULT_DRIVER="VULKAN"
    else
        DEFAULT_DRIVER="OPENGL"
    fi
fi

# Si el controlador activo sigue sin definirse, asumir el por defecto
if [ "$RUNNING_DRIVER" = "UNKNOWN" ]; then
    RUNNING_DRIVER="$DEFAULT_DRIVER"
fi

# 5. Formatear nombres y versiones para la salida
GPU_DISPLAY="${GLES_INFO:-Controlador estándar de GPU}"
GLES_VER=$(echo "$GLES_INFO" | grep -o -E "OpenGL ES [0-9]+\.[0-9]+" || echo "OpenGL ES 3.2")
VK_VER=$(echo "$VK_INFO" | grep -o -E "[0-9]+\.[0-9]+(\.[0-9]+)?" || echo "Vulkan 1.1+")

echo "DRIVER_ACTIVE=$RUNNING_DRIVER"
echo "DRIVER_DEFAULT=$DEFAULT_DRIVER"
echo "GPU_NAME=$GPU_DISPLAY"
echo "GLES_VERSION=$GLES_VER"
echo "VULKAN_VERSION=$VK_VER"
echo "CONFIGURED=$CONFIGURED_DRIVER"
exit 0
