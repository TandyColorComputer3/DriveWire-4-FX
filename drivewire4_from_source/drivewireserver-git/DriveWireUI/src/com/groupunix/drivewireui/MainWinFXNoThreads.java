package com.groupunix.drivewireui;

import javafx.application.Application;
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
import java.util.Vector;

/**
 * MainWinFX without background threads - to test if threads are causing the issue.
 */
public class MainWinFXNoThreads extends Application {
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFXNoThreads.init() called ===");
        
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        System.out.println("Logging initialized");
        
        MainWin.config = new org.apache.commons.configuration.XMLConfiguration(MainWin.configfile);
        System.out.println("Configuration loaded");
        
        System.setProperty("drivewire.ui.mode", "true");
        System.out.println("drivewire.ui.mode set");
        
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
        
        // SKIP threads - don't start NineServer or GrapherThread
        System.out.println("Skipping background threads (NineServer, GrapherThread)");
        
        System.out.println("=== MainWinFXNoThreads.init() completed ===");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println("=== MainWinFXNoThreads.start() called ===");
        System.err.flush();
        
        try {
            Label label = new Label("DriveWire JavaFX - No Threads Test\n\nAll initialization completed WITHOUT background threads.\nWindow should be visible!");
            VBox root = new VBox(label);
            Scene scene = new Scene(root, 500, 300);
            
            System.err.println("About to call setTitle()...");
            System.err.flush();
            primaryStage.setTitle("DriveWire User Interface (No Threads)");
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
        System.out.println("=== MainWinFXNoThreads.main() called ===");
        launch(args);
    }
}

