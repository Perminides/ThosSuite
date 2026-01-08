package app.ui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.data.DeckType;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class AnkiPlayConfigDialog {
    
    private final Dialog<AnkiPlayConfig> dialog;
    private final TextField minIndexField;
    private final TextField maxIndexField;
    private final TextField maxCardsField;
    private final Set<CheckBox> labelCheckBoxes = new HashSet<>();

    @SuppressWarnings("unchecked")
    public AnkiPlayConfigDialog(Window parent, Skin skin, DeckType deckType, Set<String> availableLabelsSet) {
    	List<String> availableLabels = new ArrayList<>(availableLabelsSet);
    	Collections.sort(availableLabels);
        this.dialog = (Dialog<AnkiPlayConfig>) skin.createDialog(parent, "Filter");
        
        dialog.setTitle(deckType.getDisplayName() + " spielen");
        
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_CENTER);
        
        // Parameter-Zeile (Min, Max, Anzahl)
        HBox parameterBox = new HBox(10);
        parameterBox.setAlignment(Pos.CENTER);
        
        Label minLabel = new Label("Min Index:");
        minIndexField = new TextField("0");
        minIndexField.setPrefColumnCount(8);
        
        Label maxLabel = new Label("Max Index:");
        maxIndexField = new TextField("2000000");
        maxIndexField.setPrefColumnCount(8);
        
        Label maxCardsLabel = new Label("Max Karten:");
        maxCardsField = new TextField("20");
        maxCardsField.setPrefColumnCount(3);
        
        parameterBox.getChildren().addAll(
            minLabel, minIndexField,
            maxLabel, maxIndexField,
            maxCardsLabel, maxCardsField
        );
        
        mainContent.getChildren().add(parameterBox);
        
        // Label-Filter (wenn vorhanden)
        if (!availableLabels.isEmpty()) {
            Label filterLabel = new Label("Label-Filter (optional):");
            mainContent.getChildren().add(filterLabel);
            
            // Spalten erstellen (max 10 pro Spalte)
            HBox columnsBox = new HBox(20);
            columnsBox.setAlignment(Pos.TOP_CENTER);
            
            int columnSize = 10;
            VBox currentColumn = new VBox(5);
            currentColumn.setAlignment(Pos.TOP_LEFT);
            
            for (int i = 0; i < availableLabels.size(); i++) {
                if (i > 0 && i % columnSize == 0) {
                    columnsBox.getChildren().add(currentColumn);
                    currentColumn = new VBox(5);
                    currentColumn.setAlignment(Pos.TOP_LEFT);
                }
                
                String label = availableLabels.get(i);
                String displayLabel = label.length() > 25 ? label.substring(0, 22) + "..." : label;
                CheckBox checkBox = new CheckBox(displayLabel);
                checkBox.setTooltip(new Tooltip(label)); // Voller Text beim Hover
                labelCheckBoxes.add(checkBox);
                currentColumn.getChildren().add(checkBox);
            }
            
            // Letzte Spalte hinzufügen
            if (!currentColumn.getChildren().isEmpty()) {
                columnsBox.getChildren().add(currentColumn);
            }
            
            mainContent.getChildren().add(columnsBox);
        }
        
        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

     // Lösche deinen kompletten sceneProperty().addListener(...) Block und nimm das hier:

        dialog.setOnShown(event -> {
            Platform.runLater(() -> {
                minIndexField.requestFocus();
                minIndexField.selectAll();
            });
        });
        
        // Result Converter
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    int minIndex = minIndexField.getText().trim().isEmpty() ? 0 : Integer.parseInt(minIndexField.getText().trim());
                    int maxIndex = maxIndexField.getText().trim().isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(maxIndexField.getText().trim());
                    int maxCards = maxCardsField.getText().trim().isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(maxCardsField.getText().trim());
                    
                    Set<String> selectedLabels = new HashSet<>();
                    for (CheckBox checkBox : labelCheckBoxes) {
                        if (checkBox.isSelected()) {
                            selectedLabels.add(checkBox.getText());
                        }
                    }
                    
                    return new AnkiPlayConfig(minIndex, maxIndex, maxCards, selectedLabels);
                } catch (NumberFormatException e) {
                	SkinService.get().createAlert(null, null, "Stehen da Buchstaben in den Zahlenfeldern?", false, false).showAndWait();
    	            return null;
                }
            }
            return null;
        });
    }
    
    public java.util.Optional<AnkiPlayConfig> showAndWait() {
        return dialog.showAndWait();
    }
    
    public Dialog getDialog() {
    	return dialog;
    }
    
    public record AnkiPlayConfig(int minIndex, int maxIndex, int maxCards, Set<String> selectedLabels) {}
}