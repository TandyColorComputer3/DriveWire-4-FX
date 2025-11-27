# PowerShell script to install Apache Ant on Windows
# Run as Administrator for system-wide installation, or as regular user for user-only installation

param(
    [string]$AntVersion = "1.10.14",
    [string]$InstallPath = "$env:ProgramFiles\Apache\ant",
    [switch]$UserInstall = $false
)

# If UserInstall is specified, install to user directory
if ($UserInstall) {
    $InstallPath = "$env:USERPROFILE\Apache\ant"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Apache Ant Installation Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator (for system-wide install)
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $UserInstall -and -not $isAdmin) {
    Write-Host "WARNING: Not running as Administrator." -ForegroundColor Yellow
    Write-Host "System-wide installation requires Administrator privileges." -ForegroundColor Yellow
    Write-Host "Use -UserInstall flag for user-only installation, or run as Administrator." -ForegroundColor Yellow
    Write-Host ""
    $response = Read-Host "Continue with user-only installation? (Y/N)"
    if ($response -ne "Y" -and $response -ne "y") {
        Write-Host "Installation cancelled." -ForegroundColor Red
        exit 1
    }
    $UserInstall = $true
    $InstallPath = "$env:USERPROFILE\Apache\ant"
}

# Check if Ant is already installed
Write-Host "Checking for existing Ant installation..." -ForegroundColor Yellow
try {
    $existingAnt = & ant -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Apache Ant is already installed:" -ForegroundColor Green
        Write-Host $existingAnt -ForegroundColor Green
        Write-Host ""
        $response = Read-Host "Do you want to reinstall? (Y/N)"
        if ($response -ne "Y" -and $response -ne "y") {
            Write-Host "Installation cancelled. Using existing Ant installation." -ForegroundColor Yellow
            exit 0
        }
    }
} catch {
    Write-Host "No existing Ant installation found. Proceeding with installation..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Installation Details:" -ForegroundColor Cyan
Write-Host "  Version: $AntVersion" -ForegroundColor White
Write-Host "  Install Path: $InstallPath" -ForegroundColor White
Write-Host "  Installation Type: $(if ($UserInstall) { 'User' } else { 'System' })" -ForegroundColor White
Write-Host ""

# Create installation directory
Write-Host "Creating installation directory..." -ForegroundColor Yellow
try {
    if (Test-Path $InstallPath) {
        Write-Host "Removing existing installation..." -ForegroundColor Yellow
        Remove-Item -Path $InstallPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
    Write-Host "Directory created: $InstallPath" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to create installation directory: $_" -ForegroundColor Red
    exit 1
}

# Download Ant
$zipFile = "$env:TEMP\apache-ant-$AntVersion-bin.zip"
$downloadUrl = "https://archive.apache.org/dist/ant/binaries/apache-ant-$AntVersion-bin.zip"

Write-Host ""
Write-Host "Downloading Apache Ant $AntVersion..." -ForegroundColor Yellow
Write-Host "  URL: $downloadUrl" -ForegroundColor Gray
Write-Host "  Destination: $zipFile" -ForegroundColor Gray

try {
    # Use TLS 1.2 for secure downloads
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile -UseBasicParsing
    $ProgressPreference = 'Continue'
    
    if (-not (Test-Path $zipFile)) {
        throw "Download failed - file not found"
    }
    
    $fileSize = (Get-Item $zipFile).Length / 1MB
    Write-Host "Download complete. Size: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to download Ant: $_" -ForegroundColor Red
    Write-Host "You may need to check your internet connection or download manually from:" -ForegroundColor Yellow
    Write-Host "  https://ant.apache.org/bindownload.cgi" -ForegroundColor Yellow
    exit 1
}

# Extract Ant
Write-Host ""
Write-Host "Extracting Apache Ant..." -ForegroundColor Yellow
try {
    Expand-Archive -Path $zipFile -DestinationPath $InstallPath -Force
    Write-Host "Extraction complete." -ForegroundColor Green
    
    # Move contents from apache-ant-X.X.X folder to InstallPath
    $extractedFolder = Get-ChildItem -Path $InstallPath -Directory | Where-Object { $_.Name -like "apache-ant-*" } | Select-Object -First 1
    if ($extractedFolder) {
        Write-Host "Moving files to installation directory..." -ForegroundColor Yellow
        Get-ChildItem -Path $extractedFolder.FullName | Move-Item -Destination $InstallPath -Force
        Remove-Item -Path $extractedFolder.FullName -Force
    }
} catch {
    Write-Host "ERROR: Failed to extract Ant: $_" -ForegroundColor Red
    exit 1
}

# Clean up zip file
Write-Host ""
Write-Host "Cleaning up..." -ForegroundColor Yellow
Remove-Item -Path $zipFile -Force -ErrorAction SilentlyContinue

# Set environment variables
Write-Host ""
Write-Host "Setting environment variables..." -ForegroundColor Yellow

$antHome = $InstallPath
$antBin = "$antHome\bin"

# Set ANT_HOME
if ($UserInstall) {
    # User environment variables
    [Environment]::SetEnvironmentVariable("ANT_HOME", $antHome, "User")
    Write-Host "  ANT_HOME set to: $antHome (User)" -ForegroundColor Green
} else {
    # System environment variables
    [Environment]::SetEnvironmentVariable("ANT_HOME", $antHome, "Machine")
    Write-Host "  ANT_HOME set to: $antHome (System)" -ForegroundColor Green
}

# Add Ant bin to PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", $(if ($UserInstall) { "User" } else { "Machine" }))
if ($currentPath -notlike "*$antBin*") {
    $newPath = "$currentPath;$antBin"
    if ($UserInstall) {
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    } else {
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
    }
    Write-Host "  Added $antBin to PATH" -ForegroundColor Green
} else {
    Write-Host "  Ant bin directory already in PATH" -ForegroundColor Yellow
}

# Refresh environment variables in current session
$env:ANT_HOME = $antHome
$env:Path = "$env:Path;$antBin"

# Verify installation
Write-Host ""
Write-Host "Verifying installation..." -ForegroundColor Yellow
try {
    $antVersion = & "$antBin\ant.bat" -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Apache Ant installed successfully!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host $antVersion -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Installation Path: $antHome" -ForegroundColor White
        Write-Host ""
        Write-Host "NOTE: You may need to:" -ForegroundColor Yellow
        Write-Host "  1. Close and reopen your terminal/PowerShell window" -ForegroundColor Yellow
        Write-Host "  2. Or run: refreshenv (if Chocolatey is installed)" -ForegroundColor Yellow
        Write-Host "  3. Or manually refresh environment variables" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To test, run: ant -version" -ForegroundColor Cyan
    } else {
        throw "Ant verification failed"
    }
} catch {
    Write-Host "WARNING: Could not verify Ant installation automatically." -ForegroundColor Yellow
    Write-Host "You may need to restart your terminal and run: ant -version" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Installation script completed." -ForegroundColor Green

