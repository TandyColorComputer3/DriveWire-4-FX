package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * JavaFX error dialog utility.
 * Replaces SWT ErrorWin.
 */
public class ErrorDialogFX {
    
    /**
     * Show an error dialog using JavaFX Alert.
     */
    public static void showError(String title, String summary, String detail) {
        showError(title, summary, detail, null);
    }
    
    /**
     * Show an error dialog using JavaFX Alert with owner window.
     */
    public static void showError(String title, String summary, String detail, Window owner) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(summary);
            alert.initOwner(owner);
            
            if (detail != null && !detail.isEmpty()) {
                Label label = new Label("Details:");
                TextArea textArea = new TextArea(detail);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);
                
                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);
                
                alert.getDialogPane().setExpandableContent(expContent);
                alert.getDialogPane().setExpanded(true);
            }
            
            alert.showAndWait();
        });
    }
    
    /**
     * Show an error dialog from DWError object.
     */
    public static void showError(DWError dwerror, Window owner) {
        showError(dwerror.getTitle(), dwerror.getSummary(), dwerror.getDetail(), owner);
    }
}

