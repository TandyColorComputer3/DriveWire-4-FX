package com.groupunix.drivewireui;

import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

/**
 * Controller for the main DriveWire UI window.
 * Handles all UI interactions and updates.
 */
public class MainWindowController {
    
    @FXML private MenuBar menuBar;
    @FXML private TableView<DiskTableItem> diskTable;
    @FXML private TableColumn<DiskTableItem, Integer> ledColumn;
    @FXML private TableColumn<DiskTableItem, Integer> driveColumn;
    @FXML private TableColumn<DiskTableItem, String> fileColumn;
    @FXML private TableColumn<DiskTableItem, Integer> readsColumn;
    @FXML private TableColumn<DiskTableItem, Integer> writesColumn;
    @FXML private TabPane outputTabPane;
    @FXML private Tab logTab;
    @FXML private Tab commandTab;
    @FXML private Tab graphsTab;
    @FXML private TextArea logTextArea;
    @FXML private TextArea commandHistoryArea;
    @FXML private TextField commandTextField;
    @FXML private Canvas canvasMemUse;
    @FXML private Canvas canvasDiskOps;
    @FXML private Canvas canvasVSerialOps;
    @FXML private Label statusLabel;
    @FXML private Label serverLabel;
    @FXML private Label versionLabel;
    @FXML private CheckMenuItem hdbdosTranslationMenuItem;
    @FXML private CheckMenuItem restartClientsMenuItem;
    @FXML private CheckMenuItem useInternalServerMenuItem;
    @FXML private CheckMenuItem useRemoteFileMenuItem;
    @FXML private CheckMenuItem noBrowsersMenuItem;
    @FXML private CheckMenuItem lockInstrumentsMenuItem;
    @FXML private CheckMenuItem darkModeMenuItem;
    @FXML private MenuItem configEditorMenuItem;
    
    private Stage primaryStage;
    private boolean darkModeEnabled = false;
    private ObservableList<DiskTableItem> diskTableData = FXCollections.observableArrayList();
    
    /**
     * Initialize the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        System.out.println("=== MainWindowController.initialize() called ===");
        try {
            // Initialize disk table
            diskTable.setItems(diskTableData);
            System.out.println("Disk table items set");
        
        // Set up cell value factories
        ledColumn.setCellValueFactory(new PropertyValueFactory<>("led"));
        driveColumn.setCellValueFactory(new PropertyValueFactory<>("drive"));
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("file"));
        readsColumn.setCellValueFactory(new PropertyValueFactory<>("reads"));
        writesColumn.setCellValueFactory(new PropertyValueFactory<>("writes"));
        
        // Prevent column header text truncation - ensure minimum widths accommodate full text
        ledColumn.setMinWidth(65);    // "LED" needs at least 65px (with padding)
        driveColumn.setMinWidth(85);  // "Drive" needs at least 85px (with padding)
        readsColumn.setMinWidth(70);  // "Reads" needs at least 70px
        writesColumn.setMinWidth(75); // "Writes" needs at least 75px
        
        // Use UNCONSTRAINED_RESIZE_POLICY to allow manual resizing while maintaining minimum widths
        diskTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // LED column uses custom cell factory for image display - center aligned
        ledColumn.setCellFactory(column -> new TableCell<DiskTableItem, Integer>() {
            private final ImageView imageView = new ImageView();
            
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // LED state: 0=dark, 1=green, 2=red
                    Image image = null;
                    if (item == 1) {
                        image = loadImage("/disk/diskdrive-ledgreen.png");
                    } else if (item == 2) {
                        image = loadImage("/disk/diskdrive-ledred.png");
                    } else {
                        image = loadImage("/disk/diskdrive-leddark.png");
                    }
                    if (image != null) {
                        imageView.setImage(image);
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        // Drive column - center aligned text
        driveColumn.setCellFactory(column -> new TableCell<DiskTableItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });
        
        // File column - center aligned text
        fileColumn.setCellFactory(column -> new TableCell<DiskTableItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });
        
        // Reads column - center aligned text
        readsColumn.setCellFactory(column -> new TableCell<DiskTableItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });
        
        // Writes column - center aligned text
        writesColumn.setCellFactory(column -> new TableCell<DiskTableItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });
        
        // Handle disk table double-click
        diskTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleDiskDoubleClick();
            }
        });
        
        // Handle disk table selection
        diskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                MainWin.currentDisk = newSelection.getDrive();
                MainWin.sdisk = newSelection.getDrive();
            }
        });
        
        // Initialize version label
        versionLabel.setText("DriveWire " + MainWin.DWUIVersion.toString());
        
        // Initialize status
        updateConnectionStatus(false);
        
        // Initialize menu states
        updateMenuStates();
        
        // Load dark mode preference
        loadDarkModePreference();
        
        // Start disk table updater thread
        if (MainWin.diskTableUpdater == null) {
            MainWin.diskTableUpdater = new DiskTableUpdateThread();
            Thread dtuThread = new Thread(MainWin.diskTableUpdater);
            dtuThread.setDaemon(true);
            dtuThread.start();
        }
        
        // Start JavaFX grapher thread if canvases are available
        if (canvasMemUse != null && canvasDiskOps != null && canvasVSerialOps != null) {
            GrapherThreadFX grapher = new GrapherThreadFX();
            grapher.setCanvases(canvasMemUse, canvasDiskOps, canvasVSerialOps);
            Thread grapherThread = new Thread(grapher);
            grapherThread.setDaemon(true);
            grapherThread.setName("GrapherThreadFX");
            grapherThread.start();
            System.out.println("JavaFX GrapherThread started - waiting for server connection...");
        } else {
            System.err.println("WARNING: Graph canvases not available - graphs will not be displayed");
        }
        
        // Initialize disk table with empty entries
        for (int i = 0; i < 256; i++) {
            diskTableData.add(new DiskTableItem(i, "", 0, 0, 0));
        }
        System.out.println("Disk table entries initialized");
        
        // Load disk paths from config (restore disks from previous session)
        loadDiskPathsFromConfig();
        
        System.out.println("=== MainWindowController.initialize() completed ===");
        } catch (Exception e) {
            System.err.println("Exception in MainWindowController.initialize(): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MainWindowController", e);
        }
    }
    
    /**
     * Set the primary stage for this controller.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    
    /**
     * Update the window title (called from MainWin.updateTitlebar()).
     */
    public void updateTitle(String title) {
        if (primaryStage != null) {
            Platform.runLater(() -> {
                primaryStage.setTitle(title);
            });
        }
    }
    
    /**
     * Update the connection status display.
     */
    public void updateConnectionStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusLabel.setText("Connected");
                statusLabel.getStyleClass().removeAll("status-disconnected");
                statusLabel.getStyleClass().add("status-connected");
                serverLabel.setText("Server: " + MainWin.host + ":" + MainWin.port + " (Instance " + MainWin.instance + ")");
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.getStyleClass().removeAll("status-connected");
                statusLabel.getStyleClass().add("status-disconnected");
                serverLabel.setText("Server: Not connected");
            }
        });
    }
    
    /**
     * Update menu states based on current configuration.
     */
    private void updateMenuStates() {
        if (MainWin.config != null) {
            // Try to get instance config, but fall back to main config if null
            org.apache.commons.configuration.HierarchicalConfiguration instanceConfig = MainWin.getInstanceConfig();
            if (instanceConfig == null) {
                System.err.println("WARNING: MainWin.getInstanceConfig() returned null, using MainWin.config");
                instanceConfig = MainWin.config;
            }
            
            hdbdosTranslationMenuItem.setSelected(
                instanceConfig.getBoolean("HDBDOSMode", false));
            restartClientsMenuItem.setSelected(
                instanceConfig.getBoolean("RestartClientsOnOpen", false));
            useInternalServerMenuItem.setSelected(
                MainWin.config.getBoolean("LocalServer", false));
            useRemoteFileMenuItem.setSelected(
                MainWin.config.getBoolean("UseRemoteFilebrowser", false));
            noBrowsersMenuItem.setSelected(
                MainWin.config.getBoolean("NoBrowsers", false));
            
            // Enable config editor when connected
            configEditorMenuItem.setDisable(!MainWin.connected);
        } else {
            System.err.println("WARNING: MainWin.config is null, cannot update menu states");
        }
    }
    
    /**
     * Refresh the disk table display.
     */
    public void refreshDiskTable() {
        Platform.runLater(() -> {
            synchronized (diskTableData) {
                for (int i = 0; i < 256; i++) {
                    DiskTableItem item = diskTableData.get(i);
                    if (MainWin.disks[i] != null && MainWin.disks[i].isLoaded()) {
                        item.setFile(MainWin.disks[i].getPath());
                        item.setReads(Integer.parseInt(MainWin.disks[i].getParam("_reads").toString()));
                        item.setWrites(Integer.parseInt(MainWin.disks[i].getParam("_writes").toString()));
                    } else {
                        item.setFile("");
                        item.setReads(0);
                        item.setWrites(0);
                    }
                }
            }
        });
    }
    
    /**
     * Update a specific disk table entry.
     */
    public void updateDiskTableEntry(int disk, String key, Object value) {
        Platform.runLater(() -> {
            if (disk >= 0 && disk < diskTableData.size()) {
                DiskTableItem item = diskTableData.get(disk);
                if (item == null) {
                    System.err.println("Warning: DiskTableItem is null for disk " + disk);
                    return;
                }
                
                try {
                    if (key.equals("LED")) {
                        // LED is handled via integer state (0=dark, 1=green, 2=red)
                        // The cell factory will handle the image display
                        if (value instanceof Integer) {
                            item.setLed((Integer)value);
                        } else if (value != null) {
                            // Try to determine LED state from value
                            String valStr = value.toString();
                            if (valStr.contains("green") || valStr.equals("1")) {
                                item.setLed(1);
                            } else if (valStr.contains("red") || valStr.equals("2")) {
                                item.setLed(2);
                            } else {
                                item.setLed(0);
                            }
                        }
                    } else if (key.equals("_reads")) {
                        try {
                            item.setReads(Integer.parseInt(value.toString()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    } else if (key.equals("_writes")) {
                        try {
                            item.setWrites(Integer.parseInt(value.toString()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    } else if (key.equals("*insert")) {
                        // Extract filename from URI/path
                        String filePath = value.toString();
                        String filename = UIUtils.getFilenameFromURI(filePath);
                        
                        // Ensure drive number is set (should already be set, but ensure it)
                        item.setDrive(disk);
                        
                        // Set the filename - this is the main visual indicator
                        item.setFile(filename);
                        
                        // Reset reads/writes when disk is inserted
                        item.setReads(0);
                        item.setWrites(0);
                        
                        // LED will be set to 0 by the separate LED update, but set it here too for immediate feedback
                        item.setLed(0);
                        
                        System.out.println("Disk " + disk + " inserted: " + filename + " (from " + filePath + ")");
                        System.out.println("  -> Drive: " + item.getDrive() + ", File: " + item.getFile() + ", Reads: " + item.getReads() + ", Writes: " + item.getWrites() + ", LED: " + item.getLed());
                        
                        // Force table refresh to ensure UI updates immediately
                        diskTable.refresh();
                    } else if (key.equals("*eject")) {
                        item.setFile("");
                        item.setReads(0);
                        item.setWrites(0);
                        item.setLed(0);
                        System.out.println("Disk " + disk + " ejected");
                        // Force table refresh to ensure UI updates
                        diskTable.refresh();
                    } else if (key.equals("File")) {
                        // Direct file name update (may come after *insert)
                        String filePath = value.toString();
                        String filename = UIUtils.getFilenameFromURI(filePath);
                        item.setFile(filename);
                        System.out.println("Disk " + disk + " file updated: " + filename);
                        // Force table refresh to ensure UI updates
                        diskTable.refresh();
                    } else if (key.equals("Drive")) {
                        // Drive number update
                        try {
                            item.setDrive(Integer.parseInt(value.toString()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    } else if (key.equals("Location")) {
                        // Location update - we don't display this in the table, but log it
                        System.out.println("Disk " + disk + " location: " + value.toString());
                    }
                } catch (Exception e) {
                    System.err.println("Error updating disk table entry for disk " + disk + ", key " + key + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Warning: Invalid disk number " + disk + " (valid range: 0-" + (diskTableData.size() - 1) + ")");
            }
        });
    }
    
    /**
     * Append text to the log area.
     */
    public void appendLog(String text) {
        Platform.runLater(() -> {
            logTextArea.appendText(text + "\n");
            // Auto-scroll to bottom
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Append text to the command history area.
     */
    public void appendCommandHistory(String text) {
        Platform.runLater(() -> {
            commandHistoryArea.appendText(text + "\n");
            commandHistoryArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    // Menu handlers
    
    @FXML
    private void handleChooseServer() {
        ChooseServerDialogController.showDialog(primaryStage);
    }
    
    @FXML
    private void handleExit() {
        MainWin.doShutdown();
        Platform.exit();
    }
    
    @FXML
    private void handleEjectAllDisks() {
        MainWin.sendCommand("dw disk eject all");
    }
    
    @FXML
    private void handleToggleAppendMode() {
        MainWin.append_mode = !MainWin.append_mode;
        // Update UI to reflect append mode state
    }
    
    @FXML
    private void handleHdbdosTranslation() {
        boolean enabled = hdbdosTranslationMenuItem.isSelected();
        MainWin.sendCommand("dw config set HDBDOSMode " + enabled);
    }
    
    @FXML
    private void handleRestartClients() {
        boolean enabled = restartClientsMenuItem.isSelected();
        try {
            org.apache.commons.configuration.HierarchicalConfiguration instanceConfig = MainWin.getInstanceConfig();
            if (instanceConfig != null) {
                instanceConfig.setProperty("RestartClientsOnOpen", enabled);
                // Try to save if it's an XMLConfiguration
                if (instanceConfig instanceof org.apache.commons.configuration.XMLConfiguration) {
                    ((org.apache.commons.configuration.XMLConfiguration) instanceConfig).save();
                }
            } else if (MainWin.config != null) {
                // Fallback: save to MainWin.config
                MainWin.config.setProperty("RestartClientsOnOpen", enabled);
                if (MainWin.config instanceof org.apache.commons.configuration.XMLConfiguration) {
                    ((org.apache.commons.configuration.XMLConfiguration) MainWin.config).save();
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not save RestartClientsOnOpen setting: " + e.getMessage());
            e.printStackTrace();
            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Could not save setting");
            alert.setContentText("Failed to save 'Restart Clients on Open' setting: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleSimpleConfigWizard() {
        SimpleWizardFX wizard = new SimpleWizardFX(primaryStage);
        wizard.show();
    }
    
    @FXML
    private void handleConfigEditor() {
        // TODO: Open ConfigEditor dialog
    }
    
    @FXML
    private void handleConfigManager() {
        ConfigManagerFX manager = new ConfigManagerFX(primaryStage);
        manager.show();
    }
    
    @FXML
    private void handleUseInternalServer() {
        boolean enabled = useInternalServerMenuItem.isSelected();
        MainWin.config.setProperty("LocalServer", enabled);
        if (enabled) {
            MainWin.startDWServer(null);
        } else {
            MainWin.stopDWServer();
        }
    }
    
    @FXML
    private void handleUseRemoteFile() {
        boolean enabled = useRemoteFileMenuItem.isSelected();
        MainWin.config.setProperty("UseRemoteFilebrowser", enabled);
    }
    
    @FXML
    private void handleNoBrowsers() {
        boolean enabled = noBrowsersMenuItem.isSelected();
        MainWin.config.setProperty("NoBrowsers", enabled);
    }
    
    @FXML
    private void handleLoadSoundbank() {
        // TODO: Open file chooser for soundbank
    }
    
    @FXML
    private void handleLockInstruments() {
        MainWin.sendCommand("dw midi synth lock");
    }
    
    @FXML
    private void handleAbout() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("About DriveWire");
        dialog.initOwner(primaryStage);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        
        // Add a Close button so the dialog can be closed
        javafx.scene.control.ButtonType closeButton = new javafx.scene.control.ButtonType("Close", 
            javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/groupunix/drivewireui/fxml/AboutDialog.fxml"));
            javafx.scene.control.DialogPane dialogPane = loader.load();
            
            javafx.scene.control.Label versionLabel = (javafx.scene.control.Label) 
                dialogPane.lookup("#versionLabel");
            javafx.scene.control.Label dateLabel = (javafx.scene.control.Label) 
                dialogPane.lookup("#dateLabel");
            javafx.scene.control.TextArea thanksArea = (javafx.scene.control.TextArea) 
                dialogPane.lookup("#thanksTextArea");
            
            if (versionLabel != null) {
                versionLabel.setText("Version " + MainWin.DWUIVersion.toString());
            }
            if (dateLabel != null) {
                dateLabel.setText("Date: " + MainWin.DWUIVersionDate);
            }
            if (thanksArea != null) {
                thanksArea.setText("Cloud-9\n#coco_chat\nMalted Media\nThe Glenside Color Computer Club\n" +
                    "Darren Atkinson\nBoisy Pitre\nJohn Linville\nRandomRodder\nlorddragon\nlostwizard\n" +
                    "beretta\nGary Becker\nJim Hathaway\nGene Heskett\nWayne Campbell\nStephen Fischer\n" +
                    "Christopher Hawks\nJohn Orwen\n\nAnd apologies to any I forgot!");
            }
            
            // Ensure the dialog pane has a close button
            if (!dialogPane.getButtonTypes().contains(closeButton)) {
                dialogPane.getButtonTypes().add(closeButton);
            }
            
            dialog.setDialogPane(dialogPane);
            
            // Make sure the close button works
            javafx.scene.control.Button closeBtn = (javafx.scene.control.Button) dialogPane.lookupButton(closeButton);
            if (closeBtn != null) {
                closeBtn.setOnAction(e -> dialog.close());
            }
            
            // Also handle window close (X button)
            dialog.setOnCloseRequest(e -> dialog.close());
            
            dialog.showAndWait();
        } catch (Exception e) {
            // Fallback to simple alert
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("About DriveWire");
            alert.setHeaderText("DriveWire " + MainWin.DWUIVersion.toString());
            alert.setContentText("DriveWire User Interface\nVersion: " + MainWin.DWUIVersion.toString() + 
                               "\nDate: " + MainWin.DWUIVersionDate);
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleDarkMode() {
        boolean enabled = darkModeMenuItem.isSelected();
        darkModeEnabled = enabled;
        applyDarkMode(enabled);
        saveDarkModePreference(enabled);
    }
    
    /**
     * Load dark mode preference from configuration.
     */
    private void loadDarkModePreference() {
        try {
            org.apache.commons.configuration.HierarchicalConfiguration instanceConfig = MainWin.getInstanceConfig();
            boolean darkMode = false;
            if (instanceConfig != null) {
                darkMode = instanceConfig.getBoolean("DarkMode", false);
            } else if (MainWin.config != null) {
                darkMode = MainWin.config.getBoolean("DarkMode", false);
            }
            darkModeEnabled = darkMode;
            darkModeMenuItem.setSelected(darkMode);
            applyDarkMode(darkMode);
        } catch (Exception e) {
            System.err.println("Warning: Could not load dark mode preference: " + e.getMessage());
        }
    }
    
    /**
     * Save dark mode preference to configuration.
     */
    private void saveDarkModePreference(boolean enabled) {
        try {
            org.apache.commons.configuration.HierarchicalConfiguration instanceConfig = MainWin.getInstanceConfig();
            if (instanceConfig != null) {
                instanceConfig.setProperty("DarkMode", enabled);
                if (instanceConfig instanceof org.apache.commons.configuration.XMLConfiguration) {
                    ((org.apache.commons.configuration.XMLConfiguration) instanceConfig).save();
                }
            } else if (MainWin.config != null) {
                MainWin.config.setProperty("DarkMode", enabled);
                if (MainWin.config instanceof org.apache.commons.configuration.XMLConfiguration) {
                    ((org.apache.commons.configuration.XMLConfiguration) MainWin.config).save();
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not save dark mode preference: " + e.getMessage());
        }
    }
    
    /**
     * Apply dark mode stylesheet to the scene.
     */
    private void applyDarkMode(boolean enabled) {
        if (primaryStage == null || primaryStage.getScene() == null) {
            return;
        }
        
        Platform.runLater(() -> {
            javafx.scene.Scene scene = primaryStage.getScene();
            java.util.List<String> stylesheets = scene.getStylesheets();
            
            // Get dark mode stylesheet URL
            java.net.URL darkModeUrl = getClass().getResource("/com/groupunix/drivewireui/css/dark-mode.css");
            if (darkModeUrl == null) {
                System.err.println("Warning: Dark mode CSS file not found");
                return;
            }
            
            String darkModeStylesheet = darkModeUrl.toExternalForm();
            
            // Remove existing dark mode stylesheet if present
            stylesheets.remove(darkModeStylesheet);
            
            if (enabled) {
                // Add dark mode stylesheet
                stylesheets.add(darkModeStylesheet);
                System.out.println("Dark mode enabled");
            } else {
                System.out.println("Dark mode disabled");
            }
        });
    }
    
    // Disk table handlers
    
    @FXML
    private void handleDiskDoubleClick() {
        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int drive = selected.getDrive();
            if (MainWin.disks[drive] != null && MainWin.disks[drive].isLoaded()) {
                // Open disk window
                // TODO: Open DiskWin dialog (JavaFX version)
            } else {
                // Quick insert disk
                quickInDiskFX(drive);
            }
        }
    }
    
    @FXML
    private void handleInsertDisk() {
        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            quickInDiskFX(selected.getDrive());
        }
    }
    
    /**
     * JavaFX-compatible version of quickInDisk.
     */
    private void quickInDiskFX(int diskno) {
        String curpath = "";
        if ((diskno > -1) && (diskno < MainWin.disks.length) && 
            MainWin.disks[diskno] != null && MainWin.disks[diskno].isLoaded()) {
            curpath = MainWin.disks[diskno].getPath();
        }
        
        // Use JavaFX file chooser
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose an image for drive " + diskno);
        
        // Try to set initial directory from last used directory or current disk path
        java.io.File initialDir = null;
        if (!curpath.isEmpty()) {
            java.io.File initialFile = new java.io.File(curpath);
            if (initialFile.exists()) {
                initialDir = initialFile.getParentFile();
                fileChooser.setInitialFileName(initialFile.getName());
            }
        }
        
        // If no current path, use last remembered directory
        if (initialDir == null && MainWin.config != null) {
            String lastDir = MainWin.config.getString("LastDiskDirectory", null);
            if (lastDir != null && !lastDir.isEmpty()) {
                java.io.File lastDirFile = new java.io.File(lastDir);
                if (lastDirFile.exists() && lastDirFile.isDirectory()) {
                    initialDir = lastDirFile;
                }
            }
        }
        
        if (initialDir != null) {
            fileChooser.setInitialDirectory(initialDir);
        }
        
        // Set file filters based on supported extensions
        // TODO: Get supported extensions from DWImageMounter
        javafx.stage.FileChooser.ExtensionFilter allFiles = 
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*");
        javafx.stage.FileChooser.ExtensionFilter diskFiles = 
            new javafx.stage.FileChooser.ExtensionFilter("Disk Images", "*.dsk", "*.vdk", "*.os9");
        fileChooser.getExtensionFilters().addAll(diskFiles, allFiles);
        
        java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            String path = selectedFile.getAbsolutePath();
            
            // Save last directory used
            if (MainWin.config != null) {
                java.io.File selectedDir = selectedFile.getParentFile();
                if (selectedDir != null && selectedDir.exists()) {
                    try {
                        MainWin.config.setProperty("LastDiskDirectory", selectedDir.getAbsolutePath());
                        MainWin.config.save();
                    } catch (Exception e) {
                        System.err.println("Failed to save last directory: " + e.getMessage());
                    }
                }
            }
            
            // Send command directly to server (bypassing SWT-based DWImageMounter for now)
            // TODO: Create JavaFX-compatible version of DWImageMounter
            MainWin.sendCommand("dw disk insert " + diskno + " " + path);
            
            // Save disk path to config for persistence
            saveDiskPathToConfig(diskno, path);
            
            // Refresh disk status after insert (server may not send sync events immediately)
            refreshDiskStatusAfterInsert(diskno, path);
        }
    }
    
    @FXML
    private void handleEjectDisk() {
        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int diskno = selected.getDrive();
            MainWin.sendCommand("dw disk eject " + diskno);
            
            // Refresh disk status after eject
            refreshDiskStatusAfterEject(diskno);
        }
    }
    
    /**
     * Refresh disk status after insert by updating UI immediately.
     * We don't query the server to avoid native library loading issues.
     * The sync protocol will eventually update reads/writes/LED.
     */
    private void refreshDiskStatusAfterInsert(int diskno, String filePath) {
        // Extract filename immediately
        final String filename = UIUtils.getFilenameFromURI(filePath);
        
        // Save disk path to config for persistence
        saveDiskPathToConfig(diskno, filePath);
        
        // Update UI immediately on JavaFX thread (no server query to avoid crashes)
        Platform.runLater(() -> {
            try {
                if (diskno >= 0 && diskno < diskTableData.size()) {
                    DiskTableItem item = diskTableData.get(diskno);
                    if (item != null) {
                        item.setDrive(diskno);
                        item.setFile(filename);
                        item.setReads(0); // Will be updated by sync protocol
                        item.setWrites(0); // Will be updated by sync protocol
                        item.setLed(0); // Reset LED on insert
                        
                        System.out.println("Updated disk " + diskno + " filename after insert: " + filename + " (reads/writes will update via sync protocol)");
                        diskTable.refresh();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating disk table after insert: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Save disk path to configuration for persistence across sessions.
     */
    private void saveDiskPathToConfig(int diskno, String filePath) {
        if (MainWin.config != null && filePath != null && !filePath.isEmpty()) {
            try {
                MainWin.config.setProperty("DiskPath_" + diskno, filePath);
                MainWin.config.save();
                System.out.println("Saved disk " + diskno + " path to config: " + filePath);
            } catch (Exception e) {
                System.err.println("Failed to save disk path to config: " + e.getMessage());
            }
        }
    }
    
    /**
     * Load disk paths from configuration and restore them.
     */
    private void loadDiskPathsFromConfig() {
        if (MainWin.config == null) {
            return;
        }
        
        // Load disk paths in background thread to avoid blocking UI
        javafx.concurrent.Task<Void> loadTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() {
                try {
                    // Wait for server connection
                    int waitCount = 0;
                    while ((MainWin.host == null || MainWin.port <= 0) && waitCount < 50) {
                        Thread.sleep(100);
                        waitCount++;
                    }
                    
                    // Small additional delay to ensure server is ready
                    Thread.sleep(500);
                    
                    // Load each disk path from config
                    for (int i = 0; i < 256; i++) {
                        String diskPath = MainWin.config.getString("DiskPath_" + i, null);
                        if (diskPath != null && !diskPath.isEmpty()) {
                            java.io.File diskFile = new java.io.File(diskPath);
                            if (diskFile.exists()) {
                                final int diskno = i;
                                final String path = diskPath;
                                
                                // Send insert command to server
                                MainWin.sendCommand("dw disk insert " + diskno + " " + path);
                                
                                // Update UI
                                Platform.runLater(() -> {
                                    String filename = UIUtils.getFilenameFromURI(path);
                                    if (diskno >= 0 && diskno < diskTableData.size()) {
                                        DiskTableItem item = diskTableData.get(diskno);
                                        if (item != null) {
                                            item.setDrive(diskno);
                                            item.setFile(filename);
                                            item.setReads(0);
                                            item.setWrites(0);
                                            item.setLed(0);
                                            diskTable.refresh();
                                        }
                                    }
                                });
                                
                                // Small delay between disk inserts
                                Thread.sleep(200);
                            } else {
                                // File doesn't exist, remove from config
                                MainWin.config.clearProperty("DiskPath_" + i);
                            }
                        }
                    }
                    
                    // Save config after cleanup
                    if (MainWin.config != null) {
                        MainWin.config.save();
                    }
                    
                    System.out.println("Loaded disk paths from config");
                } catch (Exception e) {
                    System.err.println("Error loading disk paths from config: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    /**
     * Refresh disk status after eject by clearing the entry.
     */
    private void refreshDiskStatusAfterEject(int diskno) {
        // Remove disk path from config
        if (MainWin.config != null) {
            try {
                MainWin.config.clearProperty("DiskPath_" + diskno);
                MainWin.config.save();
                System.out.println("Removed disk " + diskno + " path from config");
            } catch (Exception e) {
                System.err.println("Failed to remove disk path from config: " + e.getMessage());
            }
        }
        
        // Run in background thread
        javafx.concurrent.Task<Void> refreshTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() {
                try {
                    // Wait a moment for server to process the eject
                    Thread.sleep(500);
                    
                    // Clear disk table entry on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            if (diskno >= 0 && diskno < diskTableData.size()) {
                                DiskTableItem item = diskTableData.get(diskno);
                                if (item != null) {
                                    item.setFile("");
                                    item.setReads(0);
                                    item.setWrites(0);
                                    item.setLed(0);
                                    
                                    System.out.println("Cleared disk " + diskno + " after eject");
                                    diskTable.refresh();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error updating disk table in JavaFX thread: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Disk eject refresh thread interrupted");
                } catch (Exception e) {
                    System.err.println("Error refreshing disk status after eject: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        
        // Set exception handler for the task
        refreshTask.setOnFailed(event -> {
            Throwable e = refreshTask.getException();
            if (e != null) {
                System.err.println("Task failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        Thread refreshThread = new Thread(refreshTask);
        refreshThread.setDaemon(true);
        refreshThread.setUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in disk eject refresh thread: " + exception.getMessage());
            exception.printStackTrace();
        });
        refreshThread.start();
    }
    
    @FXML
    private void handleDiskProperties() {
        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
        if (selected != null && MainWin.disks[selected.getDrive()] != null) {
            // TODO: Open DiskAdvancedWin dialog
        }
    }
    
    // Command handlers
    
    @FXML
    private void handleSendCommand() {
        String command = commandTextField.getText().trim();
        if (!command.isEmpty()) {
            MainWin.sendCommand(command);
            appendCommandHistory(command);
            MainWin.addCommandToHistory(command);
            commandTextField.clear();
        }
    }
    
    // Helper methods
    
    private Image loadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Data model for disk table items.
     */
    public static class DiskTableItem {
        private final javafx.beans.property.IntegerProperty drive;
        private final javafx.beans.property.StringProperty file;
        private final javafx.beans.property.IntegerProperty reads;
        private final javafx.beans.property.IntegerProperty writes;
        private final javafx.beans.property.IntegerProperty led;
        
        public DiskTableItem(int drive, String file, int reads, int writes, int led) {
            this.drive = new javafx.beans.property.SimpleIntegerProperty(drive);
            this.file = new javafx.beans.property.SimpleStringProperty(file);
            this.reads = new javafx.beans.property.SimpleIntegerProperty(reads);
            this.writes = new javafx.beans.property.SimpleIntegerProperty(writes);
            this.led = new javafx.beans.property.SimpleIntegerProperty(led);
        }
        
        public int getDrive() { return drive.get(); }
        public void setDrive(int value) { drive.set(value); }
        public javafx.beans.property.IntegerProperty driveProperty() { return drive; }
        
        public String getFile() { return file.get(); }
        public void setFile(String value) { file.set(value); }
        public javafx.beans.property.StringProperty fileProperty() { return file; }
        
        public int getReads() { return reads.get(); }
        public void setReads(int value) { reads.set(value); }
        public javafx.beans.property.IntegerProperty readsProperty() { return reads; }
        
        public int getWrites() { return writes.get(); }
        public void setWrites(int value) { writes.set(value); }
        public javafx.beans.property.IntegerProperty writesProperty() { return writes; }
        
        public int getLed() { return led.get(); }
        public void setLed(int value) { led.set(value); }
        public javafx.beans.property.IntegerProperty ledProperty() { return led; }
    }
}

