package app.ui.components;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import app.data.persistence.DiaryRepository;
import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class DiaryEntryEditDialog {

    private final DiaryRepository repository = new DiaryRepository();
    private final TagInputComponent tagInput = new TagInputComponent();

    private TextArea textArea;
    private Button saveButton;

    public void show(Window owner, LocalDateTime createdAt, LocalDate entryDate, String text, java.util.List<String> tags) {
        tagInput.setAllTags(repository.loadAllTags());
        for (String tag : tags) {
            tagInput.addTag(tag);
        }

        Dialog<?> dialog = SkinService.get().createDialog(owner, "Eintrag bearbeiten");

        // CANCEL_CLOSE damit JavaFX den Dialog grundsätzlich schließen lässt
        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);
        var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);
        
        DatePicker datePicker = new DatePicker(entryDate);
        VBox content = buildContent(datePicker);
        textArea.setText(text);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Löschen", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, deleteButtonType);

        saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            repository.updateEntry(
                    createdAt,
                    datePicker.getValue(),
                    textArea.getText().trim(),
                    new ArrayList<>(tagInput.getSelectedTags()));
            dialog.close();
        });

        deleteButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            repository.deleteEntry(createdAt);
            dialog.close();
        });

        dialog.setResultConverter(_ -> null);

        Platform.runLater(() -> textArea.requestFocus());
        dialog.showAndWait();
    }

    private VBox buildContent(DatePicker datePicker) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(datePicker, tagInput.getTagInput());

        textArea = new TextArea();
        textArea.setPromptText("Was beschäftigt dich?");
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(10);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                if (e.isShiftDown()) {
                    tagInput.requestFocus();
                } else if (saveButton != null) {
                    saveButton.requestFocus();
                }
            }
        });

        root.getChildren().addAll(topRow, tagInput.getChipPane(), textArea);
        return root;
    }
}