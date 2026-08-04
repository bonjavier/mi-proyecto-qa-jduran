# API Tests — DummyJSON (k6)

Suite de pruebas de API en **k6** (JavaScript) contra [DummyJSON](https://dummyjson.com), cubriendo autenticación, lectura y mutaciones (POST/PUT/DELETE) con validación de contrato y SLA de rendimiento.

## Prerrequisitos

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

## Estructura

```
api-tests/
├── run.sh                 # orquestador: funcional / carga / ambos condicionados
├── src/
│   ├── main-flow.js       # validación funcional (login → auth user → user → mutaciones)
│   ├── load-test.js       # prueba de carga sobre los mismos endpoints críticos
│   ├── config/env.js      # BASE_URL, credenciales, SLA_MS (sobreescribibles por entorno)
│   ├── schemas/           # contratos por endpoint (loginSchema, userSchema, addUserSchema)
│   └── helpers/
│       ├── checks.js      # aserciones reutilizables: status, SLA, schema, headers
│       └── report.js      # generación de reporte HTML/JSON (compartido por ambos scripts)
└── reports/                # salida de reportes (generada al correr la suite)
```

## Configuración

`BASE_URL`, las credenciales y el umbral de SLA están centralizados en `src/config/env.js`, con defaults que apuntan a la API pública de DummyJSON. Para aislar variables sin tocar el código, sobreescribe por variables de entorno al invocar `k6 run`:

```bash
k6 run -e BASE_URL=https://dummyjson.com -e API_USERNAME=emilys -e API_PASSWORD=emilyspass -e SLA_MS=1500 src/main-flow.js
```

## Ejecutar la suite

Desde `api-tests/` (en Windows, vía Git Bash: `bash run.sh ...`):

### Solo validación funcional (consumir/validar los endpoints)

```bash
./run.sh functional
# equivalente directo: k6 run src/main-flow.js
```

Corre el flujo completo (2 VUs × 2 iteraciones) y valida status, headers, contrato y SLA por endpoint. Reporte: `reports/functional-summary.html`.

### Solo prueba de carga

```bash
./run.sh load
# equivalente directo: k6 run src/load-test.js
```

Carga pequeña y deliberadamente conservadora (ramp-up 10s → 5 VUs, sostenido 20s → 10 VUs, ramp-down 10s) sobre los mismos endpoints críticos (login, lectura, escritura). Reporte: `reports/load-summary.html`.

### Ambas, condicionadas (recomendado para demo/video)

```bash
./run.sh all
# o simplemente: ./run.sh
```

Corre primero la validación funcional. **Si pasa**, lanza la prueba de carga a continuación. **Si falla**, la carga no se ejecuta — no tiene sentido cargar endpoints que ya sabemos que están rotos — y el reporte funcional queda regenerado reflejando el error, listo para inspeccionar.

Puedes reenviar flags de k6 después del modo, por ejemplo para forzar un fallo controlado y verificar el gate:
```bash
./run.sh all -e API_PASSWORD=credencial-incorrecta
```

### Escenario de demostración de SLA (falla a propósito)

Dentro de `main-flow.js`, suma un quinto grupo (`05 - DEMO`) al flujo funcional, con una petición `?delay=2000` que fuerza una respuesta lenta para demostrar que la aserción de SLA realmente detecta degradación (no solo "compila"). Va en el mismo reporte que el resto de checks, no en una corrida aparte:

```bash
./run.sh functional -e RUN_DELAY_DEMO=true
```

Este comando **debe terminar con código de salida distinto de cero** (threshold `checks: rate>0.99` incumplido) — es el comportamiento esperado. Por defecto (sin la variable), el grupo de demo no se ejecuta y la corrida queda 100% en verde.

### Reportes HTML

Se generan automáticamente en cada corrida (vía [k6-reporter](https://github.com/benc-uk/k6-reporter), importado por URL, sin necesidad de Node/npm): `reports/functional-summary.html` y `reports/load-summary.html`, cada uno con su propio `.json` equivalente.

## Decisiones de diseño

**Por qué el flujo valida la respuesta y no la persistencia:** DummyJSON es un mock. `POST /users/add`, `PUT /users/{id}` y `DELETE /users/{id}` simulan la operación y devuelven un objeto coherente, pero no persisten el cambio — un `GET` posterior por id no reflejará nada de esto. Por eso cada mutación se valida contra su propia respuesta inmediata (status, contrato, eco de los campos enviados), nunca releyendo el recurso después.

**Por qué el threshold usa `p(95)<1500` y no un máximo:** un único outlier (cold start del servidor, blip de red) no debería tumbar toda la corrida. `p(95)` refleja la experiencia de la gran mayoría de las peticiones y sí detecta degradación real y sostenida, que es lo que un SLA debe medir.

**Por qué la validación de esquema es manual y no usa una librería (ajv):** k6 no corre sobre Node —usa el motor `goja`—, por lo que las librerías estándar de Node no funcionan sin bundlear. `ajv` en particular compila validadores generando código en runtime (`new Function(...)`), un patrón con problemas de compatibilidad conocidos sobre `goja`. Se optó por un validador propio y liviano (`matchesSchema` en `helpers/checks.js`) que verifica campos requeridos y tipos (`typeof`) — suficiente para el alcance de este reto y sin dependencias externas ni paso de build.
