package app.alc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import app.data.persistence.AlcoholRepository;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class AlcoholStartupService {
    
    private final AlcoholRepository repository;
    
    public AlcoholStartupService() {
        this.repository = new AlcoholRepository();
    }
    
    /**
     * Prüft ob Eintrag für heute fehlt und zeigt ggf. Dialog.
     * Wird in Controller.runPostTasks() aufgerufen.
     */
    public void checkAndPrompt() {
    	LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Letzten Eintrag finden
        LocalDate lastEntry = repository.getLastEntryDate();
        
        if (lastEntry == null) {
            showInputDialog(yesterday);
            return;
        }
        
        LocalDate current = lastEntry.plusDays(1);
        while (!current.isAfter(yesterday)) {
            boolean continued = showInputDialog(current); // BOOLEAN zurück!
            if (!continued) {
                Log.info(this, "Alkohol-Eingabe abgebrochen bei " + current);
                return; // STOP!
            }
            current = current.plusDays(1);
        }
    }


    
    /**
     * !Später: Das Padding ist einigermaßen irre. Buttons nicht bündig mit dem Text. Überall andere Abstände.
     */
    private boolean showInputDialog(LocalDate date) {
        // Buttons erstellen
    	ButtonType btnGreen = new ButtonType("Grün", ButtonBar.ButtonData.OTHER);
    	ButtonType btnYellow = new ButtonType("Gelb", ButtonBar.ButtonData.OTHER);
    	ButtonType btnRed = new ButtonType("Rot", ButtonBar.ButtonData.OTHER);
        ButtonType btnCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        // Wochentag + formatiertes Datum
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String message = "Wie war " + dayOfWeek + " der " + formattedDate + "?";
        
        // Alert erstellen
        Alert alert = SkinService.get().createAlert(
            null,
            "Alkohol-Tracker",
            message,
            btnGreen, btnYellow, btnRed, btnCancel
        );
        
        // Ergebnis verarbeiten
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isEmpty() || result.get() == btnCancel) {
            Log.info(this, "Alkohol-Eingabe abgebrochen");
            return false;
        }
        
        // Status ermitteln
        AlcoholStatus status;
        if (result.get() == btnGreen) {
            status = AlcoholStatus.GREEN;
        } else if (result.get() == btnYellow) {
            status = AlcoholStatus.YELLOW;
        } else {
            status = AlcoholStatus.RED;
        }
        
        // Speichern
        repository.saveDayStatus(date, status);
        Log.info(this, "Alkohol-Status gespeichert: " + date + " -> " + status);
        return true; // Weiter!
    }
}