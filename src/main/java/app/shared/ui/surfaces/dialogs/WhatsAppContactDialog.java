package app.shared.ui.surfaces.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import app.shared.Log;
import app.shared.skin.SkinService;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

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
 *   <li>Enter oder Tab → markierter Vorschlag wird gewählt (falls Popup offen)</li>
 *   <li>Freitext ohne Auswahl aus der Liste → neuer Kontakt wird angelegt</li>
 * </ul>
 * </p>
 *
 * <p>Bekannte Einschränkung: Wenn ein bestehender Kontakt "Michael Meier" existiert,
 * kann kein neuer Kontakt "Michael" angelegt werden, da "Michael" als Substring matcht
 * und per Enter/Tab den Vorschlag auswählen würde. Diese Einschränkung wird bewusst
 * in Kauf genommen.</p>
 *
 * <p>Das Schließen per X ist blockiert — der Import kann ohne Entscheidung
 * nicht fortgesetzt werden.</p>
 * <p>Titel-Wechsel wird über SkinService.get().setDialogTitle() realisiert, nicht über stage.setTitle().
 * Grund: Der Dialog nutzt eine custom HeaderBar mit einem Label als Titel — stage.setTitle() setzt
 * nur den nativen Fenstertitel, der hinter der HeaderBar verborgen ist.</p>
 * Alternativen die verworfen wurden:
 * Haken im TextField-Text ("✓ Wolfgang"): setText() im textProperty-Listener erzeugte
 * Endlosschleifen und einen JavaFX-Bug (IllegalArgumentException: start > end beim Löschen).
 * Button-Text togglen ("OK" / "Übernehmen"): ButtonBar setzt feste Button-Breiten,
 * die sich nach einem setText() nicht neu berechnen — der Button blieb immer gleich breit.
 */
public class WhatsAppContactDialog {

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

    private static final String TITLE_NEW      = "Neuer WhatsApp-Kontakt";
    private static final String TITLE_SELECTED = "Ausgewählter WhatsApp-Kontakt";

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
        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), TITLE_NEW);

        // Ergebnis-State
        final Integer[] selectedContactId = {null};
        final boolean[] settingFromCode   = {false};

        // X-Button blockieren
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(e -> e.consume());

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

        // OK-Button solange disabled bis etwas eingegeben wurde
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);

        // Autocomplete
        List<String> allNames      = new ArrayList<>(knownContacts.keySet());
        List<String> matches       = new ArrayList<>();
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

        Runnable selectActive = () -> {
            String chosen = matches.get(activeIndex[0]);
            selectedContactId[0] = knownContacts.get(chosen);
            Log.debug(WhatsAppContactDialog.class, "[selectActive] chosen=" + chosen + " selectedContactId=" + selectedContactId[0]);
            SkinService.get().setDialogTitle(dialog, TITLE_SELECTED);
            Log.debug(WhatsAppContactDialog.class, "[selectActive] Titel gesetzt auf: " + TITLE_SELECTED);
            settingFromCode[0] = true;
            Log.debug(WhatsAppContactDialog.class, "[selectActive] settingFromCode=true, rufe setText auf");
            nameField.setText(chosen);
            settingFromCode[0] = false;
            Log.debug(WhatsAppContactDialog.class, "[selectActive] settingFromCode=false");
            hideSuggestions.run();
        };

        nameField.textProperty().addListener((_, _, newVal) -> {
        	Log.debug(WhatsAppContactDialog.class, "[listener] newVal='" + newVal + "' settingFromCode=" + settingFromCode[0] + " selectedContactId=" + selectedContactId[0]);
            if (!settingFromCode[0]) {
                selectedContactId[0] = null;
                SkinService.get().setDialogTitle(dialog, TITLE_NEW);
                Log.debug(WhatsAppContactDialog.class, "[listener] Titel zurückgesetzt auf: " + TITLE_NEW);
            }
            okButton.setDisable(newVal == null || newVal.isBlank());
            if (newVal == null || newVal.isBlank()) {
                hideSuggestions.run();
                return;
            }
            if (settingFromCode[0]) return;

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
                    SkinService.get().setDialogTitle(dialog, TITLE_SELECTED);
                    settingFromCode[0] = true;
                    nameField.setText(match);
                    settingFromCode[0] = false;
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
            switch (e.getCode()) {
                case ENTER -> {
                    if (popup.isShowing()) {
                        e.consume();
                        selectActive.run();
                    }
                }
                case TAB -> {
                    if (popup.isShowing()) {
                        e.consume();
                        selectActive.run();
                    }
                }
                case DOWN -> {
                    if (popup.isShowing()) {
                        e.consume();
                        activeIndex[0] = Math.min(activeIndex[0] + 1, matches.size() - 1);
                        highlightActive.run();
                    }
                }
                case UP -> {
                    if (popup.isShowing()) {
                        e.consume();
                        activeIndex[0] = Math.max(activeIndex[0] - 1, 0);
                        highlightActive.run();
                    }
                }
                case ESCAPE -> {
                    if (popup.isShowing()) {
                        e.consume();
                        hideSuggestions.run();
                    }
                }
                default -> {}
            }
        });

        dialog.setOnShown(_ -> Platform.runLater(() -> {
            nameField.selectAll();
            nameField.requestFocus();
        }));

        dialog.showAndWait();

        // Ergebnis auswerten
        if (nameField.getText() == null || nameField.getText().isBlank())
            return null; // Abgebrochen
        else if (selectedContactId[0] != null) {
            return new Result(selectedContactId[0], null); // Kontakt ausgewählt
        } else { 
            return new Result(null, nameField.getText().trim()); // Neuen Namen eingegeben
        }
    }
}