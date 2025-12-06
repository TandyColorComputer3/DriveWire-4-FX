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
 * Test with only GrapherThread - to see if it's the culprit.
 */
public class MainWinFXTestGrapher extends Application {
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFXTestGrapher.init() called ===");
        
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        MainWin.config = new org.apache.commons.configuration.XMLConfiguration(MainWin.configfile);
        System.setProperty("drivewire.ui.mode", "true");
        
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
        
        MainWin.os9BufferGroups = new Vector<OS9BufferGroup>();
        MainWin.os9BufferGroups.setSize(256);
        
        // Start ONLY GrapherThread
        System.out.println("Starting GrapherThread...");
        Thread gt = new Thread(new GrapherThread());
        gt.setDaemon(true);
        gt.start();
        System.out.println("GrapherThread started");
        
        System.out.println("=== MainWinFXTestGrapher.init() completed ===");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println("=== MainWinFXTestGrapher.start() called ===");
        
        Label label = new Label("DriveWire JavaFX - GrapherThread Test\n\nTesting with ONLY GrapherThread.");
        VBox root = new VBox(label);
        Scene scene = new Scene(root, 500, 300);
        
        primaryStage.setTitle("DriveWire (GrapherThread Test)");
        primaryStage.setScene(scene);
        primaryStage.show();
        System.err.println("Stage shown");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

