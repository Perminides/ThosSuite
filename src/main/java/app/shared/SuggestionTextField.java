package app.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Textfeld mit Autocomplete-Dropdown (SWYT = "Search While You Type").
 * 
 * Einfache Single-Select-Variante: Der Benutzer tippt, sieht Vorschläge,
 * wählt per Enter/Klick einen aus. Bei Auswahl wird der Callback aufgerufen.
 * 
 * Die Vorschlagsliste wird clientseitig gefiltert (contains, case-insensitive).
 * Die Reihenfolge der Vorschläge entspricht der Reihenfolge in der übergebenen
 * Liste — der Aufrufer ist verantwortlich für die Sortierung (z.B. nach
 * Häufigkeit bewerteter Filme).
 * 
 * Popup-Mechanik übernommen aus TagInputComponent.
 */
public class SuggestionTextField {

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final int MAX_SUGGESTIONS = 20;

    private final TextField textField;
    private final Popup suggestionPopup;
    private final VBox suggestionBox;

    private List<String> allItems = new ArrayList<>();
    private final List<String> currentMatches = new ArrayList<>();
    private int activeIndex = -1;
    private boolean suppressSuggestions = false;

    /** Wird aufgerufen, wenn ein Vorschlag ausgewählt wird (Enter, Klick). */
    private Consumer<String> onSelected;

    public SuggestionTextField(String promptText) {
        textField = new TextField();
        textField.setPromptText(promptText);

        suggestionBox = new VBox();
        suggestionBox.getStyleClass().add("suggestion-box");

        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        setupListeners();
    }

    public TextField getTextField() {
        return textField;
    }

    public void setAllItems(List<String> items) {
        this.allItems = new ArrayList<>(items);
    }

    public void setOnSelected(Consumer<String> onSelected) {
        this.onSelected = onSelected;
    }

    /**
     * Setzt den Text programmatisch und löst den Callback aus,
     * ohne das Suggestion-Popup zu öffnen.
     * Wird von den Links in den Filmkacheln aufgerufen.
     */
    public void setTextAndTrigger(String text) {
        suppressSuggestions = true;
        textField.setText(text);
        suppressSuggestions = false;
        hideSuggestions();
        if (onSelected != null) {
            onSelected.accept(text);
        }
    }

    /**
     * Leert das Textfeld ohne Callback oder Popup auszulösen.
     */
    public void clearSilent() {
        suppressSuggestions = true;
        textField.clear();
        suppressSuggestions = false;
    }

    // -------------------------------------------------------------------------

    private void setupListeners() {
        textField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!suggestionPopup.isShowing() || currentMatches.isEmpty()) {
                return;
            }
            switch (e.getCode()) {
                case ENTER, TAB -> {
                    e.consume();
                    if (activeIndex >= 0 && activeIndex < currentMatches.size()) {
                        selectItem(currentMatches.get(activeIndex));
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

        textField.textProperty().addListener((_, _, newVal) -> {
            if (!suppressSuggestions) {
                updateSuggestions(newVal);
            }
        });

        // Popup schließen wenn das Feld den Fokus verliert
        textField.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                hideSuggestions();
            }
        });
    }

    private void selectItem(String item) {
        suppressSuggestions = true;
        textField.setText(item);
        suppressSuggestions = false;
        hideSuggestions();
        // Gesamten Text markieren, damit Weitertippen ihn ersetzt
        textField.selectAll();
        if (onSelected != null) {
            onSelected.accept(item);
        }
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
        currentMatches.addAll(allItems.stream()
                .filter(item -> item.toLowerCase().contains(lower))
                .limit(MAX_SUGGESTIONS)
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
            label.setOnMouseClicked(_ -> selectItem(match));

            suggestionBox.getChildren().add(label);
        }
        suggestionBox.setPrefWidth(textField.getWidth());
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
        Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
        if (bounds != null) {
            suggestionPopup.show(textField, bounds.getMinX(), bounds.getMaxY() + 2);
        }
    }
}