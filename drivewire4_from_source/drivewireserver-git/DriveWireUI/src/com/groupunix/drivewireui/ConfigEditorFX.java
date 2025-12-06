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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Modern Configuration Editor for DriveWire4.
 * Allows editing configuration properties in a user-friendly table view.
 */
public class ConfigEditorFX {
    
    private Stage dialogStage;
    private Window owner;
    private File configFile;
    private XMLConfiguration config;
    private ObservableList<ConfigProperty> propertyList;
    private TableView<ConfigProperty> propertyTable;
    
    public ConfigEditorFX(Window owner, File configFile) {
        this.owner = owner;
        this.configFile = configFile;
    }
    
    /**
     * Show the configuration editor dialog.
     */
    public void show() {
        try {
            // Load configuration
            config = new XMLConfiguration(configFile);
            
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(owner);
            dialogStage.setTitle("Configuration Editor - " + configFile.getName());
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);
            
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");
            
            // Title
            Label titleLabel = new Label("Configuration Editor");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            HBox titleBox = new HBox(titleLabel);
            titleBox.setAlignment(Pos.CENTER);
            root.setTop(titleBox);
            
            // Main content area
            VBox contentBox = new VBox(15);
            contentBox.setPadding(new Insets(20));
            
            Label descLabel = new Label("Edit configuration properties. Changes are saved immediately.");
            descLabel.setWrapText(true);
            
            // Property list
            Label listLabel = new Label("Configuration Properties:");
            listLabel.setStyle("-fx-font-weight: bold;");
            
            TableView<ConfigProperty> propertyTable = new TableView<>();
            propertyTable.setPrefHeight(400);
            propertyTable.setEditable(true);
            
            TableColumn<ConfigProperty, String> keyColumn = new TableColumn<>("Property");
            keyColumn.setCellValueFactory(cell -> cell.getValue().keyProperty());
            keyColumn.setPrefWidth(300);
            keyColumn.setEditable(false);
            
            TableColumn<ConfigProperty, String> valueColumn = new TableColumn<>("Value");
            valueColumn.setCellValueFactory(cell -> cell.getValue().valueProperty());
            valueColumn.setPrefWidth(400);
            valueColumn.setCellFactory(column -> new EditingCell());
            valueColumn.setOnEditCommit(event -> {
                ConfigProperty prop = event.getRowValue();
                prop.setValue(event.getNewValue());
                saveProperty(prop);
            });
            
            propertyTable.getColumns().addAll(keyColumn, valueColumn);
            
            propertyList = FXCollections.observableArrayList();
            propertyTable.setItems(propertyList);
            
            // Load properties
            loadProperties();
            
            // Store reference to table for filtering
            this.propertyTable = propertyTable;
            
            // Filter/search
            TextField searchField = new TextField();
            searchField.setPromptText("Search properties...");
            searchField.textProperty().addListener((obs, old, newVal) -> {
                filterProperties(propertyTable, newVal);
            });
            
            // Buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            
            Button addButton = new Button("Add Property");
            addButton.setOnAction(e -> addNewProperty());
            
            Button deleteButton = new Button("Delete Selected");
            deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            deleteButton.setOnAction(e -> deleteSelectedProperty(propertyTable));
            
            Button refreshButton = new Button("Refresh");
            refreshButton.setOnAction(e -> loadProperties());
            
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> dialogStage.close());
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            buttonBox.getChildren().addAll(addButton, deleteButton, refreshButton, spacer, closeButton);
            
            contentBox.getChildren().addAll(descLabel, listLabel, searchField, propertyTable, buttonBox);
            root.setCenter(contentBox);
            
            Scene scene = new Scene(root, 900, 650);
            dialogStage.setScene(scene);
            dialogStage.show();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load configuration");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void loadProperties() {
        propertyList.clear();
        
        try {
            // Get all keys from configuration
            List<String> keys = new ArrayList<>();
            java.util.Iterator<String> keysIter = config.getKeys();
            while (keysIter.hasNext()) {
                keys.add(keysIter.next());
            }
            Collections.sort(keys);
            
            for (String key : keys) {
                try {
                    Object value = config.getProperty(key);
                    String valueStr = value != null ? value.toString() : "";
                    propertyList.add(new ConfigProperty(key, valueStr));
                } catch (Exception e) {
                    // Skip properties that can't be read
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load properties");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void filterProperties(TableView<ConfigProperty> table, String filter) {
        if (filter == null || filter.isEmpty()) {
            table.setItems(propertyList);
            return;
        }
        
        ObservableList<ConfigProperty> filtered = FXCollections.observableArrayList();
        String lowerFilter = filter.toLowerCase();
        for (ConfigProperty prop : propertyList) {
            if (prop.getKey().toLowerCase().contains(lowerFilter) || 
                prop.getValue().toLowerCase().contains(lowerFilter)) {
                filtered.add(prop);
            }
        }
        table.setItems(filtered);
    }
    
    private void saveProperty(ConfigProperty prop) {
        try {
            config.setProperty(prop.getKey(), prop.getValue());
            config.save();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save property");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void addNewProperty() {
        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle("Add Property");
        keyDialog.setHeaderText("Enter Property Name");
        keyDialog.setContentText("Property name:");
        
        keyDialog.showAndWait().ifPresent(key -> {
            if (!key.isEmpty()) {
                TextInputDialog valueDialog = new TextInputDialog();
                valueDialog.setTitle("Add Property");
                valueDialog.setHeaderText("Enter Property Value");
                valueDialog.setContentText("Property value:");
                
                valueDialog.showAndWait().ifPresent(value -> {
                    try {
                        config.setProperty(key, value);
                        config.save();
                        propertyList.add(new ConfigProperty(key, value));
                        
                        // Sort the list
                        FXCollections.sort(propertyList, (a, b) -> a.getKey().compareTo(b.getKey()));
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to add property");
                        alert.setContentText("Error: " + e.getMessage());
                        alert.showAndWait();
                    }
                });
            }
        });
    }
    
    private void deleteSelectedProperty(TableView<ConfigProperty> propertyTable) {
        ConfigProperty selected = propertyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a property to delete");
            alert.showAndWait();
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Property");
        confirm.setContentText("Are you sure you want to delete property '" + selected.getKey() + "'?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    config.clearProperty(selected.getKey());
                    config.save();
                    propertyList.remove(selected);
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to delete property");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
    
    /**
     * Data class for configuration property.
     */
    private static class ConfigProperty {
        private javafx.beans.property.SimpleStringProperty key;
        private javafx.beans.property.SimpleStringProperty value;
        
        public ConfigProperty(String key, String value) {
            this.key = new javafx.beans.property.SimpleStringProperty(key);
            this.value = new javafx.beans.property.SimpleStringProperty(value);
        }
        
        public String getKey() { return key.get(); }
        public String getValue() { return value.get(); }
        public void setValue(String value) { this.value.set(value); }
        
        public javafx.beans.property.StringProperty keyProperty() { return key; }
        public javafx.beans.property.StringProperty valueProperty() { return value; }
    }
    
    /**
     * Editable cell for table values.
     */
    private static class EditingCell extends TableCell<ConfigProperty, String> {
        private TextField textField;
        
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
            }
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }
        
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }
        
        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEdit(textField.getText());
                }
            });
        }
        
        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }
    
}

