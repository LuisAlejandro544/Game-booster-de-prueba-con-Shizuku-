# STRUCTURE - Arquitectura del Código y Árbol de Archivos

Este documento desglosa la estructura del proyecto, la responsabilidad de cada paquete y el flujo de comunicación entre componentes.

---

## 🌳 Árbol de Archivos del Proyecto

```
/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── aidl/
│   │   │   │   └── android/
│   │   │   │       └── content/
│   │   │   │           └── pm/
│   │   │   │               └── IPackageManager.aidl      # Interfaz AIDL para IPC con el sistema
│   │   │   ├── assets/
│   │   │   │   └── scripts/
│   │   │   │       ├── compile_game_aot.sh              # Script Shell: compila DEX a código nativo AOT con dex2oat
│   │   │   │       ├── trim_background_memory.sh        # Script Shell: liberación quirúrgica de RAM vía cmd activity trim-memory
│   │   │   │       ├── detect_graphics_driver.sh        # Script Shell: detecta driver activo/nativo (/proc/maps, dumpsys)
│   │   │   │       ├── apply_graphics_driver.sh         # Script Shell: conmuta a OpenGL, Vulkan o ANGLE, y resetea
│   │   │   │       ├── apply_game_render_scale.sh       # Script Shell: downscale 3D interno vía 'cmd game set'
│   │   │   │       ├── reset_game_render_scale.sh       # Script Shell: restablece downscale vía 'cmd game reset'
│   │   │   │       ├── apply_graphic_filters.sh         # Script Shell: desactiva 4x MSAA y optimiza GPU
│   │   │   │       ├── apply_resolution.sh              # Script Shell: aplica resolución y DPI con backup previo
│   │   │   │       ├── reset_resolution.sh              # Script Shell: restablece a valores nativos
│   │   │   │       ├── get_display_info.sh              # Script Shell: consulta métricas de pantalla
│   │   │   │       └── get_foreground_app.sh            # Script Shell: detecta la app en primer plano
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                      # Actividad principal, Scaffold, navegación Edge-to-Edge
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppScanner.kt                    # Escaneo de apps/juegos en Dispatchers.IO y almacenamiento persistente
│   │   │   │   │   └── NotificationBlockerManager.kt    # Gestor de no molestar y bloqueo de notificaciones
│   │   │   │   ├── service/
│   │   │   │   │   ├── GameBoosterNotificationListener.kt # Listener de notificaciones
│   │   │   │   │   ├── GameBoosterOverlayService.kt     # Servicio en primer plano: ventana flotante y panel Red Magic
│   │   │   │   │   └── WifiLowLatencyManager.kt         # Gestor nativo del Modo Wi-Fi de Ultrabaja Latencia (WIFI_MODE_FULL_LOW_LATENCY)
│   │   │   │   ├── shizuku/
│   │   │   │   │   ├── AotCompilationManager.kt         # Gestor de la Compilación Previa AOT contra el Micro-Stuttering
│   │   │   │   │   ├── MemoryTrimManager.kt             # Gestor de la Liberación Quirúrgica de Memoria RAM
│   │   │   │   │   ├── GameRenderManager.kt             # Gestor del motor de renderizado (downscale y MSAA)
│   │   │   │   │   ├── GraphicsDriverManager.kt         # Gestor de controladores gráficos (OpenGL ES, Vulkan, ANGLE)
│   │   │   │   │   ├── ResolutionManager.kt             # Lógica matemática de DPI proporcional y control de resolución
│   │   │   │   │   ├── ScriptManager.kt                 # Despliega y ejecuta scripts .sh con Shizuku (IO Dispatcher)
│   │   │   │   │   ├── ShizukuManager.kt                # Ciclo de vida del Binder, verificación de UID y permisos
│   │   │   │   │   └── ShizukuPackageManager.java       # Implementación IPC con IPackageManager y ShizukuBinderWrapper
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── AppPickerDialog.kt           # Diálogo para buscar, filtrar y agregar apps/juegos reales
│   │   │   │   │   │   └── GameLaunchDialog.kt          # Diálogo selector: Ejecución Normal vs Compilación Previa AOT
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── AppNavigation.kt             # Definición de rutas y destinos (Shizuku, Booster, Guía, Bloqueador)
│   │   │   │   │   ├── redmagic/
│   │   │   │   │   │   ├── RedMagicPanel.kt             # Shell del panel lateral táctico y barra vertical de iconos
│   │   │   │   │   │   ├── RedMagicRenderTab.kt         # Pestaña RENDER: conmutador de driver (OGL/VK/ANGLE), slider downscale y MSAA
│   │   │   │   │   │   ├── RedMagicResolutionTab.kt     # Pestaña RESOLUCIÓN: sliders WM y cálculo auto-DPI
│   │   │   │   │   │   └── RedMagicPerformanceTab.kt    # Pestaña RENDIMIENTO y herramientas no molestar
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── BoosterScreen.kt             # Biblioteca personalizada de juegos, perfiles y control de overlay
│   │   │   │   │   │   ├── GuideScreen.kt               # Guía paso a paso para activación en móvil sin PC
│   │   │   │   │   │   ├── ShizukuScreen.kt             # Pantalla de estado y botón de activación de Shizuku
│   │   │   │   │   │   └── NotificationBlockerScreen.kt # Pantalla de configuración del bloqueador de notificaciones
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt                     # Paleta clásica sobria e indicadores de estado
│   │   │   │   │       ├── Theme.kt                     # MaterialTheme 3 (modo claro/oscuro dinámico)
│   │   │   │   │       └── Type.kt                      # Jerarquía tipográfica
│   │   │   │   └── viewmodel/
│   │   │   │       └── GameBoosterViewModel.kt          # ViewModel central: orquesta Shizuku, scripts, escaneo y UI States
│   │   │   ├── res/
│   │   │   │   ├── mipmap-*/                            # Iconos adaptativos de la aplicación
│   │   │   │   └── values/
│   │   │   │       └── strings.xml                      # Etiquetas de recursos del sistema (app_name: Game Booster)
│   │   │   └── AndroidManifest.xml                      # Declaración de permisos, servicio overlay y proveedor Shizuku
│   │   └── test/java/com/example/
│   │       ├── ExampleRobolectricTest.kt                # Pruebas unitarias de contexto y recursos
│   │       ├── ExampleUnitTest.kt                       # Pruebas de estados, drivers, render scale y biblioteca
│   │       └── GreetingScreenshotTest.kt                # Pruebas de captura de interfaz
│   └── build.gradle.kts                                 # Configuración del módulo app, dependencias y plugins
├── gradle/
│   └── libs.versions.toml                               # Catálogo de versiones de dependencias
├── build.gradle.kts                                     # Configuración Gradle raíz
├── metadata.json                                        # Identificador del proyecto en plataforma
├── README.md                                            # Documentación general y guía de uso
├── ROADMAP.md                                           # Fases y planes futuros
├── STRUCTURE.md                                         # Este archivo: arquitectura y capas
├── AI_CONTEXT.md                                        # Contexto técnico y directrices de implementación
└── AGENTS.md                                            # Roles de desarrollo y prompts de flujo
```

---

## 🏗️ Capas de la Arquitectura

### 1. Capa de Sistema y Ejecución (`shizuku/` + `assets/scripts/`)
* **`GraphicsDriverManager`:** Gestiona la detección y conmutación en caliente de controladores gráficos (OpenGL ES, Vulkan, ANGLE) durante la ejecución del juego. Analiza mapas de memoria (`/proc/$PID/maps`) y SurfaceFlinger, y garantiza la reversión al salir del juego sin tocar `persist.sys.*`.
* **`GameRenderManager`:** Orquesta la escala de renderizado 3D interno (`cmd game set --downscale`) y filtros de GPU (MSAA). Mantiene su estado reactivo (`GameRenderState`) en `StateFlow` y ejecuta llamadas exclusivamente en `Dispatchers.IO`.
* **`ScriptManager`:** Extrae los archivos `.sh` de assets al almacenamiento interno ejecutable (`/data/local/tmp/gamebooster/`). Invoca la shell del sistema a través de Shizuku para ejecutar los comandos con permisos elevados (UID 2000 ADB / UID 0 Root).
* **`ResolutionManager`:** Mantiene el estado reactivo (`StateFlow<DisplayResolutionState>`). Realiza el cálculo matemático de densidad (`(nativeDpi * (targetWidth / nativeWidth))`) y se asegura de que cualquier cambio se pueda revertir a la normalidad en caso de error.
* **`ShizukuPackageManager`:** Código Java que se conecta al Binder de `IPackageManager` del sistema Android mediante `ShizukuBinderWrapper`.

### 2. Capa de Presentación Flotante (`service/` + `ui/redmagic/`)
* **`GameBoosterOverlayService`:** Un `ForegroundService` que añade una vista Compose directamente al `WindowManager` con la bandera `TYPE_APPLICATION_OVERLAY`.
* **Burbuja Flotante:** Componente deslizable que detecta gestos táctiles y permite abrir el panel sin pausar el juego.
* **`RedMagicPanelContent` (Modularizado):**
  - **`RedMagicRenderTab`:** Selector táctico de controlador gráfico (OpenGL ES, Vulkan, ANGLE) con indicador del driver nativo detectado, slider continuo de downscale en vivo (50% a 100%) y switch para anular 4x MSAA.
  - **`RedMagicResolutionTab`:** Presets rápidos competitivos, sliders de ancho/alto global y switch de cálculo proporcional automático de DPI.
  - **`RedMagicPerformanceTab`:** Priorización de hilos y herramientas para evitar interrupciones.
* **Watcher en segundo plano:** Corrutina periódica que comprueba la aplicación enfocada y restaura automáticamente tanto la resolución nativa como el render scale y el controlador gráfico al detectar que el usuario salió del juego.

### 3. Capa de Negocio (`viewmodel/`)
* **`GameBoosterViewModel`:** Coordina la actividad principal, la biblioteca de juegos, los permisos de superposición y expone los `StateFlow` para `ShizukuState`, `DisplayResolutionState` y `GameRenderState`.
