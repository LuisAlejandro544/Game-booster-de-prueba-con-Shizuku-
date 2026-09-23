# Game Booster con Shizuku y Panel Red Magic

Aplicación modular de optimización de juegos (**Game Booster**) para Android, construida en **Kotlin** y **Jetpack Compose**, con integración nativa del servicio **Shizuku (vía Binder/IPC)** y scripts Shell (`.sh`) para el control de resolución dinámica, densidad de píxeles (DPI), **downscale interno de renderizado 3D (`cmd game set --downscale`)** y perfiles de rendimiento en tiempo real.

---

## 🎯 Características Principales

1. **Burbuja Flotante Deslizable y Panel Red Magic:**
   - Burbuja táctil que se ancla al borde lateral de la pantalla durante tus partidas.
   - Despliega un panel lateral táctico inspirado en el *Game Space* de los teléfonos para juegos de alta gama.
   - Barra modular de íconos para cada herramienta (Render Scale, Resolución/DPI, Rendimiento, Ajustes rápidos).

2. **Downscale Interno de Superficie (`cmd game set --downscale`) Deslizable en Vivo:**
   - **Slider en tiempo real durante la partida:** El usuario puede deslizar el renderizado del 50% al 100% mientras juega.
   - **Diferencia con la resolución global (`wm size`):** El downscale interno ordena a la GPU renderizar únicamente los polígonos 3D del juego a una resolución menor, manteniendo los textos, menús y la barra de estado de Android 100% nítidos a resolución nativa.
   - Reduce drásticamente la carga de la GPU y eleva los FPS estables en juegos exigentes (Free Fire, PUBG, Genshin Impact, COD Mobile).

3. **Desactivación Forzada de Anti-Aliasing (4x MSAA) y Filtros Pesados:**
   - Desactiva pasadas redundantes de suavizado de bordes mediante `settings put global force_msaa 0` y optimización de buffers del compositor de ventanas.
   - Libera ancho de banda de la GPU para eliminar tirones (*micro-stuttering*).

4. **Escaneo Real de Aplicaciones y Juegos Instalados (Hilo Secundario):**
   - **Cero juegos simulados:** La aplicación ya no contiene juegos de muestra precargados ni simulaciones estáticas.
   - **Escáner nativo en segundo plano (`Dispatchers.IO`):** Analiza en un hilo secundario todas las aplicaciones y juegos reales instalados en el teléfono mediante `PackageManager`.
   - **Detección inteligente de juegos:** Identifica automáticamente qué aplicaciones son juegos (`CATEGORY_GAME` / `FLAG_IS_GAME`) y ofrece filtros para "Juegos", "Otras Apps" y "Todas", además de un buscador en tiempo real.
   - **Persistencia garantizada:** Los juegos agregados por el usuario se guardan de forma permanente para que tu biblioteca personalizada siempre esté lista.

5. **Control Dinámico de Resolución de Pantalla y Auto-DPI:**
   - Modificación en caliente de resolución de pantalla (`wm size`) y densidad de píxeles (`wm density`).
   - **Cálculo automático de DPI proporcional:** Evita que los botones o textos de los juegos se vean desproporcionados al cambiar de escala (ejemplo: 1080p a 720p calcula automáticamente la reducción de 420 a 320 DPI).
   - Presets preconfigurados para juegos competitivos (100% Nativo, 85% Óptimo, 75% HD+ 720p, 60% Max FPS 540p) y sliders manuales.

6. **Seguridad y Restauración Automática de Pantalla:**
   - **Al salir del juego:** Un monitor en segundo plano detecta cuando abandonas la partida (vía paquetes y eventos de foco) y **restaura automáticamente** tanto la resolución de pantalla como la escala de renderizado a sus valores originales.
   - **Botón dentro del juego:** Botón explícito **"Restaurar Resolución Original"** y **"Restaurar Escala y Filtros"** dentro del panel.
   - **Regla estricta:** Cero uso de variables globales persistentes (`persist.sys.*`), previniendo cualquier riesgo de bootloop o inestabilidad del sistema operativo.

7. **Activación 100% desde el Teléfono (Sin PC):**
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
  - Scripts Shell `.sh` ejecutados con privilegios ADB (UID 2000) o Root (UID 0).
- **Compatibilidad de Arquitectura:** 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).

---

## 📂 Scripts Shell Integrados

Los scripts residen en `app/src/main/assets/scripts/` y son gestionados por `ScriptManager.kt`:

| Script | Propósito | Argumentos |
| :--- | :--- | :--- |
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

### 3. Jugar con Render Scale y Resolución Optimizada
1. Selecciona tu juego en la lista y presiona **Iniciar**.
2. Aparecerá la pestaña flotante **BOOST** en el lateral de la pantalla.
3. Desliza o toca la pestaña para abrir el panel **Red Magic**.
4. En la pestaña **Render Scale**, mueve el slider en tiempo real (por ejemplo a 70% o 50%) para aumentar drásticamente los FPS.
5. Activa el interruptor **Anular 4x MSAA** para quitar carga innecesaria a la GPU.
6. Si deseas reducir también la resolución de pantalla completa, ve a la pestaña **Resolución & DPI** y elige un preset.
7. Al terminar la partida o salir al menú de inicio, tanto la resolución como el render scale volverán automáticamente a su estado nativo.
