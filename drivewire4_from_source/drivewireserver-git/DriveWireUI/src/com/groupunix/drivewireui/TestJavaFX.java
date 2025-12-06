package com.groupunix.drivewireui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Minimal JavaFX test to verify JavaFX is working correctly.
 */
public class TestJavaFX extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== TestJavaFX.start() called ===");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Is JavaFX thread: " + javafx.application.Platform.isFxApplicationThread());
        
        try {
            System.out.println("Creating simple UI...");
            
            Label label = new Label("JavaFX Test Window - If you see this, JavaFX is working!");
            VBox root = new VBox(label);
            Scene scene = new Scene(root, 400, 200);
            
            primaryStage.setTitle("JavaFX Test");
            System.out.println("Title set successfully");
            
            primaryStage.setScene(scene);
            System.out.println("Scene set successfully");
            
            primaryStage.show();
            System.out.println("Stage shown successfully");
            System.out.println("=== TestJavaFX.start() completed ===");
            
        } catch (Exception e) {
            System.err.println("=== EXCEPTION IN TestJavaFX.start() ===");
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== TestJavaFX.main() called ===");
        System.out.println("JavaFX version: " + System.getProperty("javafx.version", "unknown"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        
        try {
            launch(args);
            System.out.println("TestJavaFX launch() returned");
        } catch (Exception e) {
            System.err.println("Failed to launch TestJavaFX: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

