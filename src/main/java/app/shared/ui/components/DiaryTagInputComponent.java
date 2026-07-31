package app.shared.ui.components;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/**
 * Mehrfachauswahl von Tags: ein Vorschlagsfeld, darunter das Gewählte als Chips.
 *
 * <p>Die Vorschlagsmechanik steckt vollständig im {@link SuiteSuggestionTextField}. Hier bleibt
 * allein, was das Tagebuch daraus macht: aus einer Auswahl wird ein Chip, aus Freitext ein neuer
 * Tag, und was schon als Chip dasteht, wird nicht nochmal vorgeschlagen.</p>
 *
 * <p>Kein Node, sondern zwei — Feld und Chips landen beim Aufrufer an verschiedenen Stellen im
 * Layout, deshalb gibt es sie einzeln heraus statt in einer gemeinsamen Box.</p>
 */
public class DiaryTagInputComponent {

    private static final String TAG_REMOVE_BUTTON = "tag-chip-remove";

    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();
    private List<String> allTags = new ArrayList<>();

    private final SuiteSuggestionTextField tagInput;
    private final FlowPane chipPane;

    public DiaryTagInputComponent() {
        tagInput = new SuiteSuggestionTextField("Tag hinzufügen...");
        tagInput.setPrefWidth(200);

        // Aus der Liste gewählt.
        tagInput.setOnSelected(this::addTag);

        // Selbst getippt: ENTER kommt nur hier an, wenn die Vorschlagsliste zu ist.
        tagInput.setOnAction(_ -> {
            String text = tagInput.getText().trim();
            if (!text.isEmpty())
                addTag(text);
        });

        chipPane = new FlowPane(6, 6);
        chipPane.setAlignment(Pos.CENTER_LEFT);
        chipPane.setPadding(new Insets(4));
        chipPane.managedProperty().bind(chipPane.visibleProperty());
        updateChipPaneVisibility();
    }

    public SuiteSuggestionTextField getTagInput() {
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
        refreshSuggestionPool();
    }

    public void addTag(String tag) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty() && !selectedTags.contains(trimmed)) {
            selectedTags.add(trimmed);
            rebuildChips();
            refreshSuggestionPool();
        }
        tagInput.clearSilent();
        tagInput.requestFocus();
    }

    public void reset() {
        selectedTags.clear();
        rebuildChips();
        refreshSuggestionPool();
        tagInput.clearSilent();
    }

    public void requestFocus() {
        tagInput.requestFocus();
    }

    /** Was schon als Chip dasteht, gehört nicht mehr in die Vorschläge. */
    private void refreshSuggestionPool() {
        List<String> uebrig = new ArrayList<>();
        for (String tag : allTags)
            if (!selectedTags.contains(tag))
                uebrig.add(tag);
        tagInput.setAllItems(uebrig);
    }

    private void rebuildChips() {
        chipPane.getChildren().clear();
        for (String tag : selectedTags)
            chipPane.getChildren().add(createChip(tag));
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
            refreshSuggestionPool();
            tagInput.requestFocus();
        });
        removeBtn.setFocusTraversable(false);

        HBox chip = new HBox(2, label, removeBtn);
        chip.setAlignment(Pos.CENTER);
        chip.setFocusTraversable(false);
        return chip;
    }
}
