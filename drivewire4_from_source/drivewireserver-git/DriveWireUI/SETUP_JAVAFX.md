# Setting Up JavaFX for DriveWire4

## Current Situation

You're using **Temurin OpenJDK 25**, which does **NOT** include JavaFX.

## Option 1: Install Liberica JDK (Recommended - Easiest)

### Step 1: Download Liberica JDK Full

1. Go to: **https://bell-sw.com/pages/downloads/**
2. Select:
   - **Version**: 21 (LTS) 
   - **Package Type**: **Full JDK** (this includes JavaFX!)
   - **Operating System**: Windows
   - **Architecture**: x64
3. Download the installer (`.msi` file)
4. Run the installer and install to default location: `C:\Program Files\BellSoft\LibericaJDK-21`

### Step 2: Set JAVA_HOME in PowerShell

Open PowerShell and run:

```powershell
# Set JAVA_HOME to Liberica JDK
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify it worked
java --version
```

You should see "BellSoft" or "Liberica" in the version output.

### Step 3: Verify JavaFX Modules

```powershell
java --list-modules | Select-String "javafx"
```

You should see:
```
javafx.base
javafx.controls
javafx.fxml
javafx.graphics
javafx.web
```

### Step 4: Build and Run

```powershell
cd C:\Users\trellen\drivewire4-2\drivewire4_from_source\drivewireserver-git\DriveWireUI
ant create_run_jar
run-javafx.bat
```

---

## Option 2: Download JavaFX SDK Separately

If you want to keep using Temurin JDK:

### Step 1: Download JavaFX SDK

1. Go to: **https://openjfx.io/**
2. Click "Download" → "JavaFX 21"
3. Download **Windows x64** SDK
4. Extract the ZIP file to: `C:\javafx-sdk-21`

### Step 2: Update build.xml

Add this near the top of `build.xml` (after line 5):

```xml
<!-- JavaFX SDK path (if downloaded separately) -->
<property name="javafx.sdk.path" value="C:/javafx-sdk-21/lib"/>
```

Then update the UI compilation section (around line 33) to:

```xml
<javac srcdir="src" includes="**" encoding="windows-1252"
    destdir="classes"
    source="21" target="21" nowarn="true"
    debug="true" debuglevel="lines,vars,source"
    includeantruntime="false">
  <classpath refid="project.class.path"/>
  <compilerarg value="--module-path"/>
  <compilerarg value="${javafx.sdk.path}"/>
  <compilerarg value="--add-modules"/>
  <compilerarg value="${javafx.modules}"/>
</javac>
```

### Step 3: Build

```powershell
ant create_run_jar
```

### Step 4: Run

```powershell
java --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.web -Djava.library.path=../java/native/Windows/amd64 -cp "DriveWireUI.jar;lib/*;../java/lib/*" com.groupunix.drivewireui.MainWinFX
```

---

## Quick Check: Which Option Should I Use?

- **Use Liberica JDK** if: You want the easiest setup, don't mind switching JDKs
- **Use JavaFX SDK** if: You want to keep Temurin JDK, don't mind extra configuration

**Recommendation**: Use **Liberica JDK Full** - it's simpler and works immediately.

