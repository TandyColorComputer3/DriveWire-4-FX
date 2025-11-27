@echo off
REM Run DriveWire 4 UI with proper Windows configuration

cd /d "%~dp0"

REM Set native library path for RXTX serial communication
set JAVA_LIB_PATH=../java/native/Windows/amd64

REM Use Windows-specific SWT JAR and set platform property
java -Djava.library.path=%JAVA_LIB_PATH% ^
     -Dswt.platform=win32 ^
     -cp "lib/swt_4.37_win32_x86_amd64.jar;DriveWireUI.jar;lib/*;../java/lib/*" ^
     com.groupunix.drivewireui.MainWin %*

if errorlevel 1 (
    echo.
    echo If that didn't work, try with the alternative SWT JAR:
    java -Djava.library.path=%JAVA_LIB_PATH% ^
         -cp "lib/swt_win_amd_64.jar;DriveWireUI.jar;lib/*;../java/lib/*" ^
         com.groupunix.drivewireui.MainWin %*
)

pause

