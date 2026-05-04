package app.ui.components;

import app.ui.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Modaler Dialog zur Auflösung eines unbekannten WhatsApp-Kontakts.
 *
 * <p>Wird aufgerufen wenn der Inkrementalimport auf einen Kontakt trifft,
 * der noch nicht in der Suite-DB bekannt ist. Der Nutzer kann entweder
 * einen bestehenden Kontakt auswählen (per Autocomplete-Vorschlagsliste)
 * oder einen neuen Namen eingeben.</p>
 *
 * <p>Autocomplete-Verhalten:
 * <ul>
 *   <li>Während des Tippens werden passende bestehende Kontakte als Popup angezeigt</li>
 *   <li>Klick auf einen Vorschlag → bestehender Kontakt wird gewählt</li>
 *   <li>Enter → oberster Vorschlag wird gewählt (falls Vorschläge vorhanden)</li>
 *   <li>Freitext ohne Auswahl aus der Liste → neuer Kontakt wird angelegt</li>
 * </ul>
 * </p>
 *
 * <p>Bekannte Einschränkung: Wenn ein bestehender Kontakt "Michael Meier" existiert,
 * kann kein neuer Kontakt "Michael" angelegt werden, da "Michael" als Substring matcht
 * und per Enter den Vorschlag auswählen würde. Diese Einschränkung wird bewusst
 * in Kauf genommen.</p>
 *
 * <p>Das Schließen per X ist blockiert — der Import kann ohne Entscheidung
 * nicht fortgesetzt werden.</p>
 */
public class WhatsAppContactDialog {

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

    /**
     * Ergebnis des Dialogs. Genau eines der beiden Felder ist non-null.
     *
     * @param existingContactId contact_id eines bestehenden Kontakts, oder null
     * @param newDisplayName    Name für einen neuen Kontakt, oder null
     */
    public record Result(Integer existingContactId, String newDisplayName) {}

    /**
     * Zeigt den Dialog und wartet auf Eingabe des Nutzers.
     *
     * @param rawIdentifier    JID des Kontakts (z.B. "491234567890@s.whatsapp.net")
     * @param knownContacts    Map von Anzeigename → contact_id der bekannten Kontakte
     * @return Entscheidung des Nutzers
     * @throws IllegalStateException [FAILFAST] wenn der Dialog ohne Entscheidung geschlossen wird
     */
    public static Result show(String rawIdentifier, Map<String, Integer> knownContacts) {
        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), "Unbekannter WhatsApp-Kontakt");

        // Ergebnis-State
        final Integer[] selectedContactId = {null};

        // X-Button blockieren
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(e -> {
            e.consume();
        });

        // Content
        VBox content = SkinService.get().createDialogContent();

        Label infoLabel = new Label("Unbekannter Kontakt: " + rawIdentifier);
        Label nameLabel = new Label("Name (bestehenden auswählen oder neuen eingeben):");
        TextField nameField = new TextField();
        nameField.setPromptText("Name...");

        content.getChildren().addAll(infoLabel, nameLabel, nameField);
        dialog.getDialogPane().setContent(content);

        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(okBtn);

        // Autocomplete
        List<String> allNames   = new ArrayList<>(knownContacts.keySet());
        List<String> matches    = new ArrayList<>();
        VBox         suggestionBox = new VBox();
        suggestionBox.getStyleClass().add("suggestion-box");
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(suggestionBox);
        final int[] activeIndex = {-1};

        Runnable hideSuggestions = () -> {
            popup.hide();
            matches.clear();
            activeIndex[0] = -1;
        };

        Runnable highlightActive = () -> {
            var children = suggestionBox.getChildren();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).pseudoClassStateChanged(HIGHLIGHTED, i == activeIndex[0]);
            }
        };

        nameField.textProperty().addListener((_, _, newVal) -> {
            selectedContactId[0] = null; // Freitext → Auswahl zurücksetzen
            if (newVal == null || newVal.isBlank()) {
                hideSuggestions.run();
                return;
            }
            String lower = newVal.toLowerCase();
            matches.clear();
            matches.addAll(allNames.stream()
                .filter(n -> n.toLowerCase().contains(lower))
                .collect(Collectors.toList()));

            if (matches.isEmpty()) {
                hideSuggestions.run();
                return;
            }

            suggestionBox.getChildren().clear();
            for (int i = 0; i < matches.size(); i++) {
                String match = matches.get(i);
                Label lbl = new Label(match);
                lbl.setMaxWidth(Double.MAX_VALUE);
                int idx = i;
                lbl.setOnMouseEntered(_ -> {
                    activeIndex[0] = idx;
                    highlightActive.run();
                });
                lbl.setOnMouseClicked(_ -> {
                    selectedContactId[0] = knownContacts.get(match);
                    nameField.setText(match);
                    hideSuggestions.run();
                });
                suggestionBox.getChildren().add(lbl);
            }

            activeIndex[0] = 0;
            highlightActive.run();

            Bounds bounds = nameField.localToScreen(nameField.getBoundsInLocal());
            if (bounds != null) {
                suggestionBox.setPrefWidth(nameField.getWidth());
                popup.show(nameField, bounds.getMinX(), bounds.getMaxY() + 2);
            }
        });

        nameField.setOnKeyPressed(e -> {
            if (!popup.isShowing() || matches.isEmpty()) return;
            switch (e.getCode()) {
                case ENTER -> {
                    e.consume();
                    String chosen = matches.get(Math.max(0, activeIndex[0]));
                    selectedContactId[0] = knownContacts.get(chosen);
                    nameField.setText(chosen);
                    hideSuggestions.run();
                }
                case DOWN -> {
                    e.consume();
                    activeIndex[0] = Math.min(activeIndex[0] + 1, matches.size() - 1);
                    highlightActive.run();
                }
                case UP -> {
                    e.consume();
                    activeIndex[0] = Math.max(activeIndex[0] - 1, 0);
                    highlightActive.run();
                }
                case ESCAPE -> {
                    e.consume();
                    hideSuggestions.run();
                }
                default -> {}
            }
        });

        dialog.showAndWait();

        // Ergebnis auswerten
        if (nameField.getText() == null || nameField.getText().isBlank())
            throw new IllegalStateException(
                "[FAILFAST] WhatsApp-Import abgebrochen: Kein Name im Kontakt-Dialog eingegeben. " +
                "rawIdentifier=" + rawIdentifier);

        if (selectedContactId[0] != null) {
            return new Result(selectedContactId[0], null);
        } else {
            return new Result(null, nameField.getText().trim());
        }
    }
}