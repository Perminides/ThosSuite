package app.ui.components;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.config.Config;
import app.data.persistence.DiaryRepository;
import app.ui.skin.SkinService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class DiaryDialog {

    private static final int DEFAULT_INVASIVE_AFTER_HOURS = 18;
    private static final int DEFAULT_INVASIVE_SECONDS = 120;
    private static final int DEFAULT_MIN_CHARS = 20;
    private static final int DEFAULT_MIN_TAGS = 1;
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final String TAG_REMOVE_BUTTON = "tag-chip-remove";

    private final DiaryRepository repository = new DiaryRepository();

    // Tag autocomplete state
    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();
    private final List<String> currentMatches = new ArrayList<>();
    private List<String> allTags;
    private FlowPane chipPane;
    private TextField tagInput;
    private Popup suggestionPopup;
    private VBox suggestionBox;
    private int activeIndex = -1;
    private boolean suppressSuggestions = false;

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

        allTags = repository.loadAllTags();

        Dialog<?> dialog = SkinService.get().createDialog(owner, "Tagebuch");

        VBox content = buildContent();
        dialog.getDialogPane().setContent(content);

        // Single button: Speichern
        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(saveButtonType);
        saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);

        // Prevent dialog from closing on Speichern — we handle it ourselves
        // EventFilter (capturing phase) consumes the event before Dialog sees it
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            boolean hasText = textArea.getText() != null && !textArea.getText().isBlank();
            boolean hasTags = !selectedTags.isEmpty();
            
            if (hasText && hasTags) {
                repository.saveEntry(
                        LocalDateTime.now(),
                        ((DatePicker) content.lookup("#diaryDatePicker")).getValue(),
                        textArea.getText().trim(),
                        new ArrayList<>(selectedTags));

                // Update autocomplete with newly used tags
                for (String tag : selectedTags) {
                    if (!allTags.contains(tag)) {
                        allTags.add(tag);
                    }
                }

                resetInput();
                makeNonInvasive(dialog);
            } else if (!hasText && !hasTags){
                // Empty text: just close
                dialog.close();
            } else {
            	// Nichts tun :-)
            }
        });

        // Consume the default button-close behavior
        dialog.setResultConverter(_ -> null);

        if (invasive) {
            makeInvasive(dialog);
        }

        Platform.runLater(() -> textArea.requestFocus());
        dialog.showAndWait();
    }

    private void makeInvasive(Dialog<?> dialog) {
        // Block closing
        closeBlocker = WindowEvent::consume;
        dialog.getDialogPane().getScene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);

        // Disable save until min requirements met
        int minChars = Config.getInt("diary.minChars", DEFAULT_MIN_CHARS);
        int minTags = Config.getInt("diary.minTags", DEFAULT_MIN_TAGS);

        requirementsNotMet = new BooleanBinding() {
            { bind(textArea.textProperty(), selectedTags); }
            @Override
            protected boolean computeValue() {
                int textLen = textArea.getText() == null ? 0 : textArea.getText().length();
                return textLen < minChars || selectedTags.size() < minTags;
            }
        };

        tagsListener = _ -> requirementsNotMet.invalidate();
        selectedTags.addListener(tagsListener);

        saveButton.disableProperty().bind(requirementsNotMet);

        // After configured time, release the dialog
        int invasiveSeconds = Config.getInt("diary.invasiveSeconds", DEFAULT_INVASIVE_SECONDS);
        System.out.println(invasiveSeconds);
        Timeline timer = new Timeline(new KeyFrame(
                Duration.seconds(invasiveSeconds),
                _ -> makeNonInvasive(dialog)));
        timer.setCycleCount(1);
        timer.play();
    }

    private void makeNonInvasive(Dialog<?> dialog) {
        // Remove close blocker
        if (closeBlocker != null) {
            dialog.getDialogPane().getScene().getWindow().removeEventFilter(
                    WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);
            closeBlocker = null;
        }

        // Remove tags listener and unbind save button
        if (tagsListener != null) {
            selectedTags.removeListener(tagsListener);
            tagsListener = null;
        }
        saveButton.disableProperty().unbind();
        saveButton.setDisable(false);
        requirementsNotMet = null;
    }

    private void resetInput() {
        textArea.clear();
        selectedTags.clear();
        rebuildChips();
        tagInput.clear();
        textArea.requestFocus();
    }

    private VBox buildContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        // --- Top row: DatePicker + Tag input ---
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("diaryDatePicker");

        tagInput = new TextField();
        tagInput.setPromptText("Tag hinzufügen...");
        tagInput.setPrefWidth(200);

        setupTagAutocomplete();

        topRow.getChildren().addAll(datePicker, tagInput);

        // --- Chip pane ---
        chipPane = new FlowPane(6, 6);
        chipPane.setAlignment(Pos.CENTER_LEFT);
        chipPane.setPadding(new Insets(4));
        chipPane.managedProperty().bind(chipPane.visibleProperty());
        updateChipPaneVisibility();

        // --- Text area ---
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
        	    } else {
        	        saveButton.requestFocus();
        	    }
        	}
        });

        root.getChildren().addAll(topRow, chipPane, textArea);

        return root;
    }

    private void setupTagAutocomplete() {
        suggestionBox = new VBox();
        suggestionBox.getStyleClass().add("suggestion-box");

        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        tagInput.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!suggestionPopup.isShowing() || currentMatches.isEmpty()) {
                return;
            }
            switch (e.getCode()) {
                case ENTER, TAB -> {
                    e.consume();
                    if (activeIndex >= 0 && activeIndex < currentMatches.size()) {
                        addTag(currentMatches.get(activeIndex));
                    }
                }
                case DOWN -> {
                    e.consume();
                    setActiveIndex(Math.min(activeIndex + 1, currentMatches.size() - 1));
                }
                case UP -> {
                    e.consume();
                    setActiveIndex(Math.max(activeIndex - 1, 0));
                }
                case ESCAPE -> {
                    e.consume();
                    hideSuggestions();
                }
                default -> {}
            }
        });

        tagInput.setOnAction(_ -> {
            String text = tagInput.getText().trim();
            if (!text.isEmpty()) {
                addTag(text);
            }
        });

        tagInput.textProperty().addListener((_, _, newVal) -> {
            if (!suppressSuggestions) {
                updateSuggestions(newVal);
            }
        });
    }

    private void addTag(String tag) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty() && !selectedTags.contains(trimmed)) {
            selectedTags.add(trimmed);
            rebuildChips();
        }
        suppressSuggestions = true;
        tagInput.clear();
        suppressSuggestions = false;
        hideSuggestions();
        tagInput.requestFocus();
    }

    private void hideSuggestions() {
        suggestionPopup.hide();
        currentMatches.clear();
        activeIndex = -1;
    }

    private void updateSuggestions(String filter) {
        if (filter == null || filter.isBlank()) {
            hideSuggestions();
            return;
        }

        String lower = filter.toLowerCase();
        currentMatches.clear();
        currentMatches.addAll(allTags.stream()
                .filter(t -> t.toLowerCase().contains(lower))
                .filter(t -> !selectedTags.contains(t))
                .collect(Collectors.toList()));

        if (currentMatches.isEmpty()) {
            hideSuggestions();
            return;
        }

        rebuildSuggestionBox();
        activeIndex = 0;
        highlightActive();
        showPopupBelowInput();
    }

    private void rebuildSuggestionBox() {
        suggestionBox.getChildren().clear();
        for (int i = 0; i < currentMatches.size(); i++) {
            String match = currentMatches.get(i);
            Label label = new Label(match);
            label.setMaxWidth(Double.MAX_VALUE);
            //label.setStyle("-fx-padding: 6 12; -fx-font-size: 14px; -fx-cursor: hand; "
              //      + "-fx-background-color: white;");

            int index = i;
            label.setOnMouseEntered(_ -> setActiveIndex(index));
            label.setOnMouseClicked(_ -> addTag(match));

            suggestionBox.getChildren().add(label);
        }
        suggestionBox.setPrefWidth(tagInput.getWidth());
    }

    private void setActiveIndex(int index) {
        activeIndex = index;
        highlightActive();
    }

    private void highlightActive() {
        var children = suggestionBox.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Label label = (Label) children.get(i);
            label.pseudoClassStateChanged(HIGHLIGHTED, i == activeIndex);
        }
    }

    private void showPopupBelowInput() {
        Bounds bounds = tagInput.localToScreen(tagInput.getBoundsInLocal());
        if (bounds != null) {
            suggestionPopup.show(tagInput, bounds.getMinX(), bounds.getMaxY() + 2);
        }
    }

    private void rebuildChips() {
        chipPane.getChildren().clear();
        for (String tag : selectedTags) {
            chipPane.getChildren().add(createChip(tag));
        }
        updateChipPaneVisibility();
    }

    private void updateChipPaneVisibility() {
        chipPane.setVisible(!selectedTags.isEmpty());
    }

    private HBox createChip(String tag) {
        Label label = new Label(tag);
        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add(TAG_REMOVE_BUTTON);
        removeBtn.setOnAction(_ -> {
            selectedTags.remove(tag);
            rebuildChips();
            tagInput.requestFocus();
        });
        removeBtn.setFocusTraversable(false);

        HBox chip = new HBox(2, label, removeBtn);
        chip.setAlignment(Pos.CENTER);
       // chip.setStyle(
         //       "-fx-background-color: #e0e0e0; -fx-background-radius: 12; "
           //             + "-fx-padding: 2 8 2 8;");
        chip.setFocusTraversable(false);
        
        return chip;
    }
}