package com.groupunix.drivewireui;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Controller for the Choose Server dialog.
 */
public class ChooseServerDialogController {
    
    @FXML private ComboBox<String> hostComboBox;
    
    private Dialog<String> dialog;
    
    @FXML
    public void initialize() {
        // Load server history
        List<String> sh = MainWin.getServerHistory();
        ObservableList<String> items = FXCollections.observableArrayList();
        
        if (sh != null && !sh.isEmpty()) {
            // Add history items, filtering out nulls and empty strings
            for (int i = sh.size() - 1; i >= 0; i--) {
                String item = sh.get(i);
                if (item != null && !item.trim().isEmpty() && !item.equals("null")) {
                    items.add(item);
                }
            }
        }
        
        hostComboBox.setItems(items);
        
        // Set current host:port (handle null host gracefully)
        String currentHost = MainWin.getHost();
        int currentPort = MainWin.getPort();
        
        if (currentHost != null && !currentHost.trim().isEmpty() && currentPort > 0) {
            hostComboBox.setValue(currentHost + ":" + currentPort);
        } else {
            // Default to localhost if host is null
            hostComboBox.setValue("127.0.0.1:" + (currentPort > 0 ? currentPort : 6800));
        }
    }
    
    public void setDialog(Dialog<String> dialog) {
        this.dialog = dialog;
    }
    
    public String getServerAddress() {
        return hostComboBox.getValue();
    }
    
    public static void showDialog(javafx.stage.Window owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose Server...");
        dialog.initOwner(owner);
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                ChooseServerDialogController.class.getResource("/com/groupunix/drivewireui/fxml/ChooseServerDialog.fxml"));
            DialogPane dialogPane = loader.load();
            ChooseServerDialogController controller = loader.getController();
            controller.setDialog(dialog);
            
            dialog.setDialogPane(dialogPane);
            
            // Set result converter
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton != null && dialogButton.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                    String address = controller.getServerAddress();
                    
                    // Validate address
                    if (address == null || address.trim().isEmpty() || address.equals("null")) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Server Entry");
                        alert.setHeaderText("Server address cannot be empty");
                        alert.setContentText("Please enter a server address and port in the form host:port.\n\nFor example: 127.0.0.1:6800");
                        alert.initOwner(owner);
                        alert.showAndWait();
                        return null; // Keep dialog open
                    }
                    
                    if (address.contains(":")) {
                        String[] hp = address.split(":", 2);
                        if (hp.length == 2 && hp[0] != null && !hp[0].trim().isEmpty() && UIUtils.validateNum(hp[1], 1, 65535)) {
                            // Valid address - connect to server
                            try {
                                MainWin.addServerToHistory(address);
                                MainWin.setHost(hp[0].trim());
                                MainWin.setPort(hp[1].trim());
                                
                                // Restart connection in background thread to avoid blocking UI
                                javafx.concurrent.Task<Void> connectTask = new javafx.concurrent.Task<Void>() {
                                    @Override
                                    protected Void call() throws Exception {
                                        MainWin.restartServerConn();
                                        return null;
                                    }
                                };
                                new Thread(connectTask).start();
                                
                                // Show success message
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                                alert.setTitle("Connecting to Server");
                                alert.setHeaderText("Connecting to " + address);
                                alert.setContentText("Attempting to connect to DriveWire server...\n\nThe connection will be established in the background.");
                                alert.initOwner(owner);
                                alert.showAndWait();
                                
                                return address; // Close dialog
                            } catch (Exception e) {
                                // Show error if connection fails
                                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                alert.setTitle("Connection Error");
                                alert.setHeaderText("Failed to connect to server");
                                alert.setContentText("Error: " + e.getMessage() + "\n\nPlease check the server address and try again.");
                                alert.initOwner(owner);
                                alert.showAndWait();
                                return null; // Keep dialog open
                            }
                        } else {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Invalid Server Entry");
                            alert.setHeaderText("The port entered is not valid");
                            alert.setContentText("Valid TCP port range is 1-65535.\n\nPlease enter a server address and port in the form host:port.\n\nFor example: 127.0.0.1:6800");
                            alert.initOwner(owner);
                            alert.showAndWait();
                            return null; // Keep dialog open
                        }
                    } else {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Server Entry");
                        alert.setHeaderText("The server address format is not valid");
                        alert.setContentText("Please enter a server address and port in the form host:port.\n\nFor example: 127.0.0.1:6800");
                        alert.initOwner(owner);
                        alert.showAndWait();
                        return null; // Keep dialog open
                    }
                }
                // Cancel button or close (X) - just close the dialog
                return null;
            });
            
            // Handle window close request (X button)
            dialog.setOnCloseRequest(event -> {
                // Just close, no action needed
            });
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

