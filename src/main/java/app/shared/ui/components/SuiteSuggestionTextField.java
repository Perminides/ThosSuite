package app.shared.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Ein Textfeld, das während des Tippens passende Vorschläge unter sich einblendet
 * (SWYT = „Search While You Type").
 *
 * <p>Es <b>ist</b> ein {@link TextField} — wer es einbaut, hängt es direkt in sein Layout und nutzt
 * alles, was ein Textfeld kann: {@code setPromptText}, {@code setPrefWidth}, {@code setOnAction},
 * {@code requestFocus}.</p>
 *
 * <p>Was es <b>nicht</b> entscheidet: was eine Auswahl bedeutet. Über {@link #setOnSelected} meldet
 * es nur, dass der Benutzer einen Eintrag aus der Liste genommen hat. Ob daraus ein Chip wird
 * (Tagebuch), eine Kontakt-Id (WhatsApp) oder ein Filter (Filme), ist Sache des Aufrufers. Ebenso
 * die Reihenfolge: gefiltert wird stabil in der Reihenfolge der übergebenen Liste, sortieren muss
 * der Aufrufer.</p>
 *
 * <p>Freitext, der auf keinen Vorschlag passt, läuft am Popup vorbei: dann ist es zu, ENTER wird
 * nicht abgefangen und landet beim {@code setOnAction} des Aufrufers. Genau daran unterscheiden
 * Tagebuch und WhatsApp „aus der Liste gewählt" von „selbst getippt".</p>
 *
 * <p>Reihenfolge bei einer Auswahl: erst steht der Text im Feld, dann kommt der Callback. Ein
 * {@code textProperty}-Listener des Aufrufers hat also bereits gefeuert, wenn
 * {@link #setOnSelected} an der Reihe ist — wer beides nutzt, darf im Callback das letzte Wort
 * behalten.</p>
 *
 * <p>CSS: Liste = {@code .suggestion-box}, hervorgehobene Zeile = {@code :highlighted}.</p>
 */
public class SuiteSuggestionTextField extends TextField {

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final int MAX_SUGGESTIONS = 20;

    private final Popup suggestionPopup = new Popup();
    private final VBox suggestionBox = new VBox();

    private List<String> allItems = new ArrayList<>();
    private final List<String> currentMatches = new ArrayList<>();
    private int activeIndex = -1;

    /** Gesetzt, solange wir den Text selbst schreiben — sonst löste unser eigenes setText neue Vorschläge aus. */
    private boolean suppressSuggestions = false;

    /** Wird aufgerufen, wenn ein Vorschlag ausgewählt wird (Enter, Tab, Klick). */
    private Consumer<String> onSelected;

    public SuiteSuggestionTextField(String promptText) {
        setPromptText(promptText);

        suggestionBox.getStyleClass().add("suggestion-box");

        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        setupListeners();
    }

    public void setAllItems(List<String> items) {
        this.allItems = new ArrayList<>(items);
    }

    public void setOnSelected(Consumer<String> onSelected) {
        this.onSelected = onSelected;
    }

    /**
     * Setzt den Text programmatisch und löst den Callback aus, ohne die Vorschlagsliste zu öffnen.
     * Wird von den Links in den Filmkacheln aufgerufen.
     */
    public void setTextAndTrigger(String text) {
        setTextSilent(text);
        hideSuggestions();
        if (onSelected != null)
            onSelected.accept(text);
    }

    /**
     * Setzt den Text ohne Callback und ohne Popup — zum Wiederherstellen nach einem Neuaufbau.
     * Der Gegenspieler zu {@link #setTextAndTrigger(String)}: derselbe Text, aber niemand erfährt davon.
     */
    public void setTextSilent(String text) {
        suppressSuggestions = true;
        setText(text);
        suppressSuggestions = false;
    }

    /** Leert das Feld, ohne Callback oder Popup auszulösen. */
    public void clearSilent() {
        suppressSuggestions = true;
        clear();
        suppressSuggestions = false;
    }

    // -------------------------------------------------------------------------

    private void setupListeners() {
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!suggestionPopup.isShowing() || currentMatches.isEmpty())
                return;
            switch (e.getCode()) {
                case ENTER, TAB -> {
                    e.consume();
                    if (activeIndex >= 0 && activeIndex < currentMatches.size())
                        selectItem(currentMatches.get(activeIndex));
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

        textProperty().addListener((_, _, newVal) -> {
            if (!suppressSuggestions)
                updateSuggestions(newVal);
        });

        // Popup schließen wenn das Feld den Fokus verliert
        focusedProperty().addListener((_, _, focused) -> {
            if (!focused)
                hideSuggestions();
        });
    }

    private void selectItem(String item) {
        setTextSilent(item);
        hideSuggestions();
        // Gesamten Text markieren, damit Weitertippen ihn ersetzt
        selectAll();
        if (onSelected != null)
            onSelected.accept(item);
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
        for (String item : allItems) {
            if (currentMatches.size() == MAX_SUGGESTIONS)
                break;
            if (item.toLowerCase().contains(lower))
                currentMatches.add(item);
        }

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
        suggestionBox.setPrefWidth(getWidth());
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
        Bounds bounds = localToScreen(getBoundsInLocal());
        if (bounds != null)
            suggestionPopup.show(this, bounds.getMinX(), bounds.getMaxY() + 2);
    }
}
