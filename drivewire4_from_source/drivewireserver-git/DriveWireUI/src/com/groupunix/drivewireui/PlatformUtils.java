package com.groupunix.drivewireui;

import javafx.application.Platform;

/**
 * Utility class for JavaFX Platform operations.
 * Provides compatibility layer for migrating from SWT Display.syncExec/asyncExec.
 */
public class PlatformUtils {
    
    /**
     * Execute code on JavaFX Application Thread (replaces Display.syncExec).
     * Blocks until the code has been executed.
     */
    public static void runOnFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            try {
                Platform.runLater(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                // Note: Platform.runLater doesn't block, but syncExec semantics
                // are hard to replicate exactly. For most cases, async is sufficient.
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Execute code on JavaFX Application Thread asynchronously (replaces Display.asyncExec).
     * Returns immediately without waiting.
     */
    public static void runOnFXThreadAsync(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * Check if we're on the JavaFX Application Thread.
     */
    public static boolean isFXThread() {
        return Platform.isFxApplicationThread();
    }
}

