# Registro de cambios — SISFV (Solares Energy Group S.A.S. E.S.P.)

Este documento describe, paso a paso, todas las modificaciones realizadas en el proyecto
`App_Android_SISFV` durante las sesiones de trabajo del 27 y 28 de octubre de 2026.

- **Rama de trabajo:** `main` (sincronizada con `origin/main`)
- **Paquete:** `com.solaresenergy.sisfv`
- **Versión:** 1.0.1 (versionCode 2), compileSdk 34, minSdk 29, Java 17
- **Equipo de pruebas:** tablet Xiaomi 24075RP89G (Android 15) por ADB

> Criterio de seguridad aplicado en todo el trabajo: no se borró la base de datos, no se usó
> `fallbackToDestructiveMigration`, no se desinstaló la app ni se borraron datos de la aplicación
> sin autorización. Las migraciones de Room se mantuvieron intactas (versión 13).

---

## 1. Preparación del entorno

### 1.1 Reparación de permisos de Windows
- **Problema:** el shell no podía escribir en `C:\PROYECTO_LEIA_APK` (`SetNamedSecurityInfoW failed Win32 5`).
- **Acción:** diagnóstico y reparación de permisos de la carpeta raíz y de `.git`, con respaldo previo
  y comandos de reversión impresos.
- **Resultado:** escritura habilitada en el workspace y en `.git`.

### 1.2 Verificación de compilación
- **Comando:** `.\gradlew.bat :app:assembleDebug --console=plain`
- **Resultado:** `BUILD SUCCESSFUL`; APK generada y verificada contra la fecha de la última fuente modificada.

### 1.3 Entrega de APK
- APK copiada a `entrega\`, con hash SHA-256 publicado en cada iteración.

---

## 2. Consolidación con GitHub `main`

- `origin/main` había avanzado 5 commits y ya contenía el trabajo de reportes de hurto y galería;
  la rama `init_session_done` había sido **borrada del remoto**.
- **Acción:** el trabajo local sin commitear se preservó en la rama `wip/local-2026-10-27`
  (commit `fdf5e33`) y `main` se fast-forward a `99969e2`.
- **Resultado:** árbol de trabajo limpio sobre `main`, sin pérdida de trabajo.

---

## 3. Teclado de ingreso de datos

### 3.1 Diagnóstico
- En la tablet, al tocar un campo, `mInputShown=true` pero Gboard dibujaba solo una barra flotante
  sin teclas (insets del IME con altura 0).
- Se reprodujo **también en la app Ajustes del sistema**, por lo que no era un defecto de SISFV.
- Causa: el dispositivo táctil `chipone-tddi,pen` se declara con `Sources: KEYBOARD`, y Gboard
  quedaba en modo "teclado físico" / flotante.

### 3.2 Acciones sobre el equipo de prueba
- Reversión de la actualización de Gboard 18.4.4 a la versión de fábrica 15.4.8
  (`pm uninstall-system-updates`).
- Limpieza de datos de Gboard (`pm clear`) autorizada por el usuario.
- Reinicio de fábrica de la tablet autorizado por el usuario; con ello el teclado del sistema volvió
  a funcionar anclado y a pantalla completa.

### 3.3 Teclado numérico propio en el login
- **Problema:** en MIUI el IME podía aparecer sin teclas e impedir escribir la cédula.
- **Cambio:** `LoginScreen.kt` dejó de usar `OutlinedTextField`; ahora usa `CampoTeclado`
  (display sin conexión de entrada) y `TecladoNumericoLogin` (1-9, 0, borrar y tecla de acción
  SIGUIENTE/ENTRAR).
- **Resultado:** el login no depende del teclado del sistema.

### 3.4 Teclado numérico en documentos de identidad
- Se revisaron todos los campos de documento:
  - Mantenimiento, cambio de componente y cambio de titularidad ya usaban `KeyboardType.Number`.
  - **Reporte de hurto no lo tenía** → corregido en `ReporteHurtoScreen.kt`.

---

## 4. Sesión del técnico

- **Problema:** al salir de la app y volver, se reiniciaba en la pantalla de login (MIUI mata el
  proceso en segundo plano y se perdía el estado de Compose).
- **Cambio:** nuevo `data/local/SesionTecnico.kt` que guarda el `tecnicoId` en `SharedPreferences`.
  `MainActivity` restaura la sesión al arrancar, la descarta si el técnico ya no existe y la borra
  al pulsar **SALIR**.
- **Verificado en la tablet:** volver desde Inicio conserva la sesión; matar el proceso también;
  SALIR vuelve al login y la sesión queda borrada.

---

## 5. Visor de evidencias con zoom

- **Cambio:** `GaleriaEvidenciasScreen.kt` incorpora `FotoGaleriaZoom`: pellizco (1× a 6×), arrastre
  acotado a los bordes, doble toque para ampliar/restablecer y decodificación a 2400 px para leer
  la marca de agua.
- **Verificado en la tablet** con una foto real: zoom, arrastre y restablecimiento correctos.

---

## 6. Marca de agua

- **Cambio:** el texto de la marca de agua pasó de `COORDENADAS MANUALES: …` a `COORDENADAS: …`.
- Archivo: `EvidenciasFotograficasSection.kt` (`generarFotoConMarcaAgua`).
- Se verificó que no quedaran otras apariciones de "COORDENADAS MANUALES" en el código.

---

## 7. Sección única de ubicación en todos los formularios

- **Problema:** el cambio de componente pedía coordenadas **dentro de cada foto**; el mantenimiento
  tenía su propio bloque y el reporte de hurto otro distinto.
- **Cambio:** nuevo componente reutilizable `UbicacionFotosSection.kt` (`SeccionUbicacionFotos`) con:
  - estado de la ubicación (manual confirmada, GPS con precisión ±X m, sin permiso, buscando),
  - campos Latitud/Longitud, botón **CONFIRMAR COORDENADAS**,
  - una vez confirmada, campos bloqueados y botón **CORREGIR COORDENADAS**,
  - validación de formato (punto y 4 decimales) y rango.
- Se aplicó en: **cambio de componente, cambio de titularidad, visita no efectiva y reporte de hurto**.
  En el reporte de hurto esto además devolvió el botón CORREGIR, que antes desaparecía.
- Se eliminó el bloque de coordenadas de `CapturaFotoTitularidad` y su parámetro
  `mostrarCoordenadasManuales`; la tarjeta de foto ya solo muestra título, estado y TOMAR FOTO.
- En el apartado 6 se agregó un aviso en rojo cuando no hay ubicación: sin coordenadas no se puede
  tomar la foto porque la marca de agua las necesita.

### 7.1 Corrección de un fallo grave introducido y detectado en pruebas
- Al pulsar CORREGIR la app se cerraba:
  `IllegalStateException: OffsetMapping.originalToTransformed returned invalid mapping` en
  `CoreTextFieldKt.startInputSession` (fallo de Compose con campos `readOnly` al recibir foco).
- **Solución:** los campos confirmados ahora se **deshabilitan** (`enabled = false`) en lugar de usar
  `readOnly`. Aplicado en la sección compartida, mantenimiento y cambio de titularidad.

---

## 8. Corrección de textos con codificación dañada

- `ReporteHurtoScreen.kt`: `"…todas las fotografÃ­as…"` → eliminado al refactorizar la sección.
- `GaleriaEvidenciasViewModel.kt`: `"No se pudieron consultar las fotografÃƒÂ­as guardadas."` →
  `"No se pudieron consultar las fotografías guardadas."`
- Barrido final en `app/src/main` (`.kt` y `.xml`): sin apariciones de `Ã` ni `Â`.

---

## 9. Validación del responsable de la visita

**Regla implementada** en cambio de componente y en mantenimiento:

- Si el técnico indica que **quien recibe es el titular**:
  - los campos Nombre y Documento se deshabilitan y se vacían,
  - no se exige diligenciar nada,
  - se registran los datos maestros del SISFV (nombre y documento del usuario),
  - se muestra: «Se registrarán los datos del titular del SISFV: …».
- Si indica que **no es el titular**:
  - nombre y documento son obligatorios,
  - no pueden coincidir con los del titular registrado (nombre sin distinguir mayúsculas,
    documento exacto), con mensaje explícito que invita a marcar «Sí» si lo es.
- La validación se ejecuta **antes** de exigir fotografías, para que el técnico vea el error de una vez.
- Archivos: `CambioComponenteScreen.kt`, `CambioComponenteViewModel.kt`,
  `FormularioMantenimientoScreen.kt`, `FormularioMantenimientoViewModel.kt`.

---

## 10. Actualización de datos de contacto como menú emergente

- **Antes:** dos campos permanentes en el formulario (teléfono y correo opcional).
- **Ahora:** la tarjeta pregunta «¿Desea actualizar los datos de contacto del usuario?» con opciones
  **Sí, actualizar** / **No**.
  - Al elegir **Sí** se abre un `AlertDialog` con **Nuevo teléfono** (obligatorio) y
    **Correo electrónico (opcional)**.
  - GUARDAR valida dentro del diálogo y, al aceptar, la tarjeta muestra el resumen capturado y el
    botón **EDITAR DATOS DE CONTACTO**.
  - Al elegir **No** se limpian los datos capturados y no se genera solicitud.
  - Si no se responde, al guardar aparece: «Indique si desea actualizar los datos de contacto del usuario.».
- **Decisión de negocio aplicada:** el correo **no** es obligatorio (no todos los usuarios lo tienen),
  según indicación del usuario.
- La solicitud sigue guardándose en `solicitudes_cambio_contacto` con estado `PENDIENTE_REVISION` y
  `PENDING_SYNC`; no se modifican los datos maestros y no existe sincronización con servidor todavía.

---

## 11. Teléfonos de 10 dígitos y nombres capitalizados

### 11.1 Teléfono exacto de 10 dígitos
- El campo del diálogo filtra a **solo dígitos, máximo 10**, y valida longitud exacta.
- Se alinearon las validaciones de pantalla, `FormularioMantenimientoViewModel` y
  `CambioTitularidadViewModel` (campo y validación del cambio de titularidad).
- Mensaje: «El teléfono debe tener 10 dígitos.».
- **Verificado:** 9 dígitos se rechazan; al intentar 13 el campo se corta en 10.

### 11.2 Capitalización de nombres propios
- Se agregó `TextosComunes.kt` con `capitalizarNombre(...)`, que pone en mayúscula la inicial de
  cada palabra mientras se escribe.
- Se aplicó en los cuatro campos de nombre: mantenimiento, cambio de componente, cambio de
  titularidad y reporte de hurto.
- Motivo: `KeyboardCapitalization.Words` (también añadido) fue **ignorado por Gboard en la tablet**,
  por lo que la capitalización se garantiza por código.
- **Verificado:** al escribir `ana maria de cruz` el campo queda `Ana Maria De Cruz`.

---

## 12. Registro de commits publicados

| Commit | Descripción |
|---|---|
| `e323859` | `fix: teclado numerico propio en el login` |
| `53a4782` | `feat: conservar la sesion del tecnico entre aperturas de la app` |
| `43c4900` | `feat: visor de evidencias con zoom y correccion de texto` |
| `b5a2c3d` | `feat: seccion unica de ubicacion para todas las fotografias` |
| `8f6a794` | `chore: ignorar la carpeta de entregables` |

Los cambios de las secciones 9, 10 y 11 quedan en el commit de este informe.

---

## 13. Notas y pendientes conocidos

1. **Sincronización con la plataforma web:** `SyncWorker` sigue siendo un esbozo; las solicitudes de
   cambio de contacto y de titularidad se guardan localmente y **no** se envían a ningún servidor.
   No debe afirmarse que existe alerta en la web.
2. **Datos de prueba:** `AppDatabase.DatabaseCallback` siembra técnicos y datos maestros de prueba al
   crear la base. Antes de una APK definitiva debe planificarse una limpieza controlada y desactivar
   el sembrado, sin perder técnicos, contratos, asignaciones ni SISFV maestros.
3. **Documentación desactualizada:** el `AGENTS.md` heredado menciona Room versión 9 y 16 evidencias;
   el código real está en Room **13** y **18** evidencias.
4. **Reporte de hurto:** mantiene su propia pregunta «¿quien reporta es el titular?» sin la regla de
   documento distinto al titular. Queda a decisión del usuario si se aplica la misma validación.
5. **Riesgo de dispositivo:** el táctil de la tablet se declara como teclado físico; si Gboard se
   actualiza o se corrompe, el teclado puede volver a mostrarse flotante. Recuperación:
   `adb shell pm uninstall-system-updates com.google.android.inputmethod.latin`.
