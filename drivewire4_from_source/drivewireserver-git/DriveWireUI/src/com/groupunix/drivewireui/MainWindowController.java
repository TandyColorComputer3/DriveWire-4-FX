package com.groupunix.drivewireui;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.HierarchicalConfiguration;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Controller for the main DriveWire UI window.
 * Handles all UI interactions and updates.
 */
public class MainWindowController {
    
    @FXML private MenuBar menuBar;
    @FXML private SplitPane splitPane;
    @FXML private TableView<DiskTableItem> diskTable;
    @FXML private ContextMenu diskContextMenu;
    @FXML private MenuItem propertiesMenuItem;
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
    @FXML private Label diskPathLabel;
    @FXML private Label versionLabel;
    @FXML private CheckMenuItem hdbdosTranslationMenuItem;
    @FXML private CheckMenuItem restartClientsMenuItem;
    @FXML private CheckMenuItem useInternalServerMenuItem;
    @FXML private CheckMenuItem useRemoteFileMenuItem;
    @FXML private CheckMenuItem noBrowsersMenuItem;
    @FXML private CheckMenuItem lockInstrumentsMenuItem;
    @FXML private CheckMenuItem darkModeMenuItem;
    @FXML private MenuItem configEditorMenuItem;
    @FXML private ToggleButton viewModeToggle;
    @FXML private VBox dashboardView;
    @FXML private VBox advancedView;
    
    private Stage primaryStage;
    private boolean darkModeEnabled = false;
    private ObservableList<DiskTableItem> diskTableData = FXCollections.observableArrayList();
    
    // Per-drive tracking for delta calculation (similar to graph approach)
    private int[] lastReads = new int[256];
    private int[] lastWrites = new int[256];
    private boolean diskActivityTrackingInitialized = false;
    
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
            
            // Ensure Properties menu item is enabled
            if (propertiesMenuItem != null) {
                propertiesMenuItem.setDisable(false);
                System.out.println("Properties menu item initialized and enabled");
            } else {
                System.err.println("WARNING: propertiesMenuItem is null!");
            }
        
        // Set up cell value factories
        ledColumn.setCellValueFactory(new PropertyValueFactory<>("led"));
        driveColumn.setCellValueFactory(new PropertyValueFactory<>("drive"));
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("file"));
        readsColumn.setCellValueFactory(new PropertyValueFactory<>("reads"));
        writesColumn.setCellValueFactory(new PropertyValueFactory<>("writes"));
        
        // Prevent column header text truncation - ensure minimum widths accommodate full text
        ledColumn.setMinWidth(65);    // "LED" needs at least 65px (with padding)
        driveColumn.setMinWidth(85);  // "Drive" needs at least 85px (with padding)
        fileColumn.setMinWidth(150);  // "File" needs at least 150px to fit filenames like "HANGMAN.DSK" without truncation
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
        
        // File column - center aligned text, always extract filename even if full path is set
        fileColumn.setCellFactory(column -> new TableCell<DiskTableItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                } else {
                    // Always extract filename, even if full path was accidentally set
                    String filename = UIUtils.getFilenameFromURI(item);
                    setText(filename);
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
            System.out.println("=== Disk selection changed ===");
            System.out.println("Old selection: " + (oldSelection != null ? "Drive " + oldSelection.getDrive() : "null"));
            System.out.println("New selection: " + (newSelection != null ? "Drive " + newSelection.getDrive() : "null"));
            System.out.println("diskPathLabel is null: " + (diskPathLabel == null));
            
            if (newSelection != null) {
                MainWin.currentDisk = newSelection.getDrive();
                MainWin.sdisk = newSelection.getDrive();
                
                // Update path label in bottom status bar
                int drive = newSelection.getDrive();
                System.out.println("Checking disk " + drive + " for path...");
                System.out.println("MainWin.disks[" + drive + "] is null: " + (MainWin.disks[drive] == null));
                
                String fullPath = null;
                
                // Try to get path from MainWin.disks first
                if (MainWin.disks[drive] != null && MainWin.disks[drive].isLoaded()) {
                    fullPath = MainWin.disks[drive].getPath();
                    System.out.println("Got path from MainWin.disks[" + drive + "]: " + fullPath);
                }
                
                // If not available, try to get from config
                if ((fullPath == null || fullPath.isEmpty()) && MainWin.config != null) {
                    String configPath = MainWin.config.getString("DiskPath_" + drive, null);
                    if (configPath != null && !configPath.isEmpty()) {
                        fullPath = configPath;
                        System.out.println("Got path from config DiskPath_" + drive + ": " + fullPath);
                    }
                }
                
                if (fullPath != null && !fullPath.isEmpty()) {
                    if (diskPathLabel != null) {
                        diskPathLabel.setText(fullPath);
                        System.out.println("Set diskPathLabel text to: " + fullPath);
                        System.out.println("diskPathLabel text is now: " + diskPathLabel.getText());
                    } else {
                        System.err.println("ERROR: diskPathLabel is null when trying to set path!");
                    }
                } else {
                    System.out.println("No path available for disk " + drive);
                    if (diskPathLabel != null) {
                        diskPathLabel.setText("");
                    }
                }
            } else {
                // No selection - clear path label
                System.out.println("No disk selected, clearing path label");
                if (diskPathLabel != null) {
                    diskPathLabel.setText("");
                }
            }
        });
        
        // Initialize version label
        versionLabel.setText("DriveWire " + MainWin.DWUIVersion.toString());
        
        // Initialize disk path label
        if (diskPathLabel != null) {
            diskPathLabel.setText("");
            diskPathLabel.setVisible(true);
            diskPathLabel.setManaged(true);
            System.out.println("diskPathLabel initialized - visible: " + diskPathLabel.isVisible() + ", managed: " + diskPathLabel.isManaged());
            System.out.println("diskPathLabel parent: " + (diskPathLabel.getParent() != null ? diskPathLabel.getParent().getClass().getName() : "null"));
        } else {
            System.err.println("WARNING: diskPathLabel is null! FXML may not have loaded correctly.");
        }
        
        // Initialize status
        updateConnectionStatus(false);
        
        // Initialize menu states
        updateMenuStates();
        
        // Load dark mode preference
        loadDarkModePreference();
        
        // Load and restore view mode (Dashboard/Advanced)
        loadViewMode();
        
        // Load and restore UI layout (split pane divider, column widths)
        loadUILayout();
        
        // Set up listeners to save UI layout when changed
        setupUILayoutListeners();
        
        // Load existing log items into the log text area
        loadExistingLogs();
        
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
        
        // Save UI layout when window closes (before shutdown)
        if (stage != null) {
            stage.setOnCloseRequest(e -> {
                System.out.println("Window closing - saving UI layout...");
                saveUILayout();
                // Don't consume the event - let MainWinFX handle shutdown
            });
        }
    }
    
    /**
     * Load UI layout from config (split pane divider position, column widths).
     */
    private void loadUILayout() {
        if (MainWin.config == null) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                // Load split pane divider position
                if (splitPane != null) {
                    double dividerPos = MainWin.config.getDouble("MainWin_SplitPane_Divider", 0.3);
                    // Clamp between 0.0 and 1.0
                    dividerPos = Math.max(0.0, Math.min(1.0, dividerPos));
                    splitPane.setDividerPositions(dividerPos);
                    System.out.println("Loaded split pane divider position: " + dividerPos);
                }
                
                // Load column widths
                if (ledColumn != null) {
                    double width = MainWin.config.getDouble("DiskTable_LED_Width", ledColumn.getPrefWidth());
                    if (width >= ledColumn.getMinWidth()) {
                        ledColumn.setPrefWidth(width);
                    }
                }
                if (driveColumn != null) {
                    double width = MainWin.config.getDouble("DiskTable_Drive_Width", driveColumn.getPrefWidth());
                    if (width >= driveColumn.getMinWidth()) {
                        driveColumn.setPrefWidth(width);
                    }
                }
                if (fileColumn != null) {
                    double width = MainWin.config.getDouble("DiskTable_File_Width", fileColumn.getPrefWidth());
                    if (width >= fileColumn.getMinWidth()) {
                        fileColumn.setPrefWidth(width);
                    }
                }
                if (readsColumn != null) {
                    double width = MainWin.config.getDouble("DiskTable_Reads_Width", readsColumn.getPrefWidth());
                    if (width >= readsColumn.getMinWidth()) {
                        readsColumn.setPrefWidth(width);
                    }
                }
                if (writesColumn != null) {
                    double width = MainWin.config.getDouble("DiskTable_Writes_Width", writesColumn.getPrefWidth());
                    if (width >= writesColumn.getMinWidth()) {
                        writesColumn.setPrefWidth(width);
                    }
                }
                
                // Load selected tab in Advanced view
                if (outputTabPane != null) {
                    int selectedTabIndex = MainWin.config.getInt("AdvancedView_SelectedTab", 0);
                    // Clamp to valid range (0-2: Log, Command, Statistics)
                    selectedTabIndex = Math.max(0, Math.min(2, selectedTabIndex));
                    if (selectedTabIndex < outputTabPane.getTabs().size()) {
                        outputTabPane.getSelectionModel().select(selectedTabIndex);
                        System.out.println("Loaded selected tab index: " + selectedTabIndex);
                    }
                }
                
                System.out.println("Loaded UI layout from config");
            } catch (Exception e) {
                System.err.println("Error loading UI layout: " + e.getMessage());
            }
        });
    }
    
    /**
     * Set up listeners to save UI layout when dividers or columns are resized.
     */
    private void setupUILayoutListeners() {
        // Use a debounce mechanism to avoid excessive saves
        javafx.util.Duration debounceDelay = javafx.util.Duration.millis(500);
        javafx.animation.Timeline debounceTimeline = new javafx.animation.Timeline();
        
        Runnable debouncedSave = () -> {
            debounceTimeline.stop();
            debounceTimeline.getKeyFrames().clear();
            debounceTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(debounceDelay, e -> {
                System.out.println("Saving UI layout (debounced)...");
                saveUILayout();
            }));
            debounceTimeline.play();
        };
        
        if (splitPane != null) {
            // Listen for divider position changes - use multiple approaches for reliability
            // Approach 1: Listen to divider position property changes
            Platform.runLater(() -> {
                if (splitPane.getDividers().size() > 0) {
                    splitPane.getDividers().forEach(divider -> {
                        divider.positionProperty().addListener((obs, oldVal, newVal) -> {
                            if (oldVal != null && newVal != null && Math.abs(oldVal.doubleValue() - newVal.doubleValue()) > 0.001) {
                                System.out.println("Divider position changed: " + oldVal + " -> " + newVal);
                                debouncedSave.run();
                            }
                        });
                    });
                }
            });
            
            // Approach 2: Listen to mouse release events on the split pane (catches drag end)
            splitPane.setOnMouseReleased(e -> {
                System.out.println("Mouse released on split pane, saving divider position");
                saveUILayout();
            });
            
            // Approach 3: Periodically check divider positions (backup)
            javafx.animation.Timeline checkTimeline = new javafx.animation.Timeline(
                new KeyFrame(Duration.millis(1000), e -> {
                    if (splitPane.getDividerPositions().length > 0) {
                        double currentPos = splitPane.getDividerPositions()[0];
                        // This will be checked against last saved position if needed
                    }
                })
            );
            checkTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            // Don't start this - it's just a backup approach
        }
        
        // Listen for column width changes
        if (ledColumn != null) {
            ledColumn.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != oldVal.doubleValue()) {
                    debouncedSave.run();
                }
            });
        }
        if (driveColumn != null) {
            driveColumn.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != oldVal.doubleValue()) {
                    debouncedSave.run();
                }
            });
        }
        if (fileColumn != null) {
            fileColumn.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != oldVal.doubleValue()) {
                    debouncedSave.run();
                }
            });
        }
        if (readsColumn != null) {
            readsColumn.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != oldVal.doubleValue()) {
                    debouncedSave.run();
                }
            });
        }
        if (writesColumn != null) {
            writesColumn.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != oldVal.doubleValue()) {
                    debouncedSave.run();
                }
            });
        }
        
        // Listen for tab selection changes in Advanced view
        if (outputTabPane != null) {
            outputTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                    System.out.println("Tab selection changed: " + oldVal + " -> " + newVal);
                    // Save immediately when tab changes (no debounce needed for tab selection)
                    saveUILayout();
                }
            });
        }
    }
    
    /**
     * Save UI layout to config (split pane divider position, column widths).
     */
    public void saveUILayout() {
        if (MainWin.config == null) {
            System.err.println("Cannot save UI layout: config is null");
            return;
        }
        
        try {
            // Save split pane divider position
            if (splitPane != null && splitPane.getDividerPositions().length > 0) {
                double dividerPos = splitPane.getDividerPositions()[0];
                MainWin.config.setProperty("MainWin_SplitPane_Divider", dividerPos);
                System.out.println("Saved split pane divider position: " + dividerPos);
            } else {
                System.err.println("Cannot save divider position: splitPane is null or has no dividers");
            }
            
            // Save column widths
            if (ledColumn != null) {
                MainWin.config.setProperty("DiskTable_LED_Width", ledColumn.getWidth());
            }
            if (driveColumn != null) {
                MainWin.config.setProperty("DiskTable_Drive_Width", driveColumn.getWidth());
            }
            if (fileColumn != null) {
                MainWin.config.setProperty("DiskTable_File_Width", fileColumn.getWidth());
            }
            if (readsColumn != null) {
                MainWin.config.setProperty("DiskTable_Reads_Width", readsColumn.getWidth());
            }
            if (writesColumn != null) {
                MainWin.config.setProperty("DiskTable_Writes_Width", writesColumn.getWidth());
            }
            
            // Save selected tab in Advanced view
            if (outputTabPane != null) {
                int selectedIndex = outputTabPane.getSelectionModel().getSelectedIndex();
                MainWin.config.setProperty("AdvancedView_SelectedTab", selectedIndex);
                System.out.println("Saved selected tab index: " + selectedIndex);
            }
            
            // Save config (auto-save should handle this, but ensure it's saved)
            MainWin.config.save();
        } catch (Exception e) {
            System.err.println("Error saving UI layout: " + e.getMessage());
        }
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
                // Show "Client: IP:port" format
                String host = MainWin.host != null ? MainWin.host : "unknown";
                int port = MainWin.port > 0 ? MainWin.port : 6800;
                serverLabel.setText("Client: " + host + ":" + port);
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.getStyleClass().removeAll("status-connected");
                statusLabel.getStyleClass().add("status-disconnected");
                // Show nothing when disconnected
                serverLabel.setText("");
            }
        });
    }
    
    /**
     * Update menu states based on current configuration.
     */
    private void updateMenuStates() {
        if (MainWin.config != null) {
            // Load HDBDOSMode from UI config first (for persistence)
            // Then try instance config if available (for current server state)
            boolean hdbdosMode = MainWin.config.getBoolean("HDBDOSMode", false);
            
            // Try to get instance config to check current server state
            org.apache.commons.configuration.HierarchicalConfiguration instanceConfig = MainWin.getInstanceConfig();
            if (instanceConfig != null) {
                // If instance config has HDBDOSMode, use that (it's the source of truth when connected)
                if (instanceConfig.containsKey("HDBDOSMode")) {
                    hdbdosMode = instanceConfig.getBoolean("HDBDOSMode", false);
                } else {
                    // If instance config doesn't have it, apply the UI config value to the server
                    if (MainWin.connected) {
                        MainWin.sendCommand("dw config set HDBDOSMode " + hdbdosMode);
                    }
                }
            }
            
            hdbdosTranslationMenuItem.setSelected(hdbdosMode);
            
            // Log if HDB-DOS translation is enabled
            if (hdbdosMode) {
                com.groupunix.drivewireui.LogItem logItem = new com.groupunix.drivewireui.LogItem("HDB-DOS translation is enabled");
                logItem.setLevel("INFO");
                logItem.setSource("DriveWireUI");
                MainWin.addToServerLog(logItem);
            }
            
            restartClientsMenuItem.setSelected(
                instanceConfig != null ? instanceConfig.getBoolean("RestartClientsOnOpen", false) : 
                MainWin.config.getBoolean("RestartClientsOnOpen", false));
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
                DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                int selectedDrive = (selected != null) ? selected.getDrive() : -1;
                
                for (int i = 0; i < 256; i++) {
                    DiskTableItem item = diskTableData.get(i);
                    if (MainWin.disks[i] != null && MainWin.disks[i].isLoaded()) {
                        // Extract filename from full path for display
                        String fullPath = MainWin.disks[i].getPath();
                        String filename = UIUtils.getFilenameFromURI(fullPath);
                        item.setFile(filename);
                        item.setReads(Integer.parseInt(MainWin.disks[i].getParam("_reads").toString()));
                        item.setWrites(Integer.parseInt(MainWin.disks[i].getParam("_writes").toString()));
                        
                        // Update path label if this disk is currently selected
                        if (i == selectedDrive && diskPathLabel != null) {
                            diskPathLabel.setText(fullPath);
                            System.out.println("Updated path label for selected drive " + i + ": " + fullPath);
                        }
                    } else {
                        item.setFile("");
                        item.setReads(0);
                        item.setWrites(0);
                        
                        // Clear path label if this disk is currently selected
                        if (i == selectedDrive && diskPathLabel != null) {
                            diskPathLabel.setText("");
                        }
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
                        // Force table refresh to show updated LED state
                        diskTable.refresh();
                    } else if (key.equals("_reads")) {
                        try {
                            int readsValue = Integer.parseInt(value.toString());
                            item.setReads(readsValue);
                            System.out.println("MainWindowController: Updated disk " + disk + " reads to " + readsValue);
                            // Force table refresh to show updated read count
                            diskTable.refresh();
                        } catch (NumberFormatException e) {
                            System.err.println("MainWindowController: Error parsing _reads value: " + value);
                        }
                    } else if (key.equals("_writes")) {
                        try {
                            int writesValue = Integer.parseInt(value.toString());
                            item.setWrites(writesValue);
                            System.out.println("MainWindowController: Updated disk " + disk + " writes to " + writesValue);
                            // Force table refresh to show updated write count
                            diskTable.refresh();
                        } catch (NumberFormatException e) {
                            System.err.println("MainWindowController: Error parsing _writes value: " + value);
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
                        
                        System.out.println("Disk " + disk + " inserted: filename=" + filename + ", fullPath=" + filePath);
                        System.out.println("  -> Drive: " + item.getDrive() + ", File (stored): " + item.getFile() + ", Reads: " + item.getReads() + ", Writes: " + item.getWrites() + ", LED: " + item.getLed());
                        
                        // Update path label if this disk is currently selected
                        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getDrive() == disk) {
                            diskPathLabel.setText(filePath);
                        }
                        
                        // Force table refresh to ensure UI updates immediately
                        diskTable.refresh();
                    } else if (key.equals("*eject")) {
                        item.setFile("");
                        item.setReads(0);
                        item.setWrites(0);
                        item.setLed(0);
                        System.out.println("Disk " + disk + " ejected");
                        
                        // Clear path label if this disk is currently selected
                        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getDrive() == disk) {
                            diskPathLabel.setText("");
                        }
                        
                        // Force table refresh to ensure UI updates
                        diskTable.refresh();
                    } else if (key.equals("File")) {
                        // Direct file name update (may come after *insert)
                        String filePath = value.toString();
                        String filename = UIUtils.getFilenameFromURI(filePath);
                        item.setFile(filename);
                        System.out.println("Disk " + disk + " file updated: " + filename);
                        
                        // Update path label if this disk is currently selected
                        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getDrive() == disk) {
                            diskPathLabel.setText(filePath);
                        }
                        
                        // Force table refresh to ensure UI updates
                        diskTable.refresh();
                    } else if (key.equals("Drive")) {
                        // Drive number update
                        try {
                            item.setDrive(Integer.parseInt(value.toString()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    } else if (key.equals("_path") || key.equals("path")) {
                        // Path parameter - extract filename for File column, store full path for label
                        String filePath = value.toString();
                        String filename = UIUtils.getFilenameFromURI(filePath);
                        item.setFile(filename);
                        System.out.println("Disk " + disk + " path updated: " + filename + " (full path: " + filePath + ")");
                        
                        // Update path label if this disk is currently selected
                        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getDrive() == disk) {
                            diskPathLabel.setText(filePath);
                        }
                        
                        // Force table refresh to ensure UI updates
                        diskTable.refresh();
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
            if (logTextArea != null) {
                logTextArea.appendText(text + "\n");
                // Auto-scroll to bottom
                logTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
    
    /**
     * Load existing log items from MainWin.logItems into the log text area.
     */
    private void loadExistingLogs() {
        if (MainWin.logItems != null && logTextArea != null) {
            Platform.runLater(() -> {
                synchronized (MainWin.logItems) {
                    for (LogItem item : MainWin.logItems) {
                        logTextArea.appendText(item.toString() + "\n");
                    }
                    // Scroll to bottom after loading
                    logTextArea.setScrollTop(Double.MAX_VALUE);
                }
            });
        }
    }
    
    /**
     * Start per-drive disk activity tracking using the same delta approach as the graph.
     * This samples per-drive reads/writes and calculates deltas to update the table.
     */
    private void startDiskActivityTracking() {
        // Initialize tracking arrays
        for (int i = 0; i < 256; i++) {
            lastReads[i] = 0;
            lastWrites[i] = 0;
        }
        
        // Start background thread to sample disk activity
        Thread activityTracker = new Thread(() -> {
            int interval = 2000; // Same interval as graph (2 seconds)
            
            // Wait for server status to be available
            int waitCount = 0;
            while (MainWin.serverStatus == null && waitCount < 50) {
                try {
                    Thread.sleep(interval);
                    waitCount++;
                } catch (InterruptedException e) {
                    return;
                }
            }
            
            if (MainWin.serverStatus == null) {
                System.err.println("DiskActivityTracker: Server status never became available");
                return;
            }
            
            System.out.println("DiskActivityTracker: Starting per-drive activity tracking");
            
            // Initialize baseline values for each drive
            initializeDiskActivityBaselines();
            diskActivityTrackingInitialized = true;
            
            try {
                Thread.sleep(interval); // Wait one interval before starting delta calculations
            } catch (InterruptedException e) {
                return;
            }
            
            // Main tracking loop
            while (MainWin.serverStatus != null) {
                try {
                    // Sample current reads/writes for each drive
                    for (int drive = 0; drive < 256; drive++) {
                        final int driveNum = drive; // Make final for lambda
                        if (MainWin.disks[driveNum] != null && MainWin.disks[driveNum].isLoaded()) {
                            // Get current cumulative values
                            Object readsObj = MainWin.disks[driveNum].getParam("_reads");
                            Object writesObj = MainWin.disks[driveNum].getParam("_writes");
                            String readsStr = readsObj != null ? readsObj.toString() : null;
                            String writesStr = writesObj != null ? writesObj.toString() : null;
                            
                            if (readsStr != null && !readsStr.isEmpty()) {
                                try {
                                    int currentReads = Integer.parseInt(readsStr);
                                    int deltaReads = currentReads - lastReads[driveNum];
                                    
                                    if (deltaReads > 0) {
                                        // Update table with current cumulative value
                                        final int finalReads = currentReads;
                                        Platform.runLater(() -> {
                                            if (driveNum < diskTableData.size()) {
                                                DiskTableItem item = diskTableData.get(driveNum);
                                                if (item != null) {
                                                    item.setReads(finalReads);
                                                    // Set LED to green for read activity
                                                    item.setLed(1);
                                                    diskTable.refresh();
                                                }
                                            }
                                        });
                                    }
                                    
                                    lastReads[driveNum] = currentReads;
                                } catch (NumberFormatException e) {
                                    // Ignore invalid values
                                }
                            }
                            
                            if (writesStr != null && !writesStr.isEmpty()) {
                                try {
                                    int currentWrites = Integer.parseInt(writesStr);
                                    int deltaWrites = currentWrites - lastWrites[driveNum];
                                    
                                    if (deltaWrites > 0) {
                                        // Update table with current cumulative value
                                        final int finalWrites = currentWrites;
                                        Platform.runLater(() -> {
                                            if (driveNum < diskTableData.size()) {
                                                DiskTableItem item = diskTableData.get(driveNum);
                                                if (item != null) {
                                                    item.setWrites(finalWrites);
                                                    // Set LED to red for write activity
                                                    item.setLed(2);
                                                    diskTable.refresh();
                                                }
                                            }
                                        });
                                    }
                                    
                                    lastWrites[driveNum] = currentWrites;
                                } catch (NumberFormatException e) {
                                    // Ignore invalid values
                                }
                            }
                            
                            // If no activity, ensure LED is off (but keep reads/writes values)
                            if (readsStr != null && writesStr != null) {
                                try {
                                    int currentReads = Integer.parseInt(readsStr);
                                    int currentWrites = Integer.parseInt(writesStr);
                                    int deltaReads = currentReads - lastReads[driveNum];
                                    int deltaWrites = currentWrites - lastWrites[driveNum];
                                    
                                    if (deltaReads == 0 && deltaWrites == 0) {
                                        // No activity in this interval - turn off LED if it was on
                                        Platform.runLater(() -> {
                                            if (driveNum < diskTableData.size()) {
                                                DiskTableItem item = diskTableData.get(driveNum);
                                                if (item != null && item.getLed() != 0) {
                                                    // Only turn off LED if there's truly no activity
                                                    // (reads/writes haven't changed)
                                                    item.setLed(0);
                                                    diskTable.refresh();
                                                }
                                            }
                                        });
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        
        activityTracker.setDaemon(true);
        activityTracker.setName("DiskActivityTracker");
        activityTracker.start();
        System.out.println("Per-drive disk activity tracking started");
    }
    
    /**
     * Initialize baseline values for disk activity tracking.
     * This is called once before starting delta calculations.
     */
    private void initializeDiskActivityBaselines() {
        for (int drive = 0; drive < 256; drive++) {
            if (MainWin.disks[drive] != null && MainWin.disks[drive].isLoaded()) {
                Object readsObj = MainWin.disks[drive].getParam("_reads");
                Object writesObj = MainWin.disks[drive].getParam("_writes");
                String readsStr = readsObj != null ? readsObj.toString() : null;
                String writesStr = writesObj != null ? writesObj.toString() : null;
                
                if (readsStr != null && !readsStr.isEmpty()) {
                    try {
                        lastReads[drive] = Integer.parseInt(readsStr);
                    } catch (NumberFormatException e) {
                        lastReads[drive] = 0;
                    }
                }
                
                if (writesStr != null && !writesStr.isEmpty()) {
                    try {
                        lastWrites[drive] = Integer.parseInt(writesStr);
                    } catch (NumberFormatException e) {
                        lastWrites[drive] = 0;
                    }
                }
            }
        }
        System.out.println("Disk activity baselines initialized");
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
    private void handleDisconnect() {
        // Disconnect from current server
        Platform.runLater(() -> {
            try {
                System.out.println("Disconnecting from server...");
                
                // Clear host/port first to prevent reconnection
                String oldHost = MainWin.getHost();
                int oldPort = MainWin.getPort();
                MainWin.setHost(null);
                MainWin.setPort("0");
                
                // Restart connection (this will disconnect the old one)
                // Since host is null, it won't reconnect
                MainWin.restartServerConn();
                
                // Set connection status to disconnected
                synchronized (MainWin.connected) {
                    MainWin.connected = false;
                }
                MainWin.setItemsConnectionEnabled(false);
                
                // Update UI
                updateConnectionStatus(false);
                
                System.out.println("Disconnected from server (was: " + oldHost + ":" + oldPort + ")");
                
                // Show confirmation
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Disconnected");
                alert.setHeaderText("Disconnected from server");
                alert.setContentText("You have been disconnected from the DriveWire server.\n\nPrevious connection: " + 
                    (oldHost != null ? oldHost + ":" + oldPort : "unknown") + 
                    "\n\nUse 'Choose server...' to connect to a different server.");
                alert.initOwner(primaryStage);
                alert.showAndWait();
            } catch (Exception e) {
                System.err.println("Error disconnecting: " + e.getMessage());
                e.printStackTrace();
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Disconnect Error");
                alert.setHeaderText("Failed to disconnect");
                alert.setContentText("An error occurred while disconnecting: " + e.getMessage());
                alert.initOwner(primaryStage);
                alert.showAndWait();
            }
        });
    }
    
    @FXML
    private void handleExit() {
        MainWin.doShutdown();
        Platform.exit();
    }
    
    @FXML
    private void handleViewModeToggle() {
        boolean isAdvanced = viewModeToggle.isSelected();
        updateViewMode(isAdvanced);
        updateToggleLabels(isAdvanced);
        saveViewMode(isAdvanced);
    }
    
    /**
     * Update the label colors to show which mode is active.
     */
    private void updateToggleLabels(boolean isAdvanced) {
        // Find the labels in the toggle container
        if (viewModeToggle != null && viewModeToggle.getParent() != null) {
            javafx.scene.Parent parent = viewModeToggle.getParent();
            if (parent instanceof HBox) {
                HBox container = (HBox) parent;
                for (javafx.scene.Node node : container.getChildren()) {
                    if (node instanceof Label) {
                        Label label = (Label) node;
                        if (label.getText().equals("Dashboard")) {
                            // Highlight Dashboard when not advanced
                            if (!isAdvanced) {
                                label.setStyle("-fx-text-fill: #4a9eff; -fx-font-weight: bold;");
                            } else {
                                label.setStyle("-fx-text-fill: #666666; -fx-font-weight: normal;");
                            }
                        } else if (label.getText().equals("Advanced")) {
                            // Highlight Advanced when advanced
                            if (isAdvanced) {
                                label.setStyle("-fx-text-fill: #4a9eff; -fx-font-weight: bold;");
                            } else {
                                label.setStyle("-fx-text-fill: #666666; -fx-font-weight: normal;");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update the view mode (Dashboard or Advanced).
     * @param isAdvanced true for Advanced view, false for Dashboard
     */
    private void updateViewMode(boolean isAdvanced) {
        if (dashboardView != null && advancedView != null) {
            dashboardView.setVisible(!isAdvanced);
            dashboardView.setManaged(!isAdvanced);
            advancedView.setVisible(isAdvanced);
            advancedView.setManaged(isAdvanced);
        }
    }
    
    /**
     * Load view mode from config.
     */
    private void loadViewMode() {
        if (MainWin.config != null && viewModeToggle != null) {
            // Default to Advanced (true) if not set
            boolean isAdvanced = MainWin.config.getBoolean("ViewMode_Advanced", true);
            viewModeToggle.setSelected(isAdvanced);
            updateViewMode(isAdvanced);
            updateToggleLabels(isAdvanced);
        }
    }
    
    /**
     * Save view mode to config.
     */
    private void saveViewMode(boolean isAdvanced) {
        if (MainWin.config != null) {
            try {
                MainWin.config.setProperty("ViewMode_Advanced", isAdvanced);
                MainWin.config.save();
            } catch (Exception e) {
                System.err.println("Error saving view mode: " + e.getMessage());
            }
        }
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
        
        // Save to UI config for persistence
        if (MainWin.config != null) {
            MainWin.config.setProperty("HDBDOSMode", enabled);
            try {
                MainWin.config.save();
            } catch (Exception e) {
                System.err.println("Error saving HDBDOSMode to config: " + e.getMessage());
            }
        }
        
        // Send command to server to update instance config
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
                        
                        // Update path label if this disk is currently selected
                        DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getDrive() == diskno) {
                            diskPathLabel.setText(filePath);
                        }
                        
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
        try {
            System.out.println("handleDiskProperties() called");
            DiskTableItem selected = diskTable.getSelectionModel().getSelectedItem();
            System.out.println("Selected item: " + selected);
            
            if (selected == null) {
                System.out.println("No disk selected");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Selection");
                alert.setHeaderText("No disk selected");
                alert.setContentText("Please select a disk drive from the table first.");
                alert.showAndWait();
                return;
            }
            
            System.out.println("Selected drive: " + selected.getDrive());
            System.out.println("Selected file: " + selected.getFile());
            
            // Check if table item shows a disk is inserted (has filename)
            String filename = selected.getFile();
            if (filename == null || filename.trim().isEmpty()) {
                System.out.println("No disk filename in table item for drive " + selected.getDrive());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Disk");
                alert.setHeaderText("No disk inserted");
                alert.setContentText("There is no disk inserted in drive " + selected.getDrive() + ".");
                alert.showAndWait();
                return;
            }
            
            if (MainWin.disks == null) {
                System.out.println("MainWin.disks is null");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Disk system not initialized");
                alert.setContentText("The disk system has not been initialized yet.");
                alert.showAndWait();
                return;
            }
            
            if (selected.getDrive() < 0 || selected.getDrive() >= MainWin.disks.length) {
                System.out.println("Invalid drive number: " + selected.getDrive());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid drive number");
                alert.setContentText("Drive number " + selected.getDrive() + " is out of range.");
                alert.showAndWait();
                return;
            }
            
            DiskDef disk = MainWin.disks[selected.getDrive()];
            System.out.println("Disk from MainWin.disks: " + disk);
            
            // Check if existing disk has parameters (from sync events)
            boolean hasParameters = false;
            if (disk != null && disk.isLoaded()) {
                Iterator<String> params = disk.getParams();
                int paramCount = 0;
                while (params.hasNext()) {
                    String key = params.next();
                    if (!key.startsWith("*")) {
                        paramCount++;
                    }
                }
                hasParameters = paramCount > 2; // More than just _path and maybe _reads/_writes
                System.out.println("Existing disk has " + paramCount + " parameters");
            }
            
            // If MainWin.disks[drive] is null or not loaded, try to create DiskDef
            // Note: DiskDef constructor may fail in JavaFX mode due to SWT dependencies
            // We'll try to use submitDiskEvent first, then fall back to direct creation
            if (disk == null || !disk.isLoaded() || !hasParameters) {
                System.out.println("Disk is null or not loaded for drive " + selected.getDrive() + ", attempting to create...");
                
                // Try to get full path from config
                String diskPath = null;
                if (MainWin.config != null) {
                    diskPath = MainWin.config.getString("DiskPath_" + selected.getDrive(), null);
                }
                if (diskPath == null || diskPath.isEmpty()) {
                    diskPath = filename;
                }
                
                // Try using submitDiskEvent to create DiskDef (this is how sync events do it)
                try {
                    MainWin.submitDiskEvent(selected.getDrive(), "*insert", diskPath);
                    disk = MainWin.disks[selected.getDrive()];
                    System.out.println("Created DiskDef via submitDiskEvent for drive " + selected.getDrive());
                    
                    // Copy what we can from table item to populate basic parameters
                    if (disk != null) {
                        int reads = selected.getReads();
                        int writes = selected.getWrites();
                        if (reads > 0 && disk.getParam("_reads") == null) {
                            disk.setParam("_reads", String.valueOf(reads));
                        }
                        if (writes > 0 && disk.getParam("_writes") == null) {
                            disk.setParam("_writes", String.valueOf(writes));
                        }
                        // Set path if not already set
                        if (diskPath != null && !diskPath.isEmpty() && disk.getParam("_path") == null) {
                            disk.setParam("_path", diskPath);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("submitDiskEvent failed: " + e.getMessage());
                    e.printStackTrace();
                    // Fall back to direct creation (will likely fail in JavaFX mode)
                    try {
                        disk = new DiskDef(selected.getDrive());
                        disk.setLoaded(true);
                        if (diskPath != null && !diskPath.isEmpty()) {
                            disk.setParam("*insert", diskPath);
                            disk.setParam("_path", diskPath);
                        }
                        // Copy reads/writes from table item
                        int reads = selected.getReads();
                        int writes = selected.getWrites();
                        if (reads > 0) {
                            disk.setParam("_reads", String.valueOf(reads));
                        }
                        if (writes > 0) {
                            disk.setParam("_writes", String.valueOf(writes));
                        }
                        MainWin.disks[selected.getDrive()] = disk;
                        System.out.println("Created DiskDef directly for drive " + selected.getDrive());
                    } catch (Exception e2) {
                        System.err.println("Failed to create DiskDef (SWT initialization error in JavaFX mode): " + e2.getMessage());
                        e2.printStackTrace();
                        // DiskDef creation failed - can't proceed
                        disk = null;
                    }
                }
            }
            
            // Check disk parameters and trigger sync events if needed
            if (disk != null) {
                try {
                    // Count non-* parameters
                    Iterator<String> params = disk.getParams();
                    int paramCount = 0;
                    while (params.hasNext()) {
                        String key = params.next();
                        if (!key.startsWith("*")) {
                            paramCount++;
                        }
                    }
                    
                    System.out.println("Disk has " + paramCount + " parameters");
                    
                    // If disk has very few parameters, trigger sync events by requesting disk status
                    // This causes the server to send D: events with disk parameters
                    if (paramCount <= 2) { // Only _path and maybe _reads/_writes
                        System.out.println("Triggering sync events to populate disk parameters...");
                        // Send a command that will cause the server to send disk parameter updates
                        // The sync protocol will receive D: events and populate parameters
                        MainWin.sendCommand("dw disk status " + selected.getDrive());
                        
                        // Give sync events a moment to arrive (non-blocking - just a short delay)
                        // Sync events are processed asynchronously, so parameters will be added to the disk
                        // The Properties dialog will show what's available, and sync events will update it
                        System.out.println("Sync events triggered - parameters will be populated asynchronously");
                    }
                } catch (Exception e) {
                    System.err.println("Error checking disk parameters: " + e.getMessage());
                    e.printStackTrace();
                    // Non-fatal - continue with what we have
                }
            }
            
            if (disk == null) {
                System.out.println("Could not create or retrieve disk for drive " + selected.getDrive());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to load disk information");
                alert.setContentText("Could not retrieve disk information for drive " + selected.getDrive() + ".\n\n" +
                    "The disk appears to be inserted (filename: " + filename + "), but disk information is not available.\n" +
                    "This may be a timing issue - please try again in a moment, or restart the application.");
                alert.showAndWait();
                return;
            }
            
            System.out.println("primaryStage: " + primaryStage);
            Window owner = null;
            
            if (primaryStage != null) {
                owner = primaryStage;
            } else if (diskTable.getScene() != null) {
                owner = diskTable.getScene().getWindow();
                System.out.println("Using fallback owner: " + owner);
            }
            
            if (owner == null) {
                System.out.println("Could not determine owner window");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Window error");
                alert.setContentText("Could not determine the parent window for the dialog.");
                alert.showAndWait();
                return;
            }
            
            System.out.println("Creating DiskAdvancedWinFX dialog...");
            DiskAdvancedWinFX dialog = new DiskAdvancedWinFX(owner, disk);
            dialog.show();
            System.out.println("Dialog shown");
            
        } catch (Exception e) {
            System.err.println("Exception in handleDiskProperties: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open disk properties");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
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

