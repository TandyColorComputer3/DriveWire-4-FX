# DriveWire 4 FX

## Technical Fixes

- ✅ Migrated UI from Eclipse SWT to JavaFX 21
- ✅ Fixed threading issues: replaced `Display.syncExec()` with `Platform.runLater()` for JavaFX compatibility
- ✅ Fixed `NullPointerException` crashes in `addToServerLog()`, `applyMIDIStatus()`, and `doShutdown()`
- ✅ Implemented disk persistence: mounted disk images remembered across application restarts
- ✅ File picker remembers last used directory
- ✅ Fixed disk table updates: LED, reads, and writes now update in real-time
- ✅ UI layout persistence: split pane divider position and column widths saved/restored
- ✅ Fixed disk properties dialog: displays all disk parameters with dynamic updates
- ✅ Connection management: disconnect/reconnect to different DriveWire servers
- ✅ Fixed command console: `dw` command and subcommands now work correctly
- ✅ Fixed port normalization: SyncThread uses default port (6800) when port is 0 or invalid
- ✅ HDB-DOS translation mode persistence
- ✅ View mode toggle (Dashboard/Advanced) with persistence
- ✅ Fixed status bar display: shows "Client: IP:port" when connected
- ✅ Fixed file column display: shows only filename, full path in status bar
- ✅ Center-justified table columns (LED, Drive, File, Reads, Writes)
- ✅ Fixed TCP device connection blocking: added timeout to `ServerSocket.accept()`
- ✅ Fixed connection pool management: improved reuse of closed/stale connections
- ✅ Fixed `NullPointerException` in connection announcement when `porthandler` is null

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
