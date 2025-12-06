package com.groupunix.drivewireui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simplified MainWinFX for debugging - minimal initialization.
 */
public class MainWinFXSimple extends Application {
    
    @Override
    public void init() throws Exception {
        System.out.println("=== MainWinFXSimple.init() called ===");
        System.out.println("Minimal init - no complex initialization");
        System.out.flush();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println("=== MainWinFXSimple.start() called ===");
        System.err.println("Thread: " + Thread.currentThread().getName());
        System.err.println("Is JavaFX thread: " + Platform.isFxApplicationThread());
        System.err.println("Stage is null: " + (primaryStage == null));
        System.err.flush();
        
        try {
            System.err.println("Creating simple UI...");
            System.err.flush();
            
            // Create a simple label
            Label label = new Label("DriveWire JavaFX - Simplified Test\n\nIf you see this window, JavaFX is working!");
            VBox root = new VBox(label);
            Scene scene = new Scene(root, 400, 200);
            
            System.err.println("About to call setTitle()...");
            System.err.flush();
            primaryStage.setTitle("DriveWire User Interface (Simplified)");
            System.err.println("setTitle() completed successfully");
            System.err.flush();
            
            System.err.println("About to set scene...");
            System.err.flush();
            primaryStage.setScene(scene);
            System.err.println("Scene set successfully");
            System.err.flush();
            
            System.err.println("About to show stage...");
            System.err.flush();
            primaryStage.show();
            System.err.println("Stage shown successfully");
            System.err.flush();
            
            System.err.println("=== MainWinFXSimple.start() completed successfully ===");
            System.err.flush();
            
        } catch (Throwable e) {
            System.err.println("=== EXCEPTION IN MainWinFXSimple.start() ===");
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
            throw e;
        }
    }
    
    @Override
    public void stop() throws Exception {
        System.out.println("=== MainWinFXSimple.stop() called ===");
        super.stop();
    }
    
    public static void main(String[] args) {
        System.out.println("=== MainWinFXSimple.main() called ===");
        System.out.println("JavaFX version: " + System.getProperty("javafx.version", "unknown"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        
        try {
            launch(args);
            System.out.println("MainWinFXSimple launch() returned");
        } catch (Exception e) {
            System.err.println("Failed to launch MainWinFXSimple: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

