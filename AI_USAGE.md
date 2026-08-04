# AI_USAGE.md — Bitácora de co-pilotaje con IA

## 1. Herramientas utilizadas

- **Claude (Sonnet 5), en modo agente**, integrado en el entorno de desarrollo (extensión de VSCode / CLI), con acceso a shell (PowerShell/Bash), sistema de archivos, y herramientas de red. Actuó como copiloto durante todo el desarrollo: diagnóstico y configuración del entorno móvil, escritura de código (Java/Screenplay y k6/JavaScript), depuración interactiva contra un emulador Android y la API real de DummyJSON, control de versiones (Git/GitHub), y documentación.

## 2. Casos de uso específicos

**Capa móvil (Java + Serenity + Screenplay + Cucumber + Appium):**
- Instalación y configuración del entorno desde cero: `JAVA_HOME`, Android SDK, `ANDROID_HOME`, Appium 3 + driver UiAutomator2, creación/depuración de un emulador funcional.
- Diagnóstico de fallos reales: crash de la app por incompatibilidad de tamaño de página de memoria (16KB vs 4KB) entre el APK y el emulador; selectores rotos por idioma del dispositivo (`test-Username` vs `test-Usuario`); `UndefinedStepException` causado por falta de encoding UTF-8 explícito en la compilación Java.
- Implementación del caso de login inválido (obligatorio según el reto) y eliminación de esperas fijas (`Thread.sleep`) por esperas dinámicas ya provistas por Serenity.
- Limpieza de archivos huérfanos/config duplicada (`android.conf` con rutas de otra máquina).

**Capa de API (k6 + JavaScript):**
- Generación del andamiaje del proyecto (config centralizada, schemas por endpoint, helpers de aserciones reutilizables).
- Resolución del problema de validación de JSON Schema en el runtime de k6 (`goja`, no Node/npm) — ver prompt (b) abajo.
- Implementación del flujo E2E híbrido (login → usuario autenticado → usuario por id → crear/actualizar/borrar), con verificación empírica previa de cada endpoint contra la API real (no se asumió el contrato descrito en el prompt sin comprobarlo).
- Configuración de thresholds de SLA (`p(95)<1500ms`) y generación de reporte HTML/JSON vía `k6-reporter`, incluyendo un escenario de demostración que falla el SLA a propósito (`?delay=2000`).
- Redacción de los README de cada módulo y de esta bitácora.

## 3. Ejemplos de prompts

### (a) Prompt maestro para la suite de API en k6

Este prompt aportó valor significativo porque fijó de antemano el contrato verificado de la API (endpoints singular/plural de auth, comportamiento mock de las operaciones de creación/actualización/borrado), evitando que el modelo alucinara persistencia donde no la hay o confundiera `/user/login` con `/users/login`:

> Actúa como QA Automation Engineer senior. Vamos a construir una suite de pruebas de API en k6 (JavaScript) para una prueba técnica de automatización QA. Te doy TODO el contexto y el plan completo en este mensaje. Trabaja de forma incremental y ordenada, pero ya tienes autorización para ejecutar todos los pasos en esta sesión.
>
> [...] CRUD de usuarios: base https://dummyjson.com/users (PLURAL). Autenticación: https://dummyjson.com/user/login y https://dummyjson.com/user/me (SINGULAR "user"). NO confundir singular/plural — es el error más común con esta API. [...] COMPORTAMIENTO MOCK (crítico): POST /users/add, PUT /users/{id} y DELETE /users/{id} NO persisten. [...] El diseño DEBE respetar esto: nunca crear y luego re-consultar por id esperando encontrarlo. Se valida la RESPUESTA de cada mutación, no la persistencia.
>
> [...] Empieza ahora por el PASO 1 y ve avanzando. Al terminar cada paso, dime qué archivos creaste o modificaste antes de pasar al siguiente.

*(prompt completo disponible en el historial de la sesión; se reproduce aquí la parte que fija el contrato de la API, que es la que previno errores)*

**Nota de proceso:** antes de programar nada se verificó cada endpoint descrito con peticiones reales (`curl`) contra `dummyjson.com`. El contrato del prompt resultó ser correcto en su totalidad, incluyendo el detalle de que `POST /users/add` devuelve **201** (no 200) — un punto que el propio prompt marcaba como incierto.

### (b) Resolución de JSON Schema en k6 sin Node/npm

> Nota técnica importante: k6 NO corre sobre Node (usa el runtime goja), así que no hay npm en tiempo de ejecución y no puedes usar ajv directamente sin bundlear. Antes de implementar la validación de JSON Schema, explícame brevemente el tradeoff entre (a) validación manual por tipos en helpers y (b) una lib bundleada/import remoto, y elige la más simple y robusta para esta prueba. Deja constancia de esa decisión.

Esto evitó una alucinación probable: sin esta instrucción, el camino "por defecto" habría sido sugerir `ajv` (la librería estándar de JSON Schema en el ecosistema JS) sin advertir que su compilación de validadores vía `new Function()` en runtime tiene problemas de compatibilidad conocidos con el motor `goja` de k6. Se optó por un validador propio y liviano (campos requeridos + `typeof`), documentado en `api-tests/README.md`.

## 4. Reflexión técnica

**Velocidad y calidad:** la IA aceleró notablemente el trabajo mecánico (boilerplate de Screenplay, boilerplate de k6, redacción de README) y, más importante, aceleró el *diagnóstico* de fallos reales de entorno (crash por tamaño de página, selectores por idioma, encoding de compilación) que de otra forma habrían tomado mucho más tiempo de investigación manual. El valor más alto no estuvo en generar código nuevo, sino en verificar supuestos contra el sistema real (API real, emulador real) antes de confiar en ellos.

**Errores/alucinaciones detectados y corregidos durante la sesión:**

1. **URL de descarga inventada por una herramienta de fetch web.** Al buscar el instalador de Android cmdline-tools, una consulta a la documentación oficial devolvió una URL de descarga (`edgedl.me.gvt1.com/...`) que resultó en error 404 al usarla. Se corrigió consultando directamente el manifiesto XML oficial de Google (`repository2-3.xml`) para obtener la URL real (`dl.google.com/android/repository/...`). Lección: una respuesta de una herramienta de "resumen web" puede parafrasear o inventar un detalle tan específico como una URL; para acciones que descargan/ejecutan software, se verificó contra la fuente primaria antes de usarla.
2. **Supuesto de que forzar `language`/`locale` en las capabilities de Appium resolvería el problema de selectores dependientes del idioma del dispositivo.** En la práctica, el driver UiAutomator2 rechazó esa capability en el AVD usado (error `SessionNotCreatedException`, bug conocido del driver). Se revirtió el cambio y se documentó como precondición manual en el README en su lugar, en vez de insistir en una solución que no funcionaba en este entorno.
3. **Desalineación de paquete Java (`co.com.pragma` vs `co.com.simon`).** El usuario renombró el paquete base fuera de la sesión (para desvincular el scaffold original de la marca "pragma"); quedó una referencia residual en un string literal (`glue` del runner de Cucumber) que un refactor de IDE no actualiza automáticamente. El usuario lo señaló explícitamente ("omite lo de pragma... ajusta con lupa") y se hizo un barrido completo del repositorio para confirmar que no quedaba ninguna referencia obsoleta.
4. **README heredado no representativo del proyecto.** El scaffold inicial traía un README de "guía para principiante absoluto" genérico, que mencionaba incluso una dependencia (`serenity-appium`) que el proyecto no usa. El usuario lo detectó tras el primer push ("no validamos el readme que se tenía") y se reemplazó por una versión concisa y específica del stack real.

En los cuatro casos, la corrección no fue "la IA se equivocó y ya" — fue el resultado de verificar contra el sistema real (correr el build, correr el test, hacer la petición HTTP real) en vez de asumir que el código generado era correcto por estar bien escrito.
