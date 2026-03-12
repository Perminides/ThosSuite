package app.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.scene.layout.VBox;

public class TagInputComponent {

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final String TAG_REMOVE_BUTTON = "tag-chip-remove";

    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();
    private final List<String> currentMatches = new ArrayList<>();
    private List<String> allTags = new ArrayList<>();

    private final FlowPane chipPane;
    private final TextField tagInput;
    private final Popup suggestionPopup;
    private final VBox suggestionBox;

    private int activeIndex = -1;
    private boolean suppressSuggestions = false;

    public TagInputComponent() {
        tagInput = new TextField();
        tagInput.setPromptText("Tag hinzufügen...");
        tagInput.setPrefWidth(200);

        chipPane = new FlowPane(6, 6);
        chipPane.setAlignment(Pos.CENTER_LEFT);
        chipPane.setPadding(new Insets(4));
        chipPane.managedProperty().bind(chipPane.visibleProperty());
        updateChipPaneVisibility();

        suggestionBox = new VBox();
        suggestionBox.getStyleClass().add("suggestion-box");

        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        setupTagAutocomplete();
    }

    public TextField getTagInput() {
        return tagInput;
    }

    public FlowPane getChipPane() {
        return chipPane;
    }

    public ObservableList<String> getSelectedTags() {
        return selectedTags;
    }

    public void setAllTags(List<String> tags) {
        this.allTags = new ArrayList<>(tags);
    }

    public void reset() {
        selectedTags.clear();
        rebuildChips();
        suppressSuggestions = true;
        tagInput.clear();
        suppressSuggestions = false;
        hideSuggestions();
    }

    public void requestFocus() {
        tagInput.requestFocus();
    }

    private void setupTagAutocomplete() {
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

    public void addTag(String tag) {
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
        chip.setFocusTraversable(false);
        return chip;
    }
}