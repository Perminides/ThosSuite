package app.scripts.fitbit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.fitbit.PointsCalculator;
import app.fitbit.model.json.ActivityDaySummary;
import app.fitbit.model.json.ActivityLogList;
import app.fitbit.repository.Repository;
import app.shared.Config;

/**
 * EINMALIGE MIGRATION von alten Fitbit-Log-Daten in die neue Datenbank.
 * 
 * Liest fitbit_import.log und:
 * - Parst beide Log-Formate (alt ohne points, neu mit points)
 * - Berechnet fehlende Punkte via PointsCalculator
 * - Schreibt alle Tage in die DB
 * 
 * NACH DER MIGRATION KANN DIESE KLASSE GELÖSCHT WERDEN!
 */
public class FitbitLogMigrator {
    
    // !!! PFADE ANPASSEN VOR DEM AUSFÜHREN !!!
    private static final String DATA_FOLDER = "C:/Users/Markgraf/OneDrive/ThosSuite/";
    private static final String LOG_FILE_PATH = "C:/Users/Markgraf/OneDrive/Geographie Suite/Spielstand/fitbitStats.csv";
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Fitbit Log Migration ===");
        
        // Config initialisieren (damit DB.getConnection() funktioniert)
        Config.init(DATA_FOLDER);
        
        System.out.println("Lese: " + LOG_FILE_PATH);
        
        Repository repository = new Repository();
        int migratedCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                LogEntry entry = parseLogEntry(line);
                
                // In DB speichern
                repository.saveDayPoints(entry.date, entry.points);
                migratedCount++;
                
                if (migratedCount % 100 == 0) {
                    System.out.println("Fortschritt: " + migratedCount + " Tage migriert...");
                }
            }
            
        } catch (Exception e) {
            System.err.println("FEHLER bei der Migration!");
            e.printStackTrace();
            throw new RuntimeException("Migration fehlgeschlagen", e);
        }
        
        System.out.println("\n=== Migration abgeschlossen ===");
        System.out.println("Migriert: " + migratedCount + " Tage");
    }
    
    /**
     * Parst eine Log-Zeile und erkennt automatisch das Format.
     * 
     * Altes Format (vor 24.07.2023): Ohne "points" im summary
     * Neues Format (ab 24.07.2023): Mit "points" im summary
     * 
     * @param line Eine Zeile aus dem Log
     * @return LogEntry mit Datum und berechneten Punkten
     */
    private static LogEntry parseLogEntry(String line) throws Exception {
        JsonNode root = MAPPER.readTree(line);
        
        // Datum extrahieren (ist immer vorhanden)
        LocalDate date = LocalDate.parse(root.get("date").asText());
        
        // DaySummary parsen
        JsonNode daySummaryNode = root.get("daySummary");
        ActivityDaySummary daySummary = MAPPER.treeToValue(daySummaryNode, ActivityDaySummary.class);
        
        // ActivityLogList parsen (alter Key: "activityLogList", neuer Key würde "activityLog" sein)
        JsonNode activityLogNode = root.has("activityLogList") 
            ? root.get("activityLogList") 
            : root.get("activityLog");
        
        if (activityLogNode == null) {
            throw new Exception("Weder 'activityLogList' noch 'activityLog' gefunden");
        }
        
        ActivityLogList activityLogList = MAPPER.treeToValue(activityLogNode, ActivityLogList.class);
        
        // Punkte ermitteln
        int points;
        
        if (daySummary.getSummary().getPoints() != null) {
            // Neues Format: Punkte sind bereits vorhanden
            points = daySummary.getSummary().getPoints();
        } else {
            // Altes Format: Punkte müssen berechnet werden
            points = PointsCalculator.getDayPoints(activityLogList, daySummary);
        }
        
        return new LogEntry(date, points);
    }
    
    /**
     * Internes Record für geparste Log-Einträge.
     */
    private record LogEntry(LocalDate date, int points) {}
}