package app.data.persistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import app.config.Config;
import app.util.Log;

/**
 * Verantwortlich für die Persistierung von Fitbit-Daten.
 * 
 * <h3>Datenbank:</h3>
 * Tabelle: fitbit (date DATE PRIMARY KEY, ppints INTEGER NOT NULL)
 * Speichert die berechneten Tagespunkte für schnellen Zugriff in Statistiken.
 * 
 * <h3>Log-Datei:</h3>
 * Datei: log/fitbit_import.log
 * Speichert die kompletten Fitbit-API-Antworten für manuelle Kontrolle.
 * Ein JSON-Objekt pro Zeile, das Datum ist im JSON enthalten.
 */
public class FitbitRepository {
    
    private static final String LOG_FILE_PATH = Config.get("logFolder") + "fitbit_import.log";
    
    /**
     * Lädt das Datum des letzten importierten Tages aus der Datenbank.
     * 
     * @return Das letzte importierte Datum, oder Optional.empty() wenn noch keine Daten vorhanden
     */
    public Optional<LocalDate> getLastImportedDate() {
        String sql = "SELECT MAX(date) as last_date FROM fitbit";
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
            	String dateString = rs.getString("last_date");
            	if (dateString != null) {
            	    LocalDate lastDate = LocalDate.parse(dateString);
                    Log.debug(this, "Letztes importiertes Fitbit-Datum: " + lastDate);
                    return Optional.of(lastDate);
                }
            }
            
            Log.debug(this, "Keine Fitbit-Daten in DB gefunden");
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden des letzten Fitbit-Datums", e);
        }
    }
    
    /**
     * Speichert die Tagespunkte für ein bestimmtes Datum in der Datenbank.
     * Falls bereits ein Eintrag für dieses Datum existiert, wird er überschrieben (REPLACE).
     * 
     * @param date Das Datum
     * @param points Die berechneten Punkte
     */
    public void saveDayPoints(LocalDate date, int points) {
        String sql = "REPLACE INTO fitbit (date, points) VALUES (?, ?)";
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
        	stmt.setString(1, date.toString());
            stmt.setInt(2, points);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                Log.info(this, "Fitbit-Daten gespeichert: " + date + " → " + points + " Punkte");
            } else {
                throw new RuntimeException("Fehler beim Speichern der Fitbit-Daten für " + date);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern der Fitbit-Daten für " + date, e);
        }
    }
    
    /**
     * Schreibt die komplette API-Antwort für einen Tag in die Log-Datei.
     * Jeder Eintrag ist ein JSON-Objekt in einer eigenen Zeile.
     * Das Datum ist im JSON enthalten, daher kein separater Timestamp nötig.
     * 
     * @param date Das Datum (wird im Log nicht extra geschrieben, ist im JSON enthalten)
     * @param jsonResponse Die komplette JSON-Antwort von Fitbit-API
     */
    public void logApiResponse(LocalDate date, String jsonResponse) {
        File logFile = new File(LOG_FILE_PATH);
        
        // Log-Verzeichnis erstellen falls nicht vorhanden
        File logDir = logFile.getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(jsonResponse);
            writer.newLine();
            Log.debug(this, "Fitbit-API-Response geloggt für " + date);
            
        } catch (IOException e) {
            // Logging-Fehler sind nicht kritisch - wir werfen hier keine Exception
            // Die Daten sind ja bereits in der DB gespeichert
            Log.error(this, "Fehler beim Schreiben der Fitbit-Log-Datei für " + date, e);
        }
    }
}