package com.groupunix.drivewireui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Minimal MainWinFX - gradually adds back initialization to find the breaking point.
 */
public class MainWinFXMinimal extends Application {
    
    private static String[] applicationArgs;
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFXMinimal.init() called ===");
        
        // Step 1: Just basic logging setup
        System.out.println("Step 1: Basic logging...");
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
        System.out.println("Logging initialized");
        
        // Step 2: Load config (this might be the issue)
        System.out.println("Step 2: Loading configuration...");
        try {
            MainWin.config = new org.apache.commons.configuration.XMLConfiguration(MainWin.configfile);
            System.out.println("Configuration loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            // Don't throw - continue without config
        }
        
        // Step 3: Set drivewire.ui.mode (prevent server from exiting)
        System.setProperty("drivewire.ui.mode", "true");
        System.out.println("Set drivewire.ui.mode property");
        
        System.out.println("=== MainWinFXMinimal.init() completed ===");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println("=== MainWinFXMinimal.start() called ===");
        System.err.println("Thread: " + Thread.currentThread().getName());
        System.err.println("Is JavaFX thread: " + Platform.isFxApplicationThread());
        System.err.flush();
        
        try {
            // Create simple UI
            Label label = new Label("DriveWire JavaFX - Minimal Test\n\nConfiguration loaded: " + (MainWin.config != null));
            VBox root = new VBox(label);
            Scene scene = new Scene(root, 500, 300);
            
            System.err.println("About to call setTitle()...");
            System.err.flush();
            primaryStage.setTitle("DriveWire User Interface (Minimal)");
            System.err.println("setTitle() completed");
            System.err.flush();
            
            primaryStage.setScene(scene);
            System.err.println("Scene set");
            System.err.flush();
            
            primaryStage.show();
            System.err.println("Stage shown - window should be visible now!");
            System.err.flush();
            
            System.err.println("=== MainWinFXMinimal.start() completed successfully ===");
            System.err.flush();
            
        } catch (Throwable e) {
            System.err.println("=== EXCEPTION IN MainWinFXMinimal.start() ===");
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
            throw e;
        }
    }
    
    public static void main(String[] args) {
        applicationArgs = args;
        System.out.println("=== MainWinFXMinimal.main() called ===");
        
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Failed to launch: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

