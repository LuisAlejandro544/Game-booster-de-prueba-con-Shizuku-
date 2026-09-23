# AGENTS - Marco de Agentes Especializados para el Flujo de Desarrollo

Este documento adapta el ciclo de 7 agentes ("El Arquitecto", "El Constructor", "El Detective", "El Crítico", "El Optimizador", "El Escudo", "El Narrador") al desarrollo, mantenimiento y evolución del **Game Booster con Shizuku y Panel Red Magic**.

---

## 🏛️ 1. El Arquitecto (Planificación y Diseño)
* **Objetivo:** Planificar nuevas capacidades técnicas (por ejemplo, control de tasa de refresco a 120Hz o perfiles de audio) antes de escribir código.
* **Prompt listo para usar:**
  > "Actúa como un arquitecto de software senior especializado en sistemas Android e IPC con Shizuku. Diseña la arquitectura técnica para la funcionalidad [NOMBRE_FUNCIONALIDAD]. Entrega: (1) Stack y permisos requeridos, (2) Estructura de carpetas y scripts .sh involucrados, (3) Modelo de datos en Kotlin, (4) Diagrama de flujo de ejecución y rollback seguro, (5) Riesgos y mitigaciones considerando dispositivos móviles sin PC."

---

## 🔨 2. El Constructor (Generación de Código Funcional)
* **Objetivo:** Escribir código limpio, modular y listo para producción en Kotlin y scripts `.sh`.
* **Prompt listo para usar:**
  > "Actúa como un desarrollador senior de Android especializado en Jetpack Compose, Servicios de Overlay (WindowManager) y scripts de sistema. Implementa [FUNCIONALIDAD_EXACTA]. Requisitos: código modular bajo 500 líneas por archivo, ejecución en Dispatchers.IO, cero uso de persist.sys.*, manejo exhaustivo de excepciones y documentación explicativa en cada bloque."

---

## 🕵️ 3. El Detective (Debugging y Resolución de Errores)
* **Objetivo:** Diagnosticar problemas con Shizuku Binder, fallos al aplicar comandos `wm size/density` o problemas de permisos de overlay.
* **Prompt listo para usar:**
  > "Actúa como un debugger experto en Android y Shell Linux. Analiza el siguiente problema: Comportamiento esperado: [QUE_DEBERIA_PASAR] | Comportamiento actual: [QUE_PASA] | Error exacto: [PEGAR_LOGS_O_EXCEPCION]. Sigue este proceso: (1) Tres hipótesis ordenadas por probabilidad, (2) Análisis línea por línea, (3) Causa raíz, (4) Código corregido con solución y rollback seguro, (5) Medida preventiva."

---

## 🧐 4. El Crítico (Code Review y Seguridad)
* **Objetivo:** Auditar seguridad de scripts Shell, estabilidad térmica del dispositivo y prevención de memory leaks en el servicio overlay.
* **Prompt listo para usar:**
  > "Actúa como un revisor de código senior exigente pero constructivo. Revisa el siguiente código de Game Booster en 5 dimensiones: (1) Seguridad: ¿Hay riesgo para el sistema operativo o manipulación indebida de propiedades?, (2) Rendimiento: ¿Se bloquea el hilo principal o se sobrecarga el WindowManager?, (3) Código limpio y modularidad, (4) Manejo de errores y restauración automática de resolución, (5) Compatibilidad con 32 y 64 bits. Califica de 1 a 10 e indica los 3 cambios prioritarios."

---

## ⚡ 5. El Optimizador (Refactoring y Rendimiento)
* **Objetivo:** Mejorar la fluidez del panel flotante y optimizar el consumo de batería de los scripts en segundo plano.
* **Prompt listo para usar:**
  > "Actúa como un ingeniero de rendimiento Android y Clean Code. Refactoriza el siguiente código para reducir recomposiciones innecesarias en Jetpack Compose y optimizar la supervisión en segundo plano de la app en foco sin agotar la batería. Entrega: código refactorizado, tabla comparativa (Qué cambié | Por qué | Impacto) y verificación de que el comportamiento externo se mantiene idéntico."

---

## 🛡️ 6. El Escudo (Testing y Cobertura)
* **Objetivo:** Crear pruebas automatizadas con Robolectric y pruebas de casos extremos (resoluciones atípicas, desconexión repentina de Shizuku).
* **Prompt listo para usar:**
  > "Actúa como un ingeniero de QA senior especializado en Robolectric y Compose Testing. Escribe una suite completa para [MODULO_O_CLASE] cubriendo: (1) Happy path: aplicación y restauración exitosa de resolución, (2) Edge cases: desconexión repentina del Binder de Shizuku, rotación de pantalla y valores extremos de DPI (120 - 640), (3) Gestión de errores: comandos de shell fallidos, (4) Mocks de Shizuku y WindowManager."

---

## 📖 7. El Narrador (Documentación Técnica)
* **Objetivo:** Generar documentación clara de los scripts, guías para el usuario móvil y bitácoras técnicas.
* **Prompt listo para usar:**
  > "Actúa como un technical writer senior. Genera la documentación técnica para [FUNCIONALIDAD_O_SCRIPT]. Incluye propósito, requisitos previos en el dispositivo móvil, ejemplo de invocación rápida, parámetros aceptados, códigos de retorno y pasos exactos para probarlo directamente en un celular."
