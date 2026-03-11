package app.data.persistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.config.Config;
import app.fitbit.FitbitGoalHistoryEntry;
import app.fitbit.FitbitWeekData;
import app.util.Log;

/**
 * Verantwortlich für die Persistierung von Fitbit-Daten.
 * 
 * <h3>Datenbank:</h3>
 * Tabelle: fitbit (date DATE PRIMARY KEY, points INTEGER NOT NULL, remark TEXT)
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
     * Holt das Wochenziel, das zum gegebenen Datum gültig war.
     * Sucht den neuesten Eintrag mit valid_from <= date.
     */
    public int getWeeklyGoalForDate(LocalDate date) {
        String sql = """
            SELECT weekly_goal 
            FROM fitbit_goal_history 
            WHERE valid_from <= ?
            ORDER BY valid_from DESC
            LIMIT 1
            """;
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, date.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("weekly_goal");
            } else {
                throw new RuntimeException("Kein Fitbit-Wochenziel gefunden für " + date);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden des Fitbit-Wochenziels", e);
        }
    }
    
    /**
     * Holt die Fitbit-Punkte für die Woche, die das gegebene Datum enthält.
     * 
     * @param date Ein beliebiges Datum innerhalb der gewünschten Woche
     * @return Die Gesamtpunkte dieser Woche, oder 0 wenn keine Daten vorhanden
     */
    public int getPointsForWeek(LocalDate date) {
        // Montag der Woche berechnen (für week_start Vergleich)
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        String sql = """
            SELECT points 
            FROM fitbit_weekly_points 
            WHERE week_start = ?
            """;
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, monday.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("points");
            } else {
                return 0; // Keine Daten für diese Woche
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Fitbit-Wochenpunkte", e);
        }
    }
    
    /**
     * Lädt alle Wochen im angegebenen Zeitraum mit ihren Punkten aus der Datenbank.
     * 
     * @param from Start-Datum (sollte ein Montag sein, wird aber nicht geprüft)
     * @param to End-Datum (sollte ein Sonntag sein, wird aber nicht geprüft)
     * @return Liste der Wochen, aufsteigend sortiert (älteste zuerst)
     */
    public List<FitbitWeekData> getWeeksInRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT week_start, points, remark 
            FROM fitbit_weekly_points 
            WHERE week_start >= ? AND week_start <= ?
            ORDER BY week_start ASC
            """;
        
        List<FitbitWeekData> result = new ArrayList<>();
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, from.toString());
            stmt.setString(2, to.toString());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate weekStart = LocalDate.parse(rs.getString("week_start"));
                int points = rs.getInt("points");
                String remark = rs.getString("remark");
                result.add(new FitbitWeekData(weekStart, points, remark));
            }
            
            Log.debug(this, "Geladene Wochen im Zeitraum " + from + " bis " + to + ": " + result.size());
            return result;
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Fitbit-Wochendaten", e);
        }
    }
    
    /**
     * Lädt die komplette Goal-Historie aus der Datenbank.
     * 
     * @return Liste aller Goal-Einträge, aufsteigend nach validFrom sortiert
     */
    public List<FitbitGoalHistoryEntry> getAllGoalHistory() {
        String sql = """
            SELECT valid_from, weekly_goal 
            FROM fitbit_goal_history 
            ORDER BY valid_from ASC
            """;
        
        List<FitbitGoalHistoryEntry> result = new ArrayList<>();
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                LocalDate validFrom = LocalDate.parse(rs.getString("valid_from"));
                int weeklyGoal = rs.getInt("weekly_goal");
                result.add(new FitbitGoalHistoryEntry(validFrom, weeklyGoal));
            }
            
            Log.debug(this, "Geladene Goal-Einträge: " + result.size());
            return result;
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Fitbit-Goal-Historie", e);
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