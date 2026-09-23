# AI_CONTEXT - Directrices Técnicas y Reglas del Sistema

Este documento define el contexto, las restricciones críticas y las reglas inmutables de desarrollo para el proyecto **Game Booster**. Cualquier desarrollador o modelo de IA que trabaje sobre este repositorio debe acatar estas directrices estrictamente.

---

## 🛡️ Reglas Inmutables de Seguridad y Sistema

1. **PROHIBICIÓN ESTRICTA: Cero uso de `persist.sys.*`**
   - Bajo ninguna circunstancia se debe ejecutar o escribir código que modifique propiedades del sistema que comiencen con `persist.sys.*` (por ejemplo, `setprop persist.sys.*`).
   - Modificar propiedades persistentes en Android puede provocar bloqueos permanentes (*bootloops*) en dispositivos de usuarios reales.
   - Las modificaciones de pantalla, renderizado y controladores deben realizarse exclusivamente a nivel de tiempo de ejecución con comandos volátiles del WindowManager (`wm size` y `wm density`), la API de Game Manager (`cmd game`), y configuraciones globales temporales (`settings put global ...` / `setprop debug.*`).

2. **Controlador Gráfico Dinámico (OpenGL ES, Vulkan, ANGLE) y Detección Automática:**
   - **Detección del motor nativo:** Inspecciona los mapas de memoria del proceso en `/proc/$PID/maps` (buscando `libvulkan.so`, `libGLESv2.so`, `libfeature_support_angle.so`) y SurfaceFlinger para identificar si el juego usa Vulkan u OpenGL ES por defecto.
   - **Conmutación en caliente:**
     * **OpenGL ES:** Ajusta `angle_gl_driver_selection_values` a `native` y renderer a `skiagl`.
     * **Vulkan:** Habilita `updatable_driver_production_opt_in_apps` y renderer `skiavk`.
     * **ANGLE:** Activa la capa de traducción GLES sobre Vulkan de Google (`debug.angle.backend 2`).
   - **Ámbito estricto de juego:** Esta configuración solo debe mantenerse activa mientras el usuario se encuentra dentro del juego. Al detectar la salida del juego o cerrarse el overlay, se ejecuta de inmediato el restablecimiento a los valores estándar de Android.

3. **Downscale Interno de Superficie 3D (`cmd game set --downscale`) vs Resolución Global (`wm size`):**
   - **`cmd game set --downscale <0.50-1.00> <paquete>`:**
     * Es la técnica nativa introducida en Android 12+ (Game Manager Service).
     * Modifica el escalador de buffer de renderizado interno de la superficie del juego.
     * **Ventaja crítica:** A diferencia de `wm size`, no altera la densidad del sistema ni reduce el tamaño de las fuentes, barras de navegación ni botones del sistema operativo. Solo la carga gráfica de polígonos 3D del juego se reduce.
     * Permite al usuario deslizar en caliente (en vivo) desde el panel flotante mientras juega para ajustar el equilibrio entre FPS y nitidez.
   - **Restauración:** Se revierte mediante `cmd game reset <paquete>`.

4. **Desactivación Forzada de 4x MSAA y Filtros Pesados:**
   - Desactiva el suavizado forzado de bordes a través de `settings put global force_msaa 0`.
   - Aplica optimizaciones temporales de rasterización por hardware en tiempo de ejecución sin tocar propiedades persistentes.
   - Libera ancho de banda de la GPU y reduce la generación de calor térmico en chips de gama media.

5. **Seguridad y Rollback Automático de Todo Ajuste (Resolución, Render Scale y Driver):**
   - Antes de aplicar cualquier resolución personalizada con `wm size`, el sistema respalda las dimensiones y densidad nativas en `/data/local/tmp/gamebooster_display_backup.txt`.
   - Cuando el usuario sale del juego, cambia de aplicación, o el servicio del Game Booster se destruye (`onDestroy`), se ejecutan de forma obligatoria en un hilo `NonCancellable`:
     * `reset_resolution.sh`: Restablece el tamaño y densidad de pantalla nativos.
     * `reset_game_render_scale.sh`: Restablece el escalado interno de la superficie 3D.
     * `apply_graphics_driver.sh reset`: Restablece los controladores y propiedades gráficas a los valores del sistema.
   - Siempre debe existir un botón accesible para que el usuario pueda forzar la restauración en cualquier momento dentro del panel.

6. **Perfil del Usuario (Sin PC):**
   - El usuario opera exclusivamente desde un teléfono móvil Android. No dispone de un ordenador para ejecutar comandos `adb` vía cable USB.
   - Toda la experiencia de activación debe poder completarse directamente en el dispositivo:
     * En Android 11+: Mediante **Depuración Inalámbrica** emparejada con Shizuku.
     * En dispositivos con Root: Mediante **Sui** o **Magisk / KernelSU**.
   - Los botones de acceso directo a Ajustes de Desarrollador deben mantenerse funcionales y no asumir acceso a una consola externa.

7. **Compatibilidad de Arquitecturas (32 y 64 bits):**
   - La aplicación debe ser compatible con arquitecturas ARM de 32 bits (`armeabi-v7a`) y 64 bits (`arm64-v8a`), así como emuladores x86/x86_64.
   - Evitar binarios precompilados de arquitectura única. La integración con la shell mediante `sh` nativo de Android garantiza máxima portabilidad en cualquier chipset.

8. **Uso de Dependencias, Concurrencia y Modularidad:**
   - No implementar soluciones frágiles "sin dependencias". Se debe aprovechar el ecosistema oficial (`rikka.shizuku:api`, `rikka.shizuku:provider`, AndroidX, Jetpack Compose).
   - **Manejo de hilos:** Nunca ejecutar operaciones de E/S, llamadas a Shizuku, procesos shell o lecturas de archivos en el hilo principal (`Main thread`). Siempre delegar a `Dispatchers.IO` o `Dispatchers.Default` utilizando corrutinas de Kotlin.
   - **Modularidad de archivos:** Mantener los archivos de código fuente concisos y organizados por responsabilidad (idealmente bajo 500 líneas por archivo), dividiendo componentes en submódulos cuando crezcan.

9. **Biblioteca Real y Escaneo de Aplicaciones (Cero Datos Simulados):**
   - **Prohibición de listas mockeadas:** No precargar juegos falsos ni simulaciones fijas. La biblioteca inicial comienza vacía y el usuario agrega sus juegos reales manualmente.
   - **Escaneo nativo en hilo secundario:** El análisis de paquetes se ejecuta en `Dispatchers.IO` mediante `PackageManager.queryIntentActivities` buscando intenciones `LAUNCHER`.
   - **Detección inteligente de juegos:** Utiliza `ApplicationInfo.CATEGORY_GAME` y `ApplicationInfo.FLAG_IS_GAME` para priorizar y etiquetar los juegos instalados.
   - **Persistencia local:** Los juegos agregados se almacenan de forma persistente en `GameStorage` (`SharedPreferences`) para conservarse entre sesiones y reinicios.

---

## 🎨 Principios de Diseño Visual e Interfaz

1. **Rechazo al minimalismo extremo:**
   - La interfaz no debe ser excesivamente vacía ni plana. Debe incluir tarjetas informativas con elevación sutil, bordes estructurados, divisores claros e indicadores visuales de estado (conectado/desconectado).
2. **Modularidad de pantallas:**
   - Separar en pantallas bien definidas:
     * **Shizuku:** Estado del servicio, detalles de IPC y botón de activación.
     * **Booster:** Catálogo de juegos, perfiles de rendimiento y control del panel flotante.
     * **Guía Móvil:** Instrucciones paso a paso orientadas a usuarios en celular.
3. **Panel Flotante Red Magic (Tactical Dark Slate):**
   - Paleta oscura técnica: Fondo `#0F172A`, superficies `#1E293B`, acentos en carmesí `#E11D48` y cian `#0EA5E9`.
   - Barra lateral con íconos para cada herramienta modular:
     * Pestaña **GRÁFICOS & RENDER**: Selector de controlador gráfico (OpenGL/Vulkan/ANGLE) con detección de motor nativo, slider de downscale en vivo y switch de MSAA.
     * Pestaña **RESOLUCIÓN**: Sliders globales de pantalla y auto-cálculo de DPI.
     * Pestaña **RENDIMIENTO**: Modo boost y priorización de hilos.
     * Pestaña **AJUSTES DE JUEGO**: Modo no molestar y restauración global.
