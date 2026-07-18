package app.shared.ui.surfaces.dialogs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import app.shared.model.AnkiDialogState;
import app.shared.skin.SkinService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AnkiConfigDialog {

    private static final int LABEL_MAX_LEN = 30;

    private final String title;
    private final String minDefault, maxDefault, maxCardsDefault;
    private final List<List<String>> labelColumns;

    private final TextField minField = new TextField();
    private final TextField maxField = new TextField();
    private final TextField maxCardsField = new TextField();
    private final List<CheckBox> labelBoxes = new ArrayList<>(); // userData = volles Label (= id)
    private Button okButton;

    public AnkiConfigDialog(String title, String minDefault, String maxDefault, String maxCardsDefault,
                            List<List<String>> labelColumns) {
        this.title = title;
        this.minDefault = minDefault;
        this.maxDefault = maxDefault;
        this.maxCardsDefault = maxCardsDefault;
        this.labelColumns = labelColumns;
    }

    public Optional<AnkiDialogState> showAndWait() {
        Dialog<?> dialog = SkinService.get().createDialog(null, title); // Owner intern
        DialogPane pane = dialog.getDialogPane();

        VBox content = SkinService.get().createDialogContent();

        // Parameter-Zeile
        minField.setText(minDefault);       minField.setPrefColumnCount(8);
        maxField.setText(maxDefault);       maxField.setPrefColumnCount(8);
        maxCardsField.setText(maxCardsDefault); maxCardsField.setPrefColumnCount(3);

        HBox parameterBox = new HBox(10,
                new Label("Min Index:"), minField,
                new Label("Max Index:"), maxField,
                new Label("Max Karten:"), maxCardsField);
        parameterBox.setAlignment(Pos.CENTER);
        content.getChildren().add(parameterBox);

        // Label-Filter (optional) — Spalten kommen fertig gruppiert vom Feature
        if (!labelColumns.isEmpty()) {
            content.getChildren().add(new Label("Label-Filter (optional):"));
            HBox columns = new HBox(20);
            columns.setAlignment(Pos.TOP_CENTER);
            for (List<String> column : labelColumns) {
                VBox col = new VBox(5);
                col.setAlignment(Pos.TOP_LEFT);
                for (String label : column) {
                    String shown = label.length() > LABEL_MAX_LEN ? label.substring(0, LABEL_MAX_LEN) + "..." : label;
                    CheckBox box = new CheckBox(shown);
                    box.setUserData(label);              // volles Label bleibt id, auch wenn Anzeige gekürzt
                    box.setTooltip(new Tooltip(label));
                    labelBoxes.add(box);
                    col.getChildren().add(box);
                }
                columns.getChildren().add(col);
            }
            content.getChildren().add(columns);
        }

        pane.setContent(content);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        okButton = (Button) pane.lookupButton(ButtonType.OK);

        // OK an, sobald alle drei Zahlenfelder leer oder gültige Ganzzahl sind
        minField.textProperty().addListener((_, _, _) -> updateOk());
        maxField.textProperty().addListener((_, _, _) -> updateOk());
        maxCardsField.textProperty().addListener((_, _, _) -> updateOk());
        updateOk();

        dialog.setOnShown(_ -> Platform.runLater(() -> { minField.requestFocus(); minField.selectAll(); }));

        Optional<?> result = dialog.showAndWait();
        return (result.isPresent() && result.get() == ButtonType.OK)
                ? Optional.of(readState())
                : Optional.empty();
    }

    private void updateOk() {
        okButton.setDisable(!(isIntOrBlank(minField.getText())
                           && isIntOrBlank(maxField.getText())
                           && isIntOrBlank(maxCardsField.getText())));
    }

    private AnkiDialogState readState() {
        Set<String> selected = new LinkedHashSet<>();
        for (CheckBox box : labelBoxes)
            if (box.isSelected()) selected.add((String) box.getUserData());
        return new AnkiDialogState(minField.getText(), maxField.getText(), maxCardsField.getText(), selected);
    }

    private static boolean isIntOrBlank(String s) {
        if (s == null || s.isBlank()) return true;
        try { Integer.parseInt(s.trim()); return true; }
        catch (NumberFormatException e) { return false; }
    }
}