package app.messaging.whatsapp;

import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Modaler Dialog zur Auflösung eines unbekannten WhatsApp-Chats.
 *
 * <p>Wird aufgerufen wenn der Inkrementalimport auf einen Chat trifft,
 * der noch nicht in der Suite-DB bekannt ist. Der Nutzer entscheidet
 * ob der Chat importiert oder ignoriert (blacklisted) werden soll,
 * und kann einen Anzeigenamen vergeben.</p>
 *
 * <p>Das Schließen per X ist blockiert — der Import kann ohne Entscheidung
 * nicht fortgesetzt werden.</p>
 */
public class WhatsAppChatDialog {

    /**
     * Ergebnis des Dialogs.
     *
     * @param doImport    true wenn der Chat importiert werden soll, false wenn blacklisted
     * @param displayName Anzeigename des Chats (nie null, nie leer)
     */
    public record Result(boolean doImport, String displayName) {}

    /**
     * Zeigt den Dialog und wartet auf Eingabe des Nutzers.
     *
     * @param rawIdentifier JID des Chats (z.B. "491234567890@s.whatsapp.net")
     * @param subject       Gruppenname oder null bei Einzelchats
     * @param isGroup       true wenn es sich um einen Gruppenchat handelt
     * @param formattedTs   Zeitstempel der ersten Nachricht in diesem Chat (yyyy-MM-dd HH:mm:ss)
     * @return Entscheidung des Nutzers
     * @throws IllegalStateException [FAILFAST] wenn der Dialog ohne Entscheidung geschlossen wird
     */
    public static Result show(String rawIdentifier, String subject, boolean isGroup, String formattedTs) {
        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), "Unbekannter WhatsApp-Chat");

        // X-Button blockieren
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(e -> {
            e.consume();
        });

        // Content
        VBox content = SkinService.get().createDialogContent();

        Label infoLabel = new Label(
            "Zeitpunkt:  " + formattedTs + "\n" +
            "Identifier: " + rawIdentifier + "\n" +
            "Name:       " + (subject != null ? subject : "(kein Name)") + "\n" +
            "Typ:        " + (isGroup ? "Gruppe" : "Einzelchat"));
        infoLabel.setWrapText(true);

        String defaultName = (subject != null && !subject.isBlank()) ? subject : "Name des Chats";
        TextField nameField = new TextField(defaultName);
        dialog.setOnShown(_ -> {
            Platform.runLater(() -> {
            	nameField.selectAll();
                nameField.requestFocus();
            });
        });

        content.getChildren().addAll(
            new Label("Unbekannter Chat gefunden:"),
            infoLabel,
            new Label("Anzeigename:"),
            nameField);

        dialog.getDialogPane().setContent(content);

        ButtonType importBtn  = new ButtonType("Importieren",  ButtonBar.ButtonData.YES);
        ButtonType ignoreBtn  = new ButtonType("Ignorieren",   ButtonBar.ButtonData.NO);
        dialog.getDialogPane().getButtonTypes().setAll(importBtn, ignoreBtn);
        dialog.getDialogPane().lookupButton(importBtn).disableProperty().bind(
        	    Bindings.createBooleanBinding(
        	        () -> nameField.getText().isBlank() || nameField.getText().equals(defaultName),
        	        nameField.textProperty()
        	    )
        	);

        dialog.showAndWait();

        // Ergebnis auswerten — ButtonType.CANCEL_CLOSE bedeutet X wurde gedrückt,
        // aber das haben wir oben bereits blockiert
        ButtonType chosen = ((Dialog<ButtonType>) dialog).getResult();
        if (chosen == null)
            throw new IllegalStateException(
                "[FAILFAST] WhatsApp-Import abgebrochen: Kein Ergebnis aus Chat-Dialog. " +
                "rawIdentifier=" + rawIdentifier);

        boolean doImport  = chosen == importBtn;
        String displayName = nameField.getText().isBlank() ? defaultName : nameField.getText().trim();
        return new Result(doImport, displayName);
    }
}