package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.Iterator;

/**
 * JavaFX version of DiskAdvancedWin - shows and edits disk parameters.
 */
public class DiskAdvancedWinFX {
    
    private Stage dialogStage;
    private Window owner;
    private DiskDef disk;
    private HierarchicalConfiguration paramDefs;
    private ObservableList<DiskParameter> parameterList;
    private TableView<DiskParameter> parameterTable;
    private TextArea detailTextArea;
    private Label detailTitleLabel;
    private Button applyButton;
    
    public DiskAdvancedWinFX(Window owner, DiskDef disk) {
        this.owner = owner;
        this.disk = disk;
        
        // Load parameter definitions
        if (MainWin.master == null || MainWin.master.getMaxIndex("diskparams") < 0) {
            this.paramDefs = new HierarchicalConfiguration();
        } else {
            this.paramDefs = MainWin.master.configurationAt("diskparams");
        }
    }
    
    /**
     * Show the disk properties dialog.
     */
    public void show() {
        try {
            dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(owner);
            dialogStage.setTitle("Parameters for drive " + disk.getDriveNo());
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(500);
            
            // Restore window size/position from config
            if (MainWin.config != null) {
                int x = MainWin.config.getInt("diskadvancedwin_x", -1);
                int y = MainWin.config.getInt("diskadvancedwin_y", -1);
                int w = MainWin.config.getInt("diskadvancedwin_w", 600);
                int h = MainWin.config.getInt("diskadvancedwin_h", 500);
                
                if (x >= 0 && y >= 0) {
                    dialogStage.setX(x);
                    dialogStage.setY(y);
                }
                dialogStage.setWidth(w);
                dialogStage.setHeight(h);
            }
            
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            
            // Split pane for table and details
            SplitPane splitPane = new SplitPane();
            splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
            
            // Top: Parameter table
            VBox tableBox = new VBox(10);
            tableBox.setPadding(new Insets(10));
            
            HBox headerBox = new HBox(10);
            Label tableLabel = new Label("Disk Parameters:");
            tableLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            // Check disk parameters directly (before parameterList is initialized)
            int paramCount = 0;
            try {
                Iterator<String> params = disk.getParams();
                while (params.hasNext()) {
                    String key = params.next();
                    if (!key.startsWith("*")) {
                        paramCount++;
                    }
                }
            } catch (Exception e) {
                // Ignore - will show info label as fallback
            }
            
            // Add info label if few parameters
            if (paramCount <= 1) {
                Label infoLabel = new Label("(Parameters will appear as disk is accessed)");
                infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray; -fx-font-style: italic;");
                headerBox.getChildren().addAll(tableLabel, infoLabel);
            } else {
                headerBox.getChildren().add(tableLabel);
            }
            
            parameterTable = new TableView<>();
            parameterTable.setEditable(true);
            parameterTable.setPrefHeight(250);
            
            // Parameter column
            TableColumn<DiskParameter, String> paramColumn = new TableColumn<>("Parameter");
            paramColumn.setCellValueFactory(new PropertyValueFactory<>("parameter"));
            paramColumn.setPrefWidth(150);
            paramColumn.setEditable(false);
            
            // Current Value column
            TableColumn<DiskParameter, String> currentColumn = new TableColumn<>("Current Value");
            currentColumn.setCellValueFactory(new PropertyValueFactory<>("currentValue"));
            currentColumn.setPrefWidth(200);
            currentColumn.setEditable(false);
            
            // New Value column
            TableColumn<DiskParameter, String> newColumn = new TableColumn<>("New Value");
            newColumn.setCellValueFactory(new PropertyValueFactory<>("newValue"));
            newColumn.setPrefWidth(200);
            newColumn.setEditable(true);
            newColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            newColumn.setOnEditCommit(event -> {
                DiskParameter param = event.getRowValue();
                param.setNewValue(event.getNewValue());
                updateApplyButton();
            });
            
            parameterTable.getColumns().addAll(paramColumn, currentColumn, newColumn);
            
            parameterList = FXCollections.observableArrayList();
            parameterTable.setItems(parameterList);
            
            // Selection listener to show details
            parameterTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) {
                    displayParameterDetails(newVal.getParameter());
                }
            });
            
            tableBox.getChildren().addAll(headerBox, parameterTable);
            
            // Bottom: Parameter details
            VBox detailBox = new VBox(10);
            detailBox.setPadding(new Insets(10));
            
            detailTitleLabel = new Label("Select a parameter to view details");
            detailTitleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            
            detailTextArea = new TextArea();
            detailTextArea.setEditable(false);
            detailTextArea.setPrefHeight(150);
            detailTextArea.setWrapText(true);
            detailTextArea.setText("Select a parameter from the list above to view its description and details.");
            
            detailBox.getChildren().addAll(detailTitleLabel, detailTextArea);
            
            splitPane.getItems().addAll(tableBox, detailBox);
            
            // Set split pane divider position from config
            if (MainWin.config != null) {
                double topWeight = MainWin.config.getDouble("diskadvancedwin_sashweight_top", 0.6);
                splitPane.setDividerPositions(topWeight);
            } else {
                splitPane.setDividerPositions(0.6);
            }
            
            root.setCenter(splitPane);
            
            // Bottom: Buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setPadding(new Insets(10));
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            
            Hyperlink wikiLink = new Hyperlink("Wiki Help...");
            wikiLink.setVisible(false);
            
            applyButton = new Button("Apply");
            applyButton.setDisable(true);
            applyButton.setOnAction(e -> applyChanges());
            
            Button okButton = new Button("OK");
            okButton.setOnAction(e -> {
                applyChanges();
                saveWindowState();
                dialogStage.close();
            });
            
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(e -> {
                saveWindowState();
                dialogStage.close();
            });
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            buttonBox.getChildren().addAll(wikiLink, spacer, applyButton, okButton, cancelButton);
            
            root.setBottom(buttonBox);
            
            // Load parameters
            loadParameters();
            
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // Save window state on close
            dialogStage.setOnCloseRequest(e -> saveWindowState());
            
            dialogStage.show();
            
            // Start a background task to refresh parameters as sync events arrive
            // Sync events populate parameters asynchronously, so we check periodically
            javafx.concurrent.Task<Void> refreshTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    int lastParamCount = parameterList.size();
                    System.out.println("DiskAdvancedWinFX: Starting parameter refresh task, initial param count: " + lastParamCount);
                    
                    // Wait a moment for sync events to start arriving
                    Thread.sleep(1000);
                    
                    // Check for new parameters every 500ms for up to 10 seconds
                    for (int i = 0; i < 20; i++) {
                        Thread.sleep(500);
                        
                        // Check if new parameters have been added
                        int currentParamCount = 0;
                        Iterator<String> params = disk.getParams();
                        while (params.hasNext()) {
                            String key = params.next();
                            if (!key.startsWith("*")) {
                                currentParamCount++;
                            }
                        }
                        
                        System.out.println("DiskAdvancedWinFX: Check " + (i+1) + " - param count: " + currentParamCount + " (was " + lastParamCount + ")");
                        
                        // If we have more parameters than before, refresh
                        if (currentParamCount > lastParamCount) {
                            System.out.println("DiskAdvancedWinFX: New parameters detected! Refreshing dialog...");
                            Platform.runLater(() -> {
                                loadParameters();
                                parameterTable.refresh();
                                System.out.println("DiskAdvancedWinFX: Dialog refreshed, now showing " + parameterList.size() + " parameters");
                            });
                            lastParamCount = currentParamCount;
                        }
                    }
                    
                    System.out.println("DiskAdvancedWinFX: Parameter refresh task completed, final param count: " + lastParamCount);
                    return null;
                }
            };
            
            Thread refreshThread = new Thread(refreshTask);
            refreshThread.setDaemon(true);
            refreshThread.start();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open disk properties");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
    
    private void loadParameters() {
        parameterList.clear();
        
        try {
            Iterator<String> itr = disk.getParams();
            while (itr.hasNext()) {
                String key = itr.next();
                Object value = disk.getParam(key);
                String valueStr = value != null ? value.toString() : "";
                
                // Skip internal parameters that start with underscore
                if (!key.startsWith("*")) {
                    parameterList.add(new DiskParameter(key, valueStr, ""));
                }
            }
            
            // Sort by parameter name
            parameterList.sort((a, b) -> a.getParameter().compareTo(b.getParameter()));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void displayParameterDetails(String key) {
        if (key == null) {
            detailTitleLabel.setText("");
            detailTextArea.setText("No disk is inserted in drive " + disk.getDriveNo());
            return;
        }
        
        String type = paramDefs.getString(key + "[@type]", "system");
        String detail = paramDefs.getString(key + "[@detail]", "No description for this parameter.");
        
        detailTitleLabel.setText(key);
        
        StringBuilder detailText = new StringBuilder();
        detailText.append("Type: ").append(type).append("\n\n");
        detailText.append("Description:\n").append(detail);
        
        if (paramDefs.containsKey(key + "[@default]")) {
            String defaultValue = paramDefs.getString(key + "[@default]");
            detailText.append("\n\nDefault Value: ").append(defaultValue);
        }
        
        detailTextArea.setText(detailText.toString());
    }
    
    private void applyChanges() {
        for (DiskParameter param : parameterList) {
            if (param.getNewValue() != null && !param.getNewValue().isEmpty()) {
                MainWin.sendCommand("dw disk set " + disk.getDriveNo() + " " + param.getParameter() + " " + param.getNewValue());
                // Update current value to new value
                param.setCurrentValue(param.getNewValue());
                param.setNewValue("");
            }
        }
        
        // Reload parameters to get updated values
        loadParameters();
        updateApplyButton();
    }
    
    private void updateApplyButton() {
        boolean hasChanges = parameterList.stream()
            .anyMatch(p -> p.getNewValue() != null && !p.getNewValue().isEmpty());
        
        if (applyButton != null) {
            applyButton.setDisable(!hasChanges);
        }
    }
    
    private void saveWindowState() {
        if (MainWin.config != null && dialogStage != null) {
            try {
                MainWin.config.setProperty("diskadvancedwin_x", (int) dialogStage.getX());
                MainWin.config.setProperty("diskadvancedwin_y", (int) dialogStage.getY());
                MainWin.config.setProperty("diskadvancedwin_w", (int) dialogStage.getWidth());
                MainWin.config.setProperty("diskadvancedwin_h", (int) dialogStage.getHeight());
                
                // Save split pane divider position
                BorderPane root = (BorderPane) dialogStage.getScene().getRoot();
                SplitPane splitPane = (SplitPane) root.getCenter();
                if (splitPane != null && splitPane.getDividerPositions().length > 0) {
                    MainWin.config.setProperty("diskadvancedwin_sashweight_top", splitPane.getDividerPositions()[0]);
                }
                
                MainWin.config.save();
            } catch (Exception e) {
                System.err.println("Failed to save window state: " + e.getMessage());
            }
        }
    }
    
    /**
     * Data model for disk parameters.
     */
    public static class DiskParameter {
        private javafx.beans.property.SimpleStringProperty parameter;
        private javafx.beans.property.SimpleStringProperty currentValue;
        private javafx.beans.property.SimpleStringProperty newValue;
        
        public DiskParameter(String parameter, String currentValue, String newValue) {
            this.parameter = new javafx.beans.property.SimpleStringProperty(parameter);
            this.currentValue = new javafx.beans.property.SimpleStringProperty(currentValue);
            this.newValue = new javafx.beans.property.SimpleStringProperty(newValue);
        }
        
        public String getParameter() {
            return parameter.get();
        }
        
        public void setParameter(String parameter) {
            this.parameter.set(parameter);
        }
        
        public javafx.beans.property.StringProperty parameterProperty() {
            return parameter;
        }
        
        public String getCurrentValue() {
            return currentValue.get();
        }
        
        public void setCurrentValue(String currentValue) {
            this.currentValue.set(currentValue);
        }
        
        public javafx.beans.property.StringProperty currentValueProperty() {
            return currentValue;
        }
        
        public String getNewValue() {
            return newValue.get();
        }
        
        public void setNewValue(String newValue) {
            this.newValue.set(newValue);
        }
        
        public javafx.beans.property.StringProperty newValueProperty() {
            return newValue;
        }
    }
}

