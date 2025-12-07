# How to Run DriveWire4 JavaFX UI

## ⚠️ Important: JavaFX Required

**JavaFX is NOT included in standard OpenJDK distributions** (like Temurin). You need JavaFX to compile and run the JavaFX version.

## ✅ Easiest Solution: Use Liberica JDK

### Step 1: Download Liberica JDK Full (includes JavaFX)

1. Go to: **https://bell-sw.com/pages/downloads/**
2. Select:
   - **Version**: 21 (LTS)
   - **Package Type**: Full JDK (includes JavaFX)
   - **Operating System**: Windows
   - **Architecture**: x64
3. Download and install

### Step 2: Set JAVA_HOME to Liberica JDK

**PowerShell:**
```powershell
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

**Command Prompt:**
```cmd
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

### Step 3: Verify JavaFX is Available

```bash
java --list-modules | findstr javafx
```

You should see:
```
javafx.base
javafx.controls
javafx.fxml
javafx.graphics
javafx.web
```

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
java --add-modules javafx.controls,javafx.fxml,javafx.web -Djava.library.path=../java/native/Windows/amd64 -cp "DriveWireUI.jar;lib/*;../java/lib/*" com.groupunix.drivewireui.MainWinFX
```

---

## Alternative: Download JavaFX SDK Separately

If you want to keep your current JDK:

### Step 1: Download JavaFX SDK

1. Go to: **https://openjfx.io/**
2. Download **JavaFX 21 SDK** for Windows
3. Extract to `C:\javafx-sdk-21`

### Step 2: Update build.xml

Add this property at the top of `build.xml`:
```xml
<property name="javafx.sdk.path" value="C:/javafx-sdk-21/lib"/>
```

And update the javac task to include:
```xml
<compilerarg value="--module-path"/>
<compilerarg value="${javafx.sdk.path}"/>
<compilerarg value="--add-modules"/>
<compilerarg value="${javafx.modules}"/>
```

### Step 3: Build

```bash
ant create_run_jar
```

### Step 4: Run

```bash
java --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -Djava.library.path=../java/native/Windows/amd64 -cp "DriveWireUI.jar;lib/*;../java/lib/*" com.groupunix.drivewireui.MainWinFX
```

---

## Summary

**Recommended**: Use **Liberica JDK Full** - it's the easiest option:
- ✅ One download
- ✅ JavaFX included
- ✅ No configuration needed
- ✅ Works immediately

**Download**: https://bell-sw.com/pages/downloads/

After installing Liberica JDK and setting JAVA_HOME:
1. `ant create_run_jar` (builds the JAR)
2. `run-javafx.bat` (runs the application)

That's it! 🎉

