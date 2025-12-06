@echo off
REM Run DriveWire4 JavaFX UI
REM This script runs the JavaFX version of DriveWire4

cd /d "%~dp0"

REM Check if JAR exists
if not exist "DriveWireUI.jar" (
    echo DriveWireUI.jar not found. Building...
    call ant create_run_jar
    if errorlevel 1 (
        echo Build failed!
        pause
        exit /b 1
    )
)

REM Set native library path for RXTX serial communication
set "JAVA_LIB_PATH=../java/native/Windows/amd64"

REM Try to detect JavaFX SDK location (common locations)
set JAVAFX_PATH=
if exist "C:\javafx-sdk-21\lib" set JAVAFX_PATH=C:\javafx-sdk-21\lib
if exist "%USERPROFILE%\javafx-sdk-21\lib" set JAVAFX_PATH=%USERPROFILE%\javafx-sdk-21\lib

REM Run with JavaFX modules
if defined JAVAFX_PATH (
    echo Using JavaFX SDK at: %JAVAFX_PATH%
    java --module-path "%JAVAFX_PATH%" ^
         --add-modules javafx.controls,javafx.fxml,javafx.web ^
         -Djava.library.path="%JAVA_LIB_PATH%" ^
         -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
         com.groupunix.drivewireui.MainWinFX %*
) else (
    echo Attempting to use JavaFX from JDK...
    REM Try with modules (works if JDK includes JavaFX like Liberica/Zulu FX)
    java --add-modules javafx.controls,javafx.fxml,javafx.web ^
         -Djava.library.path="%JAVA_LIB_PATH%" ^
         -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
         com.groupunix.drivewireui.MainWinFX %*
)

if errorlevel 1 (
    echo.
    echo ========================================
    echo JavaFX modules not found!
    echo ========================================
    echo.
    echo Options:
    echo 1. Download Liberica JDK (includes JavaFX): https://bell-sw.com/pages/downloads/
    echo 2. Download JavaFX SDK separately: https://openjfx.io/
    echo    Then set JAVAFX_PATH environment variable or extract to C:\javafx-sdk-21
    echo.
    echo See QUICK_START.md for detailed instructions.
    echo.
    pause
)

