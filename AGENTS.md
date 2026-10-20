# Repository Guidelines

## Project Structure & Module Organization

This workspace contains the Android application in `App_Android_SISFV/`. The
single `app` module holds production code in
`app/src/main/java/com/solaresenergy/sisfv/` and resources in
`app/src/main/res/`. Keep features grouped by layer: Compose screens and view
models live under `ui/`, Room entities and DAOs under `data/local/`, and
background work under `worker/`. App-wide Compose styling belongs in
`ui/theme/`. Do not edit generated output under `app/build/` or `.gradle/`.

## Build, Test, and Development Commands

Run commands from `App_Android_SISFV/` using the Gradle wrapper:

- `./gradlew.bat assembleDebug` builds a debug APK.
- `./gradlew.bat installDebug` builds and installs it on a connected emulator or device.
- `./gradlew.bat test` runs local JVM unit tests when they are added.
- `./gradlew.bat connectedAndroidTest` runs instrumented tests on a connected device.
- `./gradlew.bat lint` runs Android lint checks.

The project targets Java/Kotlin 17 and Android SDK 34. Keep machine-specific
SDK paths in the untracked `local.properties` file.

## Coding Style & Naming Conventions

Write Kotlin with four-space indentation and follow the surrounding code's
Compose-first approach. Use `PascalCase` for classes, screens, composables,
and Room entities (for example, `FormularioMantenimientoScreen` and
`TecnicoEntity`); use `camelCase` for properties, functions, and callbacks.
Name ViewModels with the `ViewModel` suffix, DAOs with `Dao`, and database
entities with `Entity`. Keep package names lowercase beneath
`com.solaresenergy.sisfv`. Prefer small composables with state passed through
explicit parameters and keep persistence queries in DAOs rather than UI code.

## Testing Guidelines

Place local unit tests in `app/src/test/` and device-dependent tests in
`app/src/androidTest/`, mirroring the production package path. Name test files
`*Test.kt` and describe behavior in test names, e.g.
`login_withKnownTechnician_opensHome()`. Add tests for changed ViewModel,
database, or worker behavior; run the relevant Gradle command before opening a
review.

## Commit & Pull Request Guidelines

No Git history is available in this checkout to establish an existing message
format. Use short, imperative commits scoped to one concern, such as
`Add maintenance photo validation`. Pull requests should state the user-facing
change, list validation performed, link the relevant issue when present, and
include screenshots or a short recording for Compose UI changes. Call out
database schema or migration changes explicitly.
