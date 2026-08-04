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

**Entorno para el bonus de Kafka (Windows):**
- Diagnóstico guiado del stack completo (Git, Node, npm, k6, Python, pip, Docker) con tabla de estado y comandos de verificación, en modo mentor: el usuario ejecutaba cada comando y pegaba la salida antes de avanzar al siguiente paso.
- Instalación de WSL2 (`wsl --install`) y Docker Desktop (`winget install Docker.DockerDesktop`), incluyendo verificación explícita post-instalación (no confiar en que winget reportó "éxito" sin comprobarlo — ver reflexión técnica).
- Documentación del stack completo y la nota de WSL2 en el README raíz, sección "Prerrequisitos y Stack".

**Bonus de Kafka (Python + Docker + kafka-python):**
- `docker-compose.yml` de un Kafka local en modo KRaft (sin Zookeeper), explicando broker/controller y listeners.
- Diseño y explicación didáctica del `telemetry_schema.py` (contrato de datos vía `jsonschema`).
- Test integrado `test_gps_events.py` (producer + consumer en el mismo flujo), con explicación paso a paso de serialización, `group_id`/offset, y manejo de timeout.
- Diagnóstico y resolución de dos problemas reales encontrados al ejecutar (no al escribir) el código — ver reflexión técnica.

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

### (c) Prompt maestro para el bonus de Kafka (modo mentor)

Reproducido completo porque fijó dos decisiones de diseño no negociables (test integrado, validación robusta con rangos + timeout) y, sobre todo, exigió explicación pedagógica de cada concepto en vez de solo entregar código — el objetivo explícito era aprender, no solo tener algo funcionando:

> Actúa como QA Automation Engineer senior Y como mentor. Vamos a implementar el bonus de Apache Kafka de una prueba técnica de automatización QA. Tengo poca experiencia con Kafka, así que además de escribir el código, DEBES explicarme cada pieza en español y en términos sencillos a medida que la creas — mi objetivo principal es APRENDER Y ENTENDER, no solo tener el código funcionando.
>
> [...] DECISIONES DE DISEÑO YA TOMADAS (respétalas): 1. Test INTEGRADO con pytest [...] 2. Validación ROBUSTA en el consumidor: mensaje íntegro, validación de esquema formal con jsonschema, validación de rangos válidos (lat entre -90 y 90, lng entre -180 y 180, speed >= 0), manejo de timeout.
>
> [...] EXPLÍCAME el concepto de OFFSET y por qué configuras auto_offset_reset='earliest' (o group nuevo) para asegurar que el test lea el mensaje recién publicado y no se lo "salte". Este es el error clásico — quiero entenderlo bien.
>
> [...] Al terminar cada paso, resúmeme qué creaste y qué concepto aprendí, antes de seguir al siguiente. Recuerda: prioriza que yo ENTIENDA.

## 4. Reflexión técnica

**Velocidad y calidad:** la IA aceleró notablemente el trabajo mecánico (boilerplate de Screenplay, boilerplate de k6, redacción de README) y, más importante, aceleró el *diagnóstico* de fallos reales de entorno (crash por tamaño de página, selectores por idioma, encoding de compilación) que de otra forma habrían tomado mucho más tiempo de investigación manual. El valor más alto no estuvo en generar código nuevo, sino en verificar supuestos contra el sistema real (API real, emulador real) antes de confiar en ellos.

**Errores/alucinaciones detectados y corregidos durante la sesión:**

1. **URL de descarga inventada por una herramienta de fetch web.** Al buscar el instalador de Android cmdline-tools, una consulta a la documentación oficial devolvió una URL de descarga (`edgedl.me.gvt1.com/...`) que resultó en error 404 al usarla. Se corrigió consultando directamente el manifiesto XML oficial de Google (`repository2-3.xml`) para obtener la URL real (`dl.google.com/android/repository/...`). Lección: una respuesta de una herramienta de "resumen web" puede parafrasear o inventar un detalle tan específico como una URL; para acciones que descargan/ejecutan software, se verificó contra la fuente primaria antes de usarla.
2. **Supuesto de que forzar `language`/`locale` en las capabilities de Appium resolvería el problema de selectores dependientes del idioma del dispositivo.** En la práctica, el driver UiAutomator2 rechazó esa capability en el AVD usado (error `SessionNotCreatedException`, bug conocido del driver). Se revirtió el cambio y se documentó como precondición manual en el README en su lugar, en vez de insistir en una solución que no funcionaba en este entorno.
3. **Desalineación de paquete Java (`co.com.pragma` vs `co.com.simon`).** El usuario renombró el paquete base fuera de la sesión (para desvincular el scaffold original de la marca "pragma"); quedó una referencia residual en un string literal (`glue` del runner de Cucumber) que un refactor de IDE no actualiza automáticamente. El usuario lo señaló explícitamente ("omite lo de pragma... ajusta con lupa") y se hizo un barrido completo del repositorio para confirmar que no quedaba ninguna referencia obsoleta.
4. **README heredado no representativo del proyecto.** El scaffold inicial traía un README de "guía para principiante absoluto" genérico, que mencionaba incluso una dependencia (`serenity-appium`) que el proyecto no usa. El usuario lo detectó tras el primer push ("no validamos el readme que se tenía") y se reemplazó por una versión concisa y específica del stack real.
5. **Instalación silenciosamente fallida reportada como éxito.** Al instalar Android Studio vía `winget install`, la herramienta reportó "Successfully installed" con exit code 0, pero el instalador en realidad requiere un prompt de UAC (elevación a administrador) y una sesión no interactiva no puede completarlo — quedó sin instalar de verdad, y solo se detectó al intentar usarlo después. Lección aplicada directamente en el bonus de Kafka: al instalar Docker Desktop de la misma forma, **no se confió en el mensaje de éxito de winget** — se verificó explícitamente la carpeta de instalación (`Test-Path`) y `winget list` antes de darlo por bueno. Esta vez sí se había instalado correctamente, pero la verificación era necesaria para saberlo con certeza en vez de asumirlo.
6. **WSL2 no estaba instalado.** El diagnóstico inicial de `wsl --status` reveló que el usuario no tenía WSL2, prerrequisito de Docker Desktop en Windows. Se guio la instalación (`wsl --install` desde PowerShell como administrador) y se advirtió de antemano que requeriría reiniciar el equipo — el propio comando lo confirmó al final ("Los cambios se aplicarán una vez que se reinicie el sistema"). Para no perder el hilo de la sesión durante el reinicio, se guardó una nota de memoria del proyecto con el estado exacto y el siguiente paso pendiente, de modo que la conversación pudiera retomarse sin recapitular todo manualmente.
7. **`kafka-python` no instalaba en Python 3.14.** Al correr el test por primera vez, la importación falló con `ModuleNotFoundError: No module named 'kafka.vendor.six.moves'` — un bug de compatibilidad conocido del paquete `kafka-python` original con Python 3.12+. Se cambió a `kafka-python-ng`, el fork mantenido activamente que expone el mismo namespace `kafka`, sin tocar una sola línea del test. Esto no se detectó leyendo el código (se veía perfectamente correcto) sino corriéndolo de verdad.
8. **Falla intermitente de socket en el consumer (Windows), y empeoraba con el uso.** Corriendo el test repetidamente para confirmar que la lógica de `group_id`/offset era determinista, apareció `ValueError: Invalid file descriptor: -1` dentro del selector de sockets interno de `kafka-python-ng` — confirmado por los logs del broker como un problema puramente del lado del cliente Python (el grupo sí se formaba bien en el servidor). Un primer reintento de 3 intentos sin pausa resultó insuficiente en una corrida (3/3 fallos seguidos); se subió a 5 intentos con 1s de espera entre cada uno, y se re-verificó con más de 15 corridas adicionales que eso lo absorbe siempre. La frecuencia de la falla subía cuantas más veces seguidas se corría el test en poco tiempo — consistente con acumulación de sockets en `TIME_WAIT` en Windows por las corridas rápidas y repetidas de la sesión de prueba, no algo esperable en un uso normal. Sin correr el test muchas veces seguidas a propósito, esta falla habría pasado desapercibida — un solo "1 passed" no prueba que algo sea confiable.

En los ocho casos, la corrección no fue "la IA se equivocó y ya" — fue el resultado de verificar contra el sistema real (correr el build, correr el test —varias veces, no solo una—, hacer la petición HTTP real, comprobar que un archivo de instalación efectivamente existe) en vez de asumir que algo funcionó por haberse reportado como exitoso o por haber pasado una sola vez.
