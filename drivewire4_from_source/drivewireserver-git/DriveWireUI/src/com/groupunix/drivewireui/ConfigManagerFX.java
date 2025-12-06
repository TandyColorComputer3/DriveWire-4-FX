package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Configuration Manager for DriveWire4.
 * Allows creating, deleting, and switching between configurations.
 */
public class ConfigManagerFX {
    
    private Stage dialogStage;
    private Window owner;
    private ObservableList<ConfigInfo> configList;
    
    public ConfigManagerFX(Window owner) {
        this.owner = owner;
    }
    
    /**
     * Show the configuration manager dialog.
     */
    public void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setTitle("Configuration Manager");
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(700);
        dialogStage.setMinHeight(500);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        
        // Title
        Label titleLabel = new Label("Configuration Manager");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        root.setTop(titleBox);
        
        // Main content area
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));
        
        Label descLabel = new Label("Manage DriveWire4 configurations. Each configuration can have different serial ports, TCP settings, and other options.");
        descLabel.setWrapText(true);
        
        // Configuration list
        Label listLabel = new Label("Available Configurations:");
        listLabel.setStyle("-fx-font-weight: bold;");
        
        TableView<ConfigInfo> configTable = new TableView<>();
        configTable.setPrefHeight(300);
        
        TableColumn<ConfigInfo, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell -> cell.getValue().nameProperty());
        nameColumn.setPrefWidth(200);
        
        TableColumn<ConfigInfo, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cell -> cell.getValue().typeProperty());
        typeColumn.setPrefWidth(150);
        
        TableColumn<ConfigInfo, String> deviceColumn = new TableColumn<>("Device");
        deviceColumn.setCellValueFactory(cell -> cell.getValue().deviceProperty());
        deviceColumn.setPrefWidth(200);
        
        TableColumn<ConfigInfo, String> fileColumn = new TableColumn<>("Config File");
        fileColumn.setCellValueFactory(cell -> cell.getValue().fileProperty());
        fileColumn.setPrefWidth(150);
        
        configTable.getColumns().addAll(nameColumn, typeColumn, deviceColumn, fileColumn);
        
        configList = FXCollections.observableArrayList();
        configTable.setItems(configList);
        
        // Selection handler
        configTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            // Enable/disable buttons based on selection
        });
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button createButton = new Button("Create New");
        createButton.setStyle("-fx-font-weight: bold;");
        createButton.setOnAction(e -> createNewConfig());
        
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteSelectedConfig(configTable));
        
        Button switchButton = new Button("Switch To");
        switchButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        switchButton.setOnAction(e -> switchToConfig(configTable));
        
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        editButton.setOnAction(e -> editSelectedConfig(configTable));
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshConfigList());
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialogStage.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBox.getChildren().addAll(createButton, deleteButton, switchButton, editButton, refreshButton, spacer, closeButton);
        
        contentBox.getChildren().addAll(descLabel, listLabel, configTable, buttonBox);
        root.setCenter(contentBox);
        
        Scene scene = new Scene(root, 800, 550);
        dialogStage.setScene(scene);
        
        // Load configurations
        refreshConfigList();
        
        dialogStage.show();
    }
    
    private void refreshConfigList() {
        configList.clear();
        
        try {
            // Get the config directory
            String configDir = ".";
            File configDirFile = new File(configDir);
            
            // Use a Set to track files we've already added (by canonical path to avoid duplicates)
            java.util.Set<String> addedFiles = new java.util.HashSet<>();
            
            // Look for config.xml files
            File[] configFiles = configDirFile.listFiles((dir, name) -> 
                name.equals("config.xml") || name.matches("config-.*\\.xml"));
            
            if (configFiles != null) {
                for (File configFile : configFiles) {
                    try {
                        String canonicalPath = configFile.getCanonicalPath();
                        if (!addedFiles.contains(canonicalPath)) {
                            ConfigInfo info = loadConfigInfo(configFile);
                            if (info != null) {
                                // Check if this is the active config
                                boolean isActive = configFile.getName().equals("config.xml");
                                info.setActive(isActive);
                                
                                configList.add(info);
                                addedFiles.add(canonicalPath);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading config from " + configFile.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            // Also check for the current active config (if not already added)
            try {
                String serverConfigFile = "config.xml";
                org.apache.commons.configuration.HierarchicalConfiguration uiConfig = MainWin.getInstanceConfig();
                if (uiConfig != null) {
                    serverConfigFile = uiConfig.getString("ServerConfigFile", "config.xml");
                } else if (MainWin.config != null) {
                    serverConfigFile = MainWin.config.getString("ServerConfigFile", "config.xml");
                }
                
                File currentConfig = new File(serverConfigFile);
                if (currentConfig.exists()) {
                    String canonicalPath = currentConfig.getCanonicalPath();
                    if (!addedFiles.contains(canonicalPath)) {
                        ConfigInfo currentInfo = loadConfigInfo(currentConfig);
                        if (currentInfo != null) {
                            currentInfo.setActive(true);
                            configList.add(0, currentInfo); // Add at top
                            addedFiles.add(canonicalPath);
                        }
                    } else {
                        // Update the existing entry to mark it as active
                        for (ConfigInfo info : configList) {
                            if (info.getFile().equals(currentConfig.getName()) || 
                                info.getFile().equals(serverConfigFile)) {
                                info.setActive(true);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading current config: " + e.getMessage());
            }
            
            // Sort list: active config first, then alphabetically by name
            FXCollections.sort(configList, (a, b) -> {
                if (a.isActive() && !b.isActive()) return -1;
                if (!a.isActive() && b.isActive()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load configurations");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private ConfigInfo loadConfigInfo(File configFile) {
        try {
            XMLConfiguration config = new XMLConfiguration(configFile);
            
            String name = config.getString("[@name]", "Unnamed");
            String desc = config.getString("[@desc]", "");
            String deviceType = config.getString("DeviceType", "unknown");
            String device = "";
            
            if ("serial".equals(deviceType)) {
                device = config.getString("SerialDevice", "Not set");
            } else if ("tcp-server".equals(deviceType)) {
                device = "TCP Server (port " + config.getInt("TCPServerPort", 0) + ")";
            } else if ("tcp-client".equals(deviceType)) {
                device = config.getString("TCPClientHost", "Not set") + ":" + config.getInt("TCPClientPort", 0);
            }
            
            boolean isActive = configFile.getName().equals("config.xml");
            
            return new ConfigInfo(name, desc, deviceType, device, configFile.getName(), isActive);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void createNewConfig() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Configuration");
        dialog.setHeaderText("Enter Configuration Name");
        dialog.setContentText("Name:");
        
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isEmpty()) {
                try {
                    // Create a new config file
                    String configFileName = "config-" + name.replaceAll("[^a-zA-Z0-9]", "_") + ".xml";
                    File newConfigFile = new File(configFileName);
                    
                    // Copy from default if it exists
                    File defaultConfig = new File("default/serverconfig.xml");
                    if (defaultConfig.exists()) {
                        Files.copy(defaultConfig.toPath(), newConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // Create minimal config
                        XMLConfiguration newConfig = new XMLConfiguration();
                        newConfig.setFileName(configFileName);
                        newConfig.addProperty("AutoCreated", true);
                        newConfig.addProperty("[@name]", name);
                        newConfig.addProperty("[@desc]", "Created " + new java.sql.Timestamp(System.currentTimeMillis()).toString());
                        newConfig.save();
                    }
                    
                    // Update the name in the config
                    XMLConfiguration config = new XMLConfiguration(newConfigFile);
                    config.setProperty("[@name]", name);
                    config.setProperty("[@desc]", "Created " + new java.sql.Timestamp(System.currentTimeMillis()).toString());
                    config.save();
                    
                    refreshConfigList();
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Configuration Created");
                    alert.setContentText("Configuration '" + name + "' has been created.\n\nFile: " + configFileName);
                    alert.showAndWait();
                    
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to create configuration");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
    
    private void deleteSelectedConfig(TableView<ConfigInfo> configTable) {
        ConfigInfo selected = configTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a configuration to delete");
            alert.showAndWait();
            return;
        }
        
        if (selected.isActive()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Delete");
            alert.setHeaderText("Cannot delete active configuration");
            alert.setContentText("Please switch to another configuration first.");
            alert.showAndWait();
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Configuration");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?\n\nFile: " + selected.getFile());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Get the actual file path (handle relative paths)
                    File configFile = new File(selected.getFile());
                    if (!configFile.exists()) {
                        configFile = new File(".", selected.getFile());
                    }
                    
                    if (configFile.exists()) {
                        String deletedName = selected.getName();
                        configFile.delete();
                        
                        // Clear and refresh the list to avoid duplicates
                        configList.clear();
                        refreshConfigList();
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Configuration Deleted");
                        alert.setContentText("Configuration '" + deletedName + "' has been deleted.");
                        alert.showAndWait();
                    } else {
                        throw new IOException("Configuration file not found: " + selected.getFile());
                    }
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to delete configuration");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
    
    private void switchToConfig(TableView<ConfigInfo> configTable) {
        ConfigInfo selected = configTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a configuration to switch to");
            alert.showAndWait();
            return;
        }
        
        if (selected.isActive()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Already Active");
            alert.setHeaderText("This configuration is already active");
            alert.showAndWait();
            return;
        }
        
        try {
            // Backup current config
            File currentConfig = new File("config.xml");
            if (currentConfig.exists()) {
                String backupName = "config-backup-" + System.currentTimeMillis() + ".xml";
                Files.copy(currentConfig.toPath(), new File(backupName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Copy selected config to config.xml
            File selectedConfig = new File(selected.getFile());
            if (selectedConfig.exists()) {
                Files.copy(selectedConfig.toPath(), currentConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Update UI config to point to the new config (if available)
                try {
                    org.apache.commons.configuration.HierarchicalConfiguration uiConfig = MainWin.getInstanceConfig();
                    if (uiConfig != null) {
                        uiConfig.setProperty("ServerConfigFile", "config.xml");
                        // Try to save if it's an XMLConfiguration
                        if (uiConfig instanceof XMLConfiguration) {
                            ((XMLConfiguration) uiConfig).save();
                        }
                    } else if (MainWin.config != null) {
                        // Fallback: update MainWin.config
                        MainWin.config.setProperty("ServerConfigFile", "config.xml");
                        if (MainWin.config instanceof XMLConfiguration) {
                            ((XMLConfiguration) MainWin.config).save();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not update UI config (config file switched): " + e.getMessage());
                    // Continue - the config file is already switched
                }
                
                refreshConfigList();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Configuration Switched");
                alert.setHeaderText("Configuration Changed");
                alert.setContentText("Switched to configuration '" + selected.getName() + "'.\n\nYou may need to restart DriveWire4 for changes to take effect.");
                alert.showAndWait();
                
                dialogStage.close();
            } else {
                throw new IOException("Configuration file not found: " + selected.getFile());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to switch configuration");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void editSelectedConfig(TableView<ConfigInfo> configTable) {
        ConfigInfo selected = configTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a configuration to edit");
            alert.showAndWait();
            return;
        }
        
        try {
            // Get the actual file path
            File configFile = new File(selected.getFile());
            if (!configFile.exists()) {
                configFile = new File(".", selected.getFile());
            }
            
            if (!configFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Configuration file not found");
                alert.setContentText("File: " + selected.getFile());
                alert.showAndWait();
                return;
            }
            
            // Open configuration editor dialog
            ConfigEditorFX editor = new ConfigEditorFX(dialogStage, configFile);
            editor.show();
            
            // Refresh list after editing
            refreshConfigList();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open configuration editor");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Data class for configuration information.
     */
    private static class ConfigInfo {
        private javafx.beans.property.SimpleStringProperty name;
        private javafx.beans.property.SimpleStringProperty desc;
        private javafx.beans.property.SimpleStringProperty type;
        private javafx.beans.property.SimpleStringProperty device;
        private javafx.beans.property.SimpleStringProperty file;
        private javafx.beans.property.SimpleBooleanProperty active;
        
        public ConfigInfo(String name, String desc, String type, String device, String file, boolean active) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.desc = new javafx.beans.property.SimpleStringProperty(desc);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.device = new javafx.beans.property.SimpleStringProperty(device);
            this.file = new javafx.beans.property.SimpleStringProperty(file);
            this.active = new javafx.beans.property.SimpleBooleanProperty(active);
        }
        
        public String getName() { return name.get(); }
        public String getDesc() { return desc.get(); }
        public String getType() { return type.get(); }
        public String getDevice() { return device.get(); }
        public String getFile() { return file.get(); }
        public boolean isActive() { return active.get(); }
        
        public void setActive(boolean active) { this.active.set(active); }
        
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty descProperty() { return desc; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        public javafx.beans.property.StringProperty deviceProperty() { return device; }
        public javafx.beans.property.StringProperty fileProperty() { return file; }
        public javafx.beans.property.BooleanProperty activeProperty() { return active; }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConfigInfo) {
                return file.get().equals(((ConfigInfo) obj).file.get());
            }
            return false;
        }
    }
}

