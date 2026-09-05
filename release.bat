@echo off
REM Genera la release firmada (AAB + APK).
REM Requisitos:
REM   1) app\checklist-release.jks (o la ruta en la variable CHECKLIST_KEYSTORE)
REM   2) Variables de entorno: CHECKLIST_STORE_PASSWORD y CHECKLIST_KEY_PASSWORD
REM      (opcional: CHECKLIST_KEY_ALIAS, default "checklist")
REM Uso: release.bat

setlocal
cd /d "%~dp0"

REM Gradle 8.x no soporta Java 26+ (falla al compilar los scripts .kts).
REM Usa el JBR 21 de Android Studio si existe; si no, respeta el JAVA_HOME actual.
if exist "C:\Program Files\Android\Android Studio\jbr" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)

if not exist "app\checklist-release.jks" (
    echo [ERROR] No existe app\checklist-release.jks. Generalo una sola vez con:
    echo   keytool -genkey -v -keystore app\checklist-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias checklist
    exit /b 1
)

if "%CHECKLIST_STORE_PASSWORD%"=="" (
    echo [ERROR] Falta la variable de entorno CHECKLIST_STORE_PASSWORD
    exit /b 1
)
if "%CHECKLIST_KEY_PASSWORD%"=="" (
    echo [ERROR] Falta la variable de entorno CHECKLIST_KEY_PASSWORD
    exit /b 1
)

call gradlew.bat bundleRelease assembleRelease --no-daemon
if errorlevel 1 exit /b 1

echo.
echo Release generada:
echo   AAB: app\build\outputs\bundle\release\app-release.aab
echo   APK: app\build\outputs\apk\release\app-release.apk
