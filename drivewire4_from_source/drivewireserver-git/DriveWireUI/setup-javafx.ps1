# PowerShell script to set up JavaFX for DriveWire4 build
# This script helps you either:
# 1. Download JavaFX SDK if you have Liberica Standard, OR
# 2. Configure the build to use JavaFX SDK

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DriveWire4 JavaFX Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check current Java
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    Write-Host "JAVA_HOME is not set. Checking default installation..." -ForegroundColor Yellow
    $javaHome = "C:\Program Files\BellSoft\LibericaJDK-21"
}

Write-Host "Current JAVA_HOME: $javaHome" -ForegroundColor White

# Check if JavaFX is available
Write-Host "`nChecking for JavaFX..." -ForegroundColor Cyan
$javafxFound = $false

# Check for JavaFX modules
$modules = java --list-modules 2>&1 | Select-String "javafx"
if ($modules) {
    Write-Host "✓ JavaFX modules found in JDK!" -ForegroundColor Green
    $javafxFound = $true
} else {
    Write-Host "✗ JavaFX modules not found in JDK" -ForegroundColor Red
    Write-Host "  You have Liberica JDK Standard (doesn't include JavaFX)" -ForegroundColor Yellow
    Write-Host "  You need either:" -ForegroundColor Yellow
    Write-Host "  1. Liberica JDK Full (includes JavaFX), OR" -ForegroundColor White
    Write-Host "  2. Download JavaFX SDK separately" -ForegroundColor White
    Write-Host ""
    
    $choice = Read-Host "Download JavaFX SDK now? (Y/N)"
    if ($choice -eq "Y" -or $choice -eq "y") {
        Write-Host "`nDownloading JavaFX SDK 21..." -ForegroundColor Cyan
        
        $javafxUrl = "https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_windows-x64_bin-sdk.zip"
        $javafxZip = "$env:TEMP\openjfx-21.0.2-sdk.zip"
        $javafxDir = "C:\javafx-sdk-21"
        
        try {
            Write-Host "Downloading from: $javafxUrl" -ForegroundColor Gray
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $javafxUrl -OutFile $javafxZip -UseBasicParsing
            $ProgressPreference = 'Continue'
            
            Write-Host "Extracting to: $javafxDir" -ForegroundColor Gray
            if (Test-Path $javafxDir) {
                Remove-Item $javafxDir -Recurse -Force
            }
            Expand-Archive -Path $javafxZip -DestinationPath $javafxDir -Force
            
            # Move contents from subdirectory
            $extracted = Get-ChildItem $javafxDir -Directory | Where-Object { $_.Name -like "javafx-sdk-*" } | Select-Object -First 1
            if ($extracted) {
                Get-ChildItem $extracted.FullName | Move-Item -Destination $javafxDir -Force
                Remove-Item $extracted.FullName -Force
            }
            
            Write-Host "✓ JavaFX SDK extracted to: $javafxDir" -ForegroundColor Green
            
            # Set environment variable for this session
            $env:JAVAFX_SDK_PATH = "$javafxDir\lib"
            
            Write-Host "`nJavaFX SDK path: $env:JAVAFX_SDK_PATH" -ForegroundColor Green
            Write-Host "`nNow you can build with:" -ForegroundColor Cyan
            Write-Host "  ant -Djavafx.sdk.path=$env:JAVAFX_SDK_PATH create_run_jar" -ForegroundColor White
            
            # Clean up
            Remove-Item $javafxZip -Force -ErrorAction SilentlyContinue
            
        } catch {
            Write-Host "✗ Download failed: $_" -ForegroundColor Red
            Write-Host "`nPlease download manually from: https://openjfx.io/" -ForegroundColor Yellow
            Write-Host "Extract to: C:\javafx-sdk-21" -ForegroundColor Yellow
            Write-Host "Then run: ant -Djavafx.sdk.path=C:/javafx-sdk-21/lib create_run_jar" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "`nTo build with JavaFX SDK:" -ForegroundColor Yellow
        Write-Host "1. Download from: https://openjfx.io/" -ForegroundColor White
        Write-Host "2. Extract to: C:\javafx-sdk-21" -ForegroundColor White
        Write-Host "3. Run: ant -Djavafx.sdk.path=C:/javafx-sdk-21/lib create_run_jar" -ForegroundColor White
        exit 0
    }
}

Write-Host "`nSetup complete!" -ForegroundColor Green

