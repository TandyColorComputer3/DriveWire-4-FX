# ⚠️ JavaFX Required

## Build Error: JavaFX Not Found

The DriveWire4 JavaFX migration **requires JavaFX SDK** to compile. Your current JDK (Temurin OpenJDK) does not include JavaFX.

## Quick Fix: Install Liberica JDK

**This is the easiest solution:**

1. **Download Liberica JDK 21 Full** (includes JavaFX):
   - Go to: **https://bell-sw.com/pages/downloads/**
   - Select: **JDK 21 Full** (NOT Standard - you need "Full" which includes JavaFX)
   - Download and install

2. **Set JAVA_HOME**:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
   ```

3. **Verify JavaFX**:
   ```powershell
   java --list-modules | Select-String "javafx"
   ```
   Should show: `javafx.controls`, `javafx.fxml`, `javafx.web`, etc.

4. **Build**:
   ```powershell
   cd C:\Users\trellen\drivewire4-2\drivewire4_from_source\drivewireserver-git\DriveWireUI
   ant create_run_jar
   ```

5. **Run**:
   ```powershell
   run-javafx.bat
   ```

## Alternative: Download JavaFX SDK Separately

If you want to keep Temurin JDK:

1. Download JavaFX SDK 21 from: **https://openjfx.io/**
2. Extract to `C:\javafx-sdk-21`
3. Build with:
   ```powershell
   ant -Djavafx.sdk.path=C:/javafx-sdk-21/lib create_run_jar
   ```

## Why This Happens

JavaFX was removed from the JDK starting with Java 11. Most JDK distributions (like Temurin, Adoptium) don't include it. You need either:
- A JDK that includes JavaFX (Liberica Full, Zulu FX), OR
- Download JavaFX SDK separately

## Recommendation

**Install Liberica JDK 21 Full** - it's the simplest solution and works immediately.

