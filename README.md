# Mi Proyecto QA — Simon Movilidad

Suite de automatización para la app móvil SauceLabs Swag Labs (**Java + Screenplay + Serenity BDD + Cucumber + Appium**) y su capa de servicios sobre DummyJSON (**k6**).

```
.
├── mobile-tests/          # suite móvil (Java + Screenplay + Serenity + Cucumber + Appium)
│   ├── gradle/            # wrapper de Gradle
│   └── src/               # page objects, tasks, step definitions, features (Gherkin)
├── api-tests/             # suite de API (k6)
│   ├── src/
│   │   ├── config/        # BASE_URL, credenciales, SLA -- variables de entorno, nada quemado
│   │   ├── helpers/       # aserciones reutilizables: status, SLA, schema, headers, reportes
│   │   └── schemas/       # contratos JSON Schema por endpoint
│   └── reports/           # reportes HTML/JSON generados al correr la suite
└── event-tests/           # bonus: Kafka (Python + Docker)
    └── schemas/           # contrato de datos del evento de telemetría GPS
```

> **Atajo para VS Code:** todos los comandos de este README también están disponibles como *tasks* de VS Code (`.vscode/tasks.json`) — `Ctrl+Shift+P` → **"Tasks: Run Task"** → elige la tarea. Incluye una tarea compuesta para Kafka que abre productor y consumidor en dos terminales a la vez. Esto es una comodidad solo para VS Code; los comandos de abajo funcionan en cualquier terminal.

---

## Prerrequisitos y Stack

Herramientas necesarias para todo el proyecto (móvil + API + bonus de Kafka) y para qué se usa cada una. Las versiones de la columna "Versión verificada" son las del entorno donde se desarrolló y probó este repo (Windows 11); cualquier versión reciente equivalente debería funcionar igual.

| Herramienta | Para qué se usa | Versión verificada | Verificar con |
|---|---|---|---|
| Git | Versionar y publicar el repositorio | 2.55.0 | `git --version` |
| Node.js | Appium corre sobre Node; utilidades de apoyo para la capa de API | v24.15.0 | `node --version` |
| npm | Gestor de paquetes de Node (instala Appium) | 11.12.1 | `npm --version` |
| k6 | Framework de pruebas de API y de carga | v2.0.0 | `k6 version` |
| Java 17 (JDK) | Compilar y correr la suite móvil (Gradle/Serenity) | 17.0.12 | `java -version` |
| Android SDK + Appium 3 + UiAutomator2 | Automatizar la app móvil vía emulador/dispositivo | ver sección "Suite móvil" | `appium -v`, `adb devices` |
| Python | Scripts productor/consumidor del bonus de Kafka (`kafka-python`) | 3.14.4 | `python --version` |
| pip | Gestor de paquetes de Python | 26.0.1 | `pip --version` |
| Docker Desktop | Levantar el broker de Kafka localmente vía `docker-compose` | 29.6.2 | `docker --version`, `docker ps` |

### Nota sobre Docker en Windows: requiere WSL2

Docker Desktop en Windows necesita **WSL2** (Windows Subsystem for Linux, versión 2) como motor de bajo nivel para correr contenedores Linux. Si `docker --version` falla o Docker Desktop no arranca, verifica primero:

```powershell
wsl --status
```

Si no está instalado, instálalo desde una PowerShell **como administrador**:

```powershell
wsl --install
```

Esto requiere **reiniciar el equipo** para que los cambios apliquen (el propio comando lo indica al final). Después de reiniciar, confirma con `wsl --status` que la versión predeterminada sea `2`, y recién ahí instala/abre Docker Desktop. La primera vez que abras Docker Desktop debes esperar a que el ícono de la bandeja del sistema deje de animarse (motor arrancado) antes de correr cualquier comando `docker`.

---

## Suite móvil

### Stack

- Java 17
- Gradle (wrapper incluido)
- Serenity BDD (patrón Screenplay) + Cucumber
- Appium 3 + driver UiAutomator2

### Requisitos previos

- JDK 17 (`JAVA_HOME` configurado)
- Node.js 18+ (para Appium)
- Android SDK con `platform-tools` y una imagen de sistema instalada (`ANDROID_HOME` configurado)
- Appium 3 con el driver UiAutomator2:
  ```bash
  npm install -g appium
  appium driver install uiautomator2
  ```
- Un emulador Android corriendo, o un dispositivo físico con depuración USB activada
- El APK de la app (`mobile-tests/2-sauceLabs.apk`, incluido en este repo)

### Configuración

Las capabilities de Appium están en `mobile-tests/src/test/resources/serenity.conf`. **No necesitas configurar nada para correr la suite** — `build.gradle` calcula automáticamente la ruta del APK incluido en el proyecto (`mobile-tests/2-sauceLabs.apk`) y se la pasa como variable de entorno al proceso de test.

Solo si necesitas apuntar a otro APK/ruta (poco común), puedes sobreescribir con tu propia variable de entorno `APP_PATH` antes de correr el build:

```bash
export APP_PATH="/ruta/absoluta/a/otro.apk"
```

```powershell
$env:APP_PATH = "C:\ruta\a\otro.apk"
```

### Ejecutar la suite

Desde `mobile-tests/`:

1. Levanta el servidor de Appium:
   ```bash
   appium
   ```
2. Verifica que el emulador/dispositivo esté disponible:
   ```bash
   adb devices
   ```
3. Corre las pruebas:
   ```bash
   cd mobile-tests
   ./gradlew clean test
   ```

### Reportes

Serenity genera el reporte HTML en `mobile-tests/target/site/serenity/index.html` al finalizar la ejecución. Para abrirlo (desde `mobile-tests/`):

```powershell
start target/site/serenity/index.html
```

```bash
# macOS
open target/site/serenity/index.html
# Linux
xdg-open target/site/serenity/index.html
```

### Casos cubiertos

- Login con credenciales válidas: navegación catálogo → carrito → checkout → confirmación
- Login con credenciales inválidas: validación del mensaje de error

---

## Suite de API (k6)

Pruebas de API en **k6** (JavaScript) contra [DummyJSON](https://dummyjson.com): autenticación, lectura y operaciones de creación/actualización/borrado (POST/PUT/DELETE), con validación de contrato (JSON Schema) y SLA de rendimiento. Incluye además una prueba de carga pequeña sobre los mismos endpoints críticos.

### Prerrequisitos

- **k6** (probado con v2.0.0)

Instalación:

**Windows** (winget):
```powershell
winget install k6 --source winget
```

**macOS** (Homebrew):
```bash
brew install k6
```

**Linux** (Debian/Ubuntu):
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

Verificar instalación:
```bash
k6 version
```

### Estructura

```
api-tests/
├── run.sh                 # orquestador: funcional / carga / ambos condicionados
├── src/
│   ├── main-flow.js       # validación funcional (login → auth user → user → crea/actualiza/borra)
│   ├── load-test.js       # prueba de carga sobre los mismos endpoints críticos
│   ├── config/env.js      # BASE_URL, credenciales, SLA_MS (sobreescribibles por entorno)
│   ├── schemas/           # contratos por endpoint (loginSchema, userSchema, addUserSchema)
│   └── helpers/
│       ├── checks.js      # aserciones reutilizables: status, SLA, schema, headers
│       └── report.js      # generación de reporte HTML/JSON (compartido por ambos scripts)
└── reports/                # salida de reportes (generada al correr la suite)
```

### Configuración

`BASE_URL`, las credenciales y el umbral de SLA están centralizados en `api-tests/src/config/env.js`, con defaults que apuntan a la API pública de DummyJSON. Para aislar variables sin tocar el código, sobreescribe por variables de entorno al invocar `k6 run`:

```bash
k6 run -e BASE_URL=https://dummyjson.com -e API_USERNAME=emilys -e API_PASSWORD=emilyspass -e SLA_MS=1500 src/main-flow.js
```

### Ejecutar la suite

`run.sh` es un script bash — en Windows requiere **Git Bash** (viene con Git for Windows; ábrelo desde el menú inicio o clic derecho → "Git Bash Here"). No funciona en PowerShell/CMD directamente.

> **Nota Windows/Git Bash:** dentro de Git Bash, las rutas usan `/` en vez de `\`, y `C:\...` se escribe `/c/...`. Por ejemplo:
> ```bash
> cd /c/ruta/al/repo/api-tests
> ./run.sh functional
> ```
> Si copias una ruta de Windows con `\` tal cual, bash la interpreta como caracteres de escape y la ruta queda rota (`C:rutaalrepo...`, sin barras). Usa siempre `/` en Git Bash, o pega la ruta entre comillas simples.

Desde `api-tests/`:

#### Solo validación funcional (consumir/validar los endpoints)

```bash
./run.sh functional
# equivalente directo: k6 run src/main-flow.js
```

Corre el flujo completo (2 VUs × 2 iteraciones) y valida status, headers, contrato y SLA por endpoint. Reporte: `reports/functional-summary.html`.

#### Solo prueba de carga

```bash
./run.sh load
# equivalente directo: k6 run src/load-test.js
```

Carga pequeña y deliberadamente conservadora (ramp-up 10s → 5 VUs, sostenido 20s → 10 VUs, ramp-down 10s) sobre los mismos endpoints críticos (login, lectura, escritura). Reporte: `reports/load-summary.html`.

#### Ambas, condicionadas (recomendado para demo/video)

```bash
./run.sh all
# o simplemente: ./run.sh
```

Corre primero la validación funcional. **Si pasa**, lanza la prueba de carga a continuación. **Si falla**, la carga no se ejecuta — no tiene sentido cargar endpoints que ya sabemos que están rotos — y el reporte funcional queda regenerado reflejando el error, listo para inspeccionar.

Puedes reenviar flags de k6 después del modo, por ejemplo para forzar un fallo controlado y verificar el gate:
```bash
./run.sh all -e API_PASSWORD=credencial-incorrecta
```

#### Escenario de demostración de SLA (falla a propósito)

Dentro de `main-flow.js`, suma un quinto grupo (`05 - DEMO`) al flujo funcional, con una petición `?delay=2000` que fuerza una respuesta lenta para demostrar que la aserción de SLA realmente detecta degradación (no solo "compila"). Va en el mismo reporte que el resto de checks, no en una corrida aparte:

```bash
./run.sh functional -e RUN_DELAY_DEMO=true
```

Este comando **debe terminar con código de salida distinto de cero** (threshold `checks: rate>0.99` incumplido) — es el comportamiento esperado. Por defecto (sin la variable), el grupo de demo no se ejecuta y la corrida queda 100% en verde.

### Reportes HTML

Se generan automáticamente en cada corrida (vía [k6-reporter](https://github.com/benc-uk/k6-reporter), importado por URL, sin necesidad de Node/npm): `reports/functional-summary.html` y `reports/load-summary.html`, cada uno con su propio `.json` equivalente.

Para abrirlos (desde `api-tests/`):

```powershell
start reports/functional-summary.html
start reports/load-summary.html
```

```bash
# Git Bash / macOS / Linux
open reports/functional-summary.html   # macOS
xdg-open reports/functional-summary.html # Linux
start reports/functional-summary.html    # Git Bash en Windows también acepta "start"
```

### Decisiones de diseño

**Por qué el flujo valida la respuesta y no la persistencia:** DummyJSON es un mock. `POST /users/add`, `PUT /users/{id}` y `DELETE /users/{id}` simulan la operación y devuelven un objeto coherente, pero no persisten el cambio — un `GET` posterior por id no reflejará nada de esto. Por eso cada mutación se valida contra su propia respuesta inmediata (status, contrato, eco de los campos enviados), nunca releyendo el recurso después.

**Por qué el threshold usa `p(95)<1500` y no un máximo:** un único outlier (cold start del servidor, blip de red) no debería tumbar toda la corrida. `p(95)` refleja la experiencia de la gran mayoría de las peticiones y sí detecta degradación real y sostenida, que es lo que un SLA debe medir.

**Por qué la validación de esquema es manual y no usa una librería (ajv):** k6 no corre sobre Node —usa el motor `goja`—, por lo que las librerías estándar de Node no funcionan sin bundlear. `ajv` en particular compila validadores generando código en runtime (`new Function(...)`), un patrón con problemas de compatibilidad conocidos sobre `goja`. Se optó por un validador propio y liviano (`matchesSchema` en `helpers/checks.js`) que verifica campos requeridos y tipos (`typeof`) — suficiente para el alcance de este reto y sin dependencias externas ni paso de build.

**Por qué la corrida funcional puede fallar ocasionalmente por SLA con solo 2 iteraciones:** la primera petición de cada proceso de k6 puede tardar varios segundos por costo de "cold start" (nueva conexión TLS, a veces inspeccionada por antivirus/firewall local) — no es una petición HTTP lenta en sí. Con solo 2 iteraciones (12 peticiones), un único outlier de este tipo puede dominar el cálculo de `p(95)` y romper el threshold. Es una limitación estadística esperada de una muestra tan pequeña, no un defecto de la suite: los checks de status, headers y contrato pasan siempre; si ves fallar únicamente `response time < 1500ms` en el primer grupo (Login), es este caso. `load-test.js` no sufre esto porque corre muchas más iteraciones.

**Por qué la carga es pequeña y conservadora:** DummyJSON es una API pública compartida (sandbox de terceros), no infraestructura propia — no correspondería someterla a una carga real. El objetivo de `load-test.js` es demostrar el patrón (ramp-up/sostenido/ramp-down, thresholds bajo carga), no un benchmark de capacidad.

---

## Bonus: Eventos (Kafka)

Prueba de integración con Apache Kafka: publica un evento de telemetría GPS y lo consume de vuelta en el mismo test, validando integridad, contrato de datos (JSON Schema) y rangos válidos.

### Prerrequisitos

- **Docker Desktop** (con WSL2 activo en Windows — ver sección "Prerrequisitos y Stack" más arriba)
- **Python 3.9+**

```powershell
docker --version
python --version
```

### 1. Levantar Kafka (+ interfaz web Kafka-UI)

```bash
cd event-tests
docker compose up -d
```

Esto levanta **dos** contenedores: el broker (`kafka-local`) y [Kafka-UI](https://github.com/provectus/kafka-ui) (`kafka-ui`), una interfaz web de solo observación para ver tópicos y mensajes en vivo sin escribir código.

Verificar que quedaron arriba:
```bash
docker ps                          # kafka-local Y kafka-ui deben aparecer como "Up"
docker logs kafka-local --tail 10  # debe terminar en "Kafka Server started"
```

Abrir la interfaz:
```bash
start http://localhost:8080   # PowerShell/Git Bash en Windows
```
En el clúster **"local"** → **Topics** → `gps-raw-events` → pestaña **Messages** verás cada mensaje con su offset, timestamp y JSON completo, en tiempo real mientras corres el productor o el test.

### 2. Instalar dependencias de Python

```bash
python -m pip install -r requirements.txt
```

### 3. Correr el test

```bash
python -m pytest test_gps_events.py -v
```

Debería terminar en `1 passed`. El test:
1. Publica `{"vehicleId":"VEH-099","lat":4.60,"lng":-74.08,"speed":65}` en el tópico `gps-raw-events` (el PDF del reto usa "VEH-99"; se ajustó a 3 dígitos para cumplir el patrón estricto de `vehicleId` — ver "Decisiones de diseño" abajo).
2. Lo consume de vuelta.
3. Valida que sea idéntico a lo enviado, que cumpla el contrato de datos, que los rangos (lat/lng/speed) sean físicamente válidos, y que haya llegado dentro del timeout (10s).

### 4. Apagar Kafka al terminar

```bash
docker compose down
```

### Estructura

```
event-tests/
├── docker-compose.yml        # Kafka local (KRaft, sin Zookeeper) + Kafka-UI
├── requirements.txt          # kafka-python-ng, pytest, jsonschema
├── schemas/
│   └── telemetry_schema.py   # contrato de datos del evento GPS
├── test_gps_events.py        # test integrado: producer + consumer + validaciones
├── producer_demo.py           # script suelto: publicar mensajes a mano o en lote aleatorio
├── consumer_demo.py           # script suelto: ver el consumo y su validación en vivo
└── quality_report.py          # métricas de QA: calidad de datos + consumer lag
```

### Métricas de QA (`quality_report.py`)

Un rol de QA no se limita a "pasó/falló" — también aporta métricas para tomar decisiones. Este script genera un lote de mensajes y produce un reporte consolidado con dos métricas pensadas para eso, no para ser un dashboard de infraestructura:

```bash
python quality_report.py --count 100
python quality_report.py --count 100 --seed 42   # reproducible
```

**1. Tasa de calidad de datos + desglose de motivos de rechazo.** Qué porcentaje de mensajes es válido, y de los inválidos, por qué (tipo incorrecto, fuera de rango, campo faltante, formato de `vehicleId`). Sirve para priorizar: si la mayoría de los rechazos son por una sola categoría, ahí está el problema real aguas arriba (firmware del GPS, versión de la app, un sensor específico) — no es "Kafka fallando", es una señal de dónde mirar.

**2. Consumer lag.** Cuántos mensajes hay en el tópico que el consumidor todavía no ha leído (`highwater_offset - position`, calculado con la posición real que lleva Kafka, no un conteo manual). Es la métrica operativa más importante de Kafka. En una plataforma de telemetría de flotas, lag alto significa perder visibilidad en tiempo real de dónde están los vehículos — un riesgo de negocio concreto, no solo técnico.

Se descartaron gráficas de línea (Grafana + Prometheus) por ser mucho más pesadas de lo que pide el reto ("Kafka local ligero") — estas dos métricas se calculan con la misma librería que ya usa el resto de la suite, sin infraestructura adicional.

### Conceptos clave explicados

**Broker** — el servidor de Kafka que recibe, guarda y entrega mensajes. Es el corazón de todo: sin él no hay a dónde publicar ni de dónde leer.

**Tópico** — una categoría/canal con nombre (aquí, `gps-raw-events`) donde se publican mensajes relacionados. Es como el nombre de una carpeta de correo: todos los eventos de telemetría GPS van a ese mismo tópico.

**Producer** — el cliente que publica (envía) mensajes a un tópico. En nuestro test, es quien manda el JSON del vehículo.

**Consumer** — el cliente que lee mensajes de un tópico. Se identifica con un `group_id`: varios consumers con el mismo `group_id` se reparten el trabajo de leer (forman un "grupo de consumo"); consumers con `group_id` distintos leen todos, cada uno por su lado, de forma independiente.

**Offset** — la posición numérica de un mensaje dentro de un tópico (como el número de página de un libro). Kafka recuerda, por cada `group_id`, hasta qué offset ya leyó ese grupo. Por eso este test genera un `group_id` nuevo (un UUID) en cada corrida: así, para Kafka, ese grupo nunca ha leído nada todavía, y con `auto_offset_reset="earliest"` arranca desde el principio del tópico — garantizando que sí vea el mensaje que el test acaba de publicar, sin importar qué corrió antes.

### Decisiones de diseño

**Por qué un test integrado (producer + consumer en el mismo flujo)** en vez de dos tests separados: así el ciclo completo (publicar → leer → validar) se ve de corrido y es más fácil de explicar y demostrar en video, sin depender de que un test anterior haya dejado datos en el tópico.

**Por qué `kafka-python-ng` y no `kafka-python`:** el paquete original (`kafka-python` en PyPI) tiene un bug de compatibilidad conocido con Python 3.12+ (falla al importar `kafka.vendor.six.moves`). `kafka-python-ng` es el fork mantenido activamente que lo corrige, exponiendo el mismo namespace `kafka` — el código de importación no cambia en nada.

**Por qué el consumer reintenta la conexión (hasta 5 veces, con 1s de pausa entre intentos):** se detectó empíricamente (corriendo el test repetidamente, en algunos casos más de 15 veces seguidas) que el selector de sockets de `kafka-python-ng` en Windows falla de forma intermitente con `ValueError: Invalid file descriptor: -1` durante el primer intento de conexión al coordinador del grupo — una condición de carrera de bajo nivel de la librería, no un problema de nuestro tópico/offset (los logs del broker confirman que el grupo sí se forma correctamente del lado del servidor). La frecuencia de la falla aumentó al correr el test muchas veces seguidas en poco tiempo, consistente con acumulación de conexiones TCP en estado `TIME_WAIT` en Windows — un efecto de correr el mismo proceso Python repetidamente en segundos, no algo esperable en un uso normal (una corrida aislada, o en CI). El reintento con una breve pausa lo resuelve siempre.

**Por qué el contrato de `vehicleId` es un patrón estricto (`^[A-Z]{3}-[0-9]{3}$`) y no solo "string":** validar solo el tipo (`"type": "string"`) deja pasar cualquier texto — un identificador de vehículo real de una flota sigue un formato predecible. Se definió exactamente 3 letras mayúsculas + guion + 3 dígitos (ej. `VEH-099`). Esto obligó a **ajustar el ejemplo literal del PDF** (`VEH-99`, 2 dígitos) a `VEH-099` en el test oficial — una desviación deliberada y documentada, no un descuido; el resto del payload (lat/lng/speed) no cambió.

**Por qué lat/lng no restringen el signo:** son coordenadas geográficas estándar — latitud positiva es hemisferio norte, negativa es hemisferio sur; longitud positiva es este de Greenwich, negativa es oeste. Ambos signos son físicamente válidos según la ubicación (Colombia, por ejemplo, cae en longitud siempre negativa y latitud casi siempre positiva). El contrato ya restringía correctamente el *rango* (-90/90 y -180/180) sin necesidad de restringir el signo, que sería incorrecto hacerlo.

**Por qué el broker tiene dos listeners (`PLAINTEXT` e `INTERNAL`) en vez de uno solo:** al agregar Kafka-UI (que corre dentro de la red de Docker, a diferencia de los scripts Python que corren en Windows), el listener existente (`PLAINTEXT://localhost:9092`) no servía para ambos casos — el broker le anuncia a cada cliente una dirección de reconexión, y "localhost" significa cosas distintas según si preguntas desde dentro o fuera de Docker. Un contenedor que se conecta y recibe como respuesta "reconéctate a localhost:9092" terminaría intentando conectarse a sí mismo. La solución fue agregar un segundo listener (`INTERNAL://kafka:29092`) dedicado a clientes intra-Docker, sin tocar el listener original — los scripts Python en Windows siguen usando exactamente `localhost:9092`, cero cambios para ellos.

**Por qué los datos del tópico se perdieron al agregar Kafka-UI:** el `docker-compose.yml` no define un volumen persistente para Kafka (es un entorno de desarrollo/demo, nunca se prometió persistencia). Cambiar la configuración del broker (agregar el listener nuevo) obligó a recrear el contenedor, y sin volumen, los datos viven solo en la capa de escritura del contenedor — se perdieron los mensajes acumulados de pruebas anteriores. No afecta la validez del test ni de la demo, solo reinicia el offset desde 0.

**Por qué se agregó JMX (`JMX_PORT`, `KAFKA_CLUSTERS_0_METRICS_PORT`):** Kafka-UI puede mostrar tópicos y mensajes sin esto (es protocolo normal de Kafka), pero su pantalla de **Dashboard** (columnas "Production"/"Consumption" en bytes, bytesInPerSec/bytesOutPerSec por tópico y por broker) depende específicamente de métricas JMX — sin exponerlas, esos valores quedan vacíos/nulos aunque todo lo demás funcione. Se expuso el puerto JMX del broker (`JMX_PORT: 9101`) y se le indicó a Kafka-UI dónde encontrarlo (`KAFKA_CLUSTERS_0_METRICS_PORT: 9101`). Se verificó con `curl http://localhost:8080/api/clusters`: `bytesInPerSec` pasó de `null` a un valor real que se mueve al mandar mensajes. Nota: esta versión de Kafka-UI (v0.7.2) muestra estas métricas como **números que se actualizan**, no como gráficas de línea — para eso haría falta Grafana + Prometheus, que se descartó por ser mucho más pesado de lo que pide el reto ("Kafka local ligero").
