package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controller for the browser view component.
 * Replaces SWT Browser with JavaFX WebView.
 */
public class BrowserController {
    
    @FXML private WebView webView;
    @FXML private ComboBox<String> urlComboBox;
    @FXML private Button backButton;
    @FXML private Button forwardButton;
    @FXML private Button reloadButton;
    @FXML private Button driveToggleButton;
    @FXML private Spinner<Integer> driveSpinner;
    @FXML private Button helpButton;
    
    private WebEngine webEngine;
    private String initialUrl;
    
    /**
     * Initialize the browser controller.
     */
    @FXML
    public void initialize() {
        webEngine = webView.getEngine();
        
        // Set up WebEngine listeners
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Platform.runLater(() -> {
                    if (newValue != null && !newValue.equals(urlComboBox.getValue())) {
                        urlComboBox.setValue(newValue);
                    }
                });
            }
        });
        
        webEngine.titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Platform.runLater(() -> {
                    // Update tab title if needed
                });
            }
        });
        
        // Set up navigation button states
        updateNavigationButtons();
        
        // Set up drive spinner
        driveSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                MainWin.sdisk = newVal;
                if (MainWin.table != null) {
                    // Update table selection if SWT table still exists
                    // TODO: Update JavaFX table selection
                }
            }
        });
        
        // Initialize drive spinner with current selection
        if (MainWin.sdisk >= 0) {
            driveSpinner.getValueFactory().setValue(MainWin.sdisk);
        }
        
        // Load initial URL if provided
        if (initialUrl != null && !initialUrl.isEmpty()) {
            loadUrl(initialUrl);
        }
    }
    
    /**
     * Set the initial URL to load.
     */
    public void setInitialUrl(String url) {
        this.initialUrl = url;
        if (webEngine != null && url != null && !url.isEmpty()) {
            loadUrl(url);
        }
    }
    
    /**
     * Load a URL in the browser.
     */
    public void loadUrl(String url) {
        if (webEngine != null && url != null && !url.isEmpty()) {
            webEngine.load(url);
        }
    }
    
    /**
     * Update navigation button states based on browser history.
     */
    private void updateNavigationButtons() {
        if (webEngine != null) {
            // JavaFX WebEngine doesn't expose history directly
            // We'll track navigation state ourselves
            backButton.setDisable(false); // Simplified - could track history
            forwardButton.setDisable(false); // Simplified - could track history
        }
    }
    
    @FXML
    private void handleBack() {
        if (webEngine != null) {
            webEngine.getHistory().go(-1);
            updateNavigationButtons();
        }
    }
    
    @FXML
    private void handleForward() {
        if (webEngine != null) {
            webEngine.getHistory().go(1);
            updateNavigationButtons();
        }
    }
    
    @FXML
    private void handleReload() {
        if (webEngine != null) {
            webEngine.reload();
        }
    }
    
    @FXML
    private void handleUrlSelected() {
        String url = urlComboBox.getValue();
        if (url != null && !url.isEmpty()) {
            // Add http:// if no protocol specified
            if (!url.startsWith("http://") && !url.startsWith("https://") && 
                !url.startsWith("file://") && !url.startsWith("dw://")) {
                url = "http://" + url;
            }
            loadUrl(url);
        }
    }
    
    @FXML
    private void handleToggleAppendMode() {
        MainWin.append_mode = !MainWin.append_mode;
        // Update button icon/image based on append mode
        // TODO: Update button appearance
    }
    
    @FXML
    private void handleHelp() {
        String helpUrl = MainWin.config.getString("Browser_helppage", 
            "https://github.com/qbancoffee/drivewire4/wiki/DriveWire-4-Help");
        loadUrl(helpUrl);
    }
    
    /**
     * Get the WebEngine instance.
     */
    public WebEngine getWebEngine() {
        return webEngine;
    }
    
    /**
     * Get the current URL.
     */
    public String getCurrentUrl() {
        return webEngine != null ? webEngine.getLocation() : null;
    }
}

