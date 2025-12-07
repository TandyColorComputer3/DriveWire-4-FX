# Quick Start Guide - Running DriveWire4 JavaFX UI

## The Problem

Your current JDK (Temurin OpenJDK 25) does **not** include JavaFX. JavaFX was removed from the JDK starting with Java 11 and needs to be added separately.

## Solution: Use a JDK with JavaFX Included (Easiest)

### Step 1: Download Liberica JDK (includes JavaFX)

1. Go to: https://bell-sw.com/pages/downloads/
2. Download **Liberica JDK 21 Full** (includes JavaFX)
3. Install it (e.g., to `C:\Program Files\BellSoft\LibericaJDK-21`)

### Step 2: Set JAVA_HOME

**Windows PowerShell:**
```powershell
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

**Windows Command Prompt:**
```cmd
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

### Step 3: Verify JavaFX is Available

```bash
java --list-modules | findstr javafx
```

You should see modules like:
- `javafx.base`
- `javafx.controls`
- `javafx.fxml`
- `javafx.web`

### Step 4: Build

```bash
cd drivewireserver-git\DriveWireUI
ant create_run_jar
```

### Step 5: Run

```bash
run-javafx.bat
```

Or manually:
```bash
java --add-modules javafx.controls,javafx.fxml,javafx.web ^
     -Djava.library.path=../java/native/Windows/amd64 ^
     -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
     com.groupunix.drivewireui.MainWinFX
```

## Alternative: Download JavaFX SDK Separately

If you prefer to keep your current JDK:

1. Download JavaFX 21 SDK from: https://openjfx.io/
2. Extract to `C:\javafx-sdk-21`
3. Build (will compile without modules):
   ```bash
   ant create_run_jar
   ```
4. Run with module path:
   ```bash
   java --module-path "C:\javafx-sdk-21\lib" ^
        --add-modules javafx.controls,javafx.fxml,javafx.web ^
        -Djava.library.path=../java/native/Windows/amd64 ^
        -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
        com.groupunix.drivewireui.MainWinFX
   ```

## Recommended: Use Liberica JDK

**Liberica JDK Full** is the easiest option because:
- ✅ Includes JavaFX out of the box
- ✅ No extra downloads needed
- ✅ Works immediately after installation
- ✅ Same OpenJDK base as Temurin

Download: https://bell-sw.com/pages/downloads/

