# Mi Proyecto QA — Simon Movilidad

Suite de automatización móvil (Android) para la app SauceLabs Swag Labs, usando **Screenplay + Serenity BDD + Cucumber + Appium**.

## Stack

- Java 17
- Gradle (wrapper incluido)
- Serenity BDD (patrón Screenplay) + Cucumber
- Appium 3 + driver UiAutomator2

## Requisitos previos

- JDK 17 (`JAVA_HOME` configurado)
- Node.js 18+ (para Appium)
- Android SDK con `platform-tools` y una imagen de sistema instalada (`ANDROID_HOME` configurado)
- Appium 3 con el driver UiAutomator2:
  ```bash
  npm install -g appium
  appium driver install uiautomator2
  ```
- Un emulador Android corriendo, o un dispositivo físico con depuración USB activada
- El APK de la app (`2-sauceLabs.apk`, incluido en este repo)

## Configuración

Las capabilities de Appium están en `src/test/resources/serenity.conf`. La ruta del APK y el `noReset` se leen de variables de entorno:

```bash
export APP_PATH="/ruta/absoluta/a/2-sauceLabs.apk"
export NO_RESET=false
```

En Windows (PowerShell):
```powershell
$env:APP_PATH = "C:\ruta\a\2-sauceLabs.apk"
$env:NO_RESET = "false"
```

## Ejecutar la suite

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
   ./gradlew clean test
   ```

## Reportes

Serenity genera el reporte HTML en `target/site/serenity/index.html` al finalizar la ejecución.

## Casos cubiertos

- Login con credenciales válidas: navegación catálogo → carrito → checkout → confirmación
- Login con credenciales inválidas: validación del mensaje de error
