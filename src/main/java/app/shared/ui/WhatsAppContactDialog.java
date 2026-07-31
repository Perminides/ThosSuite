package app.shared.ui;

import java.util.ArrayList;
import java.util.Map;

import app.shared.ui.components.SuiteDialog;
import app.shared.ui.components.SuiteSuggestionTextField;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Modaler Dialog zur Auflösung eines unbekannten WhatsApp-Kontakts.
 *
 * <p>Wird aufgerufen wenn der Inkrementalimport auf einen Kontakt trifft,
 * der noch nicht in der Suite-DB bekannt ist. Der Nutzer kann entweder
 * einen bestehenden Kontakt auswählen (per Autocomplete-Vorschlagsliste)
 * oder einen neuen Namen eingeben.</p>
 *
 * <p>Die Vorschlagsmechanik selbst steckt im {@link SuiteSuggestionTextField}. Hier bleibt nur, was
 * sie für diesen Dialog bedeutet: eine Auswahl aus der Liste liefert eine contact_id und wechselt
 * den Titel, alles Selbstgetippte legt einen neuen Kontakt an.</p>
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
 * <p>Titel-Wechsel wird über SuiteDialog.setSuiteTitle() realisiert, nicht über stage.setTitle().
 * Grund: Der Dialog nutzt eine custom HeaderBar mit einem Label als Titel — stage.setTitle() setzt
 * nur den nativen Fenstertitel, der hinter der HeaderBar verborgen ist.</p>
 * Alternativen die verworfen wurden:
 * Haken im TextField-Text ("✓ Wolfgang"): setText() im textProperty-Listener erzeugte
 * Endlosschleifen und einen JavaFX-Bug (IllegalArgumentException: start > end beim Löschen).
 * Button-Text togglen ("OK" / "Übernehmen"): ButtonBar setzt feste Button-Breiten,
 * die sich nach einem setText() nicht neu berechnen — der Button blieb immer gleich breit.
 */
public class WhatsAppContactDialog {

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
    	SuiteDialog<Void> dialog = new SuiteDialog<>(TITLE_NEW);

        // Ergebnis-State. Array, weil die Lambdas unten aus einer statischen Methode heraus zugreifen.
        final Integer[] selectedContactId = {null};

        // X-Button blockieren
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(e -> e.consume());

        // Content
        VBox content = dialog.contentBox();

        Label infoLabel = new Label("Unbekannter Kontakt: " + rawIdentifier);
        Label nameLabel = new Label("Name (bestehenden auswählen oder neuen eingeben):");
        SuiteSuggestionTextField nameField = new SuiteSuggestionTextField("Name...");
        nameField.setAllItems(new ArrayList<>(knownContacts.keySet()));

        content.getChildren().addAll(infoLabel, nameLabel, nameField);
        dialog.getDialogPane().setContent(content);

        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(okBtn);

        // OK-Button solange disabled bis etwas eingegeben wurde
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);

        // Jede Texteingabe heißt erst einmal: neuer Kontakt. Kam sie aus der Vorschlagsliste, meldet
        // sich gleich danach setOnSelected und nimmt es zurück — der Callback kommt nach dem Text.
        nameField.textProperty().addListener((_, _, newVal) -> {
            selectedContactId[0] = null;
            dialog.setSuiteTitle(TITLE_NEW);
            okButton.setDisable(newVal == null || newVal.isBlank());
        });

        nameField.setOnSelected(name -> {
            selectedContactId[0] = knownContacts.get(name);
            dialog.setSuiteTitle(TITLE_SELECTED);
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