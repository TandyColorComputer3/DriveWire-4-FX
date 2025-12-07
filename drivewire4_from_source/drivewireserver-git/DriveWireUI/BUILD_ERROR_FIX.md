# Fixing Build Errors

## Current Error

```
error: package javafx.application does not exist
error: package javafx.fxml does not exist
... (many more JavaFX errors)
```

## Cause

**JavaFX is not available** in your current JDK (Temurin OpenJDK 25). JavaFX was removed from the JDK starting with Java 11.

## Solution: Install JavaFX

You have **two options**:

### Option 1: Install Liberica JDK (Recommended)

1. **Download**: https://bell-sw.com/pages/downloads/
   - Select: **JDK 21 Full** (includes JavaFX)
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
   ant create_run_jar
   ```

### Option 2: Download JavaFX SDK Separately

1. **Download JavaFX SDK**: https://openjfx.io/
   - Download **JavaFX 21 SDK** for Windows
   - Extract to `C:\javafx-sdk-21`

2. **Update build.xml** - Add this property at the top (after line 5):
   ```xml
   <property name="javafx.sdk.path" value="C:/javafx-sdk-21/lib"/>
   ```

3. **Build**:
   ```powershell
   ant create_run_jar
   ```

## Temporary Workaround

If you just want to test the build process without JavaFX:

```powershell
build-without-javafx.bat
```

This will build everything **except** the JavaFX files. The resulting JAR won't run the JavaFX UI, but you can verify the build process works.

## Quick Check

Run this to see if JavaFX is available:
```powershell
java --list-modules | Select-String "javafx"
```

- **If you see modules**: JavaFX is available, build should work
- **If empty**: JavaFX not available, install Liberica JDK or JavaFX SDK

## Recommendation

**Install Liberica JDK 21 Full** - it's the easiest solution:
- ✅ One download
- ✅ JavaFX included
- ✅ No configuration needed
- ✅ Works immediately

Download: https://bell-sw.com/pages/downloads/

