#!/system/bin/sh
# ==============================================================================
# apply_graphics_driver.sh - Game Booster
# ==============================================================================
# Configura el controlador gráfico deseado (OpenGL ES, Vulkan o ANGLE) para un
# juego en tiempo de ejecución, o restablece a la configuración nativa del sistema.
#
# Argumentos:
#   $1 = Acción ("opengl" | "vulkan" | "angle" | "reset")
#   $2 = Paquete del juego (ej: "com.dts.freefireth")
#
# Seguridad:
#   - Exclusivamente cambios en tiempo de ejecución (settings globales y propiedades debug).
#   - CERO uso de 'persist.sys.*'.
#   - Al salir del juego, la app ejecuta "reset" restaurando la normalidad del sistema.
# ==============================================================================

ACTION=$1
PKG=$2

if [ -z "$ACTION" ]; then
    echo "ERROR: Acción no especificada. Uso: sh apply_graphics_driver.sh <opengl|vulkan|angle|reset> [paquete]"
    exit 1
fi

case "$ACTION" in
    "opengl")
        if [ -z "$PKG" ]; then
            echo "ERROR: Debe indicar el paquete del juego para forzar OpenGL ES."
            exit 1
        fi
        # 1. Configurar ANGLE para que use el controlador nativo OpenGL ES
        settings put global angle_gl_driver_selection_pkgs "$PKG"
        settings put global angle_gl_driver_selection_values "native"
        
        # 2. Desactivar opt-in forzado de Game Driver (Vulkan)
        settings put global updatable_driver_production_opt_in_apps ""
        
        # 3. Forzar Skia OpenGL en el motor de renderizado
        setprop debug.hwui.renderer skiagl
        setprop debug.angle.backend ""
        
        echo "SUCCESS: Controlador OpenGL ES configurado para $PKG (Máxima compatibilidad)."
        exit 0
        ;;

    "vulkan")
        if [ -z "$PKG" ]; then
            echo "ERROR: Debe indicar el paquete del juego para forzar Vulkan."
            exit 1
        fi
        # 1. Limpiar ANGLE para permitir paso directo a la API Vulkan
        settings delete global angle_gl_driver_selection_pkgs >/dev/null 2>&1 || settings put global angle_gl_driver_selection_pkgs ""
        settings delete global angle_gl_driver_selection_values >/dev/null 2>&1 || settings put global angle_gl_driver_selection_values ""
        
        # 2. Habilitar Game Driver en producción para el paquete objetivo
        settings put global updatable_driver_production_opt_in_apps "$PKG"
        
        # 3. Forzar Skia Vulkan en la canalización gráfica
        setprop debug.hwui.renderer skiavk
        setprop debug.angle.backend ""
        
        echo "SUCCESS: Controlador Vulkan activado para $PKG (Máximo aprovechamiento de GPU)."
        exit 0
        ;;

    "angle")
        if [ -z "$PKG" ]; then
            echo "ERROR: Debe indicar el paquete del juego para forzar ANGLE."
            exit 1
        fi
        # 1. Asignar el paquete a la capa de traducción ANGLE
        settings put global angle_gl_driver_selection_pkgs "$PKG"
        settings put global angle_gl_driver_selection_values "angle"
        
        # 2. Backend Vulkan para ANGLE (debug.angle.backend 2 = Vulkan)
        setprop debug.angle.backend 2
        
        # 3. Desactivar opt-in redundante de Game Driver
        settings put global updatable_driver_production_opt_in_apps ""
        
        echo "SUCCESS: Capa de traducción ANGLE (GLES sobre Vulkan) activada para $PKG."
        exit 0
        ;;

    "reset")
        # Restablecer todas las propiedades y settings globales a valores normales del sistema
        settings delete global angle_gl_driver_selection_pkgs >/dev/null 2>&1 || settings put global angle_gl_driver_selection_pkgs ""
        settings delete global angle_gl_driver_selection_values >/dev/null 2>&1 || settings put global angle_gl_driver_selection_values ""
        settings delete global updatable_driver_production_opt_in_apps >/dev/null 2>&1 || settings put global updatable_driver_production_opt_in_apps ""
        
        setprop debug.hwui.renderer ""
        setprop debug.angle.backend ""
        
        echo "SUCCESS: Controladores gráficos restablecidos a los valores por defecto del sistema."
        exit 0
        ;;

    *)
        echo "ERROR: Opción no válida: $ACTION. Opciones disponibles: opengl, vulkan, angle, reset"
        exit 1
        ;;
esac
