# DriveWire4 JavaFX UI - Quick Reference

## Current Status

✅ **JavaFX migration complete** - Core UI migrated from SWT to JavaFX  
⚠️ **JavaFX SDK required** - Your JDK doesn't include JavaFX

## Quick Start (3 Steps)

### 1. Install Liberica JDK (includes JavaFX)
Download from: **https://bell-sw.com/pages/downloads/**
- Choose: **JDK 21 Full** (includes JavaFX)
- Install it

### 2. Set JAVA_HOME
```powershell
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-21"
```

### 3. Build and Run
```bash
cd drivewireserver-git\DriveWireUI
ant create_run_jar
run-javafx.bat
```

## What Was Migrated

- ✅ Main window (FXML + Controller)
- ✅ Browser component (WebView)
- ✅ Key dialogs (About, ChooseServer, Error)
- ✅ Threading model (Platform.runLater)
- ✅ CSS styling

## Files Created

- `MainWinFX.java` - JavaFX entry point
- `MainWindowController.java` - Main window controller  
- `BrowserController.java` - Browser controller
- `fxml/MainWindow.fxml` - Main window layout
- `fxml/BrowserView.fxml` - Browser layout
- `css/application.css` - Stylesheet

## Need Help?

See `HOW_TO_RUN.md` for detailed instructions.

