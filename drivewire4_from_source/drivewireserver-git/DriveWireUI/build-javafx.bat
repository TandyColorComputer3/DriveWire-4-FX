@echo off
REM Build DriveWire4 WITH JavaFX
REM This script helps you build with JavaFX SDK

cd /d "%~dp0"

REM Check if JavaFX SDK path is provided
if "%1"=="" (
    echo.
    echo ========================================
    echo JavaFX SDK Path Required
    echo ========================================
    echo.
    echo Usage: build-javafx.bat "C:\javafx-sdk-21\lib"
    echo.
    echo Or set JAVAFX_SDK_PATH environment variable:
    echo   set JAVAFX_SDK_PATH=C:\javafx-sdk-21\lib
    echo   ant -Djavafx.sdk.path=%JAVAFX_SDK_PATH% create_run_jar
    echo.
    echo.
    echo To get JavaFX SDK:
    echo 1. Download from: https://openjfx.io/
    echo 2. Extract to C:\javafx-sdk-21
    echo 3. Run: build-javafx.bat "C:\javafx-sdk-21\lib"
    echo.
    echo OR install Liberica JDK (includes JavaFX):
    echo   https://bell-sw.com/pages/downloads/
    echo.
    pause
    exit /b 1
)

set JAVAFX_PATH=%1

echo Building with JavaFX SDK at: %JAVAFX_PATH%
echo.

ant -Djavafx.sdk.path=%JAVAFX_PATH% create_run_jar

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

