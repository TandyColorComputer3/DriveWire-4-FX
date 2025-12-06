package com.groupunix.drivewireui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.configuration.HierarchicalConfiguration;
import com.groupunix.drivewireui.library.LibraryItem;
import com.groupunix.drivewireui.library.MountedFolderLibraryItem;
import com.groupunix.drivewireui.library.FolderLibraryItem;
import com.groupunix.drivewireui.library.CloudLibraryItem;
import com.groupunix.drivewireui.nineserver.OS9BufferGroup;
import com.groupunix.drivewireui.nineserver.NineServer;
import com.groupunix.drivewireui.GrapherThread;
import java.util.Vector;

/**
 * Step-by-step MainWinFX - adds back initialization piece by piece to find the breaking point.
 */
public class MainWinFXStepByStep extends Application {
    
    private static String[] applicationArgs;
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFXStepByStep.init() called ===");
        
        // Step 1: Basic logging
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        System.out.println("Step 1: Logging initialized");
        
        // Step 2: Load config
        MainWin.config = new org.apache.commons.configuration.XMLConfiguration(MainWin.configfile);
        System.out.println("Step 2: Configuration loaded");
        
        // Step 3: Set drivewire.ui.mode
        System.setProperty("drivewire.ui.mode", "true");
        System.out.println("Step 3: drivewire.ui.mode set");
        
        // Step 4: Initialize library roots
        System.out.println("Step 4: Initializing library roots...");
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
        System.out.println("Step 4: Library roots initialized");
        
        // Step 5: Initialize OS9 buffer groups
        System.out.println("Step 5: Initializing OS9 buffer groups...");
        MainWin.os9BufferGroups = new Vector<OS9BufferGroup>();
        MainWin.os9BufferGroups.setSize(256);
        System.out.println("Step 5: OS9 buffer groups initialized");
        
        // Step 6: Start NineServer thread (if configured)
        System.out.println("Step 6: Checking NineServer...");
        if (MainWin.config.getInt("NineServerPort", 6309) > 0) {
            System.out.println("Step 6: Starting NineServer thread...");
            MainWin.nsThread = new Thread(new NineServer(MainWin.config.getInt("NineServerPort", 6309)));
            MainWin.nsThread.setDaemon(true);
            MainWin.nsThread.start();
            System.out.println("Step 6: NineServer thread started");
        } else {
            System.out.println("Step 6: NineServer disabled");
        }
        
        // Step 7: Start grapher thread
        System.out.println("Step 7: Starting GrapherThread...");
        Thread gt = new Thread(new GrapherThread());
        gt.setDaemon(true);
        gt.start();
        System.out.println("Step 7: GrapherThread started");
        
        System.out.println("=== MainWinFXStepByStep.init() completed ===");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println("=== MainWinFXStepByStep.start() called ===");
        System.err.flush();
        
        try {
            Label label = new Label("DriveWire JavaFX - Step-by-Step Test\n\nAll initialization steps completed.\nWindow should be visible!");
            VBox root = new VBox(label);
            Scene scene = new Scene(root, 500, 300);
            
            System.err.println("About to call setTitle()...");
            System.err.flush();
            primaryStage.setTitle("DriveWire User Interface (Step-by-Step)");
            System.err.println("setTitle() completed");
            System.err.flush();
            
            primaryStage.setScene(scene);
            primaryStage.show();
            System.err.println("Stage shown - window should be visible!");
            System.err.flush();
            
        } catch (Throwable e) {
            System.err.println("=== EXCEPTION ===");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
            throw e;
        }
    }
    
    public static void main(String[] args) {
        applicationArgs = args;
        System.out.println("=== MainWinFXStepByStep.main() called ===");
        launch(args);
    }
}

