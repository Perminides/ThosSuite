package app.fitbit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.data.persistence.FitbitRepository;
import app.fitbit.json.ActivityDaySummary;
import app.fitbit.json.ActivityLogList;
import app.ui.components.FitbitActivityEditor;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.stage.Window;

/**
 * Orchestriert den kompletten Fitbit-Import-Workflow beim Start der Suite.
 * 
 * <h3>Workflow:</h3>
 * <ol>
 *   <li>Ermittelt fehlende Tage seit letztem Import (bis gestern)</li>
 *   <li>Für jeden fehlenden Tag:
 *     <ul>
 *       <li>API-Daten abholen (via FitbitImporter)</li>
 *       <li>Editierbaren Dialog zeigen zur manuellen Verifikation</li>
 *       <li>Punkte berechnen aus (korrigierten) Activities</li>
 *       <li>In Datenbank speichern + API-Response loggen</li>
 *     </ul>
 *   </li>
 *   <li>Abschluss-Popup mit Zusammenfassung (Datum → Punkte)</li>
 * </ol>
 * 
 * Bei Fehler oder Abbruch: RuntimeException (Fail-Fast)
 */
public class FitbitUpdateService {
    
    private final FitbitRepository repository;
    private FitbitImporter importer;
    
    public FitbitUpdateService() {
        this.repository = new FitbitRepository();
        // importer wird erst bei Bedarf erstellt
    }
    
    /**
     * Prüft ob Fitbit-Daten fehlen und importiert diese.
     * Blockiert bis Import abgeschlossen oder abgebrochen wurde.
     * 
     * @param parentWindow Parent-Window für Dialoge (kann null sein)
     */
    public void checkAndUpdate(Window parentWindow) {
        // 1. Letztes importiertes Datum ermitteln
        Optional<LocalDate> lastDateOpt = repository.getLastImportedDate();
        
        if (lastDateOpt.isEmpty()) {
            // Keine Daten vorhanden - Fail-Fast
            throw new RuntimeException(
                "Kein Fitbit-Import-History gefunden. " +
                "Bitte manuell das erste Datum in die Datenbank eintragen."
            );
        }
        
        LocalDate startDate = lastDateOpt.get().plusDays(1);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // 2. Prüfen ob Import nötig
        if (startDate.isAfter(yesterday)) {
            Log.debug(this, "Kein Fitbit-Import nötig. Letzter Import: " + lastDateOpt.get());
            return;
        }
        
        // 2.5 Importer erstellen, der holt sich schon mal frische Credentials, wenn er sie braucht...
        this.importer = new FitbitImporter();
        
        // 3. Fehlende Tage sammeln
        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(yesterday)) {
            missingDates.add(current);
            current = current.plusDays(1);
        }
        
        Log.info(this, "Fitbit-Import für " + missingDates.size() + " Tag(e): " + missingDates);
        
        // 4. Für jeden fehlenden Tag: Import durchführen
        List<DayImportResult> results = new ArrayList<>();
        
        for (LocalDate date : missingDates) {
            DayImportResult result = importDay(date, parentWindow);
            
            if (result == null) {
                // User hat abgebrochen
                Log.info(this, "Fitbit-Import abgebrochen bei Tag: " + date);
                break;
            }
            
            results.add(result);
        }
        
        // 5. Abschluss-Popup mit Zusammenfassung
        if (!results.isEmpty()) {
            showSummary(parentWindow, results);
        }
    }
    
    /**
     * Importiert Fitbit-Daten für einen bestimmten Tag.
     * 
     * @param date Das zu importierende Datum
     * @param parentWindow Parent-Window für Dialoge
     * @return Import-Ergebnis, oder null wenn User abgebrochen hat
     */
    private DayImportResult importDay(LocalDate date, Window parentWindow) {
        Log.info(this, "Importiere Fitbit-Daten für: " + date);
        
        // 1. API-Daten abholen (mit originalen JSON-Strings)
        FitbitImporter.ApiResponse<ActivityDaySummary> daySummaryResponse = importer.getActivityDaySummary(date);
        FitbitImporter.ApiResponse<ActivityLogList> activityLogResponse = importer.getActivitiesLogList(date);
        
        ActivityDaySummary daySummary = daySummaryResponse.data();
        ActivityLogList activityLogList = activityLogResponse.data();
        
        // 2. Editierbaren Dialog zeigen
        FitbitActivityEditor editor = new FitbitActivityEditor(
            parentWindow, 
            date, 
            activityLogList.getActivities(),
            daySummary
        );
        
        Optional<FitbitActivityEditor.EditorResult> editorResultOpt = editor.showAndWait();
        
        if (editorResultOpt.isEmpty()) {
            // User hat abgebrochen
            return null;
        }
        
        FitbitActivityEditor.EditorResult editorResult = editorResultOpt.get();
        
        // 3. ActivityLogList mit editierten Activities aktualisieren
        ActivityLogList correctedActivityLogList = new ActivityLogList();
        correctedActivityLogList.setActivities(editorResult.activities());
        
        // 4. DaySummary mit korrigierten Gesamtschritten aktualisieren
        daySummary.getSummary().setSteps(editorResult.totalSteps());
        
        // 5. Punkte berechnen
        int points = PointsCalculator.getDayPoints(correctedActivityLogList, daySummary);
        
        Log.info(this, date + " → " + points + " Punkte");
        
        // 6. In Datenbank speichern
        repository.saveDayPoints(date, points);
        
        // 7. VERWENDETE Daten loggen (nach User-Editing, nicht Original-API)
        // So kann man später nachvollziehen wie die Punkte berechnet wurden
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            // Sicherstellen dass KEIN Pretty-Print (keine Zeilenumbrüche)
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);
            
            // Daten-Objekt für Log
            java.util.Map<String, Object> logData = java.util.Map.of(
                "date", date,
                "totalSteps", editorResult.totalSteps(),
                "activities", editorResult.activities()
            );
            
            String usedDataLog = mapper.writeValueAsString(logData);
            
            repository.logApiResponse(date, usedDataLog);
            
        } catch (Exception e) {
            Log.error(this, "Fehler beim Loggen der verwendeten Daten", e);
            throw new RuntimeException(e);
        }
        
        return new DayImportResult(date, points);
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
            parentWindow,
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