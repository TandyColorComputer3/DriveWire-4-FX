@echo off
REM Build script that properly sets JavaFX SDK path
REM This ensures the property is passed correctly to Ant

cd /d "%~dp0"

set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-21
set PATH=%JAVA_HOME%\bin;%PATH%

set JAVAFX_SDK_PATH=C:/javafx-sdk-21/lib

echo Building DriveWire4 with JavaFX SDK...
echo JavaFX SDK Path: %JAVAFX_SDK_PATH%
echo.

ant "-Djavafx.sdk.path=%JAVAFX_SDK_PATH%" create_run_jar

if errorlevel 1 (
    echo.
    echo Build failed! Check errors above.
    pause
    exit /b 1
) else (
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo To run: run-javafx.bat
    echo.
    pause
)

