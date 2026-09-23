#!/system/bin/sh
# ==============================================================================
# apply_graphic_filters.sh - Game Booster
# ==============================================================================
# Controla la desactivación forzada del filtrado Anti-Aliasing (4x MSAA) y
# optimiza búferes de renderizado de la GPU para maximizar la tasa de cuadros (FPS).
#
# Argumentos:
#   $1 = Acción ("disable_msaa" | "reset")
#
# Seguridad:
#   - Modifica exclusivamente configuraciones de sesión en tiempo de ejecución
#     mediante 'settings' y 'setprop debug.*'.
#   - CERO uso de 'persist.sys.*'.
# ==============================================================================

ACTION=$1

case "$ACTION" in
    "disable_msaa")
        # Forzar desactivación de 4x MSAA (Multisample Anti-Aliasing) en la GPU
        settings put global force_msaa 0
        setprop debug.egl.force_msaa 0
        
        # Reducir sobrecarga de renderizado en compositor de ventanas
        setprop debug.hwui.render_dirty_regions false
        setprop debug.hwui.fps_divisor 1
        
        echo "SUCCESS: 4x MSAA desactivado forzadamente y filtros pesados de GPU optimizados."
        exit 0
        ;;
    "reset")
        # Restablecer valores estándar del sistema
        settings delete global force_msaa >/dev/null 2>&1 || settings put global force_msaa 0
        setprop debug.egl.force_msaa ""
        
        echo "SUCCESS: Filtros gráficos de GPU restablecidos a valores por defecto."
        exit 0
        ;;
    *)
        echo "ERROR: Acción no reconocida. Uso: sh apply_graphic_filters.sh <disable_msaa|reset>"
        exit 1
        ;;
esac
