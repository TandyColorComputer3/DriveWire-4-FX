package com.groupunix.drivewireui;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.groupunix.drivewireserver.DriveWireServer;
import com.groupunix.drivewireui.library.CloudLibraryItem;
import com.groupunix.drivewireui.library.FolderLibraryItem;
import com.groupunix.drivewireui.library.LibraryItem;
import com.groupunix.drivewireui.library.MountedFolderLibraryItem;
import com.groupunix.drivewireui.nineserver.NineServer;
import com.groupunix.drivewireui.nineserver.OS9BufferGroup;
import com.groupunix.dwlite.DWLite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for DriveWire4 UI.
 * Replaces the SWT-based MainWin.main() method.
 */
public class MainWinFX extends Application {
    
    private static Logger logger = Logger.getLogger(MainWinFX.class);
    private static PatternLayout logLayout = new PatternLayout("%d{dd MMM yyyy HH:mm:ss} %-5p [%-14t] %m%n");
    
    private static String[] applicationArgs;
    private static boolean noServer = false;
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFX.init() called ===");
        
        // Initialize logging before anything else
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getRootLogger().removeAllAppenders();
        Logger.getRootLogger().addAppender(new ConsoleAppender(logLayout));
        
        System.out.println("Logging initialized");
        
        // Set up uncaught exception handler
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("Yikes! Uncaught exception in thread " + t.getName());
                System.out.println();
                System.out.println(UIUtils.getStackTrace(e));
                e.printStackTrace();
                
                logger.warn(e.getClass().getSimpleName() + " in UI thread " + t.getName() + " " + t.getId());
                
                try {
                    Platform.runLater(() -> {
                        // Show error dialog using JavaFX Alert
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("An unexpected error occurred");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    });
                } catch (Exception ex) {
                    System.err.println("Could not show error dialog: " + ex.getMessage());
                }
                
                if ((MainWin.config != null) && 
                    (MainWin.config.getBoolean("TermServerOnExit", false) || 
                     MainWin.config.getBoolean("LocalServer", false))) {
                    if (MainWin.dwThread != null && MainWin.dwThread.isAlive()) {
                        MainWin.stopDWServer();
                    }
                }
                
                Platform.exit();
                System.exit(1);
            }
        });
        
        System.out.println("Exception handler set");
        
        // Load configuration
        System.out.println("Loading configuration...");
        try {
            loadConfig();
            System.out.println("Configuration loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // Start local server if configured (in background thread to not block JavaFX)
        if (MainWin.config.getBoolean("LocalServer", true) && !noServer) {
            System.out.println("Starting local server in background...");
            // Don't start server here - wait until after JavaFX window is shown
            // This will be done in start() method
            System.out.println("Server will start after UI is shown");
        }
        
        // Set system property to indicate we're running in UI mode
        // This prevents DriveWireServer from calling System.exit()
        System.setProperty("drivewire.ui.mode", "true");
        System.out.println("Set drivewire.ui.mode property");
        
        // Handle special UI modes
        if (UIUtils.hasArg(applicationArgs, "noui")) {
            logger.info("UI disabled (with --noui)");
            System.out.println("UI disabled (--noui flag)");
            Platform.exit();
            return;
        } else if (UIUtils.hasArg(applicationArgs, "liteui")) {
            System.out.println("Starting Lite UI...");
            DWLite.main(applicationArgs);
            Platform.exit();
            return;
        }
        
        System.out.println("Initializing library roots...");
        // Initialize library roots
        if (!MainWin.config.containsKey("Library.Local.updated")) {
            MainWin.config.addProperty("Library.Local.autocreated", System.currentTimeMillis());
            MainWin.config.addProperty("Library.Local.updated", 0);
        }
        
        if (!MainWin.config.containsKey("Library.Cloud.updated")) {
            MainWin.config.addProperty("Library.Cloud.autocreated", System.currentTimeMillis());
            MainWin.config.addProperty("Library.Cloud.updated", 0);
        }
        
        HierarchicalConfiguration locallib = MainWin.config.configurationAt("Library.Local");
        MainWin.libraryroot = new LibraryItem[3];
        MainWin.libraryroot[0] = new MountedFolderLibraryItem("Mounted");
        MainWin.libraryroot[1] = new FolderLibraryItem("Local", locallib.getRoot());
        MainWin.libraryroot[2] = new CloudLibraryItem("CoCoCloud");
        System.out.println("Library roots initialized");
        
        // Initialize OS9 buffer groups
        MainWin.os9BufferGroups = new Vector<OS9BufferGroup>();
        MainWin.os9BufferGroups.setSize(256);
        System.out.println("OS9 buffer groups initialized");
        
        // Skip UITaskMaster initialization - it requires SWT Composite
        // sendCommand() has been made null-safe to handle this case
        // TODO: Create JavaFX-compatible UITaskMaster that displays tasks in JavaFX UI
        System.out.println("Skipping UITaskMaster initialization (requires SWT, will use null-safe sendCommand)");
        
        // Start NineServer thread if configured
        if (MainWin.config.getInt("NineServerPort", 6309) > 0) {
            MainWin.nsThread = new Thread(new NineServer(MainWin.config.getInt("NineServerPort", 6309)));
            MainWin.nsThread.setDaemon(true);
            MainWin.nsThread.start();
            System.out.println("NineServer thread started");
        }
        
        // Skip SWT GrapherThread - replaced with GrapherThreadFX in MainWindowController
        // GrapherThreadFX will be started after JavaFX UI is initialized
        System.out.println("Skipping SWT GrapherThread (replaced with GrapherThreadFX)");
        
        System.out.println("=== MainWinFX.init() completed ===");
        System.out.println("About to return from init() - start() should be called next");
        System.out.flush();
        
        // Check if Platform is initialized
        try {
            boolean isFxInitialized = Platform.isFxApplicationThread() || Platform.isImplicitExit();
            System.out.println("Platform check - isFxApplicationThread: " + Platform.isFxApplicationThread());
            System.out.flush();
        } catch (Exception e) {
            System.err.println("Exception checking Platform: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Use System.err for immediate output (unbuffered)
        System.err.println("=== MainWinFX.start() called ===");
        System.err.println("Thread: " + Thread.currentThread().getName());
        System.err.println("Is JavaFX thread: " + Platform.isFxApplicationThread());
        System.err.println("Stage is null: " + (primaryStage == null));
        System.err.flush();
        
        System.err.println("About to enter try block...");
        System.err.flush();
        
        try {
            System.err.println("INSIDE try block");
            System.err.flush();
            System.out.println("=== MainWinFX.start() called ===");
            System.out.flush();
            System.out.println("Thread: " + Thread.currentThread().getName());
            System.out.flush();
            System.out.println("Is JavaFX thread: " + Platform.isFxApplicationThread());
            System.out.flush();
            System.out.println("Stage is null: " + (primaryStage == null));
            System.out.flush();
            
            // Set uncaught exception handler on JavaFX Application Thread FIRST
            System.out.println("Setting uncaught exception handler...");
            System.out.flush();
            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.err.println("=== UNCAUGHT EXCEPTION IN JAVAFX THREAD ===");
                    System.err.println("Thread: " + t.getName());
                    System.err.println("Exception: " + e.getClass().getName());
                    System.err.println("Message: " + e.getMessage());
                    System.err.println("Stack trace:");
                    e.printStackTrace();
                    System.err.flush();
                    
                    try {
                        logger.error("Uncaught exception in JavaFX thread", e);
                    } catch (Exception logEx) {
                        System.err.println("Could not log exception: " + logEx.getMessage());
                    }
                    
                    Platform.exit();
                }
            });
            System.out.println("Exception handler set");
            System.out.flush();
            
            System.err.println("About to call logger...");
            System.out.println("About to call logger...");
            System.out.flush();
            // Skip logger call for now - it seems to be causing issues
            // try {
            //     System.err.println("Calling logger.info()...");
            //     logger.info("JavaFX start() method called");
            //     System.err.println("Logger call succeeded");
            //     System.out.println("Logger call succeeded");
            //     System.out.flush();
            // } catch (Exception logEx) {
            //     System.err.println("=== EXCEPTION CALLING LOGGER ===");
            //     System.err.println("Exception type: " + logEx.getClass().getName());
            //     System.err.println("Exception message: " + logEx.getMessage());
            //     logEx.printStackTrace();
            //     System.err.flush();
            // }
            System.err.println("Skipped logger call, continuing...");
            System.out.println("Skipped logger call, continuing...");
            System.out.flush();
        } catch (Exception outerEx) {
            System.err.println("=== EXCEPTION IN START() BEFORE MAIN LOGIC ===");
            System.err.println("Exception: " + outerEx.getClass().getName());
            System.err.println("Message: " + outerEx.getMessage());
            outerEx.printStackTrace();
            System.err.flush();
            throw outerEx;
        }
        
        System.err.println("Setting stage title...");
        System.out.println("Setting stage title...");
        System.out.flush();
        
        // Set application properties with maximum defensive coding
        System.err.println("About to call primaryStage.setTitle()...");
        System.out.println("About to call primaryStage.setTitle()...");
        System.out.flush();
        
        try {
            System.err.println("INSIDE setTitle() try block");
            String title = "DriveWire User Interface";
            System.err.println("Title string created: " + title);
            
            // Check if Platform is still running
            if (!Platform.isFxApplicationThread()) {
                System.err.println("WARNING: Not on JavaFX thread when calling setTitle()!");
            }
            
            System.err.println("Calling primaryStage.setTitle() now...");
            primaryStage.setTitle(title);
            System.err.println("setTitle() call completed");
            
            // Verify it was set
            String actualTitle = primaryStage.getTitle();
            System.err.println("Title after set: " + actualTitle);
            
            System.err.println("Stage title set successfully");
            System.out.println("Stage title set successfully");
            System.out.flush();
            
        } catch (Throwable e) {  // Catch everything including Errors
            System.err.println("=== THROWABLE CAUGHT IN setTitle() ===");
            System.err.println("Throwable type: " + e.getClass().getName());
            System.err.println("Throwable message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            System.err.flush();
            // Don't rethrow - try to continue
            System.err.println("Continuing despite setTitle() throwable...");
            System.out.println("Continuing despite setTitle() throwable...");
            System.out.flush();
        }
        
        System.err.println("After setTitle() try-catch block");
        System.out.println("After setTitle() try-catch block");
        System.out.flush();
        
        // Add a small delay to see if timing helps
        try {
            System.err.println("Waiting 50ms after setTitle()...");
            Thread.sleep(50);
            System.err.println("Wait completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted during wait");
        }
        
        System.out.println("About to enter try block for FXML loading...");
        System.out.flush();
        // Load and show main window
        try {
            System.out.println("Inside try block, about to log FXML loading...");
            System.out.flush();
            logger.info("Loading FXML file...");
            System.out.println("Getting FXML resource URL...");
            System.out.flush();
            java.net.URL fxmlUrl = MainWinFX.class.getResource("/com/groupunix/drivewireui/fxml/MainWindow.fxml");
            System.out.println("FXML URL retrieved: " + (fxmlUrl != null ? fxmlUrl.toString() : "NULL"));
            System.out.flush();
            if (fxmlUrl == null) {
                System.err.println("ERROR: FXML file not found!");
                System.err.flush();
                throw new Exception("FXML file not found: /com/groupunix/drivewireui/fxml/MainWindow.fxml");
            }
            logger.info("FXML URL: " + fxmlUrl);
            System.out.println("FXML URL is valid, creating FXMLLoader...");
            System.out.flush();
            
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            logger.info("Loading FXML...");
            System.out.println("About to call loader.load()...");
            javafx.scene.Parent root;
            try {
                root = loader.load();
                System.out.println("loader.load() completed successfully");
            } catch (Exception loadEx) {
                System.err.println("Exception during loader.load(): " + loadEx.getMessage());
                loadEx.printStackTrace();
                throw loadEx;
            }
            logger.info("FXML loaded successfully");
            System.out.println("FXML loaded successfully");
            
            MainWindowController controller = loader.getController();
            if (controller == null) {
                throw new Exception("Controller is null - check FXML fx:controller attribute");
            }
            logger.info("Controller loaded: " + controller.getClass().getName());
            
            controller.setPrimaryStage(primaryStage);
            
            // Set controller reference in MainWin for compatibility
            MainWin.mainWindowController = controller;
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL cssUrl = MainWinFX.class.getResource("/com/groupunix/drivewireui/css/application.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                logger.info("CSS loaded");
            } else {
                logger.warn("CSS file not found");
            }
            
            primaryStage.setScene(scene);
            
            // Restore window position and size from config
            int width = MainWin.config.getInt("MainWin_Width", 753);
            int height = MainWin.config.getInt("MainWin_Height", 486);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            
            if (MainWin.config.containsKey("MainWin_x") && MainWin.config.containsKey("MainWin_y")) {
                int x = MainWin.config.getInt("MainWin_x", 0);
                int y = MainWin.config.getInt("MainWin_y", 0);
                primaryStage.setX(x);
                primaryStage.setY(y);
            }
            
            // Handle window close
            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                logger.info("Window close requested");
                
                // Save UI layout before shutdown
                if (MainWin.mainWindowController != null) {
                    try {
                        java.lang.reflect.Method saveMethod = MainWin.mainWindowController.getClass().getMethod("saveUILayout");
                        saveMethod.invoke(MainWin.mainWindowController);
                        logger.info("UI layout saved");
                    } catch (Exception ex) {
                        logger.warn("Failed to save UI layout: " + ex.getMessage());
                    }
                }
                
                MainWin.doShutdown();
                Platform.exit();
            });
            
            logger.info("Showing primary stage...");
            primaryStage.show();
            logger.info("Primary stage shown");
            
            // Start local server if configured (now that UI is shown)
            if (MainWin.config.getBoolean("LocalServer", true) && !noServer) {
                logger.info("Starting local server now that UI is shown...");
                Platform.runLater(() -> {
                    startDWServer(applicationArgs);
                });
            }
            
            // Ensure host/port are loaded from config before starting SyncThread
            String currentHost = MainWin.getHost();
            int currentPort = MainWin.getPort();
            System.out.println("=== Current host/port check: Host=" + currentHost + ", Port=" + currentPort + " ===");
            System.err.println("=== Current host/port check: Host=" + currentHost + ", Port=" + currentPort + " ===");
            
            if (currentHost == null || currentPort == 0) {
                System.out.println("=== Host/Port not set, loading from config ===");
                System.err.println("=== Host/Port not set, loading from config ===");
                // Load host/port from config if not already set
                if (MainWin.config != null) {
                    try {
                        // Use reflection to access private fields if needed
                        java.lang.reflect.Field hostField = MainWin.class.getDeclaredField("host");
                        hostField.setAccessible(true);
                        java.lang.reflect.Field portField = MainWin.class.getDeclaredField("port");
                        portField.setAccessible(true);
                        java.lang.reflect.Field instanceField = MainWin.class.getDeclaredField("instance");
                        instanceField.setAccessible(true);
                        
                        String hostValue = MainWin.config.getString("LastHost", MainWin.default_Host);
                        int portValue = MainWin.config.getInt("LastPort", MainWin.default_Port);
                        int instanceValue = MainWin.config.getInt("LastInstance", MainWin.default_Instance);
                        
                        hostField.set(null, hostValue);
                        portField.set(null, portValue);
                        instanceField.set(null, instanceValue);
                        
                        System.out.println("=== Loaded from config: Host=" + hostValue + ", Port=" + portValue + ", Instance=" + instanceValue + " ===");
                        System.err.println("=== Loaded from config: Host=" + hostValue + ", Port=" + portValue + ", Instance=" + instanceValue + " ===");
                    } catch (Exception e) {
                        System.err.println("=== Error setting host/port via reflection: " + e.getMessage() + " ===");
                        // Fallback: try direct access
                        MainWin.host = MainWin.config.getString("LastHost", MainWin.default_Host);
                        MainWin.port = MainWin.config.getInt("LastPort", MainWin.default_Port);
                        MainWin.instance = MainWin.config.getInt("LastInstance", MainWin.default_Instance);
                    }
                } else {
                    // Fallback to defaults
                    try {
                        java.lang.reflect.Field hostField = MainWin.class.getDeclaredField("host");
                        hostField.setAccessible(true);
                        java.lang.reflect.Field portField = MainWin.class.getDeclaredField("port");
                        portField.setAccessible(true);
                        java.lang.reflect.Field instanceField = MainWin.class.getDeclaredField("instance");
                        instanceField.setAccessible(true);
                        
                        hostField.set(null, MainWin.default_Host);
                        portField.set(null, MainWin.default_Port);
                        instanceField.set(null, MainWin.default_Instance);
                        
                        System.out.println("=== Using defaults: Host=" + MainWin.default_Host + ", Port=" + MainWin.default_Port + ", Instance=" + MainWin.default_Instance + " ===");
                        System.err.println("=== Using defaults: Host=" + MainWin.default_Host + ", Port=" + MainWin.default_Port + ", Instance=" + MainWin.default_Instance + " ===");
                    } catch (Exception e) {
                        System.err.println("=== Error setting defaults: " + e.getMessage() + " ===");
                    }
                }
            }
            
            // Set ready flag so SyncThread can start
            MainWin.setReady(true);
            System.out.println("=== MainWin.ready set to true ===");
            System.err.println("=== MainWin.ready set to true ===");
            
            // Initialize connection - do this immediately, not in Platform.runLater
            // SyncThread needs to start as soon as possible
            logger.info("Initializing server connection...");
            System.out.println("=== Calling restartServerConn() ===");
            System.err.println("=== Calling restartServerConn() ===");
            MainWin.restartServerConn();
            System.out.println("=== restartServerConn() completed ===");
            System.err.println("=== restartServerConn() completed ===");
            
        } catch (Exception e) {
            System.err.println("=== EXCEPTION IN start() METHOD ===");
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception message: " + e.getMessage());
            System.err.flush();
            logger.error("Failed to load main window", e);
            e.printStackTrace();
            System.err.println("Exception details: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            System.err.flush();
            
            // Try to show error dialog, but if JavaFX isn't initialized, just print to console
            try {
                System.out.println("Attempting to show error dialog...");
                System.out.flush();
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to load UI");
                alert.setContentText(e.getMessage() + "\n\nCheck console for details.");
                alert.showAndWait();
                System.out.println("Error dialog shown");
                System.out.flush();
            } catch (Exception ex) {
                System.err.println("Could not show error dialog: " + ex.getMessage());
                ex.printStackTrace();
                System.err.flush();
            }
            System.out.println("Calling Platform.exit()...");
            System.out.flush();
            Platform.exit();
            System.out.println("Platform.exit() called");
            System.out.flush();
        }
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup on application exit
        MainWin.doShutdown();
        super.stop();
    }
    
    public static void main(String[] args) {
        System.out.println("=== MainWinFX.main() called ===");
        System.out.println("JavaFX version: " + System.getProperty("javafx.version", "unknown"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        
        applicationArgs = args;
        noServer = UIUtils.hasArg(args, "noserver");
        
        // Set thread name
        Thread.currentThread().setName("dwuiMain-" + Thread.currentThread().getId());
        Thread.currentThread().setContextClassLoader(MainWinFX.class.getClassLoader());
        
        System.out.println("Launching JavaFX application...");
        System.out.println("Platform.isFxApplicationThread: " + Platform.isFxApplicationThread());
        
        // Launch JavaFX application - this will call init() then start()
        // Note: launch() blocks until Platform.exit() is called
        try {
            System.out.println("Calling Application.launch()...");
            System.out.flush();
            
            // Set a default uncaught exception handler BEFORE launch
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                System.err.println("=== DEFAULT UNCAUGHT EXCEPTION HANDLER ===");
                System.err.println("Thread: " + t.getName());
                System.err.println("Exception: " + e.getClass().getName());
                System.err.println("Message: " + e.getMessage());
                e.printStackTrace();
                System.err.flush();
            });
            
            launch(MainWinFX.class, args);
            System.out.println("JavaFX launch() returned (application closed)");
            System.out.flush();
        } catch (IllegalStateException e) {
            System.err.println("JavaFX already initialized: " + e.getMessage());
            // JavaFX might already be running, try to create stage manually
            Platform.runLater(() -> {
                try {
                    MainWinFX app = new MainWinFX();
                    app.init();
                    Stage stage = new Stage();
                    app.start(stage);
                } catch (Exception ex) {
                    System.err.println("Failed to start manually: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to launch JavaFX application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void loadConfig() {
        try {
            MainWin.config = new XMLConfiguration(MainWin.configfile);
            MainWin.master = new XMLConfiguration("master.xml");
        } catch (ConfigurationException e) {
            logger.fatal("Failed to load configuration: " + e.getMessage());
            System.exit(-1);
        }
    }
    
    private static void startDWServer(final String[] args) {
        MainWin.dwThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MainWin.servermagic = DriveWireServer.getMagic();
                    DriveWireServer.main(args);
                } catch (ConfigurationException e) {
                    logger.fatal(e.getMessage());
                    Platform.exit();
                    System.exit(-1);
                }
            }
        });
        MainWin.dwThread.setDaemon(true);
        MainWin.dwThread.start();
    }
}

