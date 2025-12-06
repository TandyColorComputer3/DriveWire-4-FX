# PowerShell script to run DriveWire4 JavaFX UI
# This avoids batch file parsing issues

$ErrorActionPreference = "Continue"

# Change to script directory
Set-Location $PSScriptRoot

# Check if JAR exists
if (-not (Test-Path "DriveWireUI.jar")) {
    Write-Host "DriveWireUI.jar not found. Building..." -ForegroundColor Yellow
    ant "-Djavafx.sdk.path=C:/javafx-sdk-21/lib" create_run_jar
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Set native library path for RXTX serial communication
$env:JAVA_LIB_PATH = "../java/native/Windows/amd64"

# Try to detect JavaFX SDK location
$JAVAFX_PATH = $null
if (Test-Path "C:\javafx-sdk-21\lib") {
    $JAVAFX_PATH = "C:\javafx-sdk-21\lib"
} elseif (Test-Path "$env:USERPROFILE\javafx-sdk-21\lib") {
    $JAVAFX_PATH = "$env:USERPROFILE\javafx-sdk-21\lib"
}

# Set JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

# Run with JavaFX modules
if ($JAVAFX_PATH) {
    Write-Host "Using JavaFX SDK at: $JAVAFX_PATH" -ForegroundColor Green
    Write-Host "Starting DriveWire4 JavaFX..." -ForegroundColor Cyan
    Write-Host ""
    
    $javaArgs = @(
        "--module-path", $JAVAFX_PATH,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.web",
        "-Djava.library.path=$env:JAVA_LIB_PATH",
        "-cp", "DriveWireUI.jar;lib/*;../java/lib/*",
        "com.groupunix.drivewireui.MainWinFX"
    )
    $javaArgs += $args
    & java $javaArgs
    
    $exitCode = $LASTEXITCODE
} else {
    Write-Host "Attempting to use JavaFX from JDK..." -ForegroundColor Yellow
    Write-Host "Starting DriveWire4 JavaFX..." -ForegroundColor Cyan
    Write-Host ""
    
    $javaArgs = @(
        "--add-modules", "javafx.controls,javafx.fxml,javafx.web",
        "-Djava.library.path=$env:JAVA_LIB_PATH",
        "-cp", "DriveWireUI.jar;lib/*;../java/lib/*",
        "com.groupunix.drivewireui.MainWinFX"
    )
    $javaArgs += $args
    & java $javaArgs
    
    $exitCode = $LASTEXITCODE
}

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Application exited with error code: $exitCode" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "If JavaFX modules were not found:" -ForegroundColor Yellow
    Write-Host "1. Download Liberica JDK (includes JavaFX): https://bell-sw.com/pages/downloads/" -ForegroundColor White
    Write-Host "2. Download JavaFX SDK separately: https://openjfx.io/" -ForegroundColor White
    Write-Host "   Then extract to C:\javafx-sdk-21" -ForegroundColor White
    Write-Host ""
    Read-Host "Press Enter to exit"
}

