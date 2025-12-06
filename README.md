# DriveWire 4 FX

A modernized JavaFX-based user interface for DriveWire 4, bringing a fresh, responsive design to the classic CoCo disk emulation system.

## Mission

This project modernizes the DriveWire 4 UI by migrating from Eclipse SWT to JavaFX 21, providing:

- **Modern UI**: Clean, responsive interface built with JavaFX and FXML
- **Cross-Platform**: Native look and feel on Windows, macOS, and Linux
- **Dark Mode**: Complete dark theme support
- **Better UX**: Improved disk management, configuration wizards, and real-time statistics
- **Persistence**: Disks and preferences remembered across sessions

## Latest Fixes

- ✅ Complete JavaFX UI migration (SWT → JavaFX 21)
- ✅ FXML-based declarative layouts
- ✅ Modern CSS styling with dark mode
- ✅ Disk persistence (disks remembered on restart)
- ✅ File picker remembers last directory
- ✅ Real-time disk activity indicators
- ✅ Configuration wizards and managers
- ✅ Statistics graphs and monitoring

## Requirements

- **Java 21** (with JavaFX included)
- **Apache Ant** (for building)

## Quick Start

1. **Build the project:**
   ```bash
   cd drivewire4_from_source/drivewireserver-git/DriveWireUI
   ant create_run_jar
   ```

2. **Run the JavaFX UI:**
   ```bash
   .\run-javafx.bat    # Windows
   # or
   .\run-javafx.ps1   # PowerShell
   ```

## Building

The project uses Apache Ant for building. Ensure you have Java 21 installed, then:

```bash
cd drivewire4_from_source/drivewireserver-git/DriveWireUI
ant create_run_jar
```

This will create `DriveWireUI.jar` in the `DriveWireUI` directory.

## Project Structure

- `drivewire4_from_source/drivewireserver-git/DriveWireUI/src/` - JavaFX UI source code
- `drivewire4_from_source/drivewireserver-git/DriveWireUI/src/com/groupunix/drivewireui/fxml/` - FXML layouts
- `drivewire4_from_source/drivewireserver-git/DriveWireUI/src/com/groupunix/drivewireui/css/` - Stylesheets
- `drivewire4_from_source/drivewireserver-git/java/src/` - DriveWire server core

