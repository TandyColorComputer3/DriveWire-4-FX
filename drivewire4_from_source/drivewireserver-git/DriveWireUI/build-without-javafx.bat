@echo off
REM Build DriveWire4 without JavaFX files (for testing build process)
REM This excludes JavaFX files that require JavaFX SDK to compile

echo Building DriveWire4 (excluding JavaFX files)...
echo.
echo NOTE: This build will NOT include the JavaFX UI components.
echo       To build with JavaFX, install Liberica JDK or JavaFX SDK first.
echo.

ant create_run_jar

if errorlevel 1 (
    echo.
    echo Build completed with errors (JavaFX files excluded).
    echo To build with JavaFX, see SETUP_JAVAFX.md
    pause
) else (
    echo.
    echo Build completed successfully (without JavaFX components).
    echo To run the JavaFX version, you need JavaFX SDK installed.
    echo See SETUP_JAVAFX.md for instructions.
    pause
)

