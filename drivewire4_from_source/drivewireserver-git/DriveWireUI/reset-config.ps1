# Reset DriveWire4 Configuration Script
# This will backup and clear configuration files

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DriveWire4 Configuration Reset" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$backupDir = "config-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
Write-Host "Backup directory: $backupDir" -ForegroundColor Yellow
Write-Host ""

# Files to backup and reset
$configFiles = @(
    "drivewireUI.xml",
    "config.xml",
    "master.xml"
)

$backedUp = @()
$notFound = @()

foreach ($file in $configFiles) {
    if (Test-Path $file) {
        $backupPath = Join-Path $backupDir $file
        Copy-Item $file $backupPath -Force
        $backedUp += $file
        Write-Host "  Backed up: $file" -ForegroundColor Green
    } else {
        $notFound += $file
        Write-Host "  Not found: $file" -ForegroundColor Gray
    }
}

Write-Host ""
if ($backedUp.Count -gt 0) {
    Write-Host "Backed up $($backedUp.Count) file(s)" -ForegroundColor Green
    
    $response = Read-Host "Delete original config files? (Y/N)"
    if ($response -eq "Y" -or $response -eq "y") {
        foreach ($file in $backedUp) {
            Remove-Item $file -Force
            Write-Host "  Deleted: $file" -ForegroundColor Yellow
        }
        Write-Host ""
        Write-Host "Configuration files deleted. New defaults will be created on next run." -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "Configuration files kept. Backup is in: $backupDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "No configuration files found to backup." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Done!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

