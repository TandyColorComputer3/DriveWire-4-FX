package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * JavaFX version of SimpleWizard - Configuration Wizard for DriveWire4.
 * All selections are clickable - no manual text input required (except TCP hostname).
 */
public class SimpleWizardFX {
    
    private Stage dialogStage;
    private Window owner;
    
    // Wizard state
    private PlatformDef selectedPlatform = null;
    private String serialPort = null;
    private boolean midi = true;
    private int serialRate = 115200;
    private boolean serialRTS = false;
    private boolean serialDTR = false;
    private String serialParity = "none";
    private int serialDatabits = 8;
    private String serialStopbits = "1";
    private boolean serialRTSCTSin = false;
    private boolean serialRTSCTSout = false;
    private boolean serialXONXOFFin = false;
    private boolean serialXONXOFFout = false;
    private boolean printerText = true;
    private String printerDir = "cocoprints";
    private boolean tcpMode = false;
    private boolean tcpClient = false;
    private String tcpClientHost = "localhost";
    private int tcpPort = 65504;
    
    // Wizard pages
    private List<WizardPage> pages = new ArrayList<>();
    private int currentPageIndex = 0;
    
    // Random yays for platform selection
    private List<String> yays = new ArrayList<>();
    private Random random = new Random();
    
    public SimpleWizardFX(Window owner) {
        this.owner = owner;
        initYays();
    }
    
    private void initYays() {
        yays.add("Yes!");
        yays.add("Excellent!");
        yays.add("Good choice.");
        yays.add("One of my favorites.");
        yays.add("This should be good.");
        yays.add("Let's go!");
        yays.add("Works for me..");
        yays.add("Sounds good.");
        yays.add("Sounds like fun.");
        yays.add("Let's do this!");
        yays.add("Perfect!");
        yays.add("We can handle that.");
        yays.add("Good deal.");
        yays.add("Awesome!");
        yays.add("Nice!");
        yays.add("I like it.");
        yays.add("I can't wait!");
        yays.add("Next question..");
        yays.add("Nice one.");
        yays.add("Alright!");
        yays.add("Great!");
        yays.add("Great choice!");
        yays.add("We can do this.");
        yays.add("This I like!");
    }
    
    private String getRandomYay() {
        return yays.get(random.nextInt(yays.size()));
    }
    
    /**
     * Show the configuration wizard dialog.
     */
    public void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setTitle("DriveWire4 Configuration Wizard");
        dialogStage.setResizable(false);
        
        // Create wizard pages
        createPages();
        
        // Show first page
        showPage(0);
    }
    
    private void createPages() {
        // Page 1: Introduction
        pages.add(new WizardPage("Welcome", "Welcome to DriveWire4 Configuration Wizard",
            "This wizard will help you configure DriveWire4 for your system.\n\n" +
            "You'll be asked about:\n" +
            "• Your platform (CoCo, Dragon, etc.)\n" +
            "• Serial port settings\n" +
            "• MIDI configuration\n" +
            "• Printer settings\n\n" +
            "Click Next to continue."));
        
        // Page 2: Platform selection
        pages.add(createPlatformPage());
        
        // Page 3: Serial port selection OR TCP page
        pages.add(createSerialPage());
        pages.add(createTCPPage());
        
        // Page 4: Serial parameters (only if not TCP)
        pages.add(createSerialParamsPage());
        
        // Page 5: MIDI
        pages.add(createMIDIPage());
        
        // Page 6: Printer
        pages.add(createPrinterPage());
        
        // Page 7: Finish
        pages.add(createFinishPage());
    }
    
    private WizardPage createPlatformPage() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Which will it be?");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Platform buttons in a grid
        GridPane platformGrid = new GridPane();
        platformGrid.setHgap(15);
        platformGrid.setVgap(15);
        platformGrid.setAlignment(Pos.CENTER);
        
        ToggleGroup platformGroup = new ToggleGroup();
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a9eff;");
        
        // CoCo 1
        createPlatformButton(platformGrid, platformGroup, "CoCo 1", 
            new PlatformDef("CoCo 1", null, 38400, "none", "1", false, false, false, false),
            0, 0, feedbackLabel);
        
        // CoCo 2
        createPlatformButton(platformGrid, platformGroup, "CoCo 2",
            new PlatformDef("CoCo 2", null, 57600, "none", "1", false, false, false, false),
            1, 0, feedbackLabel);
        
        // CoCo 3
        createPlatformButton(platformGrid, platformGroup, "CoCo 3",
            new PlatformDef("CoCo 3", null, 115200, "none", "1", false, false, false, false),
            2, 0, feedbackLabel);
        
        // Atari
        createPlatformButton(platformGrid, platformGroup, "Atari/Liber809",
            new PlatformDef("Atari", null, 57600, "none", "1", false, false, false, false),
            0, 1, feedbackLabel);
        
        // CoCo3FPGA
        createPlatformButton(platformGrid, platformGroup, "CoCo3FPGA",
            new PlatformDef("CoCo3FPGA", null, 115200, "none", "1", false, false, false, false),
            1, 1, feedbackLabel);
        
        // Apple II
        createPlatformButton(platformGrid, platformGroup, "Apple II",
            new PlatformDef("Apple", null, 115200, "none", "1", true, true, false, false),
            2, 1, feedbackLabel);
        
        // TCP/Emulator - make it more prominent
        Label tcpSectionLabel = new Label("Or use TCP/IP:");
        tcpSectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0 5 0;");
        platformGrid.add(tcpSectionLabel, 0, 2, 3, 1);
        
        ToggleButton tcpButton = new ToggleButton("Emulator or\nother TCP/IP");
        tcpButton.setToggleGroup(platformGroup);
        tcpButton.setPrefWidth(220);
        tcpButton.setPrefHeight(90);
        tcpButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        tcpButton.setOnAction(e -> {
            if (tcpButton.isSelected()) {
                selectedPlatform = new PlatformDef("TCP connection", null);
                selectedPlatform.tcp = true;
                tcpMode = true;
                feedbackLabel.setText("Ah.. TCP/IP connection.\n" + getRandomYay());
            } else {
                selectedPlatform = null;
                tcpMode = false;
                feedbackLabel.setText("");
            }
        });
        platformGrid.add(tcpButton, 0, 3, 3, 1);
        
        content.getChildren().addAll(titleLabel, platformGrid, feedbackLabel);
        
        return new WizardPage("Platform", "Select Client Device Type", content, () -> {
            return selectedPlatform != null;
        });
    }
    
    private void createPlatformButton(GridPane grid, ToggleGroup group, String label, 
                                     PlatformDef platformDef, int col, int row, Label feedbackLabel) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(group);
        button.setPrefWidth(150);
        button.setPrefHeight(80);
        button.setStyle("-fx-font-size: 12px;");
        button.setOnAction(e -> {
            if (button.isSelected()) {
                selectedPlatform = platformDef;
                tcpMode = platformDef.tcp;
                // Apply platform defaults
                if (!platformDef.tcp) {
                    serialRate = platformDef.rate;
                    serialParity = platformDef.parity;
                    serialStopbits = platformDef.stopbits;
                    serialRTS = platformDef.setRTS;
                    serialDTR = platformDef.setDTR;
                    serialRTSCTSin = platformDef.useRTSin;
                    serialRTSCTSout = platformDef.useRTSout;
                }
                feedbackLabel.setText("Ah.. the " + platformDef.name + ".\n" + getRandomYay());
            } else {
                selectedPlatform = null;
                feedbackLabel.setText("");
            }
        });
        grid.add(button, col, row);
    }
    
    private WizardPage createSerialPage() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label label = new Label("Choose an available serial port:");
        label.setStyle("-fx-font-weight: bold;");
        
        // TableView for serial ports
        TableView<SerialPortInfo> portTable = new TableView<>();
        portTable.setPrefHeight(300);
        
        TableColumn<SerialPortInfo, String> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(cell -> cell.getValue().portProperty());
        portColumn.setPrefWidth(200);
        
        TableColumn<SerialPortInfo, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        statusColumn.setPrefWidth(300);
        
        portTable.getColumns().addAll(portColumn, statusColumn);
        
        ObservableList<SerialPortInfo> portData = FXCollections.observableArrayList();
        portTable.setItems(portData);
        
        // Selection handler
        portTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && "Available".equals(selected.getStatus())) {
                serialPort = selected.getPort();
                label.setText("You've chosen:\n" + serialPort + "\n\n" + getRandomYay());
            }
        });
        
        // Scan button
        Button scanButton = new Button("Scan Serial Ports");
        scanButton.setOnAction(e -> scanSerialPorts(portData, label));
        
        // Manual port button
        Button manualButton = new Button("Add Port Manually");
        manualButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Manual Port Entry");
            dialog.setHeaderText("Enter Serial Port Name");
            dialog.setContentText("Port name:");
            dialog.showAndWait().ifPresent(port -> {
                if (!port.isEmpty()) {
                    portData.add(new SerialPortInfo(port, "Available"));
                    serialPort = port;
                    label.setText("You've chosen:\n" + serialPort + "\n\n" + getRandomYay());
                }
            });
        });
        
        HBox buttonBox = new HBox(10, scanButton, manualButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        content.getChildren().addAll(label, portTable, buttonBox);
        
        // Auto-scan on page show
        scanSerialPorts(portData, label);
        
        return new WizardPage("Serial Port", "Select Serial Port", content, () -> {
            return serialPort != null && !serialPort.isEmpty();
        });
    }
    
    private void scanSerialPorts(ObservableList<SerialPortInfo> portData, Label feedbackLabel) {
        feedbackLabel.setText("Scanning serial ports...");
        portData.clear();
        
        // Run scan in background thread
        new Thread(() -> {
            try {
                // Use reflection to call DriveWireServer methods since they're in a different package
                Class<?> serverClass = Class.forName("com.groupunix.drivewireserver.DriveWireServer");
                java.lang.reflect.Method getPortsMethod = serverClass.getMethod("getAvailableSerialPorts");
                @SuppressWarnings("unchecked")
                java.util.ArrayList<String> ports = (java.util.ArrayList<String>) getPortsMethod.invoke(null);
                
                final Class<?> finalServerClass = serverClass;
                Platform.runLater(() -> {
                    portData.clear();
                    for (String port : ports) {
                        try {
                            java.lang.reflect.Method getStatusMethod = finalServerClass.getMethod("getSerialPortStatus", String.class);
                            String status = (String) getStatusMethod.invoke(null, port);
                            portData.add(new SerialPortInfo(port, status));
                        } catch (Exception ex) {
                            portData.add(new SerialPortInfo(port, "Error: " + ex.getMessage()));
                        }
                    }
                    
                    if (portData.isEmpty()) {
                        feedbackLabel.setText("No serial ports found.\nYou may need to manually specify your device.");
                    } else {
                        feedbackLabel.setText("Choose an available serial port:");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    feedbackLabel.setText("Error scanning ports: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private WizardPage createTCPPage() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label descLabel = new Label("DriveWire supports two different types of TCP/IP device connections.");
        descLabel.setWrapText(true);
        
        // Server mode
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton serverMode = new RadioButton("Use Server Mode");
        serverMode.setToggleGroup(modeGroup);
        serverMode.setSelected(true);
        serverMode.setStyle("-fx-font-weight: bold;");
        
        Label serverDesc = new Label("DriveWire will listen for connections on a specified port.\nEmulators with DriveWire capability normally use this mode.");
        serverDesc.setWrapText(true);
        
        Label listenPortLabel = new Label("Listen on port:");
        Spinner<Integer> listenPortSpinner = new Spinner<>(1, 65535, 65504);
        listenPortSpinner.setEditable(true);
        listenPortSpinner.valueProperty().addListener((obs, old, val) -> {
            if (val != null) tcpPort = val;
        });
        
        HBox serverBox = new HBox(10, listenPortLabel, listenPortSpinner);
        serverBox.setAlignment(Pos.CENTER_LEFT);
        
        // Client mode
        RadioButton clientMode = new RadioButton("Use Client Mode");
        clientMode.setToggleGroup(modeGroup);
        clientMode.setStyle("-fx-font-weight: bold;");
        
        Label clientDesc = new Label("DriveWire will attempt to establish and maintain a connection to a specified host.\nThis is typically used to communicate with TCP to serial adapters.");
        clientDesc.setWrapText(true);
        
        Label hostLabel = new Label("Connect to host:");
        TextField hostField = new TextField("localhost");
        hostField.textProperty().addListener((obs, old, val) -> {
            if (val != null) tcpClientHost = val;
        });
        
        Label portLabel = new Label("On port:");
        Spinner<Integer> clientPortSpinner = new Spinner<>(1, 65535, 65504);
        clientPortSpinner.setEditable(true);
        clientPortSpinner.valueProperty().addListener((obs, old, val) -> {
            if (val != null) tcpPort = val;
        });
        
        HBox clientBox = new HBox(10, hostLabel, hostField, portLabel, clientPortSpinner);
        clientBox.setAlignment(Pos.CENTER_LEFT);
        
        // Enable/disable based on mode
        serverMode.setOnAction(e -> {
            tcpClient = false;
            serverBox.setDisable(false);
            clientBox.setDisable(true);
        });
        
        clientMode.setOnAction(e -> {
            tcpClient = true;
            serverBox.setDisable(true);
            clientBox.setDisable(false);
        });
        
        clientBox.setDisable(true);
        
        VBox serverSection = new VBox(10, serverMode, serverDesc, serverBox);
        VBox clientSection = new VBox(10, clientMode, clientDesc, clientBox);
        
        content.getChildren().addAll(descLabel, serverSection, clientSection);
        
        return new WizardPage("TCP/IP", "TCP/IP Connection Details", content, () -> {
            if (tcpClient) {
                return tcpClientHost != null && !tcpClientHost.isEmpty();
            }
            return true;
        });
    }
    
    private WizardPage createSerialParamsPage() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label label = new Label("The default values for the " + 
            (selectedPlatform != null ? selectedPlatform.name : "selected platform") + 
            " will be correct in most situations.");
        label.setWrapText(true);
        label.setStyle("-fx-font-weight: bold;");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Port speed
        grid.add(new Label("Port speed:"), 0, 0);
        ComboBox<Integer> rateCombo = new ComboBox<>(FXCollections.observableArrayList(
            19200, 38400, 57600, 115200, 230400, 460800, 921600));
        rateCombo.setValue(serialRate);
        rateCombo.setOnAction(e -> serialRate = rateCombo.getValue());
        grid.add(rateCombo, 1, 0);
        
        // Parity
        grid.add(new Label("Parity:"), 0, 1);
        ComboBox<String> parityCombo = new ComboBox<>(FXCollections.observableArrayList(
            "none", "even", "odd", "mark", "space"));
        parityCombo.setValue(serialParity);
        parityCombo.setOnAction(e -> serialParity = parityCombo.getValue());
        grid.add(parityCombo, 1, 1);
        
        // Stop bits
        grid.add(new Label("Stop bits:"), 0, 2);
        ComboBox<String> stopbitsCombo = new ComboBox<>(FXCollections.observableArrayList("1", "1.5", "2"));
        stopbitsCombo.setValue(serialStopbits);
        stopbitsCombo.setOnAction(e -> serialStopbits = stopbitsCombo.getValue());
        grid.add(stopbitsCombo, 1, 2);
        
        // Checkboxes
        CheckBox dtrBox = new CheckBox("Set DTR");
        dtrBox.setSelected(serialDTR);
        dtrBox.setOnAction(e -> serialDTR = dtrBox.isSelected());
        grid.add(dtrBox, 0, 3);
        
        CheckBox rtsBox = new CheckBox("Set RTS");
        rtsBox.setSelected(serialRTS);
        rtsBox.setOnAction(e -> serialRTS = rtsBox.isSelected());
        grid.add(rtsBox, 1, 3);
        
        CheckBox rtsctsInBox = new CheckBox("Inbound RTS/CTS");
        rtsctsInBox.setSelected(serialRTSCTSin);
        rtsctsInBox.setOnAction(e -> serialRTSCTSin = rtsctsInBox.isSelected());
        grid.add(rtsctsInBox, 0, 4);
        
        CheckBox rtsctsOutBox = new CheckBox("Outbound RTS/CTS");
        rtsctsOutBox.setSelected(serialRTSCTSout);
        rtsctsOutBox.setOnAction(e -> serialRTSCTSout = rtsctsOutBox.isSelected());
        grid.add(rtsctsOutBox, 1, 4);
        
        CheckBox xonxoffInBox = new CheckBox("Inbound XON/XOFF");
        xonxoffInBox.setSelected(serialXONXOFFin);
        xonxoffInBox.setOnAction(e -> serialXONXOFFin = xonxoffInBox.isSelected());
        grid.add(xonxoffInBox, 0, 5);
        
        CheckBox xonxoffOutBox = new CheckBox("Outbound XON/XOFF");
        xonxoffOutBox.setSelected(serialXONXOFFout);
        xonxoffOutBox.setOnAction(e -> serialXONXOFFout = xonxoffOutBox.isSelected());
        grid.add(xonxoffOutBox, 1, 5);
        
        content.getChildren().addAll(label, grid);
        
        return new WizardPage("Serial Parameters", "Choose Serial Port Parameters", content, () -> true);
    }
    
    private WizardPage createMIDIPage() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label descLabel = new Label(
            "DriveWire 4 can provide a virtual MIDI port and software synthesizer to your device.\n\n" +
            "However, this will increase the server's memory use by several megabytes.\n\n" +
            "If you are running the server on an embedded device, or need to conserve RAM for other reasons, " +
            "or simply will not be using MIDI, it is recommended to disable it.");
        descLabel.setWrapText(true);
        
        Label questionLabel = new Label("Enable virtual MIDI by default?");
        questionLabel.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup midiGroup = new ToggleGroup();
        RadioButton yesButton = new RadioButton("Yes");
        yesButton.setToggleGroup(midiGroup);
        yesButton.setSelected(true);
        yesButton.setOnAction(e -> midi = true);
        
        RadioButton noButton = new RadioButton("No, thanks");
        noButton.setToggleGroup(midiGroup);
        noButton.setOnAction(e -> midi = false);
        
        HBox buttonBox = new HBox(15, questionLabel, yesButton, noButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        content.getChildren().addAll(descLabel, buttonBox);
        
        return new WizardPage("MIDI", "Choose Virtual MIDI Support", content, () -> true);
    }
    
    private WizardPage createPrinterPage() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label descLabel = new Label(
            "There are two different types of printer output available. You can switch at any time, " +
            "but DriveWire will default to the type you choose here.\n\n" +
            "The 'text' mode will produce files containing the raw data that has been sent to the printer. " +
            "No interpretation of the contents is performed.\n\n" +
            "The 'FX80' mode will produce image files containing the output of a simulated Epson FX-80 printer. " +
            "FX-80 control codes are interpreted. These files can be viewed with an image viewer.");
        descLabel.setWrapText(true);
        
        ToggleGroup printerGroup = new ToggleGroup();
        RadioButton textButton = new RadioButton("Default to text mode");
        textButton.setToggleGroup(printerGroup);
        textButton.setSelected(true);
        textButton.setStyle("-fx-font-weight: bold;");
        textButton.setOnAction(e -> printerText = true);
        
        RadioButton fx80Button = new RadioButton("Default to FX-80 mode");
        fx80Button.setToggleGroup(printerGroup);
        fx80Button.setStyle("-fx-font-weight: bold;");
        fx80Button.setOnAction(e -> printerText = false);
        
        Label dirLabel = new Label("Choose a directory where you would like printer files to be created:");
        dirLabel.setWrapText(true);
        
        HBox dirBox = new HBox(10);
        TextField dirField = new TextField(printerDir);
        dirField.setEditable(false);
        dirField.setPrefWidth(300);
        dirField.textProperty().addListener((obs, old, val) -> {
            if (val != null) printerDir = val;
        });
        
        Button chooseButton = new Button("Choose...");
        chooseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose Printer Output Directory");
            if (!printerDir.isEmpty()) {
                File currentDir = new File(printerDir);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    chooser.setInitialDirectory(currentDir);
                }
            }
            File selected = chooser.showDialog(dialogStage);
            if (selected != null) {
                printerDir = selected.getAbsolutePath();
                dirField.setText(printerDir);
            }
        });
        
        dirBox.getChildren().addAll(dirField, chooseButton);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        
        content.getChildren().addAll(descLabel, textButton, fx80Button, dirLabel, dirBox);
        
        return new WizardPage("Printer", "Printer Options", content, () -> true);
    }
    
    private WizardPage createFinishPage() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label label = new Label("Configuration Summary:");
        label.setStyle("-fx-font-weight: bold;");
        
        TextArea summary = new TextArea();
        summary.setEditable(false);
        summary.setPrefRowCount(12);
        summary.setWrapText(true);
        
        updateSummary(summary);
        
        content.getChildren().addAll(label, summary);
        
        return new WizardPage("Finish", "Review Configuration", content, () -> true);
    }
    
    private void updateSummary(TextArea summary) {
        StringBuilder sb = new StringBuilder();
        if (selectedPlatform != null) {
            sb.append("Platform: ").append(selectedPlatform.name).append("\n");
        }
        
        if (tcpMode) {
            sb.append("Connection Type: TCP/IP\n");
            if (tcpClient) {
                sb.append("Mode: Client\n");
                sb.append("Host: ").append(tcpClientHost).append("\n");
                sb.append("Port: ").append(tcpPort).append("\n");
            } else {
                sb.append("Mode: Server\n");
                sb.append("Listen Port: ").append(tcpPort).append("\n");
            }
        } else {
            sb.append("Connection Type: Serial\n");
            sb.append("Serial Port: ").append(serialPort != null ? serialPort : "Not set").append("\n");
            sb.append("Baud Rate: ").append(serialRate).append("\n");
            sb.append("Parity: ").append(serialParity).append("\n");
            sb.append("Stop Bits: ").append(serialStopbits).append("\n");
        }
        
        sb.append("MIDI: ").append(midi ? "Enabled" : "Disabled").append("\n");
        sb.append("Printer: ").append(printerText ? "Text mode" : "FX-80 mode").append("\n");
        sb.append("Printer Directory: ").append(printerDir).append("\n");
        
        summary.setText(sb.toString());
    }
    
    private void showPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        
        currentPageIndex = index;
        WizardPage page = pages.get(index);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        
        // Title and content
        VBox topBox = new VBox(10);
        Label titleLabel = new Label(page.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label descLabel = new Label(page.getDescription());
        descLabel.setWrapText(true);
        topBox.getChildren().addAll(titleLabel, descLabel);
        root.setTop(topBox);
        
        // Page content
        if (page.getContent() != null) {
            ScrollPane scrollPane = new ScrollPane(page.getContent());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background: white;");
            root.setCenter(scrollPane);
        } else {
            Label contentLabel = new Label(page.getTextContent());
            contentLabel.setWrapText(true);
            ScrollPane scrollPane = new ScrollPane(contentLabel);
            scrollPane.setFitToWidth(true);
            root.setCenter(scrollPane);
        }
        
        // Navigation buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button prevButton = new Button("Previous");
        prevButton.setDisable(index == 0);
        prevButton.setOnAction(e -> {
            // Don't validate on Previous - just go back
            int prevIndex = index - 1;
            
            // Handle back navigation based on current page
            if (index == 6) { // Printer page
                prevIndex = 5; // Go back to MIDI
            } else if (index == 5) { // MIDI page
                if (tcpMode) {
                    prevIndex = 3; // Go back to TCP page (skip serial params)
                } else {
                    prevIndex = 4; // Go back to serial params
                }
            } else if (index == 4) { // Serial params page (only for serial connections)
                prevIndex = 2; // Go back to serial page
            } else if (index == 3) { // TCP page
                prevIndex = 1; // Go back to platform (skip serial page)
            } else if (index == 2) { // Serial page (only for serial connections)
                prevIndex = 1; // Go back to platform
            } else {
                prevIndex = index - 1;
            }
            
            if (prevIndex >= 0) {
                showPage(prevIndex);
            }
        });
        
        Button nextButton = new Button(index == pages.size() - 1 ? "Finish" : "Next");
        nextButton.setDefaultButton(true);
        nextButton.setStyle("-fx-font-weight: bold;");
        nextButton.setOnAction(e -> {
            if (page.validate()) {
                if (index == pages.size() - 1) {
                    finishWizard();
                } else {
                    int nextIndex = index + 1;
                    
                    // Handle navigation based on current page
                    if (index == 1) { // Coming from platform page
                        if (tcpMode && selectedPlatform != null && selectedPlatform.tcp) {
                            nextIndex = 3; // Skip serial page (index 2), go directly to TCP page (index 3)
                        } else {
                            nextIndex = 2; // Go to serial page (index 2)
                        }
                    } else if (index == 2) { // Coming from serial page (only for serial connections)
                        nextIndex = 4; // Go to serial params (index 4)
                    } else if (index == 3) { // Coming from TCP page
                        nextIndex = 5; // Skip serial params (index 4), go directly to MIDI (index 5)
                    } else if (index == 4) { // Coming from serial params
                        nextIndex = 5; // Go to MIDI (index 5)
                    } else {
                        nextIndex = index + 1; // Normal increment
                    }
                    
                    if (nextIndex < pages.size()) {
                        showPage(nextIndex);
                    }
                }
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> dialogStage.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBox.getChildren().addAll(prevButton, spacer, cancelButton, nextButton);
        root.setBottom(buttonBox);
        
        Scene scene = new Scene(root, 700, 600);
        dialogStage.setScene(scene);
        dialogStage.show();
    }
    
    private void finishWizard() {
        try {
            // Save configuration
            saveConfiguration();
            dialogStage.close();
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Configuration Complete");
            alert.setHeaderText("Configuration Saved");
            alert.setContentText("Your configuration has been saved.\n\nYou may need to restart DriveWire4 for changes to take effect.");
            alert.showAndWait();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Configuration Error");
            alert.setHeaderText("Failed to save configuration");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void saveConfiguration() throws Exception {
        // Get server config file path
        org.apache.commons.configuration.HierarchicalConfiguration uiConfig = MainWin.getInstanceConfig();
        String serverConfigFile = "config.xml";
        
        if (uiConfig != null) {
            serverConfigFile = uiConfig.getString("ServerConfigFile", "config.xml");
        } else {
            // Fallback: try MainWin.config directly
            if (MainWin.config != null) {
                serverConfigFile = MainWin.config.getString("ServerConfigFile", "config.xml");
            }
        }
        
        File configFile = new File(serverConfigFile);
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            File defaultConfig = new File("default/serverconfig.xml");
            if (defaultConfig.exists()) {
                try {
                    com.groupunix.drivewireui.UIUtils.fileCopy(defaultConfig.getCanonicalPath(), serverConfigFile);
                } catch (IOException e) {
                    throw new Exception("Failed to copy default config: " + e.getMessage(), e);
                }
            } else {
                // Create minimal config
                XMLConfiguration newConfig = new XMLConfiguration();
                newConfig.setFileName(serverConfigFile);
                newConfig.addProperty("AutoCreated", true);
                newConfig.save();
            }
        }
        
        // Load and update config
        XMLConfiguration config = new XMLConfiguration(serverConfigFile);
        
        // Save configuration directly to XML file (like original wizard does via commands)
        // This works even if server isn't connected yet
        
        if (tcpMode) {
            if (tcpClient) {
                config.setProperty("DeviceType", "tcp-client");
                config.setProperty("TCPClientHost", tcpClientHost);
                config.setProperty("TCPClientPort", tcpPort);
            } else {
                config.setProperty("DeviceType", "tcp-server");
                config.setProperty("TCPServerPort", tcpPort);
            }
            config.setProperty("[@name]", (selectedPlatform != null ? selectedPlatform.name : "TCP") + " via TCP");
        } else {
            config.setProperty("DeviceType", "serial");
            config.setProperty("SerialDevice", serialPort);
            config.setProperty("SerialRate", serialRate);
            config.setProperty("SerialParity", serialParity);
            config.setProperty("SerialStopbits", serialStopbits);
            config.setProperty("SerialDTR", serialDTR);
            config.setProperty("SerialRTS", serialRTS);
            config.setProperty("SerialFlowControl_RTSCTS_IN", serialRTSCTSin);
            config.setProperty("SerialFlowControl_RTSCTS_OUT", serialRTSCTSout);
            config.setProperty("SerialFlowControl_XONXOFF_IN", serialXONXOFFin);
            config.setProperty("SerialFlowControl_XONXOFF_OUT", serialXONXOFFout);
            config.setProperty("[@name]", (selectedPlatform != null ? selectedPlatform.name : "Serial") + 
                " on " + serialPort);
        }
        
        config.setProperty("[@desc]", "Autocreated " + new java.sql.Timestamp(System.currentTimeMillis()).toString());
        
        // Save MIDI settings (use UseMIDI like original wizard)
        config.setProperty("UseMIDI", midi);
        
        // Save printer settings (like original wizard)
        String printertype = printerText ? "Text" : "FX80";
        org.apache.commons.configuration.HierarchicalConfiguration printerConfig = MainWin.getInstanceConfig();
        if (printerConfig != null) {
            try {
                int maxPrinterIndex = printerConfig.getMaxIndex("Printer");
                for (int i = 0; i <= maxPrinterIndex; i++) {
                    try {
                        String printerName = printerConfig.getString("Printer(" + i + ")[@name]");
                        if (printerName != null && printerName.equals(printertype)) {
                            config.setProperty("CurrentPrinter", printertype);
                            config.setProperty("Printer(" + i + ").OutputDir", printerDir);
                            break;
                        }
                    } catch (Exception e) {
                        // Skip invalid printer entries
                    }
                }
            } catch (Exception e) {
                // If printer config can't be read, just skip printer settings
                System.err.println("Warning: Could not read printer configuration: " + e.getMessage());
            }
        } else if (MainWin.config != null) {
            // Fallback: use MainWin.config
            try {
                int maxPrinterIndex = MainWin.config.getMaxIndex("Printer");
                for (int i = 0; i <= maxPrinterIndex; i++) {
                    try {
                        String printerName = MainWin.config.getString("Printer(" + i + ")[@name]");
                        if (printerName != null && printerName.equals(printertype)) {
                            config.setProperty("CurrentPrinter", printertype);
                            config.setProperty("Printer(" + i + ").OutputDir", printerDir);
                            break;
                        }
                    } catch (Exception e) {
                        // Skip invalid printer entries
                    }
                }
            } catch (Exception e) {
                // If printer config can't be read, just skip printer settings
                System.err.println("Warning: Could not read printer configuration from MainWin.config: " + e.getMessage());
            }
        }
        
        // Save the configuration file
        config.save();
        
        // Try to send commands to server if connected (optional - config is already saved)
        // This will update the running server if it's available
        try {
            ArrayList<String> cmdList = new ArrayList<>();
            
            if (tcpMode) {
                if (tcpClient) {
                    cmdList.add("dw config set DeviceType tcp-client");
                    cmdList.add("dw config set TCPClientHost " + tcpClientHost);
                    cmdList.add("dw config set TCPClientPort " + tcpPort);
                } else {
                    cmdList.add("dw config set DeviceType tcp-server");
                    cmdList.add("dw config set TCPServerPort " + tcpPort);
                }
            } else {
                cmdList.add("dw config set DeviceType serial");
                cmdList.add("dw config set SerialDevice " + serialPort);
                cmdList.add("dw config set SerialRate " + serialRate);
                cmdList.add("dw config set SerialParity " + serialParity);
                cmdList.add("dw config set SerialStopbits " + serialStopbits);
                cmdList.add("dw config set SerialDTR " + serialDTR);
                cmdList.add("dw config set SerialRTS " + serialRTS);
                cmdList.add("dw config set SerialFlowControl_RTSCTS_IN " + serialRTSCTSin);
                cmdList.add("dw config set SerialFlowControl_RTSCTS_OUT " + serialRTSCTSout);
                cmdList.add("dw config set SerialFlowControl_XONXOFF_IN " + serialXONXOFFin);
                cmdList.add("dw config set SerialFlowControl_XONXOFF_OUT " + serialXONXOFFout);
            }
            
            // Try to update server if connected (non-blocking - config is already saved)
            if (MainWin.taskman != null) {
                try {
                    UIUtils.simpleConfigServer(cmdList);
                } catch (Exception e) {
                    System.err.println("Warning: Could not update running server (config file saved): " + e.getMessage());
                }
            } else {
                // Try to send commands if server is available
                try {
                    int instance = MainWin.getInstance();
                    for (String cmd : cmdList) {
                        UIUtils.loadList(instance, cmd);
                    }
                    UIUtils.loadList(instance, "ui instance reset protodev");
                } catch (Exception e) {
                    System.err.println("Warning: Could not update running server (config file saved): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Non-fatal - config file is already saved
            System.err.println("Warning: Could not update running server (config file saved): " + e.getMessage());
        }
    }
    
    /**
     * Inner class representing a wizard page.
     */
    private static class WizardPage {
        private String title;
        private String description;
        private String textContent;
        private javafx.scene.Node content;
        private java.util.function.Supplier<Boolean> validator;
        
        public WizardPage(String title, String description, String textContent) {
            this.title = title;
            this.description = description;
            this.textContent = textContent;
            this.validator = () -> true;
        }
        
        public WizardPage(String title, String description, javafx.scene.Node content, java.util.function.Supplier<Boolean> validator) {
            this.title = title;
            this.description = description;
            this.content = content;
            this.validator = validator;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getTextContent() { return textContent; }
        public javafx.scene.Node getContent() { return content; }
        public boolean validate() { return validator.get(); }
    }
    
    /**
     * Data class for serial port table.
     */
    private static class SerialPortInfo {
        private javafx.beans.property.SimpleStringProperty port;
        private javafx.beans.property.SimpleStringProperty status;
        
        public SerialPortInfo(String port, String status) {
            this.port = new javafx.beans.property.SimpleStringProperty(port);
            this.status = new javafx.beans.property.SimpleStringProperty(status);
        }
        
        public String getPort() { return port.get(); }
        public String getStatus() { return status.get(); }
        public javafx.beans.property.StringProperty portProperty() { return port; }
        public javafx.beans.property.StringProperty statusProperty() { return status; }
    }
}
