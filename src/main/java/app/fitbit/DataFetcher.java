package app.fitbit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.fitbit.model.json.ActivityDaySummary;
import app.fitbit.model.json.ActivityLogList;
import app.fitbit.repository.Repository;
import app.shared.Log;

/**
 * Holt Fitbit-Daten synchron im Hintergrund (während Splash sichtbar).
 * Sammelt Ergebnisse oder Fehler für spätere Verarbeitung im UI-Thread.
 */
public class DataFetcher {
    
    private final Repository repository;
    private ApiClient importer;
    private List<DayData> fetchedDays;
    private Exception fetchError;
    
    public DataFetcher() {
        this.repository = new Repository();
        this.fetchedDays = new ArrayList<>();
    }
    
    /**
     * Holt alle fehlenden Fitbit-Daten synchron.
     * Blockiert den aufrufenden Thread (Splash bleibt sichtbar).
     * Sammelt Fehler statt zu werfen.
     */
    public void fetch() {
        try {
            // 1. Letztes importiertes Datum ermitteln
            Optional<LocalDate> lastDateOpt = repository.getLastImportedDate();
            
            if (lastDateOpt.isEmpty()) {
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
            
            // 3. Importer erstellen (lädt Credentials, refresht Token)
            this.importer = new ApiClient();
            
            // 4. Fehlende Tage sammeln
            List<LocalDate> missingDates = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(yesterday)) {
                missingDates.add(current);
                current = current.plusDays(1);
            }
            
            Log.info(this, "Fitbit-Import für " + missingDates.size() + " Tag(e): " + missingDates);
            
            // 5. Für jeden fehlenden Tag: API-Daten abholen
            for (LocalDate date : missingDates) {
                DayData dayData = fetchDay(date);
                fetchedDays.add(dayData);
            }
            
        } catch (Exception e) {
            Log.error(this, "Fehler beim Fitbit-Daten-Abruf", e);
            this.fetchError = e;
        }
    }
    
    /**
     * Holt API-Daten für einen einzelnen Tag.
     */
    private DayData fetchDay(LocalDate date) {
        Log.info(this, "Hole Fitbit-Daten für: " + date);
        
        ApiClient.ApiResponse<ActivityDaySummary> daySummaryResponse = 
            importer.getActivityDaySummary(date);
        ApiClient.ApiResponse<ActivityLogList> activityLogResponse = 
            importer.getActivitiesLogList(date);
        
        return new DayData(
            date,
            activityLogResponse.data(),
            daySummaryResponse.data()
        );
    }
    
    public boolean hasData() {
        return !fetchedDays.isEmpty();
    }
    
    public boolean hasError() {
        return fetchError != null;
    }
    
    public Exception getError() {
        return fetchError;
    }
    
    public List<DayData> getFetchedDays() {
        return fetchedDays;
    }
    
    /**
     * !tmp: Schlanke, öffentliche Projektion der geholten Rohwerte pro Tag für den
     * Health-Vergleicher (Paket app.tmp). Liefert die ROHEN Schritte (vor der
     * Dialog-Korrektur, da im PreTask aufgerufen) und die Aktivitätsliste des Tages.
     * Fällt mit dem Vergleicher im September weg.
     */
    public List<FitbitDayProjection> getProjection() {
        List<FitbitDayProjection> projection = new ArrayList<>();
        for (DayData day : fetchedDays) {
            projection.add(new FitbitDayProjection(
                day.date(),
                day.daySummary().getSummary().getSteps(),
                day.activityLogList().getActivities()));
        }
        return projection;
    }
    
    /**
     * Container für die abgeholten Daten eines Tages.
     */
    record DayData(
        LocalDate date,
        ActivityLogList activityLogList,
        ActivityDaySummary daySummary
    ) {}
}