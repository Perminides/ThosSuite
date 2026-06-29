package scripts.diary;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class DiaryEntryPrototype extends Application {

    private static final List<String> ALL_TAGS = List.of(
            "Arbeit", "Privat", "Gesundheit", "Sport", "Lernen",
            "ThosSuite", "Gedanken", "Ideen", "Träume", "Dankbarkeit",
            "Ärger", "Freude", "TODO", "Wichtig", "Reise"
    );

    private static final String SUGGESTION_STYLE =
            "-fx-padding: 6 12; -fx-font-size: 14px; -fx-cursor: hand;";
    private static final String SUGGESTION_ACTIVE_STYLE =
            SUGGESTION_STYLE + " -fx-background-color: #d0d0d0;";
    private static final String SUGGESTION_INACTIVE_STYLE =
            SUGGESTION_STYLE + " -fx-background-color: white;";

    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();
    private FlowPane chipPane;
    private TextField tagInput;
    private TextArea textArea;

    private Popup suggestionPopup;
    private VBox suggestionBox;
    private List<String> currentMatches = List.of();
    private int activeIndex = 0;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        // --- Suggestion popup ---
        suggestionBox = new VBox();
        suggestionBox.setStyle("-fx-background-color: white; -fx-border-color: #bbb; "
                + "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");

        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        // --- Top row: DatePicker + Tag input ---
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        tagInput = new TextField();
        tagInput.setPromptText("Tag hinzufügen...");
        tagInput.setPrefWidth(200);

        tagInput.textProperty().addListener((obs, oldVal, newVal) -> updateSuggestions(newVal));

        tagInput.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!suggestionPopup.isShowing()) {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    String text = tagInput.getText().trim();
                    if (!text.isEmpty()) {
                        addTag(text);
                    }
                }
                if (e.getCode() == KeyCode.BACK_SPACE && tagInput.getText().isEmpty()
                        && !selectedTags.isEmpty()) {
                    selectedTags.remove(selectedTags.size() - 1);
                    rebuildChips();
                }
                return;
            }

            switch (e.getCode()) {
                case ENTER, TAB -> {
                    e.consume();
                    if (!currentMatches.isEmpty()) {
                        addTag(currentMatches.get(activeIndex));
                    }
                }
                case DOWN -> {
                    e.consume();
                    if (!currentMatches.isEmpty()) {
                        activeIndex = Math.min(activeIndex + 1, currentMatches.size() - 1);
                        updateHighlight();
                    }
                }
                case UP -> {
                    e.consume();
                    if (!currentMatches.isEmpty()) {
                        activeIndex = Math.max(activeIndex - 1, 0);
                        updateHighlight();
                    }
                }
                case ESCAPE -> {
                    e.consume();
                    suggestionPopup.hide();
                }
                default -> {}
            }
        });

        topRow.getChildren().addAll(datePicker, tagInput);

        // --- Chip pane: hidden when empty ---
        chipPane = new FlowPane(6, 6);
        chipPane.setAlignment(Pos.CENTER_LEFT);
        chipPane.setPadding(new Insets(4));
        chipPane.managedProperty().bind(chipPane.visibleProperty());

        // --- Text area ---
        textArea = new TextArea();
        textArea.setPromptText("Was beschäftigt dich?");
        textArea.setPrefColumnCount(100);
        textArea.setPrefRowCount(10);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        // --- Save button ---
        Button saveButton = new Button("Speichern");
        saveButton.setOnAction(e -> {
            System.out.println("=== Diary Entry ===");
            System.out.println("Datum: " + datePicker.getValue());
            System.out.println("Tags: " + selectedTags);
            System.out.println("Text: " + textArea.getText());
            System.out.println("Zeichen: " + textArea.getText().length());
            System.out.println("===================");
        });

        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().add(saveButton);

        root.getChildren().addAll(topRow, chipPane, textArea, buttonRow);
        updateChipPaneVisibility();

        Scene scene = new Scene(root, 750, 450);
        scene.getRoot().setStyle("-fx-font-size: 15px;");
        stage.setTitle("Diary Entry - Prototyp");
        stage.setScene(scene);
        stage.show();

        textArea.requestFocus();
    }

    private void addTag(String tag) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty() && !selectedTags.contains(trimmed)) {
            selectedTags.add(trimmed);
            rebuildChips();
        }
        suggestionPopup.hide();
        tagInput.clear();
        tagInput.requestFocus();
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
        removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-padding: 0 0 0 4; "
                        + "-fx-cursor: hand; -fx-font-size: 14;");
        removeBtn.setOnAction(e -> {
            selectedTags.remove(tag);
            rebuildChips();
            tagInput.requestFocus();
        });

        HBox chip = new HBox(2, label, removeBtn);
        chip.setAlignment(Pos.CENTER);
        chip.setStyle(
                "-fx-background-color: #e0e0e0; -fx-background-radius: 12; "
                        + "-fx-padding: 2 8 2 8;");
        return chip;
    }

    private void updateSuggestions(String filter) {
        if (filter == null || filter.isBlank()) {
            suggestionPopup.hide();
            currentMatches = List.of();
            return;
        }

        String lower = filter.toLowerCase();
        currentMatches = ALL_TAGS.stream()
                .filter(t -> t.toLowerCase().contains(lower))
                .filter(t -> !selectedTags.contains(t))
                .collect(Collectors.toList());

        if (currentMatches.isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        activeIndex = 0;
        suggestionBox.getChildren().clear();

        for (int i = 0; i < currentMatches.size(); i++) {
            String match = currentMatches.get(i);
            Label suggestionLabel = new Label(match);
            suggestionLabel.setMaxWidth(Double.MAX_VALUE);
            suggestionLabel.setStyle(i == 0 ? SUGGESTION_ACTIVE_STYLE : SUGGESTION_INACTIVE_STYLE);

            int index = i;
            suggestionLabel.setOnMouseEntered(e -> {
                activeIndex = index;
                updateHighlight();
            });
            suggestionLabel.setOnMouseClicked(e -> addTag(match));

            suggestionBox.getChildren().add(suggestionLabel);
        }

        showPopupBelowTagInput();
    }

    private void updateHighlight() {
        for (int i = 0; i < suggestionBox.getChildren().size(); i++) {
            Label label = (Label) suggestionBox.getChildren().get(i);
            label.setStyle(i == activeIndex ? SUGGESTION_ACTIVE_STYLE : SUGGESTION_INACTIVE_STYLE);
        }
    }

    private void showPopupBelowTagInput() {
        Bounds bounds = tagInput.localToScreen(tagInput.getBoundsInLocal());
        if (bounds == null) return;
        suggestionPopup.show(tagInput, bounds.getMinX(), bounds.getMaxY() + 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}