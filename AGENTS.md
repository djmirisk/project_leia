# AGENTS.md — SISFV (Solares Energy Group S.A.S. E.S.P.)

Documento único de reglas para trabajar en este repositorio. Reemplaza las versiones
anteriores (raíz y `App_Android_SISFV/AGENTS.md`), que quedaron fusionadas aquí.

---

## 1. Idioma y forma de trabajo

- Comunícate con el usuario en **español**, con instrucciones concretas, breves e incrementales.
- El usuario prefiere cambios pequeños, con rutas exactas, el bloque preciso que se va a modificar
  y una compilación o prueba antes del siguiente cambio.
- Inspecciona primero el código real del repositorio. No asumas que el código descrito en una
  conversación sigue siendo idéntico al del checkout actual.
- No reemplaces archivos completos si basta con modificar un bloque pequeño. Explica qué archivo y
  qué ubicación vas a cambiar.
- No declares que algo compila o funciona hasta que el comando de compilación o prueba haya
  terminado correctamente.
- Si aparece un error de compilación, resuelve primero el primer error relevante; no propongas
  cambios al azar.
- Después de cada tarea informa: archivos modificados, validación ejecutada y riesgos pendientes.

## 2. Proyecto

- Aplicación Android **SISFV** de Solares Energy Group S.A.S. E.S.P., para mantenimiento de
  sistemas solares individuales en ZNI.
- Ruta local en Windows: `C:\PROYECTO_LEIA_APK\App_Android_SISFV`.
- Paquete: `com.solaresenergy.sisfv`. Kotlin + Jetpack Compose + Room, arquitectura offline-first.
- Configuración actual verificada: Gradle 8.2, Android Gradle Plugin 8.2.2, Kotlin 1.9.22,
  Java 17, compileSdk 34, minSdk 29, targetSdk 34, versionCode 2, versionName 1.0.1.
  Verifica los archivos Gradle antes de depender de estos valores.
- **Room está en versión 13** y `exportSchema = false`. Cadena de migraciones registrada de 3→4
  hasta 12→13, sin `fallbackToDestructiveMigration`.
- Repositorio remoto: `https://github.com/djmirisk/project_leia.git`. La rama de trabajo es `main`.

## 3. Estructura y convenciones

- Un solo módulo `app`. Código en `app/src/main/java/com/solaresenergy/sisfv/` y recursos en
  `app/src/main/res/`.
- Organización por capas: pantallas y ViewModels en `ui/`, entidades y DAOs en `data/local/`,
  trabajo en segundo plano en `worker/`, estilos en `ui/theme/`.
- No edites salidas generadas en `app/build/` ni en `.gradle/`.
- Kotlin con indentación de cuatro espacios y enfoque Compose. `PascalCase` para clases, pantallas,
  composables y entidades; `camelCase` para propiedades, funciones y callbacks. Sufijos
  `ViewModel`, `Dao` y `Entity`. Paquetes en minúscula bajo `com.solaresenergy.sisfv`.
- Prefiere composables pequeños con estado pasado por parámetro y mantén las consultas de
  persistencia en los DAO, no en la UI.

## 4. Compilación, pruebas y ejecución

Desde `App_Android_SISFV/`, con el wrapper:

```powershell
$env:JAVA_HOME = "C:\Users\solar\.jdks\jbr-17.0.14"
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat installDebug
.\gradlew.bat test
.\gradlew.bat lint
```

- Gradle necesita escribir en `C:\Users\solar\.gradle`; en entornos confinados hay que ejecutarlo
  con acceso suficiente o fallará con "Acceso denegado".
- Los APK de entrega se guardan en `entrega/` (ignorada por `.gitignore`).
- No hay pruebas automatizadas todavía: `app/src/test/` y `app/src/androidTest/` no existen.
  Si se agregan, espejar el paquete de producción, nombrar los archivos `*Test.kt` y describir el
  comportamiento en el nombre de la prueba.

## 5. Reglas de seguridad y datos

- Nunca borres la base de datos, uses `fallbackToDestructiveMigration`, desinstales la app ni borres
  datos de la aplicación como solución de problemas sin autorización explícita.
- Las migraciones de Room deben ser no destructivas, con SQL alineado exactamente a las entidades;
  verifica la versión y la cadena completa de migraciones antes de modificar.
- No cambies los datos maestros de usuarios/SISFV directamente desde el formulario móvil cuando el
  cambio requiere aprobación de la plataforma: registra una solicitud pendiente.
- Cada técnico solo debe operar sobre usuarios/SISFV de los contratos y proyectos asignados; no
  amplíes el acceso a una base global.
- No sincronices datos de prueba. Antes de una APK definitiva debe planificarse una limpieza
  controlada de registros y evidencias sin perder técnicos, contratos, asignaciones ni SISFV maestros.
- No incluyas tokens, contraseñas, `local.properties`, keystores, credenciales ni datos personales
  reales en commits. Revisa `.gitignore` y `git status` antes de preparar el staging.
- Nunca ejecutes `git reset --hard`, `git clean -fd`, force push ni borrados masivos sin explicar el
  alcance y tener aprobación expresa.
- Antes de cambios importantes, inspecciona `git status` y usa commits de checkpoint cuando el
  usuario lo permita.

## 6. Funcionalidad implementada (verificar en el código antes de tocar)

- **Login local** con teclado numérico propio en pantalla (no usa el teclado del sistema).
  La sesión del técnico se guarda en `SharedPreferences` (`SesionTecnico`) y sobrevive al cierre
  del proceso; solo se borra con **SALIR**.
- **Dashboard** con contratos asignados, contadores de mantenimientos, titularidades, componentes y
  reportes de hurto, más el panel de sincronización.
- **Formulario de mantenimiento ML-GT-001** con mediciones eléctricas, protecciones, torque, firmas,
  observaciones y 18 evidencias obligatorias.
- **Catálogo de 18 evidencias** con marca de agua, GPS y hash SHA-256 en almacenamiento privado.
  - Evidencia 18: `VOLTAJE_TOMACORRIENTE_HERRAMIENTA`, "Medición de voltaje en tomacorriente".
    No debe revertirse a la antigua foto de medición de paneles.
  - Evidencia 11 (`MEDIDOR_GABINETE`): si el medidor está instalado se fotografía el medidor; si no,
    el título es "Ubicación prevista del medidor (no instalado)" y sigue siendo obligatoria.
  - Sin medidor no se exige lectura kWh ni código activo ficticio; se registra `NO_INSTALADO` con motivo.
- **Ubicación única por formulario** (`SeccionUbicacionFotos` en `UbicacionFotosSection.kt`):
  se confirma una sola vez para todas las fotos, muestra la precisión GPS, permite coordenada manual
  con formato `punto + 4 decimales`, y una vez confirmada queda bloqueada con el botón
  **CORREGIR COORDENADAS**. Sin ubicación confirmada no se pueden tomar fotografías.
  Se usa en mantenimiento, cambio de componente, cambio de titularidad, visita no efectiva y
  reporte de hurto.
- **Marca de agua**: la línea de coordenadas dice `COORDENADAS:` (no "COORDENADAS MANUALES").
- **Responsable de la visita** (mantenimiento y cambio de componente): si es el titular no se
  diligencia nada y se registran los datos maestros; si no lo es, nombre y documento son
  obligatorios y deben diferir de los del titular.
- **Actualización de datos de contacto**: menú emergente tras responder sí/no. Teléfono obligatorio
  de **10 dígitos**; correo **opcional** con validación de formato. Se guarda como solicitud
  `PENDIENTE_REVISION` / `PENDING_SYNC` en `solicitudes_cambio_contacto`, en la misma transacción
  del mantenimiento. **No** modifica los datos maestros y **no** hay envío al servidor.
- **Nombres propios**: se capitaliza la inicial de cada palabra al escribir
  (`capitalizarNombre` en `TextosComunes.kt`), porque algunos teclados ignoran la sugerencia del campo.
- **Galería de evidencias** por categoría, con visor que admite zoom (pellizco, arrastre y doble toque).
- **Reportes de hurto, cambios de titularidad y cambios de componente** generan solicitudes locales
  con fotos y firmas; no hay integración con la plataforma web.

## 7. Estado de integración

- `SyncWorker` es un esbozo: **no hay sincronización** con la NAS ni con la plataforma web.
- No existe alerta en la web para las solicitudes; afirmarlo sería incorrecto.
- La base se siembra con datos de prueba al crearse (`DatosPruebaSeeder`), incluyendo técnicos con
  PIN de prueba en texto plano. Debe desactivarse antes de una APK de producción.

## 8. Colaboración con Git y GitHub

- El repositorio local es la fuente de verdad. Antes de editar revisa rama, estado y último commit.
- **No hagas push automáticamente**: revisa con el usuario qué se va a incluir y espera su confirmación.
- Mantén cambios acotados y revisables; usa mensajes cortos en imperativo, un tema por commit.
- No incluyas en el repositorio la carpeta `entrega/` (APK) ni archivos de log.
- Deja constancia de los cambios relevantes en `registro de cambios.md`.

## 9. Notas de entorno de pruebas

- Tablet de pruebas: Xiaomi 24075RP89G (Android 15) por ADB. Requiere depuración USB y
  "Instalar vía USB" habilitados; si `adb install` responde `INSTALL_FAILED_USER_RESTRICTED`,
  falta ese permiso.
- El dispositivo táctil de esa tablet se declara como `KEYBOARD`, por lo que Gboard puede quedar en
  modo flotante sin teclas. Recuperación:
  `adb shell pm uninstall-system-updates com.google.android.inputmethod.latin`.
  Por eso el login y los campos de documento usan teclado numérico propio o `KeyboardType.Number`.
