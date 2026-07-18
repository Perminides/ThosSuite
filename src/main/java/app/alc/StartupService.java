package app.alc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import app.alc.model.Status;
import app.alc.repository.AlcRepository;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.skin.SkinService;

public class StartupService {
    
    private final AlcRepository repository;
    
    public StartupService() {
        this.repository = new AlcRepository();
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
        // Wochentag + formatiertes Datum
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String message = "Wie war " + dayOfWeek + " der " + formattedDate + "?";
        
        // Alert erstellen
        ButtonEnum result = SkinService.get().showAlert(
            "Alkohol-Tracker",
            message,
            ButtonEnum.GREEN, ButtonEnum.YELLOW, ButtonEnum.RED, ButtonEnum.CANCEL
        );
        
        if (result == ButtonEnum.CANCEL) {
            Log.info(this, "Alkohol-Eingabe abgebrochen");
            return false;
        }
        
        // Status ermitteln
        Status status;
        if (result == ButtonEnum.GREEN) {
            status = Status.GREEN;
        } else if (result == ButtonEnum.YELLOW) {
            status = Status.YELLOW;
        } else {
            status = Status.RED;
        }
        
        // Speichern
        repository.saveDayStatus(date, status);
        Log.info(this, "Alkohol-Status gespeichert: " + date + " -> " + status);
        return true; // Weiter!
    }
}