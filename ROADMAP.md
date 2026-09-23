# ROADMAP - Hoja de Ruta de Desarrollo

Este documento define la evolución técnica y de producto para el **Game Booster** con Shizuku, panel flotante Red Magic, control de renderizado y conmutación de controladores gráficos.

---

## 📌 Fase 1: Fundamentos del Sistema y Puente Shizuku (Completada ✅)
- [x] Implementación del servicio y proveedor de Shizuku (`ShizukuProvider`).
- [x] Definición AIDL de `IPackageManager` y envoltura con `ShizukuBinderWrapper`.
- [x] Control de ciclo de vida del Binder (`OnBinderReceivedListener`, `OnBinderDeadListener`).
- [x] Detección de privilegios de usuario (ADB UID 2000 / Root UID 0).
- [x] Guía integrada para activación sin PC mediante Depuración Inalámbrica (Android 11+).
- [x] Diseño sobrio, modular y accesible sin elementos de juego exagerados.

---

## 📌 Fase 2: Scripts Shell y Panel Flotante Red Magic (Completada ✅)
- [x] Creación de scripts Shell modulares (`apply_resolution.sh`, `reset_resolution.sh`, `get_display_info.sh`, `get_foreground_app.sh`).
- [x] Despliegue dinámico y ejecución asíncrona de scripts en hilos secundarios (`Dispatchers.IO`).
- [x] Servicio en primer plano para la burbuja flotante deslizable (`GameBoosterOverlayService` con `SYSTEM_ALERT_WINDOW`).
- [x] Panel lateral estilo Red Magic Game Space con barra de herramientas e íconos modulares.
- [x] Algoritmo matemático para cálculo automático y proporcional de DPI según resolución.
- [x] Monitoreo en segundo plano de foco de la app para restauración automática de la resolución nativa al salir del juego.
- [x] Botón directo de **"Restaurar Resolución Original"** dentro del panel y en la interfaz principal.
- [x] Simulador/vista previa interactiva del panel dentro de la app para pruebas directas en el móvil.

---

## 📌 Fase 3: Downscale de Renderizado 3D y Optimización de GPU (Completada ✅)
- [x] **Downscale Interno de Superficie (`cmd game set --downscale`):**
  - Implementación del script `apply_game_render_scale.sh` y `reset_game_render_scale.sh`.
  - Slider interactivo en tiempo real integrado en la pestaña RENDER del panel Red Magic para deslizar mientras se juega.
  - Aislamiento de la resolución del sistema: solo los polígonos 3D del juego bajan de calidad, la UI del sistema se mantiene nítida.
- [x] **Desactivación Forzada de Anti-Aliasing (4x MSAA) & Filtros Pesados:**
  - Implementación del script `apply_graphic_filters.sh`.
  - Interruptor directo para anular 4x MSAA y optimizar buffers en GPU.
- [x] **Gestor Centralizado de Render (`GameRenderManager.kt`):**
  - Manejo de estado con `StateFlow` y ejecución reactiva en `Dispatchers.IO`.
- [x] **Restauración Automática y Manual:**
  - El monitor de foco restaura automáticamente el render scale y filtros al salir del juego.
  - Botón explícito "Restaurar Escala y Filtros Nativos" en el panel flotante.

---

## 📌 Fase 4: Controlador Gráfico Dinámico (OpenGL, Vulkan, ANGLE) (Completada ✅)
- [x] **Detección Automática del Controlador Nativo:**
  - Script `detect_graphics_driver.sh` que inspecciona `/proc/$PID/maps`, SurfaceFlinger y configuraciones globales de Android.
  - Identifica el driver por defecto que usa el juego (Vulkan o OpenGL ES).
- [x] **Conmutación en Caliente desde el Panel Flotante:**
  - Script `apply_graphics_driver.sh` para forzar OpenGL ES, Vulkan o capa ANGLE en tiempo de ejecución.
  - Integración en `GraphicsDriverManager.kt` con corrutinas en `Dispatchers.IO`.
  - Tarjeta de control de driver integrada en `RedMagicRenderTab.kt` con badge de estado y explicaciones técnicas.
- [x] **Reversión Segura al Salir del Juego:**
  - Se garantiza que las configuraciones de driver solo aplican mientras se juega.
  - Restauración automática en el watcher de salida y en el ciclo de vida `onDestroy()` del servicio overlay.
  - Cero uso de `persist.sys.*`.

---

## 📌 Fase 5: Escáner Nativo y Biblioteca Personalizada (Completada ✅)
- [x] **Eliminación Total de Datos Simulados:** Eliminación de juegos fijos o mocks estáticos.
- [x] **Escaneo de Apps y Juegos en Hilo Secundario:**
  - Implementación de `AppScanner.kt` con ejecución estricta en `Dispatchers.IO`.
  - Detección de juegos mediante `ApplicationInfo.CATEGORY_GAME` y `FLAG_IS_GAME`.
  - Permiso `QUERY_ALL_PACKAGES` y bloque `<queries>` en `AndroidManifest.xml` para máxima compatibilidad (Uptodown / tiendas de terceros).
- [x] **Selector Interactivo (`AppPickerDialog.kt`):**
  - Diálogo Compose con buscador en tiempo real, chips de filtrado (Juegos / Apps / Todas), avatares dinámicos y tags de accesibilidad.
- [x] **Persistencia de Biblioteca (`GameStorage.kt`):**
  - Almacenamiento local persistente en `SharedPreferences` para preservar los juegos añadidos por el usuario.
- [x] **Gestión de Juegos en BoosterScreen:**
  - Estado vacío visual elegante con llamado a la acción.
  - Botón para agregar y botón para eliminar juegos de la biblioteca.

---

## 📌 Fase 6: Optimización de Red, AOT y Gestión Quirúrgica de RAM (Completada ✅)
- [x] **Compilación Previa AOT contra el Micro-Stuttering (dex2oat):**
  - Implementación del script `compile_game_aot.sh` utilizando `cmd package compile -m speed-profile -f <paquete>`.
  - Diálogo interactivo al iniciar un juego (`GameLaunchDialog.kt`) para seleccionar entre *Ejecución Normal* y *Compilación Previa AOT*.
  - Elimina caídas bruscas de FPS y parones causados por la compilación JIT durante partidas.
- [x] **Modo Wi-Fi de Ultrabaja Latencia (`WIFI_MODE_FULL_LOW_LATENCY`):**
  - Implementación de `WifiLowLatencyManager.kt` utilizando la API nativa de Android 10+ (API 29).
  - Adquisición de `WifiLock` durante las sesiones de juego para reducir el jitter y estabilizar el ping en partidas competitivas.
  - Switch de control tanto en la pantalla principal como en el panel lateral Red Magic.
- [x] **Liberación Quirúrgica de Memoria RAM de Apps en Segundo Plano:**
  - Implementación del script `trim_background_memory.sh` invocando `cmd activity trim-memory --all RUNNING_CRITICAL`.
  - Integración en `MemoryTrimManager.kt` con feedback en tiempo real.
  - Libera memoria purgada sin cierres abruptos ni sobrecalentamiento de CPU.

---

## 📌 Fase 7: Telemetría y Controles Tácticos Avanzados (Próxima)
- [ ] **Monitor de FPS y Temperatura en Tiempo Real:**
  - Lectura de métricas desde `/sys/class/thermal/` y llamadas `dumpsys SurfaceFlinger` mediante scripts `.sh`.
  - Widget discreto en el overlay que muestra FPS reales, temperatura de batería y consumo de CPU.
- [ ] **Control de Tasa de Refresco (Hz):**
  - Cambio forzado de tasa de refresco a 90Hz / 120Hz para juegos que limitan a 60Hz.
  - Script complementario `set_refresh_rate.sh` usando `settings put system min_refresh_rate`.
- [ ] **Configuraciones Persistentes por Juego:**
  - Integración con base de datos local (Room) para recordar la resolución, downscale, DPI y driver gráfico de cada juego específico.
  - Aplicación automática de los ajustes al detectar el lanzamiento del juego.

---

## 📌 Fase 8: Distribución y Seguridad
- [ ] Empaquetado APK optimizado para tiendas de terceros (Uptodown, APKPure, GitHub Releases).
- [ ] Verificación de compatibilidad con Android 15 y nuevas políticas de Foreground Service.
- [ ] Mantenimiento estricto de la política de cero alteración de `persist.sys.*`.
