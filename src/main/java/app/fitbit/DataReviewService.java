package app.fitbit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.fitbit.DataFetcher.DayData;
import app.fitbit.model.json.ActivityLogList;
import app.fitbit.repository.Repository;
import app.shared.Log;
import app.ui.skin.SkinService;
import javafx.stage.Window;

/**
 * Zeigt Fitbit-Dialoge und speichert die editierten Daten.
 * Bekommt bereits abgeholte Daten von FitbitDataFetcher.
 */
public class DataReviewService {
    
    private final Repository repository;
    private final DataFetcher dataFetcher;
    
    public DataReviewService(DataFetcher dataFetcher) {
        this.repository = new Repository();
        this.dataFetcher = dataFetcher;
    }
    
    /**
     * Zeigt für jeden abgeholten Tag einen Dialog.
     * User kann editieren, dann wird gespeichert.
     */
    public void showDialogsAndSave(Window parentWindow) {
        List<DayImportResult> results = new ArrayList<>();
        
        for (DayData dayData : dataFetcher.getFetchedDays()) {
            DayImportResult result = showDialogAndSave(dayData);
            
            if (result == null) {
                // User hat abgebrochen
                Log.info(this, "Fitbit-Import abgebrochen bei Tag: " + dayData.date());
                break;
            }
            
            results.add(result);
        }
        
        // Abschluss-Popup mit Zusammenfassung
        if (!results.isEmpty()) {
            showSummary(parentWindow, results);
        }
    }
    
    /**
     * Zeigt Dialog für einen Tag, speichert bei OK.
     * 
     * @return Import-Ergebnis, oder null wenn User abgebrochen hat
     */
    private DayImportResult showDialogAndSave(DayData dayData) {
        Log.info(this, "Zeige Dialog für: " + dayData.date());
        
        // Dialog zeigen
        ActivityTableDialog dialog = new ActivityTableDialog(
            dayData.date(),
            dayData.activityLogList().getActivities(),
            dayData.daySummary()
        );
        
        Optional<ActivityTableDialog.DialogResult> resultOpt = dialog.showAndWait();
        
        if (resultOpt.isEmpty()) {
            // User hat abgebrochen
            return null;
        }
        
        ActivityTableDialog.DialogResult dialogResult = resultOpt.get();
        
        // ActivityLogList mit editierten Activities aktualisieren
        ActivityLogList correctedActivityLogList = new ActivityLogList();
        correctedActivityLogList.setActivities(dialogResult.activities());
        
        // DaySummary mit korrigierten Gesamtschritten aktualisieren
        dayData.daySummary().getSummary().setSteps(dialogResult.totalSteps());
        
        // Punkte berechnen
        int points = PointsCalculator.getDayPoints(correctedActivityLogList, dayData.daySummary());
        
        Log.info(this, dayData.date() + " → " + points + " Punkte");
        
        // In Datenbank speichern
        repository.saveDayPoints(dayData.date(), points);
        
        // VERWENDETE Daten loggen
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);
            
            java.util.Map<String, Object> logData = new java.util.LinkedHashMap<>();
            logData.put("date", dayData.date());
            logData.put("totalSteps", dialogResult.totalSteps());
            logData.put("activities", dialogResult.activities());
            
            String usedDataLog = mapper.writeValueAsString(logData);
            repository.logApiResponse(dayData.date(), usedDataLog);
            
        } catch (Exception e) {
            Log.error(this, "Fehler beim Loggen der verwendeten Daten", e);
            throw new RuntimeException(e);
        }
        
        return new DayImportResult(dayData.date(), points);
    }
    
    /**
     * Zeigt Zusammenfassungs-Popup mit importierten Tagen und Punkten.
     */
    private void showSummary(Window parentWindow, List<DayImportResult> results) {
        StringBuilder message = new StringBuilder("Fitbit-Import abgeschlossen:\n\n");
        
        for (DayImportResult result : results) {
            message.append(result.date()).append(" → ").append(result.points()).append(" Punkte\n");
        }
        
        SkinService.get().createAlert(
            null,
            "Fitbit-Import",
            message.toString(),
            false,
            false
        ).showAndWait();
    }
    
    /**
     * Ergebnis eines Tag-Imports.
     */
    private record DayImportResult(LocalDate date, int points) {}
}