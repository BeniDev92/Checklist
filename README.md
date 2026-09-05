# Checklist

App Android nativa para gestionar una rutina diaria: checklist de tareas con cumplimiento por día, progreso y rachas.

## Funcionalidades

- CRUD de tareas de rutina (crear, editar, eliminar, reordenar por posición).
- Marcar/desmarcar el cumplimiento de cada tarea para el día actual.
- Progreso diario en porcentaje y racha de días consecutivos al 100 %.
- Estadísticas por tarea: tasa de cumplimiento sobre los días con registro.
- Reset diario automático: el "hoy" se deriva de la fecha local; no se borra historial.

## Stack tecnológico

- Kotlin + Jetpack Compose (Material 3).
- Room 2.7.x (SQLite) con KSP.
- MVVM mínimo sin Hilt (ViewModel con factory manual).
- Corrutinas y Flow.

## Requisitos

- JDK 17 o 21.
- Android SDK 35 (compileSdk/targetSdk 35, minSdk 26).
- Android Studio (opcional, para abrir y ejecutar el proyecto).

## Compilar y ejecutar

```bash
# Compilar el APK de debug
.\gradlew.bat assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Instálalo en un dispositivo o emulador:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Alternativa: abre el proyecto en Android Studio y pulsa **Run**.

## Ejecutar los tests

```bash
.\gradlew.bat testDebugUnitTest
```

## Generar una release

1. **Generar el keystore** (una sola vez):

   ```bash
   keytool -genkey -v -keystore app\checklist-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias checklist
   ```

   El keystore queda excluido del control de versiones (ya está en `.gitignore`). Guárdalo a salvo y no lo pierdas: es la única forma de firmar actualizaciones de la app.

2. **Definir las variables de entorno** (PowerShell):

   ```powershell
   $env:CHECKLIST_STORE_PASSWORD = "tu_password"
   $env:CHECKLIST_KEY_PASSWORD = "tu_password"
   ```

   Opcional: `$env:CHECKLIST_KEY_ALIAS = "checklist"` (default) y `$env:CHECKLIST_KEYSTORE` si el keystore no está en `app\checklist-release.jks`.

3. **Subir el `versionCode`** (entero, debe subir en cada release) y ajustar el `versionName` en `app/build.gradle.kts` (`defaultConfig`).

4. **Ejecutar el script**:

   ```bash
   .\release.bat
   ```

   O manualmente: `.\gradlew.bat bundleRelease assembleRelease`.

5. **Artefactos generados**: AAB para Google Play (`app\build\outputs\bundle\release\app-release.aab`) y APK para instalar o distribuir (`app\build\outputs\apk\release\app-release.apk`).

> Nota: si no existe el keystore, el build de release se genera sin firmar (no instalable en Android).

## Estructura del proyecto

```
app/src/main/java/com/example/checklist/
├── MainActivity.kt                  # Activity, tema (color dinámico) y navegación entre pestañas
├── data/
│   ├── Task.kt                      # Entidad Room "tasks"
│   ├── DailyCompletion.kt           # Entidad Room "daily_completions"
│   ├── TaskDao.kt                   # Consultas y toggle de cumplimiento (Room)
│   ├── AppDatabase.kt               # Base de datos Room (singleton)
│   └── ChecklistRepository.kt       # Lógica de negocio: racha, stats, fecha de hoy
└── ui/
    ├── ChecklistViewModel.kt        # UiState de la checklist y estadísticas (Flow + StateFlow)
    ├── ChecklistScreen.kt           # Pantalla checklist: lista, checkbox, diálogos CRUD
    └── StatsScreen.kt               # Pantalla de racha y estadísticas por tarea

app/src/test/java/com/example/checklist/data/
└── ChecklistRepositoryTest.kt       # 11 tests unitarios (JUnit 4) de racha y stats
```

## Modelo de datos

- **tasks**: tareas de la rutina (`title`, `position`, `active`, `created_at`). `created_at` determina desde qué día cuenta una tarea para la racha.
- **daily_completions**: registro de cumplimiento por tarea y día (`task_id`, `date`, `completed`), con índice único `(task_id, date)`.

## Notas

- **Racha**: un día cuenta como completo solo si la tarea existía ese día (según `created_at`). El día de hoy *en progreso* no suma ni rompe la racha: solo la rompe un día completo que falte.
- **Tasa de cumplimiento**: se calcula sobre los días con registro de esa tarea, no sobre días transcurridos.
- **Borrado**: eliminar una tarea es permanente; sus `daily_completions` se borran con CASCADE.
- **Reset diario**: el "hoy" se deriva de la fecha local (`LocalDate.now()`), sin cron ni borrado; se recalcula al abrir la app.
- **JDK para Gradle (línea de comandos)**: Gradle NO lee `local.properties` (eso solo lo usa el plugin de Android para `sdk.dir`). En línea de comandos usa la variable de entorno `JAVA_HOME`. Gradle 8.x requiere un JDK 17-21; Java 26+ falla al compilar los scripts `.kts` con `IllegalArgumentException: 26.0.1`. El JDK recomendado es el JBR 21 de Android Studio (`C:\Program Files\Android\Android Studio\jbr`). `release.bat` ya fija `JAVA_HOME` a ese JBR automáticamente cuando existe. Para builds manuales, defínelo antes, por ejemplo en PowerShell:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```
  o en cmd:
  ```bat
  set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
  ```
  En CI se usa el JDK 17 configurado por los workflows, así que no hace falta en el runner.
