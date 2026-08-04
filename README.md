# Guía completa y detallada para ejecutar este proyecto desde cero (principiantes absolutos)

Esta guía está pensada para una persona que parte de cero: no conoce Java, ni Gradle, ni Appium, ni automatización móvil. Aquí encontrarás explicaciones técnicas muy detalladas y pasos prácticos para clonar el repositorio, preparar el equipo (macOS o Windows), instalar herramientas, configurar emuladores/dispositivos, ejecutar Appium, lanzar la suite de pruebas y revisar los reportes.

Índice
1. Resumen del proyecto
2. Conceptos clave (explicaciones muy detalladas)
  2.1. ¿Qué es Java? JDK vs JRE vs JVM
  2.2. Gestión de dependencias: Gradle vs Maven, scopes, artefactos, transitive deps y repositorios
  2.3. ¿Qué es Serenity BDD? (detalles técnicos, módulos y plugin)
  2.4. ¿Qué es Screenplay? (patrón, roles y ejemplos)
  2.5. ¿Qué es Appium? (arquitectura: server, client, inspector, capacidades completas)
  2.6. Android Studio, SDK, AVD y emuladores (componentes, adb, herramientas CLI)
  2.7. Cucumber y Gherkin (estructura de features, escenarios, steps y mapping a Java)
  2.8. Runners y frameworks de test (qué hace `@RunWith`, JUnit4 vs JUnit5, cómo ejecutar suites y pruebas específicas)
  2.9. Elementos UI, locators y estrategias (mejores prácticas)
  2.10. Otros componentes: logging (SLF4J/logback), JUnit, plugins de Gradle y vulnerabilidades transitivas
3. Requisitos mínimos y recomendaciones
4. Preparación del entorno (instalación) — macOS y Windows (paso a paso)
5. Clonar el repositorio y primeras comprobaciones
6. Configurar la app y capabilities (archivo `serenity.conf` / `serenity.properties`) — ejemplos completos
7. Ejecutar Appium y lanzar la suite de pruebas (comandos precisos y variantes)
8. Ejecutar pruebas específicas (feature, escenario o tags)
9. Ver reportes y estructura del reporte Serenity
10. Diagnóstico y errores comunes (y cómo resolverlos), incluyendo vulnerabilidades transitivas
11. Cómo habilitar `serenity-appium` si es estrictamente necesario
12. Buenas prácticas, CI y cómo fijar versiones seguras
13. Resumen rápido de comandos útiles (lista corta)
14. FAQ (Preguntas frecuentes)

---

1) Resumen del proyecto

Este repositorio contiene una suite de pruebas automatizadas mobile (Android) implementada en Java. Las pruebas están escritas en estilo BDD con Cucumber (features en `src/test/resources/features`) y ejecutadas por Serenity BDD + Gradle. Para manipular la app en un emulador o dispositivo se usa Appium (cliente Java: `io.appium:java-client`).

El Runner principal que arranca la suite es `SwaglabsRunner` en `src/test/java/.../runners`. Al ejecutar `./gradlew test` se compilan y ejecutan las pruebas, y `./gradlew aggregate` genera el reporte HTML en `target/site/serenity/index.html`.

---

2) Conceptos clave (muy detallado)

2.1 ¿Qué es Java? JDK vs JRE vs JVM
- Java es un lenguaje de programación y una plataforma (ecosistema) que permite compilar código fuente (.java) a bytecode (.class) que se ejecuta en la JVM.
- JVM (Java Virtual Machine): ejecuta bytecode y abstrae detalles del sistema operativo. Diferentes JDK/JRE incluyen distintas versiones de la JVM.
- JRE (Java Runtime Environment): contiene la JVM y bibliotecas necesarias para ejecutar aplicaciones Java.
- JDK (Java Development Kit): incluye el JRE más herramientas (javac, jar, javadoc) necesarias para compilar y desarrollar.
- En este proyecto usamos JDK 17 porque Gradle/Groovy y algunos plugins se comportan mejor con Java 17; JDK 21 puede producir errores (por ejemplo "Unsupported class file major version 65") con versiones antiguas de herramientas.

2.2 Gestión de dependencias: Gradle vs Maven, scopes, artefactos, transitive deps y repositorios
- Maven y Gradle son herramientas para construir proyectos. Ambas manejan dependencias, pero con sintaxis y mecanismos distintos.
  - Maven usa `pom.xml` (XML). Gradle usa `build.gradle` (Groovy) o `build.gradle.kts` (Kotlin DSL).
- Dependencia: una librería que tu proyecto necesita. Ejemplo: `io.appium:java-client:8.3.0`.
- Notación: `groupId:artifactId:version`.
- Scopes (Gradle/Java típicos):
  - `implementation` o `compile` (dependencia necesaria para compilar código de producción),
  - `testImplementation` (dependencia necesaria solo para tests),
  - `runtimeOnly` (necesaria en tiempo de ejecución pero no para compilar).
- Artefacto: el `.jar` o `.pom` que contiene la dependencia.
- Repositorio: donde se almacenan artefactos (Maven Central, JitPack, Sonatype, repositorios corporativos). Gradle consulta los repos declarados en `repositories` para descargar artefactos.
- Transitive dependencies (dependencias transitivas): cuando una dependencia A depende de B y C, al añadir A, Gradle/Maven traerá B y C automáticamente. Esto es útil pero puede traer versiones vulnerables.
- Cómo inspeccionar dependencias en Gradle:
  - Ver árbol de dependencias para una configuración (por ejemplo `testImplementation`):
    ```bash
    ./gradlew dependencies --configuration testImplementation
    ```
  - Ver por qué una dependencia específica está incluida (dependencyInsight):
    ```bash
    ./gradlew dependencyInsight --dependency serenity-appium --configuration testImplementation
    ```
- Resolución de conflictos y forzar versiones:
  - Puedes declarar una versión específica en `dependencies` o usar `dependency constraints` o `force` en la configuración de resoluciones para garantizar una versión segura.
- MetadataSources: a veces un repositorio contiene artefactos sin POM; en `repositories` se puede ajustar `metadataSources { mavenPom() artifact() }` para cambiar cómo se resuelve.
- Plugins: extensiones que añaden tareas. Ejemplo: `net.serenity-bdd.serenity-gradle-plugin` agrega tareas para reportes.

2.3 ¿Qué es Serenity BDD? (detalles técnicos, módulos y plugin)
- Serenity es un conjunto de bibliotecas y plugins para pruebas que:
  - Facilita la escritura de tests legibles y mantenibles.
  - Genera reportes ricos (pasos, capturas, screenshots, estados).
  - Integra con Cucumber y Screenplay.
- Módulos comunes:
  - `serenity-core`: núcleo con utilidades, reporting y integración.
  - `serenity-screenplay`: patrones Screenplay.
  - `serenity-junit5` / `serenity-junit`: adaptadores para frameworks de test.
  - `serenity-cucumber`: integración con Cucumber.
- Serenity Gradle Plugin: añade tareas (`aggregate`) para reunir resultados y generar reportes HTML, y puede configurar rutas de resultados.
- ¿Por qué no usar directamente Selenium/Appium? Serenity añade capas de soporte (reportes, attentes implícitas, utilidades para tareas y preguntas) que aceleran el trabajo de automatización.

2.4 ¿Qué es Screenplay? (patrón, roles y ejemplos)
- Screenplay modela pruebas como interacciones entre Actores y el Sistema under Test.
  - Actor: quien realiza acciones (por ejemplo `Usuario` o `Cliente`).
  - Ability: habilidad que el actor tiene (por ejemplo `BrowseTheWeb.with(driver)` para usar WebDriver).
  - Task: una acción compuesta (por ejemplo `Login.withCredentials(user, pass)`).
  - Question: verificación que devuelve información sobre el estado (por ejemplo `Question.answeredBy(actor)`).
- Ejemplo (pseudocódigo):
  ```java
  Actor juan = Actor.named("Juan");
  juan.can(BrowseTheWeb.with(driver));
  juan.attemptsTo(Login.withCredentials("user","pass"));
  juan.should(seeThat(TheHomePage.isVisible()));
  ```
- Ventaja: el código queda organizado, reutilizable y fácil de leer.

2.5 ¿Qué es Appium? (arquitectura: server, client, inspector, capacidades completas)
- Appium es un servidor que permite a clientes (tests) controlar aplicaciones móviles usando el protocolo WebDriver (W3C).
- Componentes:
  - Appium Server: ejecutable (nodejs) que escucha peticiones HTTP con comandos WebDriver.
  - Appium Client (Java en este proyecto): librería que construye y envía comandos al servidor.
  - Appium Inspector: herramienta GUI para inspeccionar la UI de la app y generar locators.
- Tipos de sesiones y capabilities (propiedades importantes):
  - `platformName`: "Android" o "iOS".
  - `platformVersion`: versión del SO (ej. "12").
  - `deviceName`: nombre del emulador o dispositivo (ej. "emulator-5554" o "d78ce831").
  - `udid`: identificador único de dispositivo (en pruebas con dispositivos reales).
  - `app`: ruta absoluta a la app (.apk o .ipa) o un identificador remoto.
  - `appPackage` / `appActivity` (Android): paquete y activity de la app cuando no se usa `app`.
  - `automationName`: motor de automatización (Android: `UiAutomator2` o `Espresso`; iOS: `XCUITest`).
  - `noReset` / `fullReset`: comportamientos de sesión.
  - `newCommandTimeout`: tiempo de espera para nuevas órdenes.
- Ejemplo de capabilities JSON:
  ```json
  {
    "platformName": "Android",
    "platformVersion": "12",
    "deviceName": "emulator-5554",
    "automationName": "UiAutomator2",
    "app": "/absolute/path/to/app-debug.apk"
  }
  ```

2.6 Android Studio, SDK, AVD y emuladores (componentes, adb, herramientas CLI)
- Android Studio: IDE para desarrollar apps Android. Incluye SDK Manager y AVD Manager.
- Android SDK: incluye herramientas como `adb` (Android Debug Bridge), `emulator`, `sdkmanager`, `platform-tools`.
- AVD Manager: interfaz para crear emuladores.
- `adb devices`: comando para listar dispositivos/emuladores conectados.
- Iniciar un emulador por CLI:
  ```bash
  $ANDROID_SDK_ROOT/emulator/emulator -avd <NOMBRE_AVD>
  ```

2.7 Cucumber y Gherkin (estructura de features, escenarios, steps y mapping a Java)
- Gherkin: lenguaje legible para describir comportamiento. Un `.feature` contiene `Feature`, `Scenario`, `Given/When/Then`.
- Cucumber lee los archivos feature y busca métodos Java anotados (step definitions) que coincidan con las expresiones.
- Ejemplo de archivo feature y su mapping:
  - `src/test/resources/features/login.feature`:
    ```gherkin
    Feature: Login
      Scenario: login con credenciales válidas
        Given el usuario abre la app
        When ingresa usuario y contraseña
        Then ve la pantalla principal
    ```
  - Step definition (Java):
    ```java
    @Given("el usuario abre la app")
    public void abreApp() { /* código */ }

    @When("ingresa usuario y contraseña")
    public void ingresaCredenciales() { /* código */ }

    @Then("ve la pantalla principal")
    public void verificaPantalla() { /* código */ }
    ```

2.8 Runners y frameworks de test (qué hace `@RunWith`, JUnit4 vs JUnit5, cómo ejecutar suites y pruebas específicas)
- Runner: clase Java que configura qué features ejecutar y cómo. En este proyecto hay un Runner que usa `@RunWith(CucumberWithSerenity.class)`.
- `@RunWith` es anotación de JUnit4 que indica qué runner usar para ejecutar la clase.
- Por compatibilidad, muchos proyectos usan JUnit4 con Cucumber/Serenity a través de esta anotación; por eso se añade `junit:junit:4.13.2`.
- Ejecutar toda la suite: `./gradlew test` (o `./gradlew clean test`).
- Ejecutar una feature específica: puedes ejecutar la clase Runner que apunta a `features` o modificar el runner para limitar el path de features. Otra opción es filtrar por tags si tu configuración de Cucumber lo soporta (ej. `@smoke`).

2.9 Elementos UI, locators y estrategias (mejores prácticas)
- Localizadores comunes:
  - `id` / `resource-id` (Android)
  - `accessibility id` (recomendado para pruebas móviles, estable)
  - `xpath` (potente pero frágil y lento si se abusa)
  - `class name`
- Recomendación: prioriza `id` o `accessibility id` para robustez; usa `xpath` solo cuando no hay otra opción.
- Evita locators dependientes de texto o posiciones cuando sea posible.

2.10 Otros componentes: logging (SLF4J/logback), JUnit, plugins de Gradle y vulnerabilidades transitivas
- Logging: SLF4J es una API; `logback-classic` o `log4j` son implementaciones concretas. Mensajes como "No SLF4J providers found" indican que no hay una implementación en el classpath.
- Vulnerabilidades transitivas: cuando una dependencia trae bibliotecas con CVEs conocidos. Para mitigarlas:
  - Actualiza la dependencia principal a una versión que use dependencias seguras.
  - Usa constraints o `dependencyManagement` (Maven) o `resolutionStrategy` (Gradle) para forzar versiones seguras.
  - Revisa y corrige en CI con herramientas como `dependency-check`, `OWASP Dependency-Check`, o servicios SCA.

---

3) Requisitos mínimos y recomendaciones
- Recomendado: 8 GB RAM (más si vas a ejecutar emuladores), CPU moderna.
- Espacio en disco: 10+ GB para SDKs y emuladores.
- Conexión a Internet para descargar dependencias y SDK.

---

4) Preparación del entorno (instalación) — macOS y Windows

(Se describen pasos y comandos; ejecuta los que correspondan a tu OS.)

4.1 Instalar Java (JDK 17)
macOS (Homebrew):
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install --cask temurin17
```
Windows (winget o Chocolatey):
```powershell
winget install -e --id EclipseAdoptium.Temurin.17.JDK
# o
choco install temurin17 -y
```
Verificar:
```bash
java -version
```

4.2 Instalar Node.js y Appium 2 + Appium Inspector
macOS:
```bash
brew install node
npm install -g appium@next
npm install -g appium-inspector
```
Windows (PowerShell):
```powershell
winget install OpenJS.Node
npm install -g appium@next
npm install -g appium-inspector
```
Comprobar:
```bash
appium --version
```

4.3 Instalar Android Studio y configurar AVD (recomendado para principiantes)
- Descarga Android Studio desde developer.android.com/studio e instálalo.
- Abre SDK Manager e instala `Android SDK Platform-Tools` y una o más plataformas (por ejemplo Android 12 / API 33).
- Abre AVD Manager y crea un AVD (por ejemplo Pixel 5 con API 33). Asigna suficientes recursos (RAM).
- Inicia el emulador desde AVD Manager.

4.4 Configurar variables de entorno
macOS (añadir a `~/.zshrc`):
```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$PATH"
```
Windows (PowerShell temporal):
```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17'
$env:ANDROID_SDK_ROOT = 'C:\Users\%USERNAME%\AppData\Local\Android\Sdk'
$env:Path = "$env:ANDROID_SDK_ROOT\platform-tools;" + $env:Path
```

---

5) Clonar el repositorio y primeras comprobaciones

```bash
git clone <URL_DEL_REPOSITORIO>
cd AppiumJduran
```
Verificaciones básicas y salidas esperadas:
```bash
java -version        # debe mostrar Java 17
./gradlew --version  # muestra la versión de Gradle usada por el wrapper
appium --version     # versión de Appium instalada
adb devices          # lista dispositivos/emuladores conectados (si está el emulador debería aparecer)
```

Si alguno falla, vuelve a la sección de instalación y revisa variables de entorno.

---

6) Configurar la app y capabilities (archivo `serenity.conf` / `serenity.properties`) — ejemplos completos

Localiza `src/test/resources/serenity.conf` o `serenity.properties` y edita según tu entorno.

Parámetros habituales y explicaciones:
- `appium.deviceName`: nombre del dispositivo o emulador. Usa el valor de `adb devices` para dispositivos reales o `emulator-5554` para emuladores.
- `appium.platformName`: "Android" o "iOS".
- `appium.platformVersion`: versión del SO (ej. "12").
- `appium.automationName`: `UiAutomator2` (Android moderno) o `XCUITest` (iOS).
- `appium.app`: ruta absoluta a la app (APK o IPA). Si la app está instalada en el dispositivo puedes usar `appPackage` y `appActivity` en Android.
- `appium.udid`: si necesitas apuntar a un dispositivo concreto.
- `noReset`/`fullReset`: controlan si la app se reinstala en cada sesión.

Ejemplo `serenity.conf` (formato HOCON o properties según tu archivo):
```properties
appium.platformName=Android
appium.platformVersion=12
appium.deviceName=emulator-5554
appium.automationName=UiAutomator2
appium.app=/Users/tuUsuario/Downloads/app-debug.apk
serenity.outputDirectory=target/site/serenity
```

Notas:
- Usa rutas absolutas en `appium.app` para evitar problemas.
- Si la app requiere permisos o login previo, asegúrate de configurar los pasos precondición o usar `noReset=true` si quieres preservar estado.

---

7) Ejecutar Appium y lanzar la suite de pruebas (comandos precisos y variantes)

1) Inicia Appium server en una terminal:
```bash
appium
```
(En macOS/Windows verás logs del servidor. Si Appium arranca, mostrará en qué puerto escucha — por defecto `0.0.0.0:4723`).

2) Asegúrate que el emulador o dispositivo está disponible (`adb devices`).

3) Ejecuta la suite completa con Gradle wrapper:
```bash
# macOS/Linux (asegúrate de tener JAVA_HOME apuntando a JDK17)
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
./gradlew clean test aggregate --refresh-dependencies --stacktrace
```
Windows PowerShell (temporalmente):
```powershell
$env:JAVA_HOME = 'C:\ Program Files\Eclipse Adoptium\jdk-17'
./gradlew clean test aggregate --refresh-dependencies --stacktrace
```

Explicación de switches:
- `clean`: borra outputs previos.
- `test`: ejecuta pruebas unitarias/integración.
- `aggregate`: tarea de Serenity que recoge resultados y genera reportes.
- `--refresh-dependencies`: fuerza a Gradle a re-descargar dependencias.
- `--stacktrace`: imprime la traza completa de errores (útil para debugging).

---

8) Ejecutar pruebas específicas (feature, escenario o tags)

Si quieres ejecutar una sola feature o escenarios con tags, tienes varias opciones:

A) Modificar temporalmente el Runner (ej. `SwaglabsRunner`) para apuntar a una carpeta o archivo de features.

B) Usar variables de entorno o propiedades del sistema que tu configuración de Cucumber/Serenity pueda leer. Ejemplo común (si tu proyecto soporta `cucumber.filter.tags`):
```bash
# Ejecutar escenarios marcados con @smoke
./gradlew test -Dcucumber.filter.tags="@smoke" --no-daemon
```
Nota: la sintaxis y el soporte dependen de la versión de Cucumber y la integración del proyecto. Si el runner no respeta `cucumber.filter.tags`, es necesario añadir soporte en `build.gradle` o editar el runner.

C) Ejecutar una clase Runner específica desde tu IDE o usar Gradle para ejecutar una tarea que invoque solo esa clase.

---

9) Ver reportes y estructura del reporte Serenity

- Ruta principal del reporte: `target/site/serenity/index.html`.
- El reporte incluye:
  - Lista de features ejecutadas.
  - Escenarios y pasos detallados (incluyendo pantallazos y logs si están habilitados).
  - Estado (passed / failed), tiempos y evidencia.
- Para abrir (macOS):
```bash
open target/site/serenity/index.html
```
Windows PowerShell:
```powershell
Start-Process target/site/serenity/index.html
```

---

10) Diagnóstico y errores comunes (y cómo resolverlos), incluyendo vulnerabilidades transitivas

10.1 "Unsupported class file major version 65"
- Síntoma: fallo en compilación con "Unsupported class file major version 65".
- Causa: estás usando JDK 21 (major 65) para compilar scripts/archivos de Gradle/Groovy que no soportan esa versión.
- Solución: apunta `JAVA_HOME` a JDK 17 y reintenta.

10.2 "Could not find net.serenity-bdd:serenity-appium:3.9.8"
- Síntoma: Gradle no resuelve esa dependencia.
- Causa: esa versión concreta de `serenity-appium` no está publicada en los repositorios públicos consultados.
- Solución: usar `serenity-screenplay-webdriver` + `io.appium:java-client` (implementación actual), o añadir el repositorio que contenga `serenity-appium` (p.ej. repo privado), o cambiar a una versión soportada.

10.3 "package org.junit.runner does not exist" / `@RunWith` not found
- Síntoma: compilador no encuentra `org.junit.runner`.
- Causa: falta `junit` (JUnit4) en las dependencias.
- Solución: añadir `testImplementation 'junit:junit:4.13.2'` (ya incluido en este proyecto).

10.4 Appium no detecta el dispositivo/emulador
- Ejecuta `adb devices` y verifica que hay un dispositivo "device" o "emulator-5554" en la lista.
- Si no aparece, asegúrate de que el emulador está iniciado o que el dispositivo está conectado con USB debugging activo.

10.5 SLF4J warnings y logging inestable
- Mensaje: "SLF4J: No SLF4J providers were found." o "Ignoring binding found at ...".
- Significado: hay conflictos de implementaciones de logging en el classpath. Normalmente `logback-classic` es suficiente.
- Solución: eliminar bindings duplicados o forzar la versión deseada.

10.6 Vulnerabilidades transitivas detectadas (SCA warnings)
- Observación: `serenity-core` y otras bibliotecas pueden traer dependencias transitivas con CVEs (por ejemplo `commons-beanutils`, `netty`, `jackson` etc.).
- Pasos para mitigar:
  1. Ejecuta un escaneo SCA en CI (OWASP Dependency-Check, Snyk, Dependabot, Mend, etc.).
  2. Identifica la dependencia transitiva vulnerable y la dependencia directa que la trae.
  3. Actualiza la dependencia directa a una versión que use dependencias seguras (ej. actualiza `serenity-core` a una versión que traiga versiones seguras).  
  4. Si no es posible actualizar, forzar la versión segura en Gradle mediante `dependency constraints` o `resolutionStrategy`.

Ejemplo: forzar una versión segura en `build.gradle` (simplificado):
```groovy
configurations.all {
  resolutionStrategy {
    force 'com.fasterxml.jackson.core:jackson-databind:2.14.2'
  }
}
```
- Nota: forzar versiones puede romper compatibilidad; prueba localmente y en CI.

---

11) Cómo habilitar `serenity-appium` (si es estrictamente necesario)

Si necesitas `net.serenity-bdd:serenity-appium` y esa versión no está en Maven Central, añade el repositorio que la contiene en `build.gradle` (por ejemplo `maven { url 'https://s01.oss.sonatype.org/content/repositories/releases/' }` o tu Nexus interno) y descomenta/añade la dependencia en `dependencies`.

Ejemplo:
```groovy
repositories {
  mavenCentral()
  maven { url 'https://s01.oss.sonatype.org/content/repositories/releases/' }
  maven { url 'https://mi-nexus/repository/maven-releases/' }
}

dependencies {
  testImplementation "net.serenity-bdd:serenity-appium:${serenityCoreVersion}"
}
```
Recuerda: si el repo requiere autenticación, utiliza `gradle.properties` o variables de entorno para no versionar credenciales.

---

12) Buenas prácticas y CI

- En CI fija `JAVA_HOME` a JDK 17 o usa una imagen con JDK 17 instalada.
- Usa `./gradlew` (wrapper) para reproducibilidad.
- Ejecuta escaneos SCA periódicos y actualiza dependencias en cuanto sea posible.
- Publica `target/site/serenity` como artifacts del job para revisión.

---

13) Resumen rápido de comandos útiles

```bash
# Instalar Java 17 (macOS)
brew install --cask temurin17

# Ver Java
a java -version

# Iniciar Appium
appium

# Listar dispositivos
a db devices

# Ejecutar tests
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
./gradlew clean test aggregate --refresh-dependencies --stacktrace

# Inspección de dependencias
a ./gradlew dependencies --configuration testImplementation
./gradlew dependencyInsight --dependency serenity-appium --configuration testImplementation
```
(En Windows sustituir `export` por `set` o usar PowerShell `$env:JAVA_HOME = '...'`)

---

14) FAQ (Preguntas frecuentes)
- ¿Necesito Android Studio?  
  No necesariamente si tienes un dispositivo real; para crear y gestionar emuladores Android es la opción más sencilla.
- ¿Por qué aparece el error sobre `serenity-appium`?  
  Porque esa dependencia no está siempre publicada para todas las versiones; por eso la configuración actual usa `serenity-screenplay-webdriver` y `io.appium:java-client`.
- ¿Cómo ejecuto una sola prueba?  
  En general, filtra por tags (`@smoke`) o ejecuta un Runner específico desde tu IDE.

Termina la guía aquí.
---