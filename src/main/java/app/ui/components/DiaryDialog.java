package app.ui.components;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import app.config.Config;
import app.data.persistence.DiaryRepository;
import app.ui.skin.SkinService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
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
import javafx.util.Duration;
import javafx.event.EventHandler;

public class DiaryDialog {

    private static final int DEFAULT_INVASIVE_AFTER_HOURS = 18;
    private static final int DEFAULT_INVASIVE_SECONDS = 120;
    private static final int DEFAULT_MIN_CHARS = 20;
    private static final int DEFAULT_MIN_TAGS = 1;

    private final DiaryRepository repository = new DiaryRepository();
    private final TagInputComponent tagInput = new TagInputComponent();

    // Dialog state
    private TextArea textArea;
    private Button saveButton;
    private BooleanBinding requirementsNotMet;
    private EventHandler<WindowEvent> closeBlocker;
    private ListChangeListener<String> tagsListener;

    public void show(Window owner) {
        int invasiveAfterHours = Config.getInt("diary.invasiveAfterHours", DEFAULT_INVASIVE_AFTER_HOURS);
        LocalDateTime lastEntry = repository.findLastEntryTimestamp();
        boolean invasive = lastEntry == null
                || ChronoUnit.HOURS.between(lastEntry, LocalDateTime.now()) >= invasiveAfterHours;

        tagInput.setAllTags(repository.loadAllTags());

        Dialog<?> dialog = SkinService.get().createDialog(owner, "Tagebuch");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        VBox content = buildContent(datePicker);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(saveButtonType);
        saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            boolean hasText = textArea.getText() != null && !textArea.getText().isBlank();
            boolean hasTags = !tagInput.getSelectedTags().isEmpty();

            if (hasText && hasTags) {
                repository.saveEntry(
                        LocalDateTime.now(),
                        datePicker.getValue(),
                        textArea.getText().trim(),
                        new ArrayList<>(tagInput.getSelectedTags()));

                tagInput.setAllTags(repository.loadAllTags());

                tagInput.reset();
                textArea.clear();
                makeNonInvasive(dialog);
            } else if (!hasText && !hasTags) {
                dialog.close();
            }
            // Sonst: nichts tun
        });

        dialog.setResultConverter(_ -> null);

        if (invasive) {
            makeInvasive(dialog);
        }

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
        textArea.setId("diaryTextArea");
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

    private void makeInvasive(Dialog<?> dialog) {
        closeBlocker = WindowEvent::consume;
        dialog.getDialogPane().getScene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);

        int minChars = Config.getInt("diary.minChars", DEFAULT_MIN_CHARS);
        int minTags = Config.getInt("diary.minTags", DEFAULT_MIN_TAGS);

        requirementsNotMet = new BooleanBinding() {
            { bind(textArea.textProperty(), tagInput.getSelectedTags()); }
            @Override
            protected boolean computeValue() {
                int textLen = textArea.getText() == null ? 0 : textArea.getText().length();
                return textLen < minChars || tagInput.getSelectedTags().size() < minTags;
            }
        };

        tagsListener = _ -> requirementsNotMet.invalidate();
        tagInput.getSelectedTags().addListener(tagsListener);

        saveButton.disableProperty().bind(requirementsNotMet);

        int invasiveSeconds = Config.getInt("diary.invasiveSeconds", DEFAULT_INVASIVE_SECONDS);
        Timeline timer = new Timeline(new KeyFrame(
                Duration.seconds(invasiveSeconds),
                _ -> makeNonInvasive(dialog)));
        timer.setCycleCount(1);
        timer.play();
    }

    private void makeNonInvasive(Dialog<?> dialog) {
        if (closeBlocker != null) {
            dialog.getDialogPane().getScene().getWindow().removeEventFilter(
                    WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);
            closeBlocker = null;
        }
        if (tagsListener != null) {
            tagInput.getSelectedTags().removeListener(tagsListener);
            tagsListener = null;
        }
        saveButton.disableProperty().unbind();
        saveButton.setDisable(false);
        requirementsNotMet = null;
    }
}