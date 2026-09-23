# Game Booster con Shizuku y Panel Red Magic

Aplicación modular de optimización de juegos (**Game Booster**) para Android, construida en **Kotlin** y **Jetpack Compose**, con integración nativa del servicio **Shizuku (vía Binder/IPC)** y scripts Shell (`.sh`) para el control de resolución dinámica, densidad de píxeles (DPI), **downscale interno de renderizado 3D (`cmd game set --downscale`)**, **conmutación de controladores gráficos (OpenGL ES, Vulkan, ANGLE)** con detección automática, y perfiles de rendimiento en tiempo real.

---

## 🎯 Características Principales

1. **Burbuja Flotante Deslizable y Panel Red Magic:**
   - Burbuja táctil que se ancla al borde lateral de la pantalla durante tus partidas.
   - Despliega un panel lateral táctico inspirado en el *Game Space* de los teléfonos para juegos de alta gama.
   - Barra modular de íconos para cada herramienta (Gráficos & Render, Resolución/DPI, Rendimiento, Ajustes rápidos).

2. **Controlador Gráfico con Detección Automática (OpenGL ES, Vulkan, ANGLE):**
   - **Detección inteligente del controlador por defecto:** Inspecciona los mapas de memoria del proceso del juego (`/proc/$PID/maps`), la telemetría de SurfaceFlinger y las configuraciones de Android para identificar si el juego corre sobre Vulkan u OpenGL ES.
   - **Conmutador en caliente dentro de la burbuja:** Permite forzar en vivo el controlador deseado:
     * **OpenGL ES:** Mayor compatibilidad en títulos clásicos o emuladores.
     * **Vulkan:** Máximo aprovechamiento multinúcleo de GPU modernas con mínima sobrecarga de CPU.
     * **ANGLE (Google):** Traduce llamadas de OpenGL ES a Vulkan para optimizar pipelines de shaders y erradicar caídas bruscas de FPS.
   - **Reversión garantizada:** El controlador gráfico personalizado solo tiene vigencia durante la partida activa; al salir del juego o cerrar el overlay, se restablece de inmediato a la configuración normal del sistema.

3. **Downscale Interno de Superficie (`cmd game set --downscale`) Deslizable en Vivo:**
   - **Slider en tiempo real durante la partida:** El usuario puede deslizar el renderizado del 50% al 100% mientras juega.
   - **Diferencia con la resolución global (`wm size`):** El downscale interno ordena a la GPU renderizar únicamente los polígonos 3D del juego a una resolución menor, manteniendo los textos, menús y la barra de estado de Android 100% nítidos a resolución nativa.
   - Reduce drásticamente la carga de la GPU y eleva los FPS estables en juegos exigentes (Free Fire, PUBG, Genshin Impact, COD Mobile).

4. **Desactivación Forzada de Anti-Aliasing (4x MSAA) y Filtros Pesados:**
   - Desactiva pasadas redundantes de suavizado de bordes mediante `settings put global force_msaa 0` y optimización de buffers del compositor de ventanas.
   - Libera ancho de banda de la GPU para eliminar tirones (*micro-stuttering*).

5. **Escaneo Real de Aplicaciones y Juegos Instalados (Hilo Secundario):**
   - **Cero juegos simulados:** La aplicación no contiene juegos de muestra precargados ni simulaciones estáticas.
   - **Escáner nativo en segundo plano (`Dispatchers.IO`):** Analiza en un hilo secundario todas las aplicaciones y juegos reales instalados en el teléfono mediante `PackageManager`.
   - **Detección inteligente de juegos:** Identifica automáticamente qué aplicaciones son juegos (`CATEGORY_GAME` / `FLAG_IS_GAME`) y ofrece filtros para "Juegos", "Otras Apps" y "Todas", además de un buscador en tiempo real.
   - **Persistencia garantizada:** Los juegos agregados por el usuario se guardan de forma permanente para que tu biblioteca personalizada siempre esté lista.

6. **Control Dinámico de Resolución de Pantalla y Auto-DPI:**
   - Modificación en caliente de resolución de pantalla (`wm size`) y densidad de píxeles (`wm density`).
   - **Cálculo automático de DPI proporcional:** Evita que los botones o textos de los juegos se vean desproporcionados al cambiar de escala (ejemplo: 1080p a 720p calcula automáticamente la reducción de 420 a 320 DPI).
   - Presets preconfigurados para juegos competitivos (100% Nativo, 85% Óptimo, 75% HD+ 720p, 60% Max FPS 540p) y sliders manuales.

7. **Seguridad y Restauración Automática Total:**
   - **Al salir del juego:** Un monitor en segundo plano detecta cuando abandonas la partida (vía paquetes y eventos de foco) y **restaura automáticamente** la resolución de pantalla, la escala de renderizado y el controlador gráfico a sus valores originales.
   - **Botón dentro del juego:** Botones explícitos **"Restaurar Resolución Original"** y **"Restaurar Escala y Controlador Nativo"** dentro del panel.
   - **Regla estricta:** Cero uso de variables globales persistentes (`persist.sys.*`), previniendo cualquier riesgo de bootloop o inestabilidad del sistema operativo.

8. **Activación 100% desde el Teléfono (Sin PC):**
   - Diseñado específicamente para usuarios que solo cuentan con su teléfono móvil.
   - Guía interactiva paso a paso para vincular Shizuku mediante **Depuración Inalámbrica** (Android 11 o superior) o acceso **Root / Sui**.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.0+ (100% tipado, corrutinas y Flow asíncrono en `Dispatchers.IO`).
- **Framework UI:** Jetpack Compose + Material Design 3 (personalización táctica militar oscura para el panel Red Magic).
- **Control de Ventanas:** Android `WindowManager` con `ComposeView` para el overlay flotante (`SYSTEM_ALERT_WINDOW`).
- **Integración de Sistema:**
  - `dev.rikka.shizuku:api:13.1.5`
  - `dev.rikka.shizuku:provider:13.1.5`
  - Llamadas AIDL (`IPackageManager`, `ShizukuBinderWrapper`).
  - Android Game Manager (`cmd game set --downscale`, `cmd game reset`).
  - Control de controladores gráficos (ANGLE, Updatable Game Driver, Skia Vulkan/OpenGL).
  - Scripts Shell `.sh` ejecutados con privilegios ADB (UID 2000) o Root (UID 0).
- **Compatibilidad de Arquitectura:** 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).

---

## 📂 Scripts Shell Integrados

Los scripts residen en `app/src/main/assets/scripts/` y son gestionados por `ScriptManager.kt`:

| Script | Propósito | Argumentos |
| :--- | :--- | :--- |
| `detect_graphics_driver.sh` | Detecta el controlador gráfico activo y nativo del juego (`/proc/$PID/maps`, SurfaceFlinger). | `<paquete>` |
| `apply_graphics_driver.sh` | Conmuta el controlador a OpenGL, Vulkan o ANGLE, o restablece al sistema. | `<opengl\|vulkan\|angle\|reset> [paquete]` |
| `apply_game_render_scale.sh` | Aplica la escala de renderizado interno a la superficie 3D del juego (`cmd game`). | `<escala 0.5-1.0> <paquete>` |
| `reset_game_render_scale.sh` | Restablece el render scale del juego a 100% nativo (`cmd game reset`). | `<paquete>` |
| `apply_graphic_filters.sh` | Desactiva forzadamente 4x MSAA y optimiza buffers en la GPU. | `<disable_msaa\|reset>` |
| `apply_resolution.sh` | Aplica una resolución global y DPI objetivo, creando un backup previo. | `<ancho> <alto> [dpi]` |
| `reset_resolution.sh` | Restablece tamaño y densidad a los valores nativos del panel. | Ninguno |
| `get_display_info.sh` | Consulta métricas reales y overrides activos del display. | Ninguno |
| `get_foreground_app.sh`| Obtiene el paquete de la aplicación que está en foco. | Ninguno |

---

## 🚀 Cómo Usarlo

### 1. Activar Shizuku en el Celular (Sin PC)
1. Abre la aplicación y dirígete a la pestaña **Guía Móvil**.
2. Conéctate a una red Wi-Fi y abre **Ajustes > Opciones de desarrollador**.
3. Activa **Depuración Inalámbrica** y selecciona *Vincular con código*.
4. Introduce el código en la notificación de Shizuku y presiona *Iniciar*.
5. Vuelve al Game Booster y pulsa el botón **Activar Shizuku** para conceder la autorización Binder.

### 2. Conceder Permiso de Superposición
* En la pantalla del Booster, pulsa **Permitir** para otorgar el permiso de *Mostrar sobre otras aplicaciones*.

### 3. Jugar con Controlador Gráfico y Render Scale Optimizado
1. Selecciona tu juego en la biblioteca y presiona **Iniciar**.
2. Aparecerá la pestaña flotante **BOOST** en el lateral de la pantalla.
3. Desliza o toca la pestaña para abrir el panel táctico **Red Magic**.
4. En la pestaña **Gráficos & Render**:
   - Observa el controlador por defecto detectado (por ejemplo, *Vulkan* u *OpenGL ES*).
   - Si deseas menor latencia y más FPS, selecciona **Vulkan**; para emuladores o compatibilidad selecciona **OpenGL ES**; para traducir shaders complejos a Vulkan selecciona **ANGLE**.
   - Mueve el slider de **Escala de Renderizado** en vivo (ej: 70% o 50%) para aliviar la carga de polígonos 3D en la GPU.
   - Activa **Anular 4x MSAA** para liberar ancho de banda.
5. Al terminar de jugar o pulsar el botón de inicio del teléfono, el monitor en segundo plano detecta la salida y restaura automáticamente el controlador gráfico, la resolución y el render scale a los valores originales.
