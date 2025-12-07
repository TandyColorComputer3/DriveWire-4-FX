# How to Run DriveWire4 JavaFX UI

## Prerequisites

### Option 1: Use a JDK with JavaFX Included (Recommended)

Download a JDK distribution that includes JavaFX:
- **Liberica JDK**: https://bell-sw.com/pages/downloads/ (includes JavaFX)
- **Azul Zulu JDK FX**: https://www.azul.com/downloads/?package=jdk-fx (includes JavaFX)
- **Amazon Corretto**: Does NOT include JavaFX

### Option 2: Download JavaFX SDK Separately

1. Download JavaFX 21 SDK from: https://openjfx.io/
2. Extract to a directory (e.g., `C:\javafx-sdk-21`)
3. Update the build and run scripts to include the JavaFX module path

## Building

### Quick Build
```bash
cd drivewireserver-git/DriveWireUI
ant create_run_jar
```

### If JavaFX Modules Not Found

If you get "module not found" errors during compilation, you have two options:

#### Option A: Compile without modules (add JavaFX JARs to classpath)

1. Download JavaFX SDK from https://openjfx.io/
2. Extract the `lib` folder from the JavaFX SDK
3. Copy JavaFX JARs to `DriveWireUI/lib/javafx/`:
   - `javafx-base.jar`
   - `javafx-controls.jar`
   - `javafx-fxml.jar`
   - `javafx-graphics.jar`
   - `javafx-web.jar`
4. Update `build.xml` to include these JARs in the classpath instead of using modules

#### Option B: Use module path (recommended)

1. Download JavaFX SDK from https://openjfx.io/
2. Extract to `C:\javafx-sdk-21` (or your preferred location)
3. Update `build.xml` to add `--module-path` pointing to JavaFX SDK

## Running

### Method 1: Using the Batch Script (Windows)

```bash
cd drivewireserver-git/DriveWireUI
run-javafx.bat
```

### Method 2: Manual Command Line

#### If JavaFX is in JDK (Liberica/Zulu):
```bash
java --add-modules javafx.controls,javafx.fxml,javafx.web ^
     -Djava.library.path=../java/native/Windows/amd64 ^
     -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
     com.groupunix.drivewireui.MainWinFX
```

#### If JavaFX SDK is separate:
```bash
java --module-path "C:\javafx-sdk-21\lib" ^
     --add-modules javafx.controls,javafx.fxml,javafx.web ^
     -Djava.library.path=../java/native/Windows/amd64 ^
     -cp "DriveWireUI.jar;lib/*;../java/lib/*" ^
     com.groupunix.drivewireui.MainWinFX
```

### Method 3: Direct JAR Execution

If the manifest is set up correctly:
```bash
java -jar DriveWireUI.jar
```

## Troubleshooting

### "module not found: javafx.controls"
- **Solution**: Download JavaFX SDK from https://openjfx.io/ and add `--module-path` pointing to the SDK's `lib` folder

### "Could not find or load main class"
- **Solution**: Make sure you're running from the `DriveWireUI` directory and the JAR was built successfully

### "Native library not found" (RXTX)
- **Solution**: Make sure `-Djava.library.path` points to the correct native library directory:
  - Windows: `../java/native/Windows/amd64`
  - Linux: `../java/native/Linux/x86_64`
  - macOS: `../java/native/MacOS/x86_64`

### Application starts but UI doesn't appear
- **Check**: Look for errors in the console output
- **Check**: Make sure FXML files are included in the JAR (they should be automatically included by the build)

## Quick Start (Recommended)

1. **Download Liberica JDK 21** (includes JavaFX): https://bell-sw.com/pages/downloads/
2. **Install and set JAVA_HOME** to point to Liberica JDK
3. **Build**: `ant create_run_jar`
4. **Run**: `run-javafx.bat`

This is the easiest path as Liberica JDK includes JavaFX out of the box.

